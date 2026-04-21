package com.demo.controller.admin;

import com.demo.entity.Order;
import com.demo.entity.Venue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.NestedServletException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AdminOrderControllerIT extends AbstractAdminControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void reservationManageLoadsCurrentAuditAndPendingOrders() throws Exception {
        Venue venue = saveVenue("Court A", 200);
        saveOrder("user-a", venue.getVenueID(), confirmedOrderState());
        saveOrder("user-b", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attributeExists("order_list"))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void getOrderListReturnsPendingOrders() throws Exception {
        Venue venue = saveVenue("Court A", 200);
        Order order = saveOrder("user-a", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(get("/admin/getOrderList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(order.getOrderID()))
                .andExpect(jsonPath("$[0].venueName").value("Court A"));
    }

    @Test
    void getOrderListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        Venue venue = saveVenue("Court A", 200);
        Order order = saveOrder("user-a", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(get("/admin/getOrderList.do"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(order.getOrderID()));
    }

    @Test
    void getOrderListThrowsWhenPageIsZero() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/admin/getOrderList.do").param("page", "0")).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void passOrderUpdatesDatabaseState() throws Exception {
        Venue venue = saveVenue("Court A", 200);
        Order order = saveOrder("user-a", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(post("/passOrder.do").param("orderID", String.valueOf(order.getOrderID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertEquals(confirmedOrderState(), orderDao.findByOrderID(order.getOrderID()).getState());
    }

    @Test
    void passOrderReturnsBadRequestWhenOrderIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/passOrder.do")));
        assertTrue(exception.getMessage().contains("orderID"));
    }

    @Test
    void rejectOrderUpdatesDatabaseState() throws Exception {
        Venue venue = saveVenue("Court A", 200);
        Order order = saveOrder("user-a", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(post("/rejectOrder.do").param("orderID", String.valueOf(order.getOrderID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertEquals(rejectedOrderState(), orderDao.findByOrderID(order.getOrderID()).getState());
    }
}
