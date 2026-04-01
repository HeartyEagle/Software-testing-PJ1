package com.demo.controller.user;

import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.entity.vo.OrderVo;
import com.demo.entity.vo.VenueOrder;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
import com.demo.service.VenueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderVoService orderVoService;

    @Mock
    private VenueService venueService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "orderVoService", orderVoService);
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
    void orderManageReturnsViewForLoggedInUser() throws Exception {
        User user = new User();
        user.setUserID("alice");
        when(orderService.findUserOrder(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(new Order())));

        mockMvc.perform(get("/order_manage").sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(view().name("order_manage"))
                .andExpect(model().attributeExists("total"));
    }

    @Test
    void orderPlaceDoLoadsVenueIntoModel() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(4);
        venue.setVenueName("Gym A");
        when(venueService.findByVenueID(4)).thenReturn(venue);

        mockMvc.perform(get("/order_place.do").param("venueID", "4"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void orderPlaceReturnsPlainView() throws Exception {
        mockMvc.perform(get("/order_place"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_place"));
    }

    @Test
    void getOrderListReturnsCurrentUsersOrders() throws Exception {
        User user = new User();
        user.setUserID("alice");
        Order order = new Order();
        order.setOrderID(1);
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderID(1);
        when(orderService.findUserOrder(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(order)));
        when(orderVoService.returnVo(Collections.singletonList(order))).thenReturn(Collections.singletonList(orderVo));

        mockMvc.perform(get("/getOrderList.do")
                        .param("page", "1")
                        .sessionAttr("user", user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(1));
    }

    @Test
    void addOrderParsesStartTimeAndRedirects() throws Exception {
        User user = new User();
        user.setUserID("alice");
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        mockMvc.perform(post("/addOrder.do")
                        .param("venueName", "Gym A")
                        .param("date", "2026-04-10")
                        .param("startTime", "2026-04-10 09:00")
                        .param("hours", "2")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        verify(orderService).submit(org.mockito.Mockito.eq("Gym A"), timeCaptor.capture(), org.mockito.Mockito.eq(2), org.mockito.Mockito.eq("alice"));
        assertEquals(LocalDateTime.of(2026, 4, 10, 9, 0), timeCaptor.getValue());
    }

    @Test
    void finishOrderDelegatesToService() throws Exception {
        mockMvc.perform(post("/finishOrder.do").param("orderID", "12"))
                .andExpect(status().isOk());

        verify(orderService).finishOrder(12);
    }

    @Test
    void modifyOrderDoLoadsExistingOrder() throws Exception {
        Order order = new Order();
        order.setOrderID(9);
        order.setVenueID(3);
        Venue venue = new Venue();
        venue.setVenueID(3);
        when(orderService.findById(9)).thenReturn(order);
        when(venueService.findByVenueID(3)).thenReturn(venue);

        mockMvc.perform(get("/modifyOrder.do").param("orderID", "9"))
                .andExpect(status().isOk())
                .andExpect(view().name("order_edit"))
                .andExpect(model().attribute("order", order))
                .andExpect(model().attribute("venue", venue));
    }

    @Test
    void modifyOrderParsesTimeAndReturnsTrue() throws Exception {
        User user = new User();
        user.setUserID("bob");
        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        mockMvc.perform(post("/modifyOrder")
                        .param("venueName", "Gym B")
                        .param("date", "2026-05-01")
                        .param("startTime", "2026-05-01 14:00")
                        .param("hours", "3")
                        .param("orderID", "18")
                        .sessionAttr("user", user))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("order_manage"));

        verify(orderService).updateOrder(org.mockito.Mockito.eq(18), org.mockito.Mockito.eq("Gym B"), timeCaptor.capture(), org.mockito.Mockito.eq(3), org.mockito.Mockito.eq("bob"));
        assertEquals(LocalDateTime.of(2026, 5, 1, 14, 0), timeCaptor.getValue());
    }

    @Test
    void deleteOrderReturnsTrue() throws Exception {
        mockMvc.perform(post("/delOrder.do").param("orderID", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(orderService).delOrder(20);
    }

    @Test
    void getOrderReturnsVenueAndOrdersForSelectedDate() throws Exception {
        Venue venue = new Venue();
        venue.setVenueID(7);
        venue.setVenueName("Gym C");
        Order order = new Order();
        order.setOrderID(25);
        when(venueService.findByVenueName("Gym C")).thenReturn(venue);
        when(orderService.findDateOrder(org.mockito.Mockito.eq(7), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.singletonList(order));

        mockMvc.perform(get("/order/getOrderList.do")
                        .param("venueName", "Gym C")
                        .param("date", "2026-05-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.venue.venueID").value(7))
                .andExpect(jsonPath("$.orders", hasSize(1)))
                .andExpect(jsonPath("$.orders[0].orderID").value(25));
    }
}
