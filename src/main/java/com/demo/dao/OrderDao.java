package com.demo.dao;

import com.demo.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderDao extends JpaRepository<Order,Integer> {

    Order findByOrderID(int orderID);

    Page<Order> findAllByState(int state,Pageable pageable);

    List<Order> findByVenueIDAndStartTimeIsBetween(int venueID, LocalDateTime startTime, LocalDateTime startTime2);

    List<Order> findAllByStateIn(List<Integer> states);

    Page<Order> findAllByUserID(String userID, Pageable pageable);
}
