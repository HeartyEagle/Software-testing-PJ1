package com.demo.controller;

import com.demo.exception.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class IndexControllerIT extends AbstractControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void indexLoadsLatestHomepageDataFromRealServices() throws Exception {
        saveNews("Homepage News", "content");
        saveVenue("Court A", 200);
        saveUser("alice", "Alice", 0);
        saveMessage("alice", "approved message", passedMessageState(), LocalDateTime.now().minusMinutes(30));

        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("news_list", hasSize(1)))
                .andExpect(model().attribute("venue_list", hasSize(1)))
                .andExpect(model().attribute("message_list", hasSize(1)))
                .andExpect(model().attribute("user", (Object) null));
    }

    @Test
    void adminIndexReturnsAdminHomeView() throws Exception {
        mockMvc.perform(get("/admin_index"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/admin_index"));
    }

    @Test
    void adminIndexShouldRequireAdminLogin() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/admin_index").sessionAttr("user", saveUser("alice", "Alice", 0))).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void indexReturnsEmptyCollectionsWhenNoHomepageDataExists() throws Exception {
        mockMvc.perform(get("/index"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("news_list", hasSize(0)))
                .andExpect(model().attribute("venue_list", hasSize(0)))
                .andExpect(model().attribute("message_list", hasSize(0)));
    }
}
