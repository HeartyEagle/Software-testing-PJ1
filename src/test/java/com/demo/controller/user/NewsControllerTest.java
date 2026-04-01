package com.demo.controller.user;

import com.demo.entity.News;
import com.demo.service.NewsService;
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
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class NewsControllerTest {

    @Mock
    private NewsService newsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NewsController controller = new NewsController();
        ReflectionTestUtils.setField(controller, "newsService", newsService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void newsPageLoadsSingleNewsItem() throws Exception {
        News news = new News(5, "notice", "content", LocalDateTime.now());
        when(newsService.findById(5)).thenReturn(news);

        mockMvc.perform(get("/news").param("newsID", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attribute("news", news));
    }

    @Test
    void getNewsListReturnsPagedJson() throws Exception {
        News news = new News(2, "n1", "body", LocalDateTime.now());
        when(newsService.findAll(any())).thenReturn(new PageImpl<>(Collections.singletonList(news)));

        mockMvc.perform(get("/news/getNewsList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].newsID").value(2))
                .andExpect(jsonPath("$.content[0].title").value("n1"));
    }

    @Test
    void newsListReturnsViewAndInitialPageData() throws Exception {
        News first = new News(1, "a", "a", LocalDateTime.now());
        News second = new News(2, "b", "b", LocalDateTime.now());
        when(newsService.findAll(any())).thenReturn(new PageImpl<>(Arrays.asList(first, second)));

        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attributeExists("news_list"))
                .andExpect(model().attributeExists("total"));
    }
}
