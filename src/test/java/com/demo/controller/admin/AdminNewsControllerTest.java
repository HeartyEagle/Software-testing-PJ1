package com.demo.controller.admin;

import com.demo.entity.News;
import com.demo.service.NewsService;
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
class AdminNewsControllerTest {

    @Mock
    private NewsService newsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminNewsController controller = new AdminNewsController();
        ReflectionTestUtils.setField(controller, "newsService", newsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void newsManageReturnsViewAndTotalPages() throws Exception {
        when(newsService.findAll(any())).thenReturn(new PageImpl<>(Collections.singletonList(new News())));

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attributeExists("total"));
    }

    @Test
    void newsAddReturnsFormView() throws Exception {
        mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_add"));
    }

    @Test
    void newsEditLoadsExistingNews() throws Exception {
        News news = new News(8, "title", "content", LocalDateTime.now());
        when(newsService.findById(8)).thenReturn(news);

        mockMvc.perform(get("/news_edit").param("newsID", "8"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"))
                .andExpect(model().attribute("news", news));
    }

    @Test
    void newsListReturnsPagedNews() throws Exception {
        News news = new News(2, "n1", "body", LocalDateTime.now());
        when(newsService.findAll(any())).thenReturn(new PageImpl<>(Collections.singletonList(news)));

        mockMvc.perform(get("/newsList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newsID").value(2))
                .andExpect(jsonPath("$[0].title").value("n1"));
    }

    @Test
    void deleteNewsDelegatesToService() throws Exception {
        mockMvc.perform(post("/delNews.do").param("newsID", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(newsService).delById(5);
    }

    @Test
    void modifyNewsUpdatesExistingEntityAndRedirects() throws Exception {
        News news = new News();
        news.setNewsID(7);
        when(newsService.findById(7)).thenReturn(news);

        mockMvc.perform(post("/modifyNews.do")
                        .param("newsID", "7")
                        .param("title", "new title")
                        .param("content", "new content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        verify(newsService).update(news);
        assertEquals("new title", news.getTitle());
        assertEquals("new content", news.getContent());
        assertNotNull(news.getTime());
    }

    @Test
    void addNewsCreatesEntityAndRedirects() throws Exception {
        ArgumentCaptor<News> captor = ArgumentCaptor.forClass(News.class);

        mockMvc.perform(post("/addNews.do")
                        .param("title", "fresh")
                        .param("content", "body"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        verify(newsService).create(captor.capture());
        assertEquals("fresh", captor.getValue().getTitle());
        assertEquals("body", captor.getValue().getContent());
        assertNotNull(captor.getValue().getTime());
    }
}
