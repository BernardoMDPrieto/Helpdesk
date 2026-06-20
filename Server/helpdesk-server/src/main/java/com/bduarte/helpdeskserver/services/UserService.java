package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.UserSpecification;
import com.bduarte.helpdeskserver.api.requests.CreateUserDTO;
import com.bduarte.helpdeskserver.api.filters.UserFilter;
import com.bduarte.helpdeskserver.api.requests.UpdateUserDTO;
import com.bduarte.helpdeskserver.api.responses.UserResponse;
import com.bduarte.helpdeskserver.models.AvailableTokens;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.models.UserRegisteredEvent;
import com.bduarte.helpdeskserver.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.Builder;
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
    private final ApplicationEventPublisher publisher;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AvailableTokensService availableTokensService;
    private final BCryptPasswordEncoder passwordEncoder;


    @Transactional
    public void CreateNewUser(CreateUserDTO userDTO) throws MessagingException {
        Optional<User> existingUser = userRepository.findByEmail(userDTO.getEmail());

        if (existingUser.isPresent()) {
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

        String token = availableTokensService.newUserToken(savedUser.getId());
        registerUser(savedUser.getEmail(), savedUser.getUserName(), token);

    }

    public void registerNewUserPassword(String password, String token) {
        Optional<AvailableTokens> availableToken = availableTokensService.validateToken(token);

        if (availableToken.isEmpty()) {
            return;
        }

        Optional<User> user = userRepository.findById(availableToken.get().getUserId());

        if (user.isEmpty()) {
            return;
        }

        user.get().setPassword(passwordEncoder.encode(password));
        userRepository.save(user.get());

        availableToken.get().setAvailable(false);
        availableTokensService.saveToken(availableToken.get());

    }

    private void registerUser(String email, String userName, String token) {
        publisher.publishEvent(new UserRegisteredEvent(email, userName, token));
    }

    public Page<UserResponse> getUsers(UserFilter userFilter, Pageable pageable) {
        try {
            Specification<User> spec = UserSpecification.bySpecification(userFilter);

            Page<User> users = userRepository.findAll(spec, pageable);
            List<UserResponse> userResponseList = users.getContent().stream().map(this::converUserResponse).toList();

            return new PageImpl<>(userResponseList, pageable, users.getTotalElements());
        } catch (Exception exception) {
            throw exception;
        }
    }

    @Transactional
    public void updateUser(UpdateUserDTO updateUserDTO, UUID id) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Usuário não encontrado"
                ));

        if (updateUserDTO.getUserName() != null) {
            existingUser.setUserName(updateUserDTO.getUserName());
        }

        existingUser.setEnabled(updateUserDTO.getActive());

        if (updateUserDTO.getRole() != null) {
            existingUser.setRole(updateUserDTO.getRole());
        }

        userRepository.save(existingUser);
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
