package com.demo.service.impl;

import com.demo.dao.OrderDao;
import com.demo.dao.VenueDao;
import com.demo.entity.Order;
import com.demo.entity.Venue;
import com.demo.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderDao orderDao;

    @Mock
    private VenueDao venueDao;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void findByIdReturnsOrder() {
        Order order = new Order();
        when(orderDao.getOne(2)).thenReturn(order);

        Order result = orderService.findById(2);

        assertSame(order, result);
    }

    @Test
    void findDateOrderDelegatesToDao() {
        LocalDateTime start = LocalDateTime.of(2026, 4, 1, 10, 0);
        LocalDateTime end = start.plusDays(1);
        List<Order> orders = Arrays.asList(new Order());
        when(orderDao.findByVenueIDAndStartTimeIsBetween(3, start, end)).thenReturn(orders);

        List<Order> result = orderService.findDateOrder(3, start, end);

        assertSame(orders, result);
    }

    @Test
    void findUserOrderDelegatesToDao() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<Order> page = new PageImpl<>(Arrays.asList(new Order()), pageable, 1);
        when(orderDao.findAllByUserID("alice", pageable)).thenReturn(page);

        Page<Order> result = orderService.findUserOrder("alice", pageable);

        assertSame(page, result);
    }

    @Test
    void updateOrderRefreshesCoreFieldsAndSaves() {
        Venue venue = new Venue();
        venue.setVenueID(7);
        venue.setPrice(180);
        Order order = new Order();
        order.setOrderID(15);
        when(venueDao.findByVenueName("Gym A")).thenReturn(venue);
        when(orderDao.findByOrderID(15)).thenReturn(order);
        LocalDateTime startTime = LocalDateTime.of(2026, 4, 2, 9, 0);

        orderService.updateOrder(15, "Gym A", startTime, 3, "alice");

        assertEquals(OrderService.STATE_NO_AUDIT, order.getState());
        assertEquals(3, order.getHours());
        assertEquals(7, order.getVenueID());
        assertEquals(startTime, order.getStartTime());
        assertEquals("alice", order.getUserID());
        assertEquals(540, order.getTotal());
        assertNotNull(order.getOrderTime());
        verify(orderDao).save(order);
    }

    @Test
    void submitBuildsNewOrderAndSavesIt() {
        Venue venue = new Venue();
        venue.setVenueID(6);
        venue.setPrice(200);
        when(venueDao.findByVenueName("Gym B")).thenReturn(venue);
        LocalDateTime startTime = LocalDateTime.of(2026, 5, 3, 14, 0);
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);

        orderService.submit("Gym B", startTime, 2, "bob");

        verify(orderDao).save(captor.capture());
        Order saved = captor.getValue();
        assertEquals(OrderService.STATE_NO_AUDIT, saved.getState());
        assertEquals(2, saved.getHours());
        assertEquals(6, saved.getVenueID());
        assertEquals(startTime, saved.getStartTime());
        assertEquals("bob", saved.getUserID());
        assertEquals(400, saved.getTotal());
        assertNotNull(saved.getOrderTime());
    }

    @Test
    void deleteOrderDelegatesToDao() {
        orderService.delOrder(8);

        verify(orderDao).deleteById(8);
    }

    @Test
    void confirmOrderUpdatesStateWhenOrderExists() {
        Order order = new Order();
        order.setOrderID(5);
        when(orderDao.findByOrderID(5)).thenReturn(order);

        orderService.confirmOrder(5);

        assertEquals(OrderService.STATE_WAIT, order.getState());
        verify(orderDao).save(order);
    }

    @Test
    void finishOrderUpdatesStateWhenOrderExists() {
        Order order = new Order();
        order.setOrderID(4);
        when(orderDao.findByOrderID(4)).thenReturn(order);

        orderService.finishOrder(4);

        assertEquals(OrderService.STATE_FINISH, order.getState());
        verify(orderDao).save(order);
    }

    @Test
    void rejectOrderUpdatesStateWhenOrderExists() {
        Order order = new Order();
        order.setOrderID(3);
        when(orderDao.findByOrderID(3)).thenReturn(order);

        orderService.rejectOrder(3);

        assertEquals(OrderService.STATE_REJECT, order.getState());
        verify(orderDao).save(order);
    }

    @Test
    void confirmOrderThrowsWhenOrderMissing() {
        when(orderDao.findByOrderID(100)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> orderService.confirmOrder(100));
    }

    @Test
    void finishOrderThrowsWhenOrderMissing() {
        when(orderDao.findByOrderID(101)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> orderService.finishOrder(101));
    }

    @Test
    void rejectOrderThrowsWhenOrderMissing() {
        when(orderDao.findByOrderID(102)).thenReturn(null);

        assertThrows(RuntimeException.class, () -> orderService.rejectOrder(102));
    }

    @Test
    void updateOrderThrowsWhenVenueMissing() {
        Order order = new Order();
        when(venueDao.findByVenueName("Missing Gym")).thenReturn(null);
        when(orderDao.findByOrderID(15)).thenReturn(order);

        assertThrows(NullPointerException.class,
                () -> orderService.updateOrder(15, "Missing Gym", LocalDateTime.now(), 2, "alice"));
    }

    @Test
    void updateOrderThrowsWhenOrderMissing() {
        Venue venue = new Venue();
        venue.setVenueID(7);
        venue.setPrice(180);
        when(venueDao.findByVenueName("Gym A")).thenReturn(venue);
        when(orderDao.findByOrderID(999)).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderService.updateOrder(999, "Gym A", LocalDateTime.now(), 2, "alice"));
    }

    @Test
    void submitThrowsWhenVenueMissing() {
        when(venueDao.findByVenueName("Missing Gym")).thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> orderService.submit("Missing Gym", LocalDateTime.now(), 2, "alice"));
    }

    @Test
    void findNoAuditOrderReturnsPage() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(Arrays.asList(new Order()), pageable, 1);
        when(orderDao.findAllByState(OrderService.STATE_NO_AUDIT, pageable)).thenReturn(page);

        Page<Order> result = orderService.findNoAuditOrder(pageable);

        assertSame(page, result);
    }

    @Test
    void findAuditOrderReturnsConfirmedAndFinishedOrders() {
        List<Order> orders = Arrays.asList(new Order(), new Order());
        when(orderDao.findAllByStateIn(Arrays.asList(OrderService.STATE_WAIT, OrderService.STATE_FINISH)))
                .thenReturn(orders);

        List<Order> result = orderService.findAuditOrder();

        assertSame(orders, result);
        verify(orderDao).findAllByStateIn(Arrays.asList(OrderService.STATE_WAIT, OrderService.STATE_FINISH));
    }

    @Test
    void findAuditOrderReturnsEmptyListWhenDaoHasNoMatchingOrder() {
        when(orderDao.findAllByStateIn(Arrays.asList(OrderService.STATE_WAIT, OrderService.STATE_FINISH)))
                .thenReturn(Arrays.asList());

        List<Order> result = orderService.findAuditOrder();

        assertEquals(0, result.size());
    }
}
