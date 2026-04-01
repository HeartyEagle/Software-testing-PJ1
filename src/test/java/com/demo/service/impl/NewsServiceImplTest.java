package com.demo.service.impl;

import com.demo.dao.NewsDao;
import com.demo.entity.News;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceImplTest {

    @Mock
    private NewsDao newsDao;

    @InjectMocks
    private NewsServiceImpl newsService;

    @Test
    void findAllReturnsPagedNews() {
        PageRequest pageable = PageRequest.of(0, 5);
        Page<News> page = new PageImpl<>(Arrays.asList(new News()), pageable, 1);
        when(newsDao.findAll(pageable)).thenReturn(page);

        Page<News> result = newsService.findAll(pageable);

        assertSame(page, result);
    }

    @Test
    void findByIdReturnsNews() {
        News news = new News();
        when(newsDao.getOne(4)).thenReturn(news);

        News result = newsService.findById(4);

        assertSame(news, result);
    }

    @Test
    void createReturnsGeneratedId() {
        News news = new News();
        news.setNewsID(6);
        when(newsDao.save(news)).thenReturn(news);

        int result = newsService.create(news);

        assertEquals(6, result);
    }

    @Test
    void deleteByIdDelegatesToDao() {
        newsService.delById(8);

        verify(newsDao).deleteById(8);
    }

    @Test
    void updateSavesNews() {
        News news = new News();

        newsService.update(news);

        verify(newsDao).save(news);
    }
}
