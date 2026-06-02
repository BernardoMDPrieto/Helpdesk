package com.bduarte.helpdeskserver.models;

public record UserRegisteredEvent(String email, String userName, String token) {
}
