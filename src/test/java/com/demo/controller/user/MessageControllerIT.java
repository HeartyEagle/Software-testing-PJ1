package com.demo.controller.user;

import com.demo.controller.AbstractControllerIT;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.exception.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MessageControllerIT extends AbstractControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void messageListReturnsViewForLoggedInUser() throws Exception {
        User user = saveUser("alice", "Alice", 0);
        saveMessage("alice", "approved message", passedMessageState());
        saveMessage("alice", "pending message", pendingMessageState());

        mockMvc.perform(get("/message_list").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("message_list"))
                .andExpect(model().attribute("total", 1))
                .andExpect(model().attribute("user_total", 1));
    }

    @Test
    void publicMessageListReturnsApprovedMessages() throws Exception {
        saveUser("alice", "Alice", 0);
        Message approved = saveMessage("alice", "approved message", passedMessageState());
        saveMessage("alice", "pending message", pendingMessageState());

        mockMvc.perform(get("/message/getMessageList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(approved.getMessageID()))
                .andExpect(jsonPath("$[0].userID").value("alice"))
                .andExpect(jsonPath("$[0].userName").value("Alice"));
    }

    @Test
    void publicMessageListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        saveUser("alice", "Alice", 0);
        Message approved = saveMessage("alice", "approved message", passedMessageState());

        mockMvc.perform(get("/message/getMessageList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(approved.getMessageID()));
    }

    @Test
    void publicMessageListThrowsWhenPageIsZero() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/message/getMessageList").param("page", "0")).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void userMessageListReturnsCurrentUsersMessages() throws Exception {
        User user = saveUser("alice", "Alice", 0);
        Message message = saveMessage("alice", "pending message", pendingMessageState());
        saveUser("bob", "Bob", 0);
        saveMessage("bob", "other message", pendingMessageState());

        mockMvc.perform(get("/message/findUserList")
                        .param("page", "1")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(message.getMessageID()))
                .andExpect(jsonPath("$[0].userID").value("alice"));
    }

    @Test
    void sendMessageCreatesPendingMessageAndRedirects() throws Exception {
        saveUser("alice", "Alice", 0);

        mockMvc.perform(post("/sendMessage")
                        .param("userID", "alice")
                        .param("content", "hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));

        Message saved = messageDao.findAllByUserID("alice", of(0, 10)).getContent().get(0);
        assertEquals("hello", saved.getContent());
        assertEquals(pendingMessageState(), saved.getState());
        assertTrue(saved.getTime().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void modifyMessageUpdatesContentTimeAndState() throws Exception {
        saveUser("alice", "Alice", 0);
        Message message = saveMessage("alice", "original", passedMessageState(), LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/modifyMessage.do")
                        .param("messageID", String.valueOf(message.getMessageID()))
                        .param("content", "updated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        Message updated = messageDao.findByMessageID(message.getMessageID());
        assertEquals("updated", updated.getContent());
        assertEquals(pendingMessageState(), updated.getState());
        assertTrue(updated.getTime().isAfter(message.getTime()));
    }

    @Test
    void sendMessageShouldUseLoggedInUserInsteadOfArbitrarySubmittedUserId() throws Exception {
        User loggedInUser = saveUser("bob", "Bob", 0);
        saveUser("alice", "Alice", 0);

        mockMvc.perform(post("/sendMessage")
                        .param("userID", "alice")
                        .param("content", "forged")
                        .sessionAttr("user", loggedInUser))
                .andExpect(status().is3xxRedirection());

        assertEquals(0, messageDao.findAllByUserID("alice", of(0, 10)).getContent().size(),
                "submitted userID should not let a logged-in user impersonate someone else");
        assertEquals(1, messageDao.findAllByUserID("bob", of(0, 10)).getContent().size(),
                "the created message should belong to the logged-in user");
    }

    @Test
    void deleteMessageRemovesPersistedMessage() throws Exception {
        saveUser("alice", "Alice", 0);
        Message message = saveMessage("alice", "to delete", pendingMessageState());

        mockMvc.perform(post("/delMessage.do").param("messageID", String.valueOf(message.getMessageID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertFalse(messageDao.findById(message.getMessageID()).isPresent());
    }

    @Test
    void modifyMessageShouldRequireLogin() {
        saveUser("alice", "Alice", 0);
        Message message = saveMessage("alice", "original", passedMessageState(), LocalDateTime.now().minusDays(1));

        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/modifyMessage.do")
                        .param("messageID", String.valueOf(message.getMessageID()))
                        .param("content", "updated")).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void deleteMessageShouldRequireLogin() {
        saveUser("alice", "Alice", 0);
        Message message = saveMessage("alice", "to delete", pendingMessageState());

        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/delMessage.do")
                        .param("messageID", String.valueOf(message.getMessageID()))).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void messageListRequiresLogin() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/message_list")).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void userMessageListRequiresLogin() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/message/findUserList").param("page", "1")).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void userMessageListThrowsWhenPageIsZero() {
        User user = saveUser("alice", "Alice", 0);

        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/message/findUserList")
                        .param("page", "0")
                        .sessionAttr("user", user)).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }
}
