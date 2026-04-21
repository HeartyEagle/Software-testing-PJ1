package com.demo.controller.admin;

import com.demo.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminUserControllerIT extends AbstractAdminControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void userManageReturnsViewAndCountsNormalUsers() throws Exception {
        saveUser("normal-user", "Normal User", 0);
        saveUser("admin-user", "Admin User", 1);

        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void userManageReturnsZeroPagesWhenNoNormalUserExists() throws Exception {
        mockMvc.perform(get("/user_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_manage"))
                .andExpect(model().attribute("total", 0));
    }

    @Test
    void userAddReturnsFormView() throws Exception {
        mockMvc.perform(get("/user_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_add"));
    }

    @Test
    void userListReturnsOnlyPersistedNormalUsers() throws Exception {
        User user = saveUser("normal-user", "Normal User", 0);
        saveUser("admin-user", "Admin User", 1);

        mockMvc.perform(get("/userList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(user.getId()))
                .andExpect(jsonPath("$[0].userID").value("normal-user"));
    }

    @Test
    void userListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        User user = saveUser("normal-user", "Normal User", 0);

        mockMvc.perform(get("/userList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(user.getId()));
    }

    @Test
    void userListThrowsWhenPageIsZero() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/userList.do").param("page", "0")).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void userEditLoadsPersistedUser() throws Exception {
        User user = saveUser("normal-user", "Normal User", 0);

        mockMvc.perform(get("/user_edit").param("id", String.valueOf(user.getId())))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user_edit"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void userEditReturnsBadRequestWhenIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/user_edit")));
        assertTrue(exception.getMessage().contains("id"));
    }

    @Test
    void modifyUserUpdatesPersistedUser() throws Exception {
        User user = saveUser("old-user", "Old Name", 0);

        mockMvc.perform(post("/modifyUser.do")
                        .param("userID", "new-user")
                        .param("oldUserID", "old-user")
                        .param("userName", "New Name")
                        .param("password", "newpass")
                        .param("email", "new@test.com")
                        .param("phone", "999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        User updated = userDao.findById(user.getId());
        assertEquals("new-user", updated.getUserID());
        assertEquals("New Name", updated.getUserName());
        assertEquals("newpass", updated.getPassword());
        assertEquals("new@test.com", updated.getEmail());
        assertEquals("999", updated.getPhone());
    }

    @Test
    void addUserCreatesPersistedUser() throws Exception {
        mockMvc.perform(post("/addUser.do")
                        .param("userID", "created-user")
                        .param("userName", "Created User")
                        .param("password", "pass")
                        .param("email", "created@test.com")
                        .param("phone", "10086"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user_manage"));

        User created = userDao.findByUserID("created-user");
        assertEquals("Created User", created.getUserName());
        assertEquals("", created.getPicture());
        assertEquals(0, created.getIsadmin());
    }

    @Test
    void checkUserIdReturnsTrueWhenIdIsAvailable() throws Exception {
        mockMvc.perform(post("/checkUserID.do").param("userID", "available-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void checkUserIdReturnsFalseWhenIdAlreadyExists() throws Exception {
        saveUser("taken-user", "Taken User", 0);

        mockMvc.perform(post("/checkUserID.do").param("userID", "taken-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }

    @Test
    void deleteUserRemovesPersistedRecord() throws Exception {
        User user = saveUser("delete-user", "Delete User", 0);

        mockMvc.perform(post("/delUser.do").param("id", String.valueOf(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertFalse(userDao.existsById(user.getId()));
    }
}
