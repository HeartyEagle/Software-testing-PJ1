package com.demo.controller.user;

import com.demo.controller.AbstractControllerIT;
import com.demo.entity.News;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.NestedServletException;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class NewsControllerIT extends AbstractControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void newsPageLoadsSingleNewsItem() throws Exception {
        News news = saveNews("notice", "content");

        mockMvc.perform(get("/news").param("newsID", String.valueOf(news.getNewsID())))
                .andExpect(status().isOk())
                .andExpect(view().name("news"))
                .andExpect(model().attribute("news", hasProperty("newsID", is(news.getNewsID()))))
                .andExpect(model().attribute("news", hasProperty("title", is("notice"))))
                .andExpect(model().attribute("news", hasProperty("content", is("content"))));
    }

    @Test
    void newsPageReturnsBadRequestWhenNewsIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/news")));
        assertTrue(exception.getMessage().contains("newsID"));
    }

    @Test
    void getNewsListReturnsPagedJson() throws Exception {
        News news = saveNews("n1", "body");

        mockMvc.perform(get("/news/getNewsList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].newsID").value(news.getNewsID()))
                .andExpect(jsonPath("$.content[0].title").value("n1"));
    }

    @Test
    void getNewsListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        News news = saveNews("n1", "body");

        mockMvc.perform(get("/news/getNewsList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].newsID").value(news.getNewsID()));
    }

    @Test
    void getNewsListThrowsWhenPageIsZero() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/news/getNewsList").param("page", "0")).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void newsListReturnsViewAndInitialPageData() throws Exception {
        saveNews("a", "a");
        saveNews("b", "b");

        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attribute("news_list", hasSize(2)))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void newsListReturnsEmptyPageWhenNoNewsExists() throws Exception {
        mockMvc.perform(get("/news_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("news_list"))
                .andExpect(model().attribute("news_list", hasSize(0)))
                .andExpect(model().attribute("total", 0));
    }
}
