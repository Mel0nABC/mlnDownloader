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

    /**
     * Before start spring boot, check if download folder exist, when create or
     * exist, change owner and grop with PUID and PGID set in docker-compose.yaml if
     * yaml not exist, get user from system host
     * 
     * @param args
     */
    public static void main(String[] args) {
        Path pathFolder = MlnDownloaderDownloadService.getDOWNLOAD_FOLDER();

        String ownerName = System.getenv("PUID");
        String groupName = System.getenv("PGID");

        if (ownerName == null) {
            ownerName = System.getenv("USER");
        }

        if (groupName == null) {
            groupName = System.getenv("USER");
        }

        if (!Files.exists(pathFolder)) {

            try {
                Files.createDirectory(pathFolder);

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        try {
            System.out.println("SET NEW OWNER:GROUP" + ownerName + " - " + groupName);
            Process p = new ProcessBuilder("chown", ownerName + ":" + groupName, pathFolder.toString())
                    .inheritIO()
                    .start();

            int code = p.waitFor();
            System.out.println("CHOWN exit code: " + code);

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SpringApplication.run(App.class, args);
    }
}
