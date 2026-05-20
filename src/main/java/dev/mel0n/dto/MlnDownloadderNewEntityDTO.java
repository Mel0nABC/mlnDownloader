/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.dto;

import java.net.URI;

import lombok.Builder;

/**
 * DTO to create new download
 */
@Builder
public record MlnDownloadderNewEntityDTO(
        URI uri,
        int chunks,
        String fileName,
        double speedLimit) {
}
