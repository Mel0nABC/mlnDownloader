package dev.mel0n.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import dev.mel0n.entity.MlnDownloaderPartFile;

/**
 * Class to implement all options with download speed.
 */
@Service
public class MlnDownloaderSpeedService {

    /**
     * Compara file before and after one second. Generate string with speed in Mb/s
     * 
     * @param mlnDownloaderPartFile downloading part.
     */
    @Async
    public void getSpeedDownloadFromFile(MlnDownloaderPartFile mlnDownloaderPartFile) {

        while (mlnDownloaderPartFile.isDownloading()) {
            try {

                if (Files.exists(Path.of(mlnDownloaderPartFile.getPath()))) {

                    Long startSize = Files.size(Path.of(mlnDownloaderPartFile.getPath()));
                    Thread.sleep(1000);
                    Long endSize = Files.size(Path.of(mlnDownloaderPartFile.getPath()));

                    Long speed = (endSize - startSize);
                    Long speedString = speed;

                    mlnDownloaderPartFile.setSpeedFile(speedString);
                }

            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }

}
