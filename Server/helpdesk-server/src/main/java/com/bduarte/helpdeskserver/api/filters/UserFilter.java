package com.bduarte.helpdeskserver.api.filters;

import lombok.Data;

@Data
public class UserFilter {

    private String email;
    private String userName;
    private Integer role;
    private boolean status;
}
