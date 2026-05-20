package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.UserFilter;
import com.bduarte.helpdeskserver.api.requests.CreateUserDTO;
import com.bduarte.helpdeskserver.api.responses.UserResponse;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/user")
    public ResponseEntity<Void> CreateUser(@Valid @RequestBody CreateUserDTO userDTO) {
        try {
            User response = userService.CreateNewUser(userDTO);

            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (Exception ex) {
            throw new RuntimeException("Falha na criação de usuário: " + ex.getMessage());
        }
    }

    @GetMapping("/user")
    public ResponseEntity<Page<UserResponse>> getUsers(@ModelAttribute UserFilter userFilter,
                                                       @PageableDefault(page = 0, size = 10, sort = "userName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<UserResponse> users = userService.getUsers(userFilter, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(users);
    }
}
