/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */

package dev.mel0n;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

import tools.jackson.databind.ObjectMapper;

import dev.mel0n.dto.MlnDownloadderNewEntityDTO;
import dev.mel0n.service.MlnDownloaderDownloadService;

@SpringBootTest
@AutoConfigureMockMvc
public class MlnDownloaderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private URI uri;
    private File filePath, dataFile;
    private String fileName;

    @BeforeEach
    public void setup() {
        try {

            this.dataFile = new File("data.bin");

            this.uri = new URI("https://es.mirrors.cicku.me/archlinux/iso/2026.05.01/archlinux-2026.05.01-x86_64.iso");

            String path = uri.getPath();

            this.fileName = path.substring(path.lastIndexOf('/') + 1);

            filePath = new File(MlnDownloaderDownloadService.getDOWNLOAD_FOLDER() + "/" + fileName);

            if (this.filePath.exists())
                this.filePath.delete();

            if (this.dataFile.exists())
                this.dataFile.delete();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    @AfterEach
    public void clean() {

        if (this.filePath.exists())
            this.filePath.delete();

        if (this.dataFile.exists())
            this.dataFile.delete();
    }

    /**
     * Download arch linux iso to test. Download, check if this exist and delete all
     * temporal files
     */
    @Test
    public void startDownloadTestResultOK() {

        try {

            MlnDownloadderNewEntityDTO mlnDownloadderEntityDTO = MlnDownloadderNewEntityDTO.builder()
                    .uri(this.uri)
                    .chunks(10)
                    .fileName(this.fileName)
                    .build();

            mockMvc.perform(post("/api/downloads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mlnDownloadderEntityDTO)))
                    .andExpect(status().isOk())
                    .andDo(print());

            Thread.sleep(2000);

            while (!Files.exists(filePath.toPath())) {
                Thread.sleep(1000);
            }

            assertTrue(filePath.exists());

            // To delete parts
            Thread.sleep(2000);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Download arch linux iso, after, repeat download arch linux iso
     */
    @Test
    public void downloadTestResultFileAlreadyDownloadederException() {

        try {

            MlnDownloadderNewEntityDTO mlnDownloadderEntityDTO = MlnDownloadderNewEntityDTO.builder()
                    .uri(this.uri)
                    .chunks(10)
                    .fileName(this.fileName)
                    .build();

            mockMvc.perform(post("/api/downloads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mlnDownloadderEntityDTO)))
                    .andExpect(status().isOk())
                    .andDo(print());

            while (!filePath.exists()) {
                Thread.sleep(1000);
            }

            assertTrue(filePath.exists());

            // To delete parts
            Thread.sleep(2000);

            mockMvc.perform(post("/api/downloads")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(mlnDownloadderEntityDTO)))
                    .andExpect(status().isFound())
                    .andDo(print());

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}