package com.demo.service.impl;

import com.demo.dao.UserDao;
import com.demo.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findByUserIdReturnsDaoResult() {
        User user = new User();
        user.setUserID("alice");
        when(userDao.findByUserID("alice")).thenReturn(user);

        User result = userService.findByUserID("alice");

        assertSame(user, result);
    }

    @Test
    void findByUserIdPageReturnsNonAdminUsers() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(Arrays.asList(new User(), new User()), pageable, 2);
        when(userDao.findAllByIsadmin(0, pageable)).thenReturn(page);

        Page<User> result = userService.findByUserID(pageable);

        assertSame(page, result);
    }

    @Test
    void checkLoginDelegatesToDao() {
        User user = new User();
        user.setUserID("alice");
        when(userDao.findByUserIDAndPassword("alice", "secret")).thenReturn(user);

        User result = userService.checkLogin("alice", "secret");

        assertSame(user, result);
    }

    @Test
    void createReturnsCurrentUserCountAfterSaving() {
        User user = new User();
        when(userDao.findAll()).thenReturn(Arrays.asList(new User(), new User(), new User()));

        int result = userService.create(user);

        verify(userDao).save(user);
        assertEquals(3, result);
    }

    @Test
    void updateUserSavesEntity() {
        User user = new User();

        userService.updateUser(user);

        verify(userDao).save(user);
    }

    @Test
    void countUserIdReturnsDaoCount() {
        when(userDao.countByUserID("alice")).thenReturn(1);

        int result = userService.countUserID("alice");

        assertEquals(1, result);
    }

    @Test
    void deleteByIdDelegatesToDao() {
        userService.delByID(7);

        verify(userDao).deleteById(7);
    }

    @Test
    void findByIdReturnsDaoResult() {
        User user = new User();
        when(userDao.findById(3)).thenReturn(user);

        User result = userService.findById(3);

        assertSame(user, result);
    }
}
