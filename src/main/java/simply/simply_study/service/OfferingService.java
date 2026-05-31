package simply.simply_study.service;

import org.springframework.data.domain.Pageable;
import simply.simply_study.dto.*;
import java.util.List;

public interface OfferingService {

    OfferingResponseDto createOffering(CreateOfferingRequest request);
    List<SessionResponseDto> addSessions(Long offeringId, String teacherId, CreateSessionsRequest request);
    List<OfferingResponseDto> getTeacherOfferings(String teacherId, String timezone, Pageable pageable);
    List<OfferingResponseDto> getAvailableOfferings(String userId, String timezone, Pageable pageable);

    default List<OfferingResponseDto> getTeacherOfferings(String teacherId, String timezone) {
        return getTeacherOfferings(teacherId, timezone, Pageable.unpaged());
    }

    default List<OfferingResponseDto> getAvailableOfferings(String userId, String timezone) {
        return getAvailableOfferings(userId, timezone, Pageable.unpaged());
    }
}