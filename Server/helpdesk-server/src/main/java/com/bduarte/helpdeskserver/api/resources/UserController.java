package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.UserFilter;
import com.bduarte.helpdeskserver.api.requests.CreateUserDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateUserDTO;
import com.bduarte.helpdeskserver.api.responses.UserResponse;
import com.bduarte.helpdeskserver.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;

    @PostMapping("/user")
    public ResponseEntity<Void> CreateUser(@Valid @RequestBody CreateUserDTO userDTO) {
        logger.info("Create user request received for email: {}", userDTO.getEmail());
        try {
            userService.CreateNewUser(userDTO);
            logger.info("User created successfully with email: {}", userDTO.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception ex) {
            logger.error("Error creating user with email: {}", userDTO.getEmail(), ex);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user")
    public ResponseEntity<Page<UserResponse>> getUsers(@ModelAttribute UserFilter userFilter,
                                                       @PageableDefault(page = 0, size = 10, sort = "userName", direction = Sort.Direction.ASC) Pageable pageable) {
        logger.info("Fetching users with pagination: page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());
        try {
            Page<UserResponse> users = userService.getUsers(userFilter, pageable);
            logger.debug("Users retrieved successfully, total count: {}", users.getTotalElements());
            return ResponseEntity.status(HttpStatus.OK).body(users);
        } catch (Exception ex) {
            logger.error("Error fetching users", ex);
            throw ex;
        }
    }

    @PutMapping("/user/{userId}")
    public ResponseEntity<Void> updateUser(@RequestBody UpdateUserDTO userDTO, @PathVariable UUID userId) {
        logger.info("Update user request received for user ID: {}", userId);
        try {
            userService.updateUser(userDTO, userId);
            logger.info("User updated successfully with ID: {}", userId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (Exception ex) {
            logger.error("Error updating user with ID: {}", userId, ex);
            return ResponseEntity.badRequest().build();
        }
    }
}
