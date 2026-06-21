package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.repositories.UserRepository;
import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);
    
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("Loading user details for email: {}", email);
        return userRepository.findByEmail(email)
                .map(user -> {
                    logger.debug("User found for email: {}", email);
                    return new UserDetailsImpl(user);
                })
                .orElseThrow(() -> {
                    logger.warn("User not found for email: {}", email);
                    return new UsernameNotFoundException("Usuário não encontrado: " + email);
                });
    }
}
