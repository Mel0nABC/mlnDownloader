/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import dev.mel0n.dto.MlnDownloadderNewEntityDTO;
import dev.mel0n.dto.MlnDownloaderChangeSpeedLimitDTO;
import dev.mel0n.service.MlnDownloaderDownloadService;
import dev.mel0n.service.MlnDownloaderMergeFileService;

/**
 * Controller to manage download options
 */
@Controller
@RequestMapping("/api")
public class MlnDownloaderController {

    private final MlnDownloaderDownloadService mlnDownloaderService;
    private final MlnDownloaderMergeFileService mlnDownloaderMergeFileService;

    /**
     * Constructir to inyect dependencies
     * 
     * @param mlnDownloaderService
     */
    public MlnDownloaderController(MlnDownloaderDownloadService mlnDownloaderService,
            MlnDownloaderMergeFileService mlnDownloaderMergeFileService) {
        this.mlnDownloaderService = mlnDownloaderService;
        this.mlnDownloaderMergeFileService = mlnDownloaderMergeFileService;
    }

    /**
     * 
     * Create new download and start automatic
     * 
     * @param mlnDownloadderEntityDTO basic informatión tu create new download
     * @return ResponseEntity with map, message value is a String text
     */
    @PostMapping("/downloads")
    public ResponseEntity<Map<String, Object>> newDownload(
            @RequestBody MlnDownloadderNewEntityDTO mlnDownloadderEntityDTO) {

        this.mlnDownloaderService.newDownload(mlnDownloadderEntityDTO);

        return ResponseEntity
                .ok(Map.of("success", true, "message", "Nueva descarga iniciada"));
    }

    /**
     * 
     * Delete download activity
     * 
     * @param fileName String file name to delete download activity
     * @return ResponseEntity with map, message value is a String text
     */
    @DeleteMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> deleteDownloaded(@PathVariable String id) {

        this.mlnDownloaderService.deleteDownload(UUID.fromString(id));

        return ResponseEntity
                .ok(Map.of("success", true, "message", "La descarga se elimino satisfactoriamente"));
    }

    /**
     * To clean all finished downloads from memory list
     * 
     * @return ResponseEntity with map, message value is a String text
     */
    @DeleteMapping("/downloads")
    public ResponseEntity<Map<String, Object>> cleanFinishDownloads() {

        this.mlnDownloaderService.cleanFinishDownloads();

        return ResponseEntity
                .ok(Map.of("success", true, "message", "Se eliminaron las descargas finalizadas"));
    }

    /**
     * 
     * To pause or resume download activity
     * 
     * @param fileName String from download file name
     * @return ResponseEntity with map, message value is a String text
     */
    @PutMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> pauseOrResumeDownload(@PathVariable String id) {

        this.mlnDownloaderService.pauseOrResumeDownload(UUID.fromString(id));

        return ResponseEntity.ok(Map.of("success", true, "message", "Descarga pausada"));
    }

    /**
     * When automatic merge have some error, client send new merge petition
     * 
     * @param id
     * @return
     */
    @PostMapping("/downloads/{id}")
    public ResponseEntity<Map<String, Object>> forceMergeFiles(@PathVariable String id) {

        this.mlnDownloaderMergeFileService.forceMergeFilesFromClient(UUID.fromString(id));

        return ResponseEntity.ok(Map.of("success", true, "message", "Ficheros unidos satisfactoriamente"));
    }

    /**
     * To change speed limit
     * 
     * @param mlnDownloaderChangeSpeedLimitDTO
     * @return
     */
    @PutMapping("/downloads")
    public ResponseEntity<Map<String, Object>> changeSpeedLimit(
            @RequestBody MlnDownloaderChangeSpeedLimitDTO mlnDownloaderChangeSpeedLimitDTO) {

        this.mlnDownloaderService.changeSpeedLimit(mlnDownloaderChangeSpeedLimitDTO);

        return ResponseEntity.ok(Map.of("success", true, "message", "Velocidad cambiada satisfactoriamente"));
    }
}