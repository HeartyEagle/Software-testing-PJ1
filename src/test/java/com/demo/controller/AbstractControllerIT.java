package com.demo.controller;

import com.demo.dao.MessageDao;
import com.demo.dao.NewsDao;
import com.demo.dao.OrderDao;
import com.demo.dao.UserDao;
import com.demo.dao.VenueDao;
import com.demo.demoApplication;
import com.demo.entity.Message;
import com.demo.entity.News;
import com.demo.entity.Order;
import com.demo.entity.User;
import com.demo.entity.Venue;
import com.demo.service.MessageService;
import com.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Objects;

@SpringBootTest(classes = demoApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractControllerIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected UserDao userDao;

    @Autowired
    protected VenueDao venueDao;

    @Autowired
    protected NewsDao newsDao;

    @Autowired
    protected OrderDao orderDao;

    @Autowired
    protected MessageDao messageDao;

    protected void cleanDatabase() {
        orderDao.deleteAll();
        messageDao.deleteAll();
        newsDao.deleteAll();
        venueDao.deleteAll();
        userDao.deleteAll();
    }

    protected User saveUser(String userID, String userName, int isadmin) {
        return saveUser(userID, userName, "pwd", isadmin);
    }

    protected User saveUser(String userID, String userName, String password, int isadmin) {
        User user = new User();
        user.setUserID(userID);
        user.setUserName(userName);
        user.setPassword(password);
        user.setEmail(userID + "@test.com");
        user.setPhone("123456");
        user.setIsadmin(isadmin);
        user.setPicture("");
        return userDao.save(user);
    }

    protected Venue saveVenue(String venueName, int price) {
        Venue venue = new Venue();
        venue.setVenueName(venueName);
        venue.setDescription(venueName + " description");
        venue.setPrice(price);
        venue.setPicture("");
        venue.setAddress("Shanghai");
        venue.setOpen_time("09:00");
        venue.setClose_time("18:00");
        return venueDao.save(venue);
    }

    protected News saveNews(String title, String content) {
        News news = new News();
        news.setTitle(title);
        news.setContent(content);
        news.setTime(LocalDateTime.now().minusDays(1));
        return newsDao.save(news);
    }

    protected Message saveMessage(String userID, String content, int state) {
        return saveMessage(userID, content, state, LocalDateTime.now().minusHours(1));
    }

    protected Message saveMessage(String userID, String content, int state, LocalDateTime time) {
        Message message = new Message();
        message.setUserID(userID);
        message.setContent(content);
        message.setState(state);
        message.setTime(time);
        return messageDao.save(message);
    }

    protected Order saveOrder(String userID, int venueID, int state) {
        return saveOrder(userID, venueID, state, LocalDateTime.now().plusDays(1), 2);
    }

    protected Order saveOrder(String userID, int venueID, int state, LocalDateTime startTime, int hours) {
        Order order = new Order();
        order.setUserID(userID);
        order.setVenueID(venueID);
        order.setState(state);
        order.setOrderTime(LocalDateTime.now().minusHours(2));
        order.setStartTime(startTime);
        order.setHours(hours);
        order.setTotal(hours * venueDao.findByVenueID(venueID).getPrice());
        return orderDao.save(order);
    }

    protected static int pendingMessageState() {
        return MessageService.STATE_NO_AUDIT;
    }

    protected static int passedMessageState() {
        return MessageService.STATE_PASS;
    }

    protected static int rejectedMessageState() {
        return MessageService.STATE_REJECT;
    }

    protected static int pendingOrderState() {
        return OrderService.STATE_NO_AUDIT;
    }

    protected static int confirmedOrderState() {
        return OrderService.STATE_WAIT;
    }

    protected static int finishedOrderState() {
        return OrderService.STATE_FINISH;
    }

    protected static int rejectedOrderState() {
        return OrderService.STATE_REJECT;
    }

    protected Path resolveStaticPath(String relativePath) throws Exception {
        return Path.of(Objects.requireNonNull(
                Thread.currentThread().getContextClassLoader().getResource("static")).toURI())
                .resolve(relativePath);
    }
}
