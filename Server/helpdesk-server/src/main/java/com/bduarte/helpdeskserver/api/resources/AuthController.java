package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.requests.RegisterPasswordDTO;
import com.bduarte.helpdeskserver.api.requests.ResetPasswordDTO;
import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.services.JwtService;
import com.bduarte.helpdeskserver.services.UserService;
import lombok.RequiredArgsConstructor;
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

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest req) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password()));

        UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
        String token = jwtService.generateToken(userDetails);
        return ResponseEntity.ok(token);
    }

    @PostMapping("/register-password")
    public ResponseEntity<Void> registerPassword(@RequestBody RegisterPasswordDTO request) {
        userService.registerPassword(request.getPassword(), request.getToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> ResetPassword(@RequestBody RegisterPasswordDTO request) {
        userService.registerPassword(request.getPassword(), request.getToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/request-reset-password")
    public ResponseEntity<Void> requestResetPassword(@RequestBody ResetPasswordDTO resetPasswordDTO) {
        userService.requestResetPassword(resetPasswordDTO.getEmail());
        return ResponseEntity.ok().build();
    }

    public record LoginRequest(String email, String password) {
    }
}
