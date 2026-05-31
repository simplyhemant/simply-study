package simply.simply_study.service.impl;

import org.springframework.stereotype.Service;
import simply.simply_study.exception.InvalidInputException;
import simply.simply_study.service.TimezoneService;

import java.time.*;
import java.time.format.DateTimeFormatter;

@Service
public class TimezoneServiceImpl implements TimezoneService {

    private ZoneId getZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new InvalidInputException("Invalid timezone: " + timezone);
        }
    }

    @Override
    public Instant toUTC(LocalDateTime localTime, String timezone) {
        if (localTime == null) return null;
        return localTime.atZone(getZoneId(timezone)).toInstant();
    }

    @Override
    public LocalDateTime toLocal(Instant utcTime, String timezone) {
        if (utcTime == null) return null;
        return LocalDateTime.ofInstant(utcTime, getZoneId(timezone));
    }

    @Override
    public String formatToLocalString(Instant utcTime, String timezone) {
        if (utcTime == null) return null;
        ZonedDateTime zdt = utcTime.atZone(getZoneId(timezone));
        return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
