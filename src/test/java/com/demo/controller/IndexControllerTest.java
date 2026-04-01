package com.demo.controller;

import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Venue;
import com.demo.entity.vo.MessageVo;
import com.demo.service.MessageService;
import com.demo.service.MessageVoService;
import com.demo.service.NewsService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class IndexControllerTest {

    @Mock
    private NewsService newsService;

    @Mock
    private VenueService venueService;

    @Mock
    private MessageVoService messageVoService;

    @Mock
    private MessageService messageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        IndexController controller = new IndexController();
        ReflectionTestUtils.setField(controller, "newsService", newsService);
        ReflectionTestUtils.setField(controller, "venueService", venueService);
        ReflectionTestUtils.setField(controller, "messageVoService", messageVoService);
        ReflectionTestUtils.setField(controller, "messageService", messageService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void indexLoadsLatestHomepageData() throws Exception {
        News news = new News(1, "title", "content", LocalDateTime.now());
        Venue venue = new Venue();
        venue.setVenueID(2);
        Message message = new Message(3, "alice", "hello", LocalDateTime.now(), 2);
        MessageVo messageVo = new MessageVo(3, "alice", "hello", message.getTime(), "Alice", "", 2);
        when(newsService.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(news)));
        when(venueService.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(venue)));
        when(messageService.findPassState(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(message)));
        when(messageVoService.returnVo(Arrays.asList(message))).thenReturn(Collections.singletonList(messageVo));

        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("news_list"))
                .andExpect(model().attributeExists("venue_list"))
                .andExpect(model().attributeExists("message_list"))
                .andExpect(model().attribute("user", (Object) null));
    }

    @Test
    void adminIndexReturnsAdminHomeView() throws Exception {
        mockMvc.perform(get("/admin_index"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin_index"));
    }
}
