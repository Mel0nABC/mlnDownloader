package dev.mel0n.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartException;

import dev.mel0n.entity.MlnDownloaderDownloadFile;
import dev.mel0n.entity.MlnDownloaderPartFile;
import dev.mel0n.exception.FileNotFoundException;
import dev.mel0n.exception.FileSizeException;
import dev.mel0n.exception.StorageException;

@Service
public class MlnDownloaderMergeFileService {

    private final MlnDownloaderDownloadService mlnDownloaderDownloadService;

    public MlnDownloaderMergeFileService(MlnDownloaderDownloadService mlnDownloaderDownloadService) {
        this.mlnDownloaderDownloadService = mlnDownloaderDownloadService;
    }

    public void forceMergeFilesFromClient(UUID id) {

        Optional<MlnDownloaderDownloadFile> mOptional = mlnDownloaderDownloadService.getMlnDownloadList().stream()
                .filter(mln -> mln.getId().equals(id))
                .findFirst();

        if (mOptional.isEmpty())
            throw new FileNotFoundException("Error al forzar el merge del fichero solicitado");

        MlnDownloaderDownloadFile mlnDownloaderDownloadFile = mOptional.get();

        startMergeFiles(mlnDownloaderDownloadFile);
    }

    /**
     * To start process to merge files
     * 
     * @param mlnDownloaderEntity
     */
    public void startMergeFiles(MlnDownloaderDownloadFile mlnDownloaderEntity) {
        try {

            if (Files.exists(Path.of(mlnDownloaderEntity.getFilePath()))) {
                if (Files.size(Path.of(mlnDownloaderEntity.getFilePath())) == mlnDownloaderEntity.getLength()) {
                    throw new FileAlreadyExistsException(
                            "El archivo de salida de la unión de partes ya existe y tiene el tamaño correcto");
                } else {
                    Files.delete(Path.of(mlnDownloaderEntity.getFilePath()));
                }
            }

            mlnDownloaderEntity.setMerging(true);

            if (!mlnDownloaderEntity.isDownloaded())
                return;

            Path destionatioFolder = Path.of(MlnDownloaderDownloadService.getDOWNLOAD_FOLDER().toUri());

            FileStore fileStore = Files.getFileStore(destionatioFolder);

            long result = mlnDownloaderEntity.getParts().stream()
                    .mapToLong(p -> {
                        long resultate = 0L;
                        try {
                            resultate = Files.size(Path.of(p.getPath()));
                        } catch (IOException e) {
                            // TODO: handle exception
                        }
                        return resultate;

                    }).sum();

            mlnDownloaderEntity.setDownloadedBytes(result);

            if (fileStore.getUsableSpace() < mlnDownloaderEntity.getLength()) {
                mlnDownloaderEntity.setMerging(false);
                throw new StorageException("No hay suficiente espacio para realizar la unión de los ficheros");
            }

            mlnDownloaderEntity.getParts().stream().sorted(Comparator.comparingInt(path -> {
                return Integer.parseInt(path.toString().split(MlnDownloaderDownloadService.SUFIX)[1]);
            }));

        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            multipartMergeAndDelete(mlnDownloaderEntity);
        }).start();
    }

    /**
     * When download have multi files, merge all in finish file
     * 
     * @param mlnDownloadEntity
     */
    public void multipartMergeAndDelete(MlnDownloaderDownloadFile mlnDownloadEntity) {

        try {
            System.out.println("############################ Merge downloaded files ############################");

            try (OutputStream out = Files.newOutputStream(Path.of(mlnDownloadEntity.getFilePath()))) {

                for (MlnDownloaderPartFile part : mlnDownloadEntity.getParts()) {

                    System.out.println(
                            "MERGE: " + part.getPath() + " - LOCAL SIZE: " + Files.size(Path.of(part.getPath())));

                    Files.copy(Path.of(part.getPath()), out);

                }
            }

            mlnDownloadEntity.setMerging(false);

            System.out.println("################################################################################");

            if (!mlnDownloadEntity.getLength().equals(Files.size(Path.of(mlnDownloadEntity.getFilePath())))) {

                System.out.println(
                        mlnDownloadEntity.getLength() + " - " + Files.size(Path.of(mlnDownloadEntity.getFilePath())));
                throw new MultipartException("Some problem on merge all files");

            } else {

                System.out.println("######################## Delete downloaded temp files ########################");

                for (MlnDownloaderPartFile part : mlnDownloadEntity.getParts()) {

                    if (Files.exists(Path.of(part.getPath())))
                        Files.delete(Path.of(part.getPath()));

                    if (!Files.exists(Path.of(part.getPath())))
                        System.out.println("DELETE: " + part.getPath());

                }

                System.out.println("##############################################################################");
            }

            if (Files.exists(Path.of(mlnDownloadEntity.getFilePath()))
                    && (Files.size(Path.of(mlnDownloadEntity.getFilePath())) == mlnDownloadEntity.getLength())) {
                mlnDownloadEntity.setDownloading(false);
                mlnDownloadEntity.setDownloaded(true);
                mlnDownloadEntity.setFileExist(true);
                mlnDownloadEntity.setMerget(true);
                mlnDownloadEntity.setDownloadedBytes(mlnDownloadEntity.getLength());
                if (mlnDownloadEntity.getThreads() != null)
                    mlnDownloadEntity.getThreads().clear();

                changeOwnerGroup(Path.of(mlnDownloadEntity.getFilePath()));

            } else {

                mlnDownloadEntity.setFileExist(false);

                Files.delete(Path.of(mlnDownloadEntity.getFilePath()));
            }

            mlnDownloaderDownloadService.saveDownloadList();

            Long checkFileSizeOnParts = Files.size(Path.of(mlnDownloadEntity.getFilePath()));

            if (!mlnDownloadEntity.getLength().equals(checkFileSizeOnParts)) {
                System.out.println(
                        "EL TAMAÑO TOTAL NO COINCIDE: LENGTH: " + mlnDownloadEntity.getLength() + ", PARTS: "
                                + checkFileSizeOnParts);
                throw new FileSizeException("Posible archivo corrupto: Web ->" + mlnDownloadEntity.getLength()
                        + " - Local -> " + checkFileSizeOnParts);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * To change owner and group to downloaded file
     * 
     * @param path file to change owner:group
     */
    public void changeOwnerGroup(Path path) {
        try {

            String ownerName = System.getenv("PUID");
            String groupName = System.getenv("PGID");

            if (ownerName == null) {
                ownerName = System.getenv("USER");
            }

            if (groupName == null) {
                groupName = System.getenv("USER");
            }

            new ProcessBuilder("chown", ownerName + ":" + groupName, path.toString()).start().waitFor();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
