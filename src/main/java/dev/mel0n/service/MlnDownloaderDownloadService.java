package dev.mel0n.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import dev.mel0n.dto.MlnDownloadderNewEntityDTO;
import dev.mel0n.dto.MlnDownloaderChangeSpeedLimitDTO;
import dev.mel0n.entity.MlnDownloaderDownloadFile;
import dev.mel0n.entity.MlnDownloaderPartFile;
import dev.mel0n.exception.FileAlreadyDownloadederException;
import dev.mel0n.exception.FileAlreadyInDownloadListException;
import dev.mel0n.exception.FileAlreadyMerginException;
import dev.mel0n.exception.FileNotFoundException;
import dev.mel0n.exception.StorageException;

/**
 * Multi thread download service
 *
 */
@Service
@EnableAsync
public class MlnDownloaderDownloadService {

    private HttpClient client = HttpClient.newHttpClient();
    public static final String SUFIX = "_PART_";
    private final Long BYTE_TO_MBYTE = 1000000L;
    private final int SOME_BYTE = 1;
    private List<MlnDownloaderDownloadFile> mlnDownloadList = new ArrayList<>();
    private static final Path DOWNLOAD_FOLDER = Path.of("/home/mel0n/Downloads/PROGRAMACION/mlnDownloader/downloads");
    private final MlnDownloaderSpeedService mlnDownloaderSpeedService;
    private final MlnDownloaderMergeFileService mlnDownloaderMergeFileService;

    public MlnDownloaderDownloadService(MlnDownloaderSpeedService mlnDownloaderSpeedService,
            @Lazy MlnDownloaderMergeFileService mlnDownloaderMergeFileService) {
        this.mlnDownloaderSpeedService = mlnDownloaderSpeedService;
        this.mlnDownloaderMergeFileService = mlnDownloaderMergeFileService;

    }

    /**
     * To start new download
     * 
     * @param mlnDownloadderEntityDTO basic information to create new download
     */
    public void newDownload(MlnDownloadderNewEntityDTO mlnDownloaderEntityDTO) {

        URI uri = mlnDownloaderEntityDTO.uri();
        int chunks = mlnDownloaderEntityDTO.chunks();
        String filePath = DOWNLOAD_FOLDER + "/" + mlnDownloaderEntityDTO.fileName();
        Long length = 0L;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            length = Long.parseLong(response.headers().map().get("content-length").getFirst());

            checkWriteOptions(length, mlnDownloaderEntityDTO.fileName());

            checkNewDownloadExist(filePath, length);

            MlnDownloaderDownloadFile mlnDownloaderEntity = MlnDownloaderDownloadFile.builder()
                    .uri(uri)
                    .filePath(filePath)
                    .length(length)
                    .chunks(chunks)
                    .speedLimit(getMbyteToByte(mlnDownloaderEntityDTO.speedLimit()))
                    .build();

            mlnDownloadList.add(mlnDownloaderEntity);

            saveDownloadList();

            startDownload(mlnDownloaderEntity);

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Validate if destination folder have space and permission to write download
     * file
     * 
     * @param fileSize lenght file to download
     * @param fileName name file to download
     */
    public void checkWriteOptions(Long fileSize, String fileName) throws IOException {

        Path path = MlnDownloaderDownloadService.getDOWNLOAD_FOLDER();

        FileStore fileStore = Files.getFileStore(path);

        if (!Files.isWritable(path))
            throw new StorageException("No tienes permiso de escritura en la ubicación: " + DOWNLOAD_FOLDER);

        if (fileStore.getUsableSpace() < fileSize.longValue())
            throw new StorageException("Espacio insuficiente para guardar el archivo: " + fileName);

    }

    /**
     * When new download is create, start this download
     * 
     * @param mlnDownloaderEntity
     */
    public void startDownload(MlnDownloaderDownloadFile mlnDownloaderEntity) {
        try {

            Long length = mlnDownloaderEntity.getLength();
            Long chunkSize = length / mlnDownloaderEntity.getChunks();

            Long start = 0L;
            int count = 0;

            System.out.println("################################# Descargando #################################");

            while (start < length) {

                long finalStart = start;

                Long finalEnd = (start + chunkSize) >= length ? length : start + chunkSize - SOME_BYTE;

                String partFileName = mlnDownloaderEntity.getFilePath() + SUFIX + count;

                this.downPartFile(mlnDownloaderEntity, finalStart, finalEnd, partFileName);

                start = finalEnd + SOME_BYTE;
                count++;

            }

            System.out.println("###############################################################################");

            new Thread(() -> {
                startDownloadControl(mlnDownloaderEntity);
            }).start();

        } catch (Exception e) {
            // System.out.println("PARADOOOOOOOOOOOOOOOOO");
        }
    }

    /**
     * When all download parts are created, start all processos to download parts.
     * Wait to finish download parts and merge now
     * 
     * @param mlnDownloaderEntity
     */
    public void startDownloadControl(MlnDownloaderDownloadFile mlnDownloaderEntity) {

        setDownloadedPartSize(mlnDownloaderEntity);

        try {

            for (Thread t : mlnDownloaderEntity.getThreads()) {
                t.start();
            }

            for (Thread t : mlnDownloaderEntity.getThreads()) {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        } catch (java.util.concurrent.CancellationException e) {
            System.out.println("Se canceló la descarga por parte del usuario");
        }

        Long checkFileSizeOnParts = 0L;

        for (MlnDownloaderPartFile p : mlnDownloaderEntity.getParts()) {
            try {
                checkFileSizeOnParts += Files.size(Path.of(p.getPath()));
                p.setDownloading(false);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        mlnDownloaderEntity.setDownloaded(true);

        mlnDownloaderMergeFileService.startMergeFiles(mlnDownloaderEntity);

        System.out.println("################################# FINISH MERGE FILES #################################");
    }

    /**
     * When download have multi part, get independent downloads
     * 
     * @param mlnDownloadEntity download information
     * @param start             start chunk range
     * @param end               end chunk range
     * @param partFileName      chunk file name
     */
    public void downPartFile(MlnDownloaderDownloadFile mlnDownloadEntity, Long start, Long end, String partFileName) {

        Optional<MlnDownloaderPartFile> mOptional = mlnDownloadEntity.getParts().stream()
                .filter(p -> p.getPath().equals(partFileName)).findFirst();

        MlnDownloaderPartFile mlnDownloaderPartFile;

        if (mOptional.isEmpty()) {

            mlnDownloaderPartFile = MlnDownloaderPartFile.builder()
                    .path(partFileName)
                    .length(end - start)
                    .actualSize(0L)
                    .start(start)
                    .end(end)
                    .isDownloading(true)
                    .speedLimit(mlnDownloadEntity.getSpeedLimit())
                    .build();

            mlnDownloadEntity.getParts().add(mlnDownloaderPartFile);

        } else {
            mlnDownloaderPartFile = mOptional.get();
        }

        HttpRequest requestFile = HttpRequest.newBuilder()
                .uri(mlnDownloadEntity.getUri())
                .header("Range", "bytes=" + start + "-" + end)
                .GET()
                .build();

        int bufferSize = 65536;

        Thread thread = new Thread(() -> {

            try {

                try {
                    HttpResponse<InputStream> response = client.send(requestFile,
                            HttpResponse.BodyHandlers.ofInputStream());

                    try (InputStream in = response.body()) {

                        try (OutputStream out = Files.newOutputStream(
                                Path.of(partFileName),
                                StandardOpenOption.CREATE,
                                StandardOpenOption.APPEND)) {

                            byte[] buffer = new byte[bufferSize];

                            Long bytesActualSeccond = 0L;
                            Long startSeccond = System.currentTimeMillis();

                            int readBytes;

                            mlnDownloaderSpeedService.getSpeedDownloadFromFile(mlnDownloaderPartFile);

                            while ((readBytes = in.read(buffer)) != -1 && mlnDownloaderPartFile.isDownloading()) {

                                out.write(buffer, 0, readBytes);

                                bytesActualSeccond += readBytes;

                                Long currentTime = System.currentTimeMillis();
                                Long elapsed = currentTime - startSeccond;

                                if (bytesActualSeccond >= mlnDownloaderPartFile.getSpeedLimit()) {

                                    if (elapsed < 1000)
                                        Thread.sleep(1000 - elapsed);

                                    bytesActualSeccond = 0L;
                                    startSeccond = System.currentTimeMillis();
                                }
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        });

        if (mlnDownloadEntity.getThreads() == null)
            mlnDownloadEntity.setThreads(new ArrayList<>());

        mlnDownloadEntity.getThreads().add(thread);

        System.out.println(
                "DOWNLOAD: " + partFileName + " - " + "Range bytes=" + start + "-" + end + " - " + (end - start));

    }

    /**
     * Stop and delete actual download
     * 
     * @param fileName name for file to stop
     */
    public void deleteDownload(UUID id) {

        Optional<MlnDownloaderDownloadFile> mOptional = mlnDownloadList.stream()
                .filter(m -> m.getId().equals(id))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("El archivo que intenta eliminar no está en la lista de descargas.");

        MlnDownloaderDownloadFile mlnDownloaderEntity = mOptional.get();

        mlnDownloaderEntity.setDownloading(false);

        System.out.println("######################## Delete files ########################");

        mlnDownloaderEntity.getParts().forEach(p -> {

            Path partToDelete = Path.of(p.getPath());

            if (Files.exists(partToDelete)) {
                try {
                    Files.delete(partToDelete);
                    System.out.println("DELETE PART: " + partToDelete);
                } catch (IOException e) {
                    System.out.println("ERROR TO DELETE PART: " + partToDelete);
                }

            }
        });

        Path pathFile = Path.of(mlnDownloaderEntity.getFilePath());

        if (Files.exists(pathFile)) {
            try {
                Files.delete(pathFile);

                if (Files.exists(pathFile)) {
                    System.out.println("ERROR TO DELETE FILE: " + pathFile);
                } else {
                    System.out.println("DELETE FILE: " + pathFile);
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        System.out.println("##############################################################");

        System.out.println(mlnDownloadList);
        this.mlnDownloadList.remove(mlnDownloaderEntity);
        System.out.println(mlnDownloadList);

        saveDownloadList();

    }

    /**
     * Clean downloaded files from memory
     */
    public void cleanFinishDownloads() {
        this.mlnDownloadList = new ArrayList<>(
                mlnDownloadList.stream().filter(m -> !m.isDownloaded() || !m.isMerget()).toList());
        saveDownloadList();
        System.out.println("######################## CLEANED FINISH DOWNLOADS ########################");
    }

    /**
     * Pause or resume download with file name
     * 
     * @param fileName
     */
    public void pauseOrResumeDownload(UUID id) {

        Optional<MlnDownloaderDownloadFile> mOptional = mlnDownloadList.stream()
                .filter(down -> down.getId().equals(id))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("El archivo que quieres pausar no se encuntra en la lista");

        MlnDownloaderDownloadFile mlnDownloaderEntity = mOptional.get();

        if (Files.exists(Path.of(mlnDownloaderEntity.getFilePath())))
            throw new FileAlreadyDownloadederException("El archivo que quieres resumir ya está descargado");

        if (mlnDownloaderEntity.isDownloaded())
            throw new FileAlreadyMerginException("No puedes cancelar ahora el fichero ya se ha descargado");

        // Pause download
        if (mlnDownloaderEntity.isDownloading()) {

            System.out.println("################################# Pause download #################################");

            for (MlnDownloaderPartFile part : mlnDownloaderEntity.getParts()) {

                try {
                    part.setDownloading(false);
                    part.setActualSize(Files.size(Path.of(part.getPath())));
                    System.out.println("PAUSE ACTUAL SIZE: " + part.getActualSize());
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            System.out.println("##################################################################################");

            mlnDownloaderEntity.setDownloading(false);

            mlnDownloaderEntity.getThreads().clear();

            saveDownloadList();

            return;
        }

        System.out.println("################################# Resume download #################################");

        // Resume Download
        for (MlnDownloaderPartFile p : mlnDownloaderEntity.getParts()) {

            try {
                Path fileSizeDownloadedPath = Path.of(p.getPath());

                Long finalStart = p.getStart() + Files.size(fileSizeDownloadedPath);
                if (finalStart >= p.getEnd())
                    continue;

                this.downPartFile(mlnDownloaderEntity, finalStart, p.getEnd(), p.getPath());

                p.setDownloading(true);

            } catch (IOException e) {
                System.out.println("ERROR ON RESUME DOWNLOAD -> " + p.getPath());
            }

        }

        mlnDownloaderEntity.setDownloading(true);

        startDownloadControl(mlnDownloaderEntity);

        saveDownloadList();

        System.out.println("##################################################################################");
    }

    /**
     * Comprove if downloading file exist in database and local folder
     * 
     * @param fileName file name to check
     * @param length   file size in bytes
     */
    public void checkNewDownloadExist(String fileName, Long length) {

        Path checkFile = Path.of(fileName);

        try {
            if (Files.exists(checkFile)) {
                if ((Files.size(checkFile) == length))
                    throw new FileAlreadyDownloadederException("El archivo ya existe localmente");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Optional<MlnDownloaderDownloadFile> mOptional = mlnDownloadList.stream()
                .filter(m -> m.getFilePath().endsWith(fileName))
                .findFirst();

        if (!mOptional.isEmpty()) {
            throw new FileAlreadyInDownloadListException("Este archivo está en la lista de descarga");
        }

    }

    /**
     * Convert bytes to mbytes
     * 
     * @param bytes bytes size
     * @return Long with mbytes size
     */
    public Long getByteToMbyte(Long bytes) {
        return bytes / BYTE_TO_MBYTE;
    }

    /**
     * Convert mbytes to bytes
     * 
     * @param mbytes mbytes size
     * @return Long with bytes size
     */
    public Long getMbyteToByte(double mbytes) {
        return (long) (mbytes * BYTE_TO_MBYTE);
    }

    /**
     * Update size from download file
     * 
     * @param mlnDownloadEntity
     */
    public void setDownloadedPartSize(MlnDownloaderDownloadFile mlnDownloadEntity) {

        new Thread(() -> {

            while (mlnDownloadEntity.isDownloading()) {

                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                AtomicLong downloaderFilesSize = new AtomicLong(0);

                mlnDownloadEntity.getParts().forEach(p -> {

                    Path pathFile = Path.of(p.getPath());

                    if (Files.exists(pathFile)) {
                        try {
                            downloaderFilesSize.addAndGet(Files.size(pathFile));
                            p.setActualSize(Files.size(pathFile));
                        } catch (IOException e) {
                            System.out.println("Error en la lectura de tamaño: " + e.getMessage());
                        }
                    }
                });

                if (downloaderFilesSize.get() != 0L) {
                    mlnDownloadEntity.setDownloadedBytes(downloaderFilesSize.get());
                }

                this.saveDownloadList();
            }

            System.out.println("STOP CONTROL DOWNLOAD SIZE: " + mlnDownloadEntity.getFilePath());

        }).start();
    }

    /**
     * Change speed limit
     * 
     * @param mlnDownloaderChangeSpeedLimitDTO
     */
    public void changeSpeedLimit(MlnDownloaderChangeSpeedLimitDTO mlnDownloaderChangeSpeedLimitDTO) {

        String id = mlnDownloaderChangeSpeedLimitDTO.id();
        Long speedLimit = getMbyteToByte(mlnDownloaderChangeSpeedLimitDTO.speedLimit());

        MlnDownloaderDownloadFile mlnDownloaderDownloadFile = this.mlnDownloadList.stream()
                .filter(p -> p.getId().toString().equals(id)).findFirst().get();

        mlnDownloaderDownloadFile.setSpeedLimit(speedLimit);

        for (MlnDownloaderPartFile part : mlnDownloaderDownloadFile.getParts()) {
            part.setSpeedLimit(speedLimit);
        }
    }

    /**
     * To save list files information
     */
    public void saveDownloadList() {

        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("data.bin"))) {

            out.writeObject(this.mlnDownloadList);

        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Send all download activity
     * 
     * @return list from downloads information
     */
    public List<MlnDownloaderDownloadFile> getMlnDownloadList() {
        return mlnDownloadList;
    }

    public void setMlnDownloadList(List<MlnDownloaderDownloadFile> mlnDownloadList) {
        this.mlnDownloadList = mlnDownloadList;
    }

    public static Path getDOWNLOAD_FOLDER() {
        return DOWNLOAD_FOLDER;
    }
}
