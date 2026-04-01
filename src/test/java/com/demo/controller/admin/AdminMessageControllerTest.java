package com.demo.controller.admin;

import com.demo.entity.Message;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AdminMessageControllerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private MessageVoService messageVoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminMessageController controller = new AdminMessageController();
        ReflectionTestUtils.setField(controller, "messageService", messageService);
        ReflectionTestUtils.setField(controller, "messageVoService", messageVoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void messageManageReturnsViewAndTotalPages() throws Exception {
        when(messageService.findWaitState(any())).thenReturn(new PageImpl<>(Collections.singletonList(new Message())));

        mockMvc.perform(get("/message_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/message_manage"))
                .andExpect(model().attributeExists("total"));
    }

    @Test
    void messageListReturnsPendingMessages() throws Exception {
        Message message = new Message();
        MessageVo vo = new MessageVo();
        vo.setMessageID(13);
        when(messageService.findWaitState(any())).thenReturn(new PageImpl<>(Collections.singletonList(message)));
        when(messageVoService.returnVo(Collections.singletonList(message))).thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/messageList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].messageID").value(13));
    }

    @Test
    void passMessageDelegatesToService() throws Exception {
        mockMvc.perform(post("/passMessage.do").param("messageID", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(messageService).confirmMessage(4);
    }

    @Test
    void rejectMessageDelegatesToService() throws Exception {
        mockMvc.perform(post("/rejectMessage.do").param("messageID", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(messageService).rejectMessage(5);
    }

    @Test
    void deleteMessageDelegatesToService() throws Exception {
        mockMvc.perform(post("/delMessage.do").param("messageID", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(messageService).delById(6);
    }
}
