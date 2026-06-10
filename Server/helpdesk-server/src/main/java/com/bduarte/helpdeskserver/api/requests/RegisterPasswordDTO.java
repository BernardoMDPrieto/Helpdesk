package com.bduarte.helpdeskserver.api.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterPasswordDTO {
    private String password;
    private String token;
}
