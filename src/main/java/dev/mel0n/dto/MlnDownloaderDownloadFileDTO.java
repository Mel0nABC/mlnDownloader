/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.dto;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import dev.mel0n.entity.MlnDownloaderPartFile;
import lombok.Builder;

/**
 * Dto to send information to front
 */
@Builder
public record MlnDownloaderDownloadFileDTO(
                UUID id,
                URI uri,
                String filePath,
                Long length,
                int chunks,
                List<MlnDownloaderPartFile> parts,
                boolean isDownloading,
                boolean isDownloaded,
                boolean isFileExist,
                boolean isMerging,
                boolean isMerget,
                Long downloadedBytes,
                Long speedLimit) {

}
