package com.demo.controller.admin;

import com.demo.entity.Message;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminMessageControllerIT extends AbstractAdminControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void messageManageReturnsAdminViewWithPendingPageCount() throws Exception {
        saveUser("msg-user", "Message User", 0);
        saveMessage("msg-user", "pending message", pendingMessageState());

        mockMvc.perform(get("/message_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/message_manage"))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void messageListReturnsPendingMessagesFromRealServices() throws Exception {
        saveUser("msg-user", "Message User", 0);
        Message message = saveMessage("msg-user", "pending message", pendingMessageState());

        mockMvc.perform(get("/messageList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(message.getMessageID()))
                .andExpect(jsonPath("$[0].userID").value("msg-user"))
                .andExpect(jsonPath("$[0].userName").value("Message User"));
    }

    @Test
    void messageListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        saveUser("msg-user", "Message User", 0);
        Message message = saveMessage("msg-user", "pending message", pendingMessageState());

        mockMvc.perform(get("/messageList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(message.getMessageID()));
    }

    @Test
    void messageListThrowsWhenPageIsZero() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/messageList.do").param("page", "0")).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void passMessageUpdatesDatabaseState() throws Exception {
        saveUser("msg-user", "Message User", 0);
        Message message = saveMessage("msg-user", "pending message", pendingMessageState());

        mockMvc.perform(post("/passMessage.do").param("messageID", String.valueOf(message.getMessageID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertEquals(passedMessageState(), messageDao.findByMessageID(message.getMessageID()).getState());
    }

    @Test
    void passMessageReturnsBadRequestWhenMessageIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/passMessage.do")));
        assertTrue(exception.getMessage().contains("messageID"));
    }

    @Test
    void rejectMessageUpdatesDatabaseState() throws Exception {
        saveUser("msg-user", "Message User", 0);
        Message message = saveMessage("msg-user", "pending message", pendingMessageState());

        mockMvc.perform(post("/rejectMessage.do").param("messageID", String.valueOf(message.getMessageID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertEquals(rejectedMessageState(), messageDao.findByMessageID(message.getMessageID()).getState());
    }

    @Test
    void deleteMessageRemovesRecordFromDatabase() throws Exception {
        saveUser("msg-user", "Message User", 0);
        Message message = saveMessage("msg-user", "pending message", pendingMessageState());

        mockMvc.perform(post("/delMessage.do").param("messageID", String.valueOf(message.getMessageID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertFalse(messageDao.findById(message.getMessageID()).isPresent());
    }
}
