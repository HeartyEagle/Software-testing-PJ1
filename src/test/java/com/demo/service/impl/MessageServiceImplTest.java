package com.demo.service.impl;

import com.demo.dao.MessageDao;
import com.demo.entity.Message;
import com.demo.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceImplTest {

    @Mock
    private MessageDao messageDao;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void findByIdReturnsMessage() {
        Message message = new Message();
        when(messageDao.getOne(10)).thenReturn(message);

        Message result = messageService.findById(10);

        assertSame(message, result);
    }

    @Test
    void findByUserReturnsPagedMessages() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Message> page = new PageImpl<>(Arrays.asList(new Message()), pageable, 1);
        when(messageDao.findAllByUserID("alice", pageable)).thenReturn(page);

        Page<Message> result = messageService.findByUser("alice", pageable);

        assertSame(page, result);
    }

    @Test
    void createReturnsSavedMessageId() {
        Message message = new Message();
        message.setMessageID(5);
        when(messageDao.save(message)).thenReturn(message);

        int result = messageService.create(message);

        assertEquals(5, result);
    }

    @Test
    void deleteByIdDelegatesToDao() {
        messageService.delById(3);

        verify(messageDao).deleteById(3);
    }

    @Test
    void updateSavesMessage() {
        Message message = new Message();

        messageService.update(message);

        verify(messageDao).save(message);
    }

    @Test
    void confirmMessageUpdatesStateWhenMessageExists() {
        Message message = new Message();
        message.setMessageID(9);
        when(messageDao.findByMessageID(9)).thenReturn(message);

        messageService.confirmMessage(9);

        verify(messageDao).updateState(MessageService.STATE_PASS, 9);
    }

    @Test
    void rejectMessageUpdatesStateWhenMessageExists() {
        Message message = new Message();
        message.setMessageID(11);
        when(messageDao.findByMessageID(11)).thenReturn(message);

        messageService.rejectMessage(11);

        verify(messageDao).updateState(MessageService.STATE_REJECT, 11);
    }

    @Test
    void confirmMessageThrowsWhenMessageMissing() {
        when(messageDao.findByMessageID(99)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> messageService.confirmMessage(99));
    }

    @Test
    void rejectMessageThrowsWhenMessageMissing() {
        when(messageDao.findByMessageID(100)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> messageService.rejectMessage(100));
    }

    @Test
    void findWaitStateReturnsPagedMessages() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Message> page = new PageImpl<>(Arrays.asList(new Message()), pageable, 1);
        when(messageDao.findAllByState(MessageService.STATE_NO_AUDIT, pageable)).thenReturn(page);

        Page<Message> result = messageService.findWaitState(pageable);

        assertSame(page, result);
    }

    @Test
    void findPassStateReturnsPagedMessages() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Message> page = new PageImpl<>(Arrays.asList(new Message()), pageable, 1);
        when(messageDao.findAllByState(MessageService.STATE_PASS, pageable)).thenReturn(page);

        Page<Message> result = messageService.findPassState(pageable);

        assertSame(page, result);
    }
}
