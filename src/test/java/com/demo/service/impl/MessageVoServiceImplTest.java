package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.dao.UserDao;
import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageVoServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @Mock
    private UserDao userDao;

    @InjectMocks
    private MessageVoServiceImpl messageVoService;

    @Test
    void returnMessageVoByMessageIdCombinesMessageAndUser() {
        LocalDateTime time = LocalDateTime.of(2026, 4, 5, 11, 30);
        Message message = new Message(9, "alice", "hello", time, 2);
        User user = new User();
        user.setUserID("alice");
        user.setUserName("Alice");
        user.setPicture("avatar.png");
        when(messageDao.findByMessageID(9)).thenReturn(message);
        when(userDao.findByUserID("alice")).thenReturn(user);

        MessageVo result = messageVoService.returnMessageVoByMessageID(9);

        assertEquals(9, result.getMessageID());
        assertEquals("alice", result.getUserID());
        assertEquals("hello", result.getContent());
        assertEquals(time, result.getTime());
        assertEquals("Alice", result.getUserName());
        assertEquals("avatar.png", result.getPicture());
        assertEquals(2, result.getState());
    }

    @Test
    void returnVoBuildsVoForEveryMessage() {
        Message first = new Message(1, "alice", "a", LocalDateTime.now(), 2);
        Message second = new Message(2, "bob", "b", LocalDateTime.now(), 1);
        User alice = new User();
        alice.setUserID("alice");
        alice.setUserName("Alice");
        User bob = new User();
        bob.setUserID("bob");
        bob.setUserName("Bob");
        when(messageDao.findByMessageID(1)).thenReturn(first);
        when(messageDao.findByMessageID(2)).thenReturn(second);
        when(userDao.findByUserID("alice")).thenReturn(alice);
        when(userDao.findByUserID("bob")).thenReturn(bob);

        List<MessageVo> result = messageVoService.returnVo(Arrays.asList(first, second));

        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).getUserName());
        assertEquals("Bob", result.get(1).getUserName());
    }

    @Test
    void returnVoReturnsEmptyListWhenInputIsEmpty() {
        List<MessageVo> result = messageVoService.returnVo(Collections.emptyList());

        assertEquals(0, result.size());
    }

    @Test
    void returnMessageVoByMessageIdThrowsWhenMessageMissing() {
        when(messageDao.findByMessageID(404)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> messageVoService.returnMessageVoByMessageID(404));
    }

    @Test
    void returnMessageVoByMessageIdThrowsWhenUserMissing() {
        Message message = new Message(9, "alice", "hello", LocalDateTime.now(), 2);
        when(messageDao.findByMessageID(9)).thenReturn(message);
        when(userDao.findByUserID("alice")).thenReturn(null);

        assertThrows(NullPointerException.class, () -> messageVoService.returnMessageVoByMessageID(9));
    }

    @Test
    void returnMessageVoByMessageIdShouldThrowMeaningfulExceptionWhenMessageMissing() {
        when(messageDao.findByMessageID(404)).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> messageVoService.returnMessageVoByMessageID(404));

        assertTrue(exception.getMessage() != null && !exception.getMessage().isBlank(),
                "missing messages should not surface as bare null pointers from the VO service");
    }
}
