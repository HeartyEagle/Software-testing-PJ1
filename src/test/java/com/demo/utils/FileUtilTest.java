package com.demo.utils;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.ClassUtils;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilTest {

    @Test
    void saveVenueFileReturnsEmptyStringWhenMultipartFileIsEmpty() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        String result = FileUtil.saveVenueFile(picture);

        assertEquals("", result);
    }

    @Test
    void saveUserFileReturnsEmptyStringWhenMultipartFileIsEmpty() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "", "application/octet-stream", new byte[0]);

        String result = FileUtil.saveUserFile(picture);

        assertEquals("", result);
    }

    @Test
    void saveVenueFilePersistsFileAndKeepsSuffix() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "venue.png", "image/png", new byte[]{1, 2, 3});

        String result = FileUtil.saveVenueFile(picture);

        assertTrue(result.startsWith("file/venue/"));
        assertTrue(result.endsWith(".png"));
        File savedFile = new File(ClassUtils.getDefaultClassLoader().getResource("static").getPath() + "/" + result);
        assertTrue(savedFile.exists());
    }

    @Test
    void saveUserFilePersistsFileAndKeepsSuffix() throws Exception {
        MockMultipartFile picture = new MockMultipartFile("picture", "avatar.png", "image/png", new byte[]{1, 2, 3});

        String result = FileUtil.saveUserFile(picture);

        assertTrue(result.startsWith("file/user/"));
        assertTrue(result.endsWith(".png"));
        File savedFile = new File(ClassUtils.getDefaultClassLoader().getResource("static").getPath() + "/" + result);
        assertTrue(savedFile.exists());
    }
}
