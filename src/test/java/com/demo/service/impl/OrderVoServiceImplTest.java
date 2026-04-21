package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderVoServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderVoServiceImpl orderVoService;

    @Test
    void returnOrderVoByOrderIdCombinesOrderAndVenue() {
        LocalDateTime orderTime = LocalDateTime.of(2026, 4, 6, 10, 0);
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 8, 14, 0);
        Order order = new Order(12, "alice", 7, 2, orderTime, startTime, 3, 600);
        Venue venue = new Venue();
        venue.setVenueID(7);
        venue.setVenueName("Gym A");
        when(orderDao.findByOrderID(12)).thenReturn(order);
        when(venueDao.findByVenueID(7)).thenReturn(venue);

        OrderVo result = orderVoService.returnOrderVoByOrderID(12);

        assertEquals(12, result.getOrderID());
        assertEquals("alice", result.getUserID());
        assertEquals(7, result.getVenueID());
        assertEquals("Gym A", result.getVenueName());
        assertEquals(2, result.getState());
        assertEquals(orderTime, result.getOrderTime());
        assertEquals(startTime, result.getStartTime());
        assertEquals(3, result.getHours());
        assertEquals(600, result.getTotal());
    }

    @Test
    void returnVoBuildsVoForEveryOrder() {
        Order first = new Order();
        first.setOrderID(1);
        first.setVenueID(10);
        Order second = new Order();
        second.setOrderID(2);
        second.setVenueID(20);
        Venue venueA = new Venue();
        venueA.setVenueID(10);
        venueA.setVenueName("A");
        Venue venueB = new Venue();
        venueB.setVenueID(20);
        venueB.setVenueName("B");
        when(orderDao.findByOrderID(1)).thenReturn(first);
        when(orderDao.findByOrderID(2)).thenReturn(second);
        when(venueDao.findByVenueID(10)).thenReturn(venueA);
        when(venueDao.findByVenueID(20)).thenReturn(venueB);

        List<OrderVo> result = orderVoService.returnVo(Arrays.asList(first, second));

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getVenueName());
        assertEquals("B", result.get(1).getVenueName());
    }

    @Test
    void returnVoReturnsEmptyListWhenInputIsEmpty() {
        List<OrderVo> result = orderVoService.returnVo(Collections.emptyList());

        assertEquals(0, result.size());
    }

    @Test
    void returnOrderVoByOrderIdThrowsWhenOrderMissing() {
        when(orderDao.findByOrderID(404)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> orderVoService.returnOrderVoByOrderID(404));
    }

    @Test
    void returnOrderVoByOrderIdThrowsWhenVenueMissing() {
        Order order = new Order();
        order.setOrderID(12);
        order.setVenueID(7);
        when(orderDao.findByOrderID(12)).thenReturn(order);
        when(venueDao.findByVenueID(7)).thenReturn(null);

        assertThrows(NullPointerException.class, () -> orderVoService.returnOrderVoByOrderID(12));
    }
}
