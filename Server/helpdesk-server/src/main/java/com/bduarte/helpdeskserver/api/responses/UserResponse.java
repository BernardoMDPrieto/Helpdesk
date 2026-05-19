package com.bduarte.helpdeskserver.api.responses;

import com.bduarte.helpdeskserver.models.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserResponse {
    private String email;
    private String userName;
    private boolean status;
    private Role role;
}
