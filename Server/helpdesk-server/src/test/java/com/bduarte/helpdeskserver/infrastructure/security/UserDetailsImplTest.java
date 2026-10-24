package com.bduarte.helpdeskserver.infrastructure.security;

import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.core.GrantedAuthority;

import static com.bduarte.helpdeskserver.support.TestFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {

    @ParameterizedTest
    @EnumSource(Role.RoleName.class)
    void authorityIsTheRoleName(Role.RoleName roleName) {
        UserDetailsImpl details = new UserDetailsImpl(user(roleName));

        assertThat(details.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly(roleName.name());
    }

    @Test
    void exposesUserCredentials() {
        User user = user(Role.RoleName.ROLE_CUSTOMER, "ana@helpdesk.com");
        UserDetailsImpl details = new UserDetailsImpl(user);

        assertThat(details.getUsername()).isEqualTo("ana@helpdesk.com");
        assertThat(details.getPassword()).isEqualTo(user.getPassword());
        assertThat(details.getUser()).isSameAs(user);
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void enabledFollowsUserFlag() {
        User user = user(Role.RoleName.ROLE_CUSTOMER);
        UserDetailsImpl details = new UserDetailsImpl(user);

        assertThat(details.isEnabled()).isTrue();
        user.setEnabled(false);
        assertThat(details.isEnabled()).isFalse();
    }
}
