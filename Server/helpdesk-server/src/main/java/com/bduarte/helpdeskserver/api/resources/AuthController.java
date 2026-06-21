package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.requests.RegisterPasswordDTO;
import com.bduarte.helpdeskserver.api.requests.ResetPasswordDTO;
import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.services.JwtService;
import com.bduarte.helpdeskserver.services.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest req) {
        logger.info("Login attempt for email: {}", req.email());
        try {
            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.email(), req.password()));

            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            String token = jwtService.generateToken(userDetails);
            logger.info("Login successful for email: {}", req.email());
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            logger.warn("Login failed for email: {}", req.email());
            throw e;
        }
    }

    @PostMapping("/register-password")
    public ResponseEntity<Void> registerPassword(@RequestBody RegisterPasswordDTO request) {
        logger.info("Register password request received");
        userService.registerPassword(request.getPassword(), request.getToken());
        logger.info("Password registered successfully");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> ResetPassword(@RequestBody RegisterPasswordDTO request) {
        logger.info("Reset password request received");
        userService.registerPassword(request.getPassword(), request.getToken());
        logger.info("Password reset successfully");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/request-reset-password")
    public ResponseEntity<Void> requestResetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        logger.info("Reset password request initiated for email: {}", resetPasswordDTO.getEmail());
        userService.requestResetPassword(resetPasswordDTO.getEmail());
        logger.info("Reset password request sent successfully");
        return ResponseEntity.ok().build();
    }

    public record LoginRequest(String email, String password) {
    }
}
