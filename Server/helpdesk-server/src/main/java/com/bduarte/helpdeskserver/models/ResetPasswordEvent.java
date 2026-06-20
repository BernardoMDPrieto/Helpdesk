package com.bduarte.helpdeskserver.models;

public record ResetPasswordEvent(String email, String userName, String token) {
}
