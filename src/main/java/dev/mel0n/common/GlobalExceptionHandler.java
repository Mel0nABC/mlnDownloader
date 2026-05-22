/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import dev.mel0n.exception.FileAlreadyDownloadederException;
import dev.mel0n.exception.FileAlreadyInDownloadListException;
import dev.mel0n.exception.FileNotFoundException;
import dev.mel0n.exception.MultipartMergeException;
import dev.mel0n.exception.StorageException;
import lombok.NoArgsConstructor;

/**
 * Unificate all exceptions in one file
 */
@NoArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * When file is in HD or in database
     * 
     * @param e Exception
     * @return ResponseEntity with map, message value is a String text
     */
    @ExceptionHandler(FileAlreadyDownloadederException.class)
    public ResponseEntity<Map<String, Object>> fileAlreadyDownloadederException(Exception e) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    /**
     * When download is en database but not in hd
     * 
     * @param e Exception
     * @return ResponseEntity with map, message value is a String text
     */
    @ExceptionHandler(FileAlreadyInDownloadListException.class)
    public ResponseEntity<Map<String, Object>> fileAlreadyInDownloadListException(Exception e) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    /**
     * When merge all files launch some error
     * 
     * @param e Exception
     * @return ResponseEntity with map, message value is a String text
     */
    @ExceptionHandler(MultipartMergeException.class)
    public ResponseEntity<Map<String, Object>> multipartMergeException(Exception e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    /**
     * When user go to delete download activity but this not exist
     * 
     * @param e Exception
     * @return ResponseEntity with map, message value is a String text
     */
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<Map<String, Object>> fileNotFoundException(Exception e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    /**
     * When file to download have some problem to download
     * 
     * @param e Exception
     * @return ResponseEntity with map, message value is a String text
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> storageException(StorageException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

}
