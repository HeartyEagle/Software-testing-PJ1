package com.demo.controller.user;

import com.demo.entity.Message;
import com.demo.entity.User;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
class MessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private MessageVoService messageVoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MessageController controller = new MessageController();
        ReflectionTestUtils.setField(controller, "messageService", messageService);
        ReflectionTestUtils.setField(controller, "messageVoService", messageVoService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void messageListReturnsViewForLoggedInUser() throws Exception {
        User user = new User();
        user.setUserID("alice");
        Message message = new Message();
        MessageVo messageVo = new MessageVo();
        when(messageService.findPassState(any())).thenReturn(new PageImpl<>(Collections.singletonList(message)));
        when(messageVoService.returnVo(Collections.singletonList(message))).thenReturn(Collections.singletonList(messageVo));
        when(messageService.findByUser(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(message)));

        mockMvc.perform(get("/message_list").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("message_list"))
                .andExpect(model().attributeExists("total"))
                .andExpect(model().attributeExists("user_total"));
    }

    @Test
    void publicMessageListReturnsApprovedMessages() throws Exception {
        Message message = new Message();
        message.setMessageID(4);
        MessageVo messageVo = new MessageVo();
        messageVo.setMessageID(4);
        when(messageService.findPassState(any())).thenReturn(new PageImpl<>(Collections.singletonList(message)));
        when(messageVoService.returnVo(Collections.singletonList(message))).thenReturn(Collections.singletonList(messageVo));

        mockMvc.perform(get("/message/getMessageList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(4));
    }

    @Test
    void userMessageListReturnsCurrentUsersMessages() throws Exception {
        User user = new User();
        user.setUserID("alice");
        Message message = new Message();
        message.setMessageID(6);
        MessageVo messageVo = new MessageVo();
        messageVo.setMessageID(6);
        when(messageService.findByUser(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(message)));
        when(messageVoService.returnVo(Collections.singletonList(message))).thenReturn(Collections.singletonList(messageVo));

        mockMvc.perform(get("/message/findUserList")
                        .param("page", "1")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(6));
    }

    @Test
    void sendMessageCreatesPendingMessageAndRedirects() throws Exception {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        mockMvc.perform(post("/sendMessage")
                        .param("userID", "alice")
                        .param("content", "hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/message_list"));

        verify(messageService).create(captor.capture());
        Message saved = captor.getValue();
        assertEquals("alice", saved.getUserID());
        assertEquals("hello", saved.getContent());
        assertEquals(1, saved.getState());
        assertNotNull(saved.getTime());
    }

    @Test
    void modifyMessageUpdatesContentTimeAndState() throws Exception {
        Message message = new Message();
        message.setMessageID(8);
        when(messageService.findById(8)).thenReturn(message);

        mockMvc.perform(post("/modifyMessage.do")
                        .param("messageID", "8")
                        .param("content", "updated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(messageService).update(message);
        assertEquals("updated", message.getContent());
        assertEquals(1, message.getState());
        assertNotNull(message.getTime());
    }

    @Test
    void deleteMessageDelegatesToService() throws Exception {
        mockMvc.perform(post("/delMessage.do").param("messageID", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(messageService).delById(9);
    }
}
