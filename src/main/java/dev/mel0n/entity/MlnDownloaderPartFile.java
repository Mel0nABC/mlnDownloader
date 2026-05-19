/* SPDX-FileCopyrightText: 2025 Mel0nABC

 SPDX-License-Identifier: MIT */
package dev.mel0n.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    private Long speedFile = 0L;

    @Builder.Default
    private List<Long> registerSpeed = new ArrayList<>();

    public void setSpeedFile(Long speed)  {
        this.speedFile = speed;
        registerSpeed.add(speed);
    }

}
