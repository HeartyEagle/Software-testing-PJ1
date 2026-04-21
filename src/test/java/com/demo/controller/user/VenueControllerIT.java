package com.demo.controller.user;

import com.demo.controller.AbstractControllerIT;
import com.demo.entity.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class VenueControllerIT extends AbstractControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void venuePageLoadsVenueDetails() throws Exception {
        Venue venue = saveVenue("Gym A", 200);

        mockMvc.perform(get("/venue").param("venueID", String.valueOf(venue.getVenueID())))
                .andExpect(status().isOk())
                .andExpect(view().name("venue"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void getVenueListReturnsPagedJson() throws Exception {
        Venue venue = saveVenue("Gym B", 180);

        mockMvc.perform(get("/venuelist/getVenueList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].venueID").value(venue.getVenueID()))
                .andExpect(jsonPath("$.content[0].venueName").value("Gym B"));
    }

    @Test
    void venuePageReturnsBadRequestWhenVenueIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/venue")));
        assertTrue(exception.getMessage().contains("venueID"));
    }

    @Test
    void getVenueListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        Venue venue = saveVenue("Gym B", 180);

        mockMvc.perform(get("/venuelist/getVenueList"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].venueID").value(venue.getVenueID()));
    }

    @Test
    void getVenueListThrowsWhenPageIsZero() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/venuelist/getVenueList").param("page", "0")).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void venueListReturnsViewAndFirstPageData() throws Exception {
        saveVenue("Gym A", 200);
        saveVenue("Gym B", 180);

        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attribute("venue_list", hasSize(2)))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void venueListReturnsEmptyPageWhenNoVenueExists() throws Exception {
        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attribute("venue_list", hasSize(0)))
                .andExpect(model().attribute("total", 0));
    }
}
