package dev.mel0n.service;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import dev.mel0n.entity.MlnDownloaderDiscInfo;
import lombok.Getter;

/**
 * Service to monitorize local drive status
 */
@Service
@Getter
public class MlnDownloaderDiscService {

    private final MlnDownloaderDownloadService mlnDownloaderService;
    private MlnDownloaderDiscInfo mlnDownloaderDiscInfo;

    public MlnDownloaderDiscService(MlnDownloaderDownloadService mlnDownloaderService) {
        this.mlnDownloaderService = mlnDownloaderService;

        try {
            Path path = MlnDownloaderDownloadService.getDOWNLOAD_FOLDER();

            FileStore fileStore = Files.getFileStore(path);

            long freeSpace = fileStore.getUsableSpace();

            mlnDownloaderDiscInfo = MlnDownloaderDiscInfo.builder()
                    .path(path)
                    .totalSpace(fileStore.getTotalSpace())
                    .freeSpace(freeSpace)
                    .isReadable(Files.isReadable(path))
                    .isWritable(Files.isWritable(path))
                    .isExecutable(Files.isExecutable(path))
                    .build();

            System.out.println("Control de almacenamiento iniciado");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        controlFreeSpaceToFinishDownloads(mlnDownloaderDiscInfo, this.mlnDownloaderService);
    }

    /**
     * Class to control free space, stop all downloads if don't have space to
     * download all
     * 
     * @param mlnDownloaderDiscInfo object from main thread
     */
    public void controlFreeSpaceToFinishDownloads(MlnDownloaderDiscInfo mlnDownloaderDiscInfo,
            MlnDownloaderDownloadService mlnDownloaderService) {

        new Thread(() -> {

            while (true) {
                try {

                    Thread.sleep(1000);

                    Path path = MlnDownloaderDownloadService.getDOWNLOAD_FOLDER();

                    FileStore fileStore = Files.getFileStore(path);

                    long freeSpace = fileStore.getUsableSpace();

                    mlnDownloaderDiscInfo.setTotalSpace(fileStore.getTotalSpace());
                    mlnDownloaderDiscInfo.setFreeSpace(freeSpace);
                    mlnDownloaderDiscInfo.setWritable(Files.isWritable(path));
                    mlnDownloaderDiscInfo.setReadable(Files.isReadable(path));
                    mlnDownloaderDiscInfo.setReadable(Files.isReadable(path));
                    mlnDownloaderDiscInfo.setSpaceSuficient(true);

                    Long allPartsSize = mlnDownloaderService.getMlnDownloadList().stream()
                            .flatMap(mln -> mln.getParts().stream())
                            .mapToLong(part -> part.getLength() - part.getActualSize()).sum();

                    if (freeSpace < allPartsSize.longValue()) {

                        mlnDownloaderDiscInfo.setSpaceSuficient(false);

                        mlnDownloaderService.getMlnDownloadList().forEach(mln -> {
                            if (mln.isDownloading()) {
                                mlnDownloaderService.pauseOrResumeDownload(mln.getId());
                            }
                        });

                    }

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }

            }

        }).start();
    }

}
