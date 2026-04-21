package com.demo.controller.admin;

import com.demo.entity.News;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminNewsControllerIT extends AbstractAdminControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void newsManageReturnsViewWithTotalPages() throws Exception {
        saveNews("Breaking", "content");

        mockMvc.perform(get("/news_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/news_manage"))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void newsAddReturnsFormView() throws Exception {
        mockMvc.perform(get("/news_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_add"));
    }

    @Test
    void newsEditLoadsPersistedNews() throws Exception {
        News news = saveNews("Breaking", "content");

        mockMvc.perform(get("/news_edit").param("newsID", String.valueOf(news.getNewsID())))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/news_edit"))
                .andExpect(model().attributeExists("news"));
    }

    @Test
    void newsEditReturnsBadRequestWhenNewsIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/news_edit")));
        assertTrue(exception.getMessage().contains("newsID"));
    }

    @Test
    void newsListReturnsPersistedNews() throws Exception {
        News news = saveNews("Breaking", "content");

        mockMvc.perform(get("/newsList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newsID").value(news.getNewsID()))
                .andExpect(jsonPath("$[0].title").value("Breaking"));
    }

    @Test
    void newsListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        News news = saveNews("Breaking", "content");

        mockMvc.perform(get("/newsList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].newsID").value(news.getNewsID()));
    }

    @Test
    void newsListThrowsWhenPageIsZero() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/newsList.do").param("page", "0")).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void deleteNewsRemovesPersistedRecord() throws Exception {
        News news = saveNews("Breaking", "content");

        mockMvc.perform(post("/delNews.do").param("newsID", String.valueOf(news.getNewsID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertFalse(newsDao.findById(news.getNewsID()).isPresent());
    }

    @Test
    void modifyNewsUpdatesPersistedEntity() throws Exception {
        News news = saveNews("Old", "Old content");

        mockMvc.perform(post("/modifyNews.do")
                        .param("newsID", String.valueOf(news.getNewsID()))
                        .param("title", "New title")
                        .param("content", "New content"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        News updated = newsDao.findById(news.getNewsID())
                .orElseThrow(() -> new AssertionError("Expected updated news"));
        assertEquals("New title", updated.getTitle());
        assertEquals("New content", updated.getContent());
        assertNotNull(updated.getTime());
    }

    @Test
    void addNewsCreatesPersistedEntity() throws Exception {
        mockMvc.perform(post("/addNews.do")
                        .param("title", "Fresh news")
                        .param("content", "Brand new body"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("news_manage"));

        News created = newsDao.findAll().stream()
                .filter(item -> "Fresh news".equals(item.getTitle()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected created news"));
        assertEquals("Brand new body", created.getContent());
        assertNotNull(created.getTime());
    }
}
