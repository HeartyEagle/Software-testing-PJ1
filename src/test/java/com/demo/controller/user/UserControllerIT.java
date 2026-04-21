package com.demo.controller.user;

import com.demo.controller.AbstractControllerIT;
import com.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class UserControllerIT extends AbstractControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
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
        User user = saveUser("alice", "Alice", "secret", 0);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "alice")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("/index"))
                .andExpect(request().sessionAttribute("user", user));
    }

    @Test
    void loginCheckStoresAdminSessionForAdmin() throws Exception {
        User user = saveUser("admin", "Admin", "admin", 1);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "admin")
                        .param("password", "admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("/admin_index"))
                .andExpect(request().sessionAttribute("admin", user));
    }

    @Test
    void loginCheckReturnsFalseWhenCredentialsAreInvalid() throws Exception {
        saveUser("alice", "Alice", "secret", 0);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "alice")
                        .param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void loginCheckReturnsFalseWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "missing")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void loginCheckReturnsFalseWhenUserRoleIsUnexpected() throws Exception {
        saveUser("weird", "Weird", "secret", 2);

        mockMvc.perform(post("/loginCheck.do")
                        .param("userID", "weird")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void registerCreatesUserAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register.do")
                        .param("userID", "alice")
                        .param("userName", "Alice")
                        .param("password", "secret")
                        .param("email", "alice@test.com")
                        .param("phone", "123456"))
                .andExpect(status().is3xxRedirection());

        User created = userDao.findByUserID("alice");
        assertEquals("Alice", created.getUserName());
        assertEquals("secret", created.getPassword());
        assertEquals("", created.getPicture());
        assertEquals(0, created.getIsadmin());
    }

    @Test
    void logoutRemovesUserSessionAndRedirectsHome() throws Exception {
        mockMvc.perform(get("/logout.do").sessionAttr("user", new User()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void quitRemovesAdminSessionAndRedirectsHome() throws Exception {
        mockMvc.perform(get("/quit.do").sessionAttr("admin", new User()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void updateUserUpdatesProfileWithoutUploadingPicture() throws Exception {
        User user = saveUser("alice", "Alice", "old", 0);
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
                .andExpect(request().sessionAttribute("user", org.hamcrest.Matchers.hasProperty("userName", is("Alice New"))));

        User updated = userDao.findByUserID("alice");
        assertEquals("Alice New", updated.getUserName());
        assertEquals("newpass", updated.getPassword());
        assertEquals("new@test.com", updated.getEmail());
        assertEquals("987654", updated.getPhone());
        assertEquals("", updated.getPicture());
    }

    @Test
    void updateUserKeepsExistingPasswordWhenPasswordNewIsBlank() throws Exception {
        User user = saveUser("alice", "Alice", "oldpass", 0);
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/updateUser.do")
                        .file(picture)
                        .param("userName", "Alice Same Password")
                        .param("userID", "alice")
                        .param("passwordNew", "")
                        .param("email", "same@test.com")
                        .param("phone", "222222")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection());

        User updated = userDao.findByUserID("alice");
        assertEquals("oldpass", updated.getPassword());
        assertEquals("Alice Same Password", updated.getUserName());
    }

    @Test
    void updateUserStoresUploadedPictureWhenProvided() throws Exception {
        User user = saveUser("alice", "Alice", "old", 0);
        MockMultipartFile picture = new MockMultipartFile("picture", "avatar.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/updateUser.do")
                        .file(picture)
                        .param("userName", "Alice With Avatar")
                        .param("userID", "alice")
                        .param("passwordNew", "newpass")
                        .param("email", "avatar@test.com")
                        .param("phone", "333333")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection());

        User updated = userDao.findByUserID("alice");
        assertTrue(updated.getPicture().startsWith("file/user/"));
        assertTrue(updated.getPicture().endsWith(".png"));
    }

    @Test
    void checkPasswordReturnsTrueWhenPasswordMatches() throws Exception {
        saveUser("alice", "Alice", "secret", 0);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", "alice")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void checkPasswordReturnsFalseWhenPasswordDoesNotMatch() throws Exception {
        saveUser("alice", "Alice", "secret", 0);

        mockMvc.perform(get("/checkPassword.do")
                        .param("userID", "alice")
                        .param("password", "bad"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void userInfoReturnsView() throws Exception {
        User user = saveUser("viewer", "Viewer", "secret", 0);

        mockMvc.perform(get("/user_info").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("user_info"));
    }

}
