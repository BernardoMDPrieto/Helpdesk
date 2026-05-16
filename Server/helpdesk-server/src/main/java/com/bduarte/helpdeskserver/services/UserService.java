package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.dto.CreateUserDTO;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.text.html.Option;
import java.util.Optional;

@Service
@Builder
public class UserService {
    private final UserRepository userRepository;

    @Transactional
    public User CreateNewUser(CreateUserDTO userDTO) {
        Optional<User> existingUser = userRepository.findByEmail(userDTO.getEmail());

        if (existingUser.isPresent()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Usuário com e-mail já cadastrado"
            );
        }

        User user = new User(
                userDTO.getUsername(),
                userDTO.getEmail(),
                userDTO.getPassword()
        );
        return userRepository.save(user);
    }

}
