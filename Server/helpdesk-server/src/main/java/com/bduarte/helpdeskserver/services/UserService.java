package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.UserSpecification;
import com.bduarte.helpdeskserver.api.requests.CreateUserDTO;
import com.bduarte.helpdeskserver.api.filters.UserFilter;
import com.bduarte.helpdeskserver.api.requests.UpdateUserDTO;
import com.bduarte.helpdeskserver.api.responses.UserResponse;
import com.bduarte.helpdeskserver.models.AvailableTokens;
import com.bduarte.helpdeskserver.models.ResetPasswordEvent;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.models.UserRegisteredEvent;
import com.bduarte.helpdeskserver.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Builder
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final ApplicationEventPublisher publisher;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AvailableTokensService availableTokensService;
    private final BCryptPasswordEncoder passwordEncoder;


    @Transactional
    public void CreateNewUser(CreateUserDTO userDTO) throws MessagingException {
        logger.info("Creating new user with email: {}", userDTO.getEmail());
        Optional<User> existingUser = userRepository.findByEmail(userDTO.getEmail());

        if (existingUser.isPresent()) {
            logger.warn("Attempted to create user with email that already exists: {}", userDTO.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Usuário com e-mail já cadastrado"
            );
        }

        User user = new User(
                userDTO.getEmail(),
                userDTO.getUsername(),
                passwordEncoder.encode(UUID.randomUUID().toString()),
                userDTO.getRole()
        );
        User savedUser = userRepository.save(user);
        logger.debug("User saved successfully with ID: {}", savedUser.getId());

        String token = availableTokensService.newUserToken(savedUser.getId());
        logger.debug("Token generated for new user: {}", savedUser.getId());
        registerUser(savedUser.getEmail(), savedUser.getUserName(), token);
        logger.info("User registration event published for email: {}", savedUser.getEmail());
    }

    public void requestResetPassword(String email) {
        logger.info("Password reset requested for email: {}", email);
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Password reset requested but user not found for email: {}", email);
                    return new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Usuário não encontrado"
                    );
                });

        String token = availableTokensService.newUserToken(existingUser.getId());
        logger.debug("Reset password token generated for user ID: {}", existingUser.getId());
        registerRequestResetPassword(existingUser.getEmail(), existingUser.getUserName(), token);
        logger.info("Password reset event published for email: {}", existingUser.getEmail());
    }

    public void registerPassword(String password, String token) {
        logger.info("Attempting to register password with token");
        Optional<AvailableTokens> availableToken = availableTokensService.validateToken(token);

        if (availableToken.isEmpty()) {
            logger.warn("Invalid or expired token provided for password registration");
            return;
        }

        Optional<User> user = userRepository.findById(availableToken.get().getUserId());

        if (user.isEmpty()) {
            logger.error("User not found for ID: {}", availableToken.get().getUserId());
            return;
        }

        user.get().setPassword(passwordEncoder.encode(password));
        userRepository.save(user.get());
        logger.info("Password updated successfully for user ID: {}", user.get().getId());

        availableToken.get().setAvailable(false);
        availableTokensService.saveToken(availableToken.get());
        logger.debug("Token invalidated for user ID: {}", user.get().getId());
    }

    private void registerRequestResetPassword(String email, String userName, String token) {
        publisher.publishEvent(new ResetPasswordEvent(email, userName, token));
    }

    private void registerUser(String email, String userName, String token) {
        publisher.publishEvent(new UserRegisteredEvent(email, userName, token));
    }

    public Page<UserResponse> getUsers(UserFilter userFilter, Pageable pageable) {
        logger.info("Fetching users with filter: {} and pageable: page={}, size={}", userFilter, pageable.getPageNumber(), pageable.getPageSize());
        try {
            Specification<User> spec = UserSpecification.bySpecification(userFilter);

            Page<User> users = userRepository.findAll(spec, pageable);
            List<UserResponse> userResponseList = users.getContent().stream().map(this::converUserResponse).toList();

            logger.debug("Retrieved {} users from database", users.getTotalElements());
            return new PageImpl<>(userResponseList, pageable, users.getTotalElements());
        } catch (Exception exception) {
            logger.error("Error fetching users", exception);
            throw exception;
        }
    }

    @Transactional
    public void updateUser(UpdateUserDTO updateUserDTO, UUID id) {
        logger.info("Updating user with ID: {}", id);
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Update attempted but user not found with ID: {}", id);
                    return new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Usuário não encontrado"
                    );
                });

        if (updateUserDTO.getUserName() != null) {
            existingUser.setUserName(updateUserDTO.getUserName());
            logger.debug("Updated username for user ID: {}", id);
        }

        existingUser.setEnabled(updateUserDTO.getActive());
        logger.debug("Updated active status to: {} for user ID: {}", updateUserDTO.getActive(), id);

        if (updateUserDTO.getRole() != null) {
            existingUser.setRole(updateUserDTO.getRole());
            logger.debug("Updated role to: {} for user ID: {}", updateUserDTO.getRole().getName(), id);
        }

        userRepository.save(existingUser);
        logger.info("User ID: {} updated successfully", id);
    }


    private UserResponse converUserResponse(User user) {
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUserName(),
                user.getEnabled(),
                user.getRole());
        return userResponse;
    }
}
