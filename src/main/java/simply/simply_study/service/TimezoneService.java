package simply.simply_study.service;

import java.time.Instant;
import java.time.LocalDateTime;

public interface TimezoneService {
    Instant toUTC(LocalDateTime localTime, String timezone);
    LocalDateTime toLocal(Instant utcTime, String timezone);
    String formatToLocalString(Instant utcTime, String timezone);
}
