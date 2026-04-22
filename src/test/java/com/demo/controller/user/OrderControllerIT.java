package com.demo.controller.user;

import com.demo.controller.AbstractControllerIT;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.exception.LoginException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.util.NestedServletException;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class OrderControllerIT extends AbstractControllerIT {

    @BeforeEach
    void setUpData() {
        cleanDatabase();
    }

    @Test
    void orderManageReturnsViewForLoggedInUser() throws Exception {
        User user = saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        saveOrder("alice", venue.getVenueID(), confirmedOrderState());

        mockMvc.perform(get("/order_manage").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attribute("total", 1));
    }

    @Test
    void orderPlaceDoLoadsVenueIntoModel() throws Exception {
        Venue venue = saveVenue("Gym A", 200);

        mockMvc.perform(get("/order_place.do").param("venueID", String.valueOf(venue.getVenueID())))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void orderPlaceDoReturnsBadRequestWhenVenueIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/order_place.do")));
        assertTrue(exception.getMessage().contains("venueID"));
    }

    @Test
    void orderPlaceReturnsPlainView() throws Exception {
        mockMvc.perform(get("/order_place"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"));
    }

    @Test
    void getOrderListReturnsCurrentUsersOrders() throws Exception {
        User user = saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order order = saveOrder("alice", venue.getVenueID(), confirmedOrderState());

        mockMvc.perform(get("/getOrderList.do")
                        .param("page", "1")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(order.getOrderID()))
                .andExpect(jsonPath("$[0].venueName").value("Gym A"));
    }

    @Test
    void getOrderListUsesDefaultFirstPageWhenPageIsOmitted() throws Exception {
        User user = saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order order = saveOrder("alice", venue.getVenueID(), confirmedOrderState());

        mockMvc.perform(get("/getOrderList.do").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(order.getOrderID()));
    }

    @Test
    void getOrderListThrowsWhenPageIsZero() {
        User user = saveUser("alice", "Alice", 0);

        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/getOrderList.do")
                        .param("page", "0")
                        .sessionAttr("user", user)).andReturn());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void addOrderParsesStartTimeAndPersistsOrder() throws Exception {
        User user = saveUser("alice", "Alice", 0);
        saveVenue("Gym A", 200);

        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Gym A")
                        .param("date", "2026-04-10")
                        .param("startTime", "2026-04-10 09:00")
                        .param("hours", "2")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        Order saved = orderDao.findAllByUserID("alice", PageRequest.of(0, 10)).getContent().get(0);
        assertEquals(LocalDateTime.of(2026, 4, 10, 9, 0), saved.getStartTime());
        assertEquals(2, saved.getHours());
        assertEquals(pendingOrderState(), saved.getState());
        assertEquals(400, saved.getTotal());
    }

    @Test
    void finishOrderUpdatesPersistedState() throws Exception {
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order order = saveOrder("alice", venue.getVenueID(), confirmedOrderState());

        mockMvc.perform(post("/finishOrder.do").param("orderID", String.valueOf(order.getOrderID())))
                .andExpect(status().isOk());

        assertEquals(finishedOrderState(), orderDao.findByOrderID(order.getOrderID()).getState());
    }

    @Test
    void finishOrderShouldRequireLogin() {
        Venue venue = saveVenue("Gym A", 200);
        Order order = saveOrder("alice", venue.getVenueID(), confirmedOrderState());

        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/finishOrder.do")
                        .param("orderID", String.valueOf(order.getOrderID()))).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void finishOrderShouldNotAllowCompletingAnotherUsersOrder() throws Exception {
        User attacker = saveUser("mallory", "Mallory", 0);
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order victimOrder = saveOrder("alice", venue.getVenueID(), confirmedOrderState());

        mockMvc.perform(post("/finishOrder.do")
                        .param("orderID", String.valueOf(victimOrder.getOrderID()))
                        .sessionAttr("user", attacker))
                .andExpect(status().isOk());

        assertEquals(confirmedOrderState(), orderDao.findByOrderID(victimOrder.getOrderID()).getState(),
                "another user should not be able to finish the victim's order");
    }

    @Test
    void modifyOrderDoLoadsExistingOrder() throws Exception {
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order order = saveOrder("alice", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(get("/modifyOrder.do").param("orderID", String.valueOf(order.getOrderID())))
                .andExpect(status().isOk())
                .andExpect(view().name("order_edit"))
                .andExpect(model().attribute("order", hasProperty("orderID", is(order.getOrderID()))))
                .andExpect(model().attribute("order", hasProperty("venueID", is(venue.getVenueID()))))
                .andExpect(model().attribute("venue", hasProperty("venueID", is(venue.getVenueID()))))
                .andExpect(model().attribute("venue", hasProperty("venueName", is("Gym A"))));
    }

    @Test
    void modifyOrderDoReturnsBadRequestWhenOrderIdIsMissing() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/modifyOrder.do")));
        assertTrue(exception.getMessage().contains("orderID"));
    }

    @Test
    void modifyOrderDoShouldRequireLogin() {
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order order = saveOrder("alice", venue.getVenueID(), pendingOrderState());

        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/modifyOrder.do")
                        .param("orderID", String.valueOf(order.getOrderID()))).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void modifyOrderUpdatesPersistedOrder() throws Exception {
        User user = saveUser("bob", "Bob", 0);
        Venue originalVenue = saveVenue("Gym A", 200);
        Venue newVenue = saveVenue("Gym B", 300);
        Order order = saveOrder("bob", originalVenue.getVenueID(), confirmedOrderState());

        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Gym B")
                        .param("date", "2026-05-01")
                        .param("startTime", "2026-05-01 14:00")
                        .param("hours", "3")
                        .param("orderID", String.valueOf(order.getOrderID()))
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        Order updated = orderDao.findByOrderID(order.getOrderID());
        assertEquals(newVenue.getVenueID(), updated.getVenueID());
        assertEquals(LocalDateTime.of(2026, 5, 1, 14, 0), updated.getStartTime());
        assertEquals(3, updated.getHours());
        assertEquals(pendingOrderState(), updated.getState());
        assertEquals(900, updated.getTotal());
    }

    @Test
    void modifyOrderShouldNotAllowUpdatingAnotherUsersOrder() throws Exception {
        User attacker = saveUser("mallory", "Mallory", 0);
        saveUser("alice", "Alice", 0);
        Venue originalVenue = saveVenue("Gym A", 200);
        Venue newVenue = saveVenue("Gym B", 300);
        Order victimOrder = saveOrder("alice", originalVenue.getVenueID(), confirmedOrderState());

        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Gym B")
                        .param("date", "2026-05-01")
                        .param("startTime", "2026-05-01 14:00")
                        .param("hours", "3")
                        .param("orderID", String.valueOf(victimOrder.getOrderID()))
                        .sessionAttr("user", attacker))
                .andExpect(status().is3xxRedirection());

        Order untouched = orderDao.findByOrderID(victimOrder.getOrderID());
        assertEquals(originalVenue.getVenueID(), untouched.getVenueID(),
                "another user should not be able to modify the victim's order");
        assertEquals("alice", untouched.getUserID(),
                "another user's order should remain owned by the victim");
    }

    @Test
    void deleteOrderRemovesPersistedOrder() throws Exception {
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order order = saveOrder("alice", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(post("/delOrder.do").param("orderID", String.valueOf(order.getOrderID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        assertFalse(orderDao.findById(order.getOrderID()).isPresent());
    }

    @Test
    void deleteOrderShouldNotAllowDeletingAnotherUsersOrder() throws Exception {
        User attacker = saveUser("mallory", "Mallory", 0);
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym A", 200);
        Order victimOrder = saveOrder("alice", venue.getVenueID(), pendingOrderState());

        mockMvc.perform(post("/delOrder.do")
                        .param("orderID", String.valueOf(victimOrder.getOrderID()))
                        .sessionAttr("user", attacker))
                .andExpect(status().isOk());

        assertTrue(orderDao.findById(victimOrder.getOrderID()).isPresent(),
                "another user should not be able to delete the victim's order");
    }

    @Test
    void getOrderReturnsVenueAndOrdersForSelectedDate() throws Exception {
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym C", 250);
        Order order = saveOrder("alice", venue.getVenueID(), confirmedOrderState(), LocalDateTime.of(2026, 5, 2, 10, 0), 2);

        mockMvc.perform(get("/order/getOrderList.do")
                        .param("venueName", "Gym C")
                        .param("date", "2026-05-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue.venueID").value(venue.getVenueID()))
                .andExpect(jsonPath("$.orders[0].orderID").value(order.getOrderID()))
                .andExpect(jsonPath("$.orders[0].venueID").value(venue.getVenueID()));
    }

    @Test
    void getOrderReturnsEmptyOrdersWhenNoReservationExistsOnThatDate() throws Exception {
        saveUser("alice", "Alice", 0);
        Venue venue = saveVenue("Gym C", 250);

        mockMvc.perform(get("/order/getOrderList.do")
                        .param("venueName", "Gym C")
                        .param("date", "2026-05-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue.venueID").value(venue.getVenueID()))
                .andExpect(jsonPath("$.orders").isEmpty());
    }

    @Test
    void getOrderShouldHandleUnknownVenueGracefully() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/order/getOrderList.do")
                        .param("venueName", "Missing Gym")
                        .param("date", "2026-05-02")).andReturn());
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void addOrderShouldRespectSubmittedDateParameter() throws Exception {
        User user = saveUser("alice", "Alice", 0);
        saveVenue("Gym A", 200);

        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Gym A")
                        .param("date", "2026-04-10")
                        .param("startTime", "2026-04-11 09:00")
                        .param("hours", "2")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection());

        Order saved = orderDao.findAllByUserID("alice", PageRequest.of(0, 10)).getContent().get(0);
        assertEquals(LocalDateTime.of(2026, 4, 10, 9, 0), saved.getStartTime(),
                "the saved order should combine the submitted date with the submitted start time");
    }

    @Test
    void orderManageRequiresLogin() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/order_manage")).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void getOrderListRequiresLogin() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(get("/getOrderList.do").param("page", "1")).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void addOrderRequiresLogin() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Gym A")
                        .param("date", "2026-04-10")
                        .param("startTime", "2026-04-10 09:00")
                        .param("hours", "2")).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }

    @Test
    void modifyOrderRequiresLogin() {
        NestedServletException exception = assertThrows(NestedServletException.class,
                () -> mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Gym B")
                        .param("date", "2026-05-01")
                        .param("startTime", "2026-05-01 14:00")
                        .param("hours", "3")
                        .param("orderID", "1")).andReturn());
        assertTrue(exception.getCause() instanceof LoginException);
    }
}
