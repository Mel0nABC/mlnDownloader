package dev.mel0n.dto;

/**
 * Dto to change speed in actual download
 */
public record MlnDownloaderChangeSpeedLimitDTO(
        String id,
        double speedLimit) {

}
