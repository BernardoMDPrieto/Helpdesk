package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.api.filters.UserSpecification;
import com.bduarte.helpdeskserver.api.requests.CreateUserDTO;
import com.bduarte.helpdeskserver.api.filters.UserFilter;
import com.bduarte.helpdeskserver.api.responses.UserResponse;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.models.UserRegisteredEvent;
import com.bduarte.helpdeskserver.repositories.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.Builder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

        String token = availableTokensService.NewUserToken(savedUser.getId());
        registerUser(savedUser.getEmail(), savedUser.getUserName(), token);

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


    private UserResponse converUserResponse(User user) {
        UserResponse userResponse = new UserResponse(
                user.getEmail(),
                user.getUserName(),
                user.getEnabled(),
                user.getRole());
        return userResponse;
    }
}
