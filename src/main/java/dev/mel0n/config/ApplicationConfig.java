/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.mel0n.entity.MlnDownloaderDownloadFile;
import dev.mel0n.service.MlnDownloaderDiscService;
import dev.mel0n.service.MlnDownloaderDownloadService;
import dev.mel0n.service.MlnDownloaderNotificationService;

@Configuration
public class ApplicationConfig {

    @Bean
    public CommandLineRunner runner(MlnDownloaderDownloadService mlnDownloaderService,
            MlnDownloaderDiscService mlnDownloaderDiscService,
            MlnDownloaderNotificationService mlnDownloaderNotificationService) {
        return args -> {
            System.out.println("################################# Loading data #################################");

            Path fileDataBase = Path.of("data.bin");

            if (Files.exists(fileDataBase)) {
                try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileDataBase.toString()))) {

                    @SuppressWarnings("unchecked")
                    List<MlnDownloaderDownloadFile> list = (List<MlnDownloaderDownloadFile>) in.readObject();

                    mlnDownloaderService.setMlnDownloadList(list);
                }

                mlnDownloaderService.getMlnDownloadList().forEach(mln -> {

                    mln.setFileExist(Files.exists(Path.of(mln.getFilePath())));

                    if (mln.isFileExist())
                        try {
                            mln.setDownloadedBytes(Files.size(Path.of(mln.getFilePath())));

                            if (mln.getDownloadedBytes().equals(mln.getLength())) {
                                mln.setDownloaded(true);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    System.out.println(mln.getFilePath() + " - File exist: " + mln.isFileExist());

                    System.out.println("    PARTS: ");

                    mln.getParts().forEach(p -> {

                        boolean exist = Files.exists(Path.of(p.getPath()));

                        System.out.println("        " + p.getPath() + ", Files exist: " + exist);

                        if (exist) {

                            try {
                                p.setActualSize(Files.size(Path.of(p.getPath())));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    System.out.println(
                            "-------------------------------------------------------------------------------------");

                });
            } else {
                System.out.println("No hay un fichero data.bin para cargar");
            }

            System.out.println("################################################################################");

        };
    }
}
