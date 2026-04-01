package com.demo.controller.user;

import com.demo.entity.User;
import com.demo.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", userService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .setMessageConverters(new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void signupReturnsSignupView() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("signup"));
    }

    @Test
    void loginPageReturnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void loginCheckStoresUserSessionForNormalUser() throws Exception {
        User user = new User();
        user.setUserID("alice");
        user.setIsadmin(0);
        when(userService.checkLogin("alice", "secret")).thenReturn(user);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "alice")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("/index"))
                .andExpect(request().sessionAttribute("user", user));
    }

    @Test
    void loginCheckStoresAdminSessionForAdmin() throws Exception {
        User user = new User();
        user.setUserID("admin");
        user.setIsadmin(1);
        when(userService.checkLogin("admin", "admin")).thenReturn(user);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "admin")
                        .param("password", "admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("/admin_index"))
                .andExpect(request().sessionAttribute("admin", user));
    }

    @Test
    void loginCheckReturnsFalseWhenCredentialsAreInvalid() throws Exception {
        when(userService.checkLogin("alice", "bad")).thenReturn(null);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "alice")
                        .param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void registerCreatesUserAndRedirectsToLogin() throws Exception {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        mockMvc.perform(post("/register.do")
                        .param("userID", "alice")
                        .param("userName", "Alice")
                        .param("password", "secret")
                        .param("email", "alice@test.com")
                        .param("phone", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("login"));

        verify(userService).create(captor.capture());
        User saved = captor.getValue();
        assertEquals("alice", saved.getUserID());
        assertEquals("Alice", saved.getUserName());
        assertEquals("secret", saved.getPassword());
        assertEquals("", saved.getPicture());
    }

    @Test
    void logoutRemovesUserSessionAndRedirectsHome() throws Exception {
        mockMvc.perform(get("/logout.do").sessionAttr("user", new User()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));
    }

    @Test
    void quitRemovesAdminSessionAndRedirectsHome() throws Exception {
        mockMvc.perform(get("/quit.do").sessionAttr("admin", new User()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/index"));
    }

    @Test
    void updateUserUpdatesProfileWithoutUploadingPicture() throws Exception {
        User user = new User();
        user.setUserID("alice");
        user.setPassword("old");
        when(userService.findByUserID("alice")).thenReturn(user);
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/updateUser.do")
                        .file(picture)
                        .param("userName", "Alice New")
                        .param("userID", "alice")
                        .param("passwordNew", "newpass")
                        .param("email", "new@test.com")
                        .param("phone", "987654")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_info"));

        verify(userService).updateUser(user);
        assertEquals("Alice New", user.getUserName());
        assertEquals("newpass", user.getPassword());
        assertEquals("new@test.com", user.getEmail());
        assertEquals("987654", user.getPhone());
    }

    @Test
    void checkPasswordReturnsTrueWhenPasswordMatches() throws Exception {
        User user = new User();
        user.setPassword("secret");
        when(userService.findByUserID("alice")).thenReturn(user);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", "alice")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void userInfoReturnsView() throws Exception {
        mockMvc.perform(get("/user_info"))
                .andExpect(status().isOk())
                .andExpect(view().name("user_info"));
    }
}
