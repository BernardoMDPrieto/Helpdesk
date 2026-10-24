package com.bduarte.helpdeskserver.api.resources;

import com.bduarte.helpdeskserver.api.filters.UserFilter;
import com.bduarte.helpdeskserver.api.requests.CreateUserDTO;
import com.bduarte.helpdeskserver.api.requests.UpdateUserDTO;
import com.bduarte.helpdeskserver.api.responses.UserResponse;
import com.bduarte.helpdeskserver.models.Role;
import com.bduarte.helpdeskserver.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static com.bduarte.helpdeskserver.support.TestFixtures.role;
import static com.bduarte.helpdeskserver.support.TestFixtures.standaloneMockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneMockMvc(userController);
    }

    @Test
    void createUserReturnsCreated() throws Exception {
        mockMvc.perform(post("/admin/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "Ana", "email": "ana@helpdesk.com", "role": {"id": 2, "name": "ROLE_AGENT"}}
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<CreateUserDTO> captor = ArgumentCaptor.forClass(CreateUserDTO.class);
        verify(userService).CreateNewUser(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("Ana");
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@helpdesk.com");
        assertThat(captor.getValue().getRole().getName()).isEqualTo(Role.RoleName.ROLE_AGENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"email\": \"ana@helpdesk.com\"}",
            "{\"username\": \"Ana\"}",
            "{\"username\": \"Ana\", \"email\": \"not-an-email\"}"
    })
    void createUserWithInvalidBodyReturnsBadRequest(String body) throws Exception {
        mockMvc.perform(post("/admin/user").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void createUserPropagatesDuplicateEmailConflict() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Usuário com e-mail já cadastrado"))
                .when(userService).CreateNewUser(any(CreateUserDTO.class));

        mockMvc.perform(post("/admin/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username": "Ana", "email": "ana@helpdesk.com"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void getUsersUsesDefaultPagingAndBindsFilter() throws Exception {
        UserResponse user = new UserResponse(UUID.randomUUID(), "ana@helpdesk.com", "Ana", true, role(Role.RoleName.ROLE_AGENT));
        when(userService.getUsers(any(UserFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(user), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/admin/user")
                        .param("email", "ana")
                        .param("userName", "An")
                        .param("role", "2")
                        .param("status", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value("ana@helpdesk.com"))
                .andExpect(jsonPath("$.content[0].status").value(true))
                .andExpect(jsonPath("$.content[0].role.name").value("ROLE_AGENT"));

        ArgumentCaptor<UserFilter> filter = ArgumentCaptor.forClass(UserFilter.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userService).getUsers(filter.capture(), pageable.capture());
        assertThat(filter.getValue().getEmail()).isEqualTo("ana");
        assertThat(filter.getValue().getUserName()).isEqualTo("An");
        assertThat(filter.getValue().getRole()).isEqualTo(2);
        assertThat(filter.getValue().isStatus()).isTrue();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "userName"));
    }

    @Test
    void updateUserReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();

        mockMvc.perform(put("/admin/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userName": "Ana Souza", "active": false}
                                """))
                .andExpect(status().isNoContent());

        ArgumentCaptor<UpdateUserDTO> captor = ArgumentCaptor.forClass(UpdateUserDTO.class);
        verify(userService).updateUser(captor.capture(), eq(userId));
        assertThat(captor.getValue().getUserName()).isEqualTo("Ana Souza");
        assertThat(captor.getValue().getActive()).isFalse();
    }

    @Test
    void updateUserPropagatesNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"))
                .when(userService).updateUser(any(UpdateUserDTO.class), eq(userId));

        mockMvc.perform(put("/admin/user/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active": true}
                                """))
                .andExpect(status().isNotFound());
    }
}
