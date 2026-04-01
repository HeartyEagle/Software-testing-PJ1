package com.demo.controller.admin;

import com.demo.entity.Order;
import com.demo.entity.vo.OrderVo;
import com.demo.service.OrderService;
import com.demo.service.OrderVoService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderVoService orderVoService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminOrderController controller = new AdminOrderController();
        ReflectionTestUtils.setField(controller, "orderService", orderService);
        ReflectionTestUtils.setField(controller, "orderVoService", orderVoService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void reservationManageReturnsViewWithOrders() throws Exception {
        Order order = new Order();
        OrderVo orderVo = new OrderVo();
        when(orderService.findAuditOrder()).thenReturn(Collections.singletonList(order));
        when(orderVoService.returnVo(Collections.singletonList(order))).thenReturn(Collections.singletonList(orderVo));
        when(orderService.findNoAuditOrder(any())).thenReturn(new PageImpl<>(Collections.singletonList(order)));

        mockMvc.perform(get("/reservation_manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reservation_manage"))
                .andExpect(model().attributeExists("order_list"))
                .andExpect(model().attributeExists("total"));
    }

    @Test
    void getOrderListReturnsPendingOrders() throws Exception {
        Order order = new Order();
        OrderVo orderVo = new OrderVo();
        orderVo.setOrderID(3);
        when(orderService.findNoAuditOrder(any())).thenReturn(new PageImpl<>(Collections.singletonList(order)));
        when(orderVoService.returnVo(Collections.singletonList(order))).thenReturn(Collections.singletonList(orderVo));

        mockMvc.perform(get("/admin/getOrderList.do").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderID").value(3));
    }

    @Test
    void passOrderDelegatesToService() throws Exception {
        mockMvc.perform(post("/passOrder.do").param("orderID", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(orderService).confirmOrder(6);
    }

    @Test
    void rejectOrderDelegatesToService() throws Exception {
        mockMvc.perform(post("/rejectOrder.do").param("orderID", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(orderService).rejectOrder(7);
    }
}
