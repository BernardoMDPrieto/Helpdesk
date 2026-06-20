package com.bduarte.helpdeskserver.api.responses;

import com.bduarte.helpdeskserver.models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class UserResponse {
    private UUID id;
    private String email;
    private String userName;
    private boolean status;
    private Role role;
}
