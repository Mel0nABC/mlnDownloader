package dev.mel0n.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import dev.mel0n.dto.MlnDownloaderDownloadFileDTO;
import dev.mel0n.entity.MlnDownloaderDiscInfo;
import dev.mel0n.entity.MlnDownloaderDownloadFile;
import jakarta.annotation.PostConstruct;

/**
 * Service to send some information to boadcast
 */
@Service
public class MlnDownloaderNotificationService {

    private final SimpMessagingTemplate template;
    private final MlnDownloaderDownloadService mlnDownloaderService;
    private final MlnDownloaderDiscService mlnmDownloaderDiscService;

    public MlnDownloaderNotificationService(SimpMessagingTemplate template,
            MlnDownloaderDownloadService mlnDownloaderService, MlnDownloaderDiscService mlnmDownloaderDiscService) {
        this.template = template;
        this.mlnDownloaderService = mlnDownloaderService;
        this.mlnmDownloaderDiscService = mlnmDownloaderDiscService;
    }

    @PostConstruct
    public void startNotificationThread() {
        new Thread(() -> {
            starNotificationThread(mlnDownloaderService);
        }).start();
    }

    /**
     * Generate new thread to notify information
     * 
     * @param mlnDownloaderService obtain list downloads
     */
    public void starNotificationThread(MlnDownloaderDownloadService mlnDownloaderService) {

        while (true) {
            try {
                Thread.sleep(500);

                sendFiles(mlnDownloaderService.getMlnDownloadList().stream().map(MlnDownloaderDownloadFile::toDTO)
                        .toList());

                sendDiscStatus(mlnmDownloaderDiscService.getMlnDownloaderDiscInfo());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notify to topic downloads information
     * 
     * @param list download information list
     */
    public void sendFiles(List<MlnDownloaderDownloadFileDTO> list) {
        template.convertAndSend("/topic/downloads", list);
    }

    /***
     * Notify to topic disc information
     * 
     * @param disc string with free and total space
     */
    public void sendDiscStatus(MlnDownloaderDiscInfo mlnDownloaderDiscInfo) {
        template.convertAndSend("/topic/disc_info", mlnDownloaderDiscInfo);
    }

}
