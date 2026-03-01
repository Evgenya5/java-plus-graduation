package stats.client;

import dto.GetStatsDto;
import dto.SaveHitDto;
import dto.StatsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsClient {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsClientFeign statsClientFeign;

    public void saveHit(SaveHitDto saveHitDto) {
        log.info("Sending save hit request: {}", saveHitDto);

        try {
            statsClientFeign.saveStats(saveHitDto);
            log.info("Hit saved successfully");
        } catch (Exception e) {
            log.error("Failed to save hit: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save hit", e);
        }
    }

    public List<GetStatsDto> getStats(@Valid StatsRequest statsRequest) {
        log.info("Getting stats with request: {}", statsRequest);
        Boolean unique = statsRequest.getUnique() != null ? statsRequest.getUnique() : false;
        try {
            return statsClientFeign.getStats(
                    statsRequest.getStart().format(DATE_TIME_FORMATTER),
                    statsRequest.getEnd().format(DATE_TIME_FORMATTER),
                    statsRequest.getUris(),
                    unique
            );
        } catch (Exception e) {
            log.error("Failed to get stats: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to get stats", e);
        }
    }

    public List<GetStatsDto> getStats(LocalDateTime start, LocalDateTime end,
                                      List<String> uris, Boolean unique) {
        StatsRequest statsRequest = StatsRequest.builder()
                .start(start)
                .end(end)
                .uris(uris)
                .unique(unique)
                .build();

        return getStats(statsRequest);
    }

    public List<GetStatsDto> getStats(LocalDateTime start, LocalDateTime end) {
        return getStats(start, end, null, false);
    }

    public List<GetStatsDto> getStatsUnique(LocalDateTime start, LocalDateTime end, List<String> uris) {
        return getStats(start, end, uris, true);
    }

    public List<GetStatsDto> getStatsForUris(LocalDateTime start, LocalDateTime end, List<String> uris) {
        return getStats(start, end, uris, false);
    }
}
