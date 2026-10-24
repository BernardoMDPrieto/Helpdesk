package com.bduarte.helpdeskserver.services;

import com.bduarte.helpdeskserver.infrastructure.security.UserDetailsImpl;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.User;
import com.bduarte.helpdeskserver.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static com.bduarte.helpdeskserver.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadsUserByEmail() {
        User user = user(Role.RoleName.ROLE_ADMIN, "admin@helpdesk.com");
        when(userRepository.findByEmail("admin@helpdesk.com")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("admin@helpdesk.com");

        assertThat(details).isInstanceOfSatisfying(UserDetailsImpl.class,
                impl -> assertThat(impl.getUser()).isSameAs(user));
        assertThat(details.getUsername()).isEqualTo("admin@helpdesk.com");
    }

    @Test
    void unknownEmailThrowsUsernameNotFound() {
        when(userRepository.findByEmail("ghost@helpdesk.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@helpdesk.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@helpdesk.com");
    }
}
