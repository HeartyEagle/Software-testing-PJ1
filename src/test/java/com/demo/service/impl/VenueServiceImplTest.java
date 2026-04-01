package com.demo.service.impl;

import com.demo.dao.VenueDao;
import com.demo.entity.Venue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VenueServiceImplTest {

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private VenueServiceImpl venueService;

    @Test
    void findByVenueIdReturnsDaoResult() {
        Venue venue = new Venue();
        when(venueDao.getOne(2)).thenReturn(venue);

        Venue result = venueService.findByVenueID(2);

        assertSame(venue, result);
    }

    @Test
    void findByVenueNameReturnsDaoResult() {
        Venue venue = new Venue();
        when(venueDao.findByVenueName("Gym")).thenReturn(venue);

        Venue result = venueService.findByVenueName("Gym");

        assertSame(venue, result);
    }

    @Test
    void findAllPageDelegatesToDao() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Venue> page = new PageImpl<>(Arrays.asList(new Venue()), pageable, 1);
        when(venueDao.findAll(pageable)).thenReturn(page);

        Page<Venue> result = venueService.findAll(pageable);

        assertSame(page, result);
    }

    @Test
    void findAllListDelegatesToDao() {
        List<Venue> venues = Arrays.asList(new Venue(), new Venue());
        when(venueDao.findAll()).thenReturn(venues);

        List<Venue> result = venueService.findAll();

        assertSame(venues, result);
    }

    @Test
    void createReturnsSavedVenueId() {
        Venue venue = new Venue();
        venue.setVenueID(12);
        when(venueDao.save(venue)).thenReturn(venue);

        int result = venueService.create(venue);

        assertEquals(12, result);
    }

    @Test
    void updateSavesVenue() {
        Venue venue = new Venue();

        venueService.update(venue);

        verify(venueDao).save(venue);
    }

    @Test
    void deleteByIdDelegatesToDao() {
        venueService.delById(9);

        verify(venueDao).deleteById(9);
    }

    @Test
    void countVenueNameReturnsDaoCount() {
        when(venueDao.countByVenueName("Gym")).thenReturn(2);

        int result = venueService.countVenueName("Gym");

        assertEquals(2, result);
    }
}
