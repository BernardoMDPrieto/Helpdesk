package com.bduarte.helpdeskserver.controllers;

import com.bduarte.helpdeskserver.dto.CreateUserDTO;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/Account")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/")
    public ResponseEntity<String> getRoute() {
        return ResponseEntity.status(HttpStatus.OK).body("Route OK");
    }

    @PostMapping()
    public ResponseEntity<Void> CreateUser(@Valid @RequestBody CreateUserDTO userDTO) {
        try {
            User response = userService.CreateNewUser(userDTO);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception ex) {
            throw new RuntimeException("Falha na criação de usuário: " + ex.getMessage());
        }
    }
}
