/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.entity;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import dev.mel0n.dto.MlnDownloaderDownloadFileDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * Class to save download activity
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class MlnDownloaderDownloadFile implements Serializable {

    @Builder.Default
    private UUID id = UUID.randomUUID();
    private URI uri;
    private String filePath;
    private Long length;

    @Builder.Default
    private int chunks = 1;

    @Builder.Default
    private List<MlnDownloaderPartFile> parts = new ArrayList<>();

    @Builder.Default
    private transient List<Thread> threads = new ArrayList<>();

    @Builder.Default
    private boolean isDownloading = true;

    @Builder.Default
    private boolean isDownloaded = false;

    @Builder.Default
    private boolean isFileExist = false;

    @Builder.Default
    private boolean isMerging = false;

    @Builder.Default
    private boolean isMerget = false;

    @Builder.Default
    private Long downloadedBytes = 0L;

    @Builder.Default
    private Long speedLimit = 10L * 1024 * 1024 * 1024 * 1024;

    public MlnDownloaderDownloadFileDTO toDTO() {
        return MlnDownloaderDownloadFileDTO.builder()
                .id(id)
                .uri(uri)
                .filePath(filePath)
                .length(length)
                .chunks(chunks)
                .parts(parts)
                .isDownloading(isDownloading)
                .isDownloaded(isDownloaded)
                .isFileExist(isFileExist)
                .isMerging(isMerging)
                .isMerget(isMerget)
                .downloadedBytes(downloadedBytes)
                .speedLimit(speedLimit)
                .build();
    }

}