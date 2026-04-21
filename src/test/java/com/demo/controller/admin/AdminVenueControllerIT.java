package com.demo.controller.admin;

import com.demo.entity.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminVenueControllerIT extends AbstractAdminControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void venueManageReturnsViewAndTotalPages() throws Exception {
        saveVenue("Court A", 200);

        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void venueEditLoadsPersistedVenue() throws Exception {
        Venue venue = saveVenue("Court A", 200);

        mockMvc.perform(get("/venue_edit").param("venueID", String.valueOf(venue.getVenueID())))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attributeExists("venue"));
    }

    @Test
    void venueEditReturnsBadRequestWhenVenueIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/venue_edit")));
        assertTrue(exception.getMessage().contains("venueID"));
    }

    @Test
    void venueAddReturnsFormView() throws Exception {
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"));
    }

    @Test
    void venueListReturnsPersistedVenues() throws Exception {
        Venue venue = saveVenue("Court A", 200);

        mockMvc.perform(get("/venueList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].venueID").value(venue.getVenueID()))
                .andExpect(jsonPath("$[0].venueName").value("Court A"));
    }

    @Test
    void venueListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        Venue venue = saveVenue("Court A", 200);

        mockMvc.perform(get("/venueList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].venueID").value(venue.getVenueID()));
    }

    @Test
    void venueListThrowsWhenPageIsZero() {
        assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/venueList.do").param("page", "0")).andReturn());
    }

    @Test
    void addVenueCreatesPersistedVenueWithoutPicture() throws Exception {
        MockMultipartFile emptyPicture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/addVenue.do")
                        .file(emptyPicture)
                        .param("venueName", "New Court")
                        .param("address", "Shanghai")
                        .param("description", "Indoor venue")
                        .param("price", "300")
                        .param("open_time", "08:00")
                        .param("close_time", "22:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        Venue created = venueDao.findByVenueName("New Court");
        assertEquals("Indoor venue", created.getDescription());
        assertEquals("", created.getPicture());
    }

    @Test
    void addVenueStoresUploadedPictureWhenProvided() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "court.png", "image/png", new byte[]{1, 2, 3});

        mockMvc.perform(multipart("/addVenue.do")
                        .file(picture)
                        .param("venueName", "Photo Court")
                        .param("address", "Shanghai")
                        .param("description", "With image")
                        .param("price", "280")
                        .param("open_time", "08:00")
                        .param("close_time", "22:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        Venue created = venueDao.findByVenueName("Photo Court");
        assertTrue(created.getPicture().startsWith("file/venue/"));
        assertTrue(created.getPicture().endsWith(".png"));
    }

    @Test
    void modifyVenueUpdatesPersistedVenueWithoutReplacingPicture() throws Exception {
        Venue venue = saveVenue("Court A", 200);
        venue.setPicture("old.png");
        venue = venueDao.save(venue);
        MockMultipartFile emptyPicture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/modifyVenue.do")
                        .file(emptyPicture)
                        .param("venueID", String.valueOf(venue.getVenueID()))
                        .param("venueName", "Court B")
                        .param("address", "Pudong")
                        .param("description", "Updated venue")
                        .param("price", "350")
                        .param("open_time", "09:00")
                        .param("close_time", "21:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        Venue updated = venueDao.findByVenueID(venue.getVenueID());
        assertEquals("Court B", updated.getVenueName());
        assertEquals("Updated venue", updated.getDescription());
        assertEquals("old.png", updated.getPicture());
    }

    @Test
    void modifyVenueReplacesPictureWhenNewPictureIsUploaded() throws Exception {
        Venue venue = saveVenue("Court A", 200);
        venue.setPicture("old.png");
        venue = venueDao.save(venue);
        MockMultipartFile picture = new MockMultipartFile("picture", "new-court.png", "image/png", new byte[]{7, 8, 9});

        mockMvc.perform(multipart("/modifyVenue.do")
                        .file(picture)
                        .param("venueID", String.valueOf(venue.getVenueID()))
                        .param("venueName", "Court A")
                        .param("address", "Pudong")
                        .param("description", "Updated venue")
                        .param("price", "350")
                        .param("open_time", "09:00")
                        .param("close_time", "21:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        Venue updated = venueDao.findByVenueID(venue.getVenueID());
        assertTrue(updated.getPicture().startsWith("file/venue/"));
        assertTrue(!"old.png".equals(updated.getPicture()));
    }

    @Test
    void deleteVenueRemovesPersistedRecord() throws Exception {
        Venue venue = saveVenue("Court A", 200);

        mockMvc.perform(post("/delVenue.do").param("venueID", String.valueOf(venue.getVenueID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertFalse(venueDao.findById(venue.getVenueID()).isPresent());
    }

    @Test
    void checkVenueNameReturnsTrueWhenNameIsAvailable() throws Exception {
        mockMvc.perform(post("/checkVenueName.do").param("venueName", "Available Court"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void checkVenueNameReturnsFalseWhenNameAlreadyExists() throws Exception {
        saveVenue("Taken Court", 200);

        mockMvc.perform(post("/checkVenueName.do").param("venueName", "Taken Court"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(false));
    }
}
