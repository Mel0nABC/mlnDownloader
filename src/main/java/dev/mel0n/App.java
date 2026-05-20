/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import dev.mel0n.service.MlnDownloaderDownloadService;

/**
 * Class to start spring boot application
 */
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        Path pathFolder = MlnDownloaderDownloadService.getDOWNLOAD_FOLDER();

        if (!Files.exists(pathFolder))
            try {
                Files.createDirectory(pathFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }

        SpringApplication.run(App.class, args);
    }
}
