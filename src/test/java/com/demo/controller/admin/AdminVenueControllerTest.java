package com.demo.controller.admin;

import com.demo.entity.Venue;
import com.demo.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AdminVenueControllerTest {

    @Mock
    private VenueService venueService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminVenueController controller = new AdminVenueController();
        ReflectionTestUtils.setField(controller, "venueService", venueService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void venueManageReturnsViewAndTotalPages() throws Exception {
        when(venueService.findAll(any())).thenReturn(new PageImpl<>(Collections.singletonList(new Venue())));

        mockMvc.perform(get("/venue_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/venue_manage"))
                .andExpect(model().attributeExists("total"));
    }

    @Test
    void venueEditLoadsVenue() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(7);
        when(venueService.findByVenueID(7)).thenReturn(venue);

        mockMvc.perform(get("/venue_edit").param("venueID", "7"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_edit"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void venueAddReturnsFormView() throws Exception {
        mockMvc.perform(get("/venue_add"))
                .andExpect(status().isOk())
                .andExpect(view().name("/admin/venue_add"));
    }

    @Test
    void venueListReturnsPagedVenues() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(8);
        venue.setVenueName("Hall");
        when(venueService.findAll(any())).thenReturn(new PageImpl<>(Collections.singletonList(venue)));

        mockMvc.perform(get("/venueList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].venueID").value(8))
                .andExpect(jsonPath("$[0].venueName").value("Hall"));
    }

    @Test
    void addVenueCreatesVenueWithoutPictureAndRedirects() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);
        when(venueService.create(any(Venue.class))).thenReturn(1);
        ArgumentCaptor<Venue> captor = ArgumentCaptor.forClass(Venue.class);

        mockMvc.perform(multipart("/addVenue.do")
                        .file(picture)
                        .param("venueName", "Gym A")
                        .param("address", "Shanghai")
                        .param("description", "desc")
                        .param("price", "200")
                        .param("open_time", "09:00")
                        .param("close_time", "18:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        verify(venueService).create(captor.capture());
        assertEquals("Gym A", captor.getValue().getVenueName());
        assertEquals("", captor.getValue().getPicture());
    }

    @Test
    void modifyVenueUpdatesExistingVenueAndRedirects() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(9);
        venue.setPicture("old.png");
        when(venueService.findByVenueID(9)).thenReturn(venue);
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/modifyVenue.do")
                        .file(picture)
                        .param("venueID", "9")
                        .param("venueName", "Gym B")
                        .param("address", "Pudong")
                        .param("description", "new desc")
                        .param("price", "300")
                        .param("open_time", "08:00")
                        .param("close_time", "20:00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("venue_manage"));

        verify(venueService).update(venue);
        assertEquals("Gym B", venue.getVenueName());
        assertEquals("Pudong", venue.getAddress());
        assertEquals("old.png", venue.getPicture());
    }

    @Test
    void deleteVenueDelegatesToService() throws Exception {
        mockMvc.perform(post("/delVenue.do").param("venueID", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(venueService).delById(11);
    }

    @Test
    void checkVenueNameReturnsTrueWhenNameAvailable() throws Exception {
        when(venueService.countVenueName("new venue")).thenReturn(0);

        mockMvc.perform(post("/checkVenueName.do").param("venueName", "new venue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }
}
