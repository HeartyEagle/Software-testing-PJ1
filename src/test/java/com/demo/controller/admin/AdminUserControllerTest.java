package com.demo.controller.admin;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminUserController controller = new AdminUserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void userManageReturnsViewAndTotalPages() throws Exception {
        when(userService.findByUserID(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(new User())));

        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attributeExists("total"));
    }

    @Test
    void userAddReturnsFormView() throws Exception {
        mockMvc.perform(get("/user_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_add"));
    }

    @Test
    void userListReturnsPagedUsers() throws Exception {
        User user = new User();
        user.setId(2);
        user.setUserID("alice");
        when(userService.findByUserID(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(user)));

        mockMvc.perform(get("/userList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(jsonPath("$[0].userID").value("alice"));
    }

    @Test
    void userEditLoadsSelectedUser() throws Exception {
        User user = new User();
        user.setId(3);
        when(userService.findById(3)).thenReturn(user);

        mockMvc.perform(get("/user_edit").param("id", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_edit"))
                .andExpect(model().attribute("user", user));
    }

    @Test
    void modifyUserUpdatesExistingUserAndRedirects() throws Exception {
        User user = new User();
        user.setUserID("old");
        when(userService.findByUserID("old")).thenReturn(user);

        mockMvc.perform(post("/modifyUser.do")
                        .param("userID", "new")
                        .param("oldUserID", "old")
                        .param("userName", "Alice")
                        .param("password", "secret")
                        .param("email", "a@test.com")
                        .param("phone", "123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).updateUser(user);
        assertEquals("new", user.getUserID());
        assertEquals("Alice", user.getUserName());
    }

    @Test
    void addUserCreatesUserAndRedirects() throws Exception {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        mockMvc.perform(post("/addUser.do")
                        .param("userID", "bob")
                        .param("userName", "Bob")
                        .param("password", "pwd")
                        .param("email", "b@test.com")
                        .param("phone", "555"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        verify(userService).create(captor.capture());
        assertEquals("bob", captor.getValue().getUserID());
        assertEquals("", captor.getValue().getPicture());
    }

    @Test
    void checkUserIdReturnsTrueWhenIdIsAvailable() throws Exception {
        when(userService.countUserID("newId")).thenReturn(0);

        mockMvc.perform(post("/checkUserID.do").param("userID", "newId"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void deleteUserDelegatesToService() throws Exception {
        mockMvc.perform(post("/delUser.do").param("id", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(userService).delByID(5);
    }
}
