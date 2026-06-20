package com.bduarte.helpdeskserver.api.requests;

import com.bduarte.helpdeskserver.models.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserDTO {
    private String userName;
    private Boolean active;
    private Role role;
}
