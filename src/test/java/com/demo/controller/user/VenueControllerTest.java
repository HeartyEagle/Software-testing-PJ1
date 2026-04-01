package com.demo.controller.user;

import com.demo.entity.Venue;
import com.demo.service.VenueService;
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
class VenueControllerTest {

    @Mock
    private VenueService venueService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        VenueController controller = new VenueController();
        ReflectionTestUtils.setField(controller, "venueService", venueService);
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setPrefix("/templates/");
        viewResolver.setSuffix(".html");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setViewResolvers(viewResolver)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void venuePageLoadsVenueDetails() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(3);
        venue.setVenueName("Gym A");
        when(venueService.findByVenueID(3)).thenReturn(venue);

        mockMvc.perform(get("/venue").param("venueID", "3"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void getVenueListReturnsPagedJson() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(6);
        venue.setVenueName("Gym B");
        when(venueService.findAll(any())).thenReturn(new PageImpl<>(Collections.singletonList(venue)));

        mockMvc.perform(get("/venuelist/getVenueList").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].venueID").value(6))
                .andExpect(jsonPath("$.content[0].venueName").value("Gym B"));
    }

    @Test
    void venueListReturnsViewAndFirstPageData() throws Exception {
        Venue first = new Venue();
        first.setVenueID(1);
        Venue second = new Venue();
        second.setVenueID(2);
        when(venueService.findAll(any())).thenReturn(new PageImpl<>(Arrays.asList(first, second)));

        mockMvc.perform(get("/venue_list"))
                .andExpect(status().isOk())
                .andExpect(view().name("venue_list"))
                .andExpect(model().attributeExists("venue_list"))
                .andExpect(model().attributeExists("total"));
    }
}
