/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MlnDownloaderPartFile implements Serializable {

    private String path;
    private Long length;
    private Long actualSize;
    private Long start;
    private Long end;
    @Builder.Default
    private boolean isDownloading = true;
    @Builder.Default
    private Long speedLimitBytesPerSecond = 10L * 1024L * 1024L * 1024L * 1024L;

}
