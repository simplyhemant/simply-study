package simply.simply_study.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import simply.simply_study.model.BookingSession;

import java.util.List;

@Repository
public interface BookingSessionRepository extends JpaRepository<BookingSession, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT bs FROM BookingSession bs WHERE bs.booking.parent.id = :parentId")
    List<BookingSession> lockParentBookings(@Param("parentId") String parentId);


    @Query("SELECT COUNT(bs) FROM BookingSession bs " +
           "WHERE bs.booking.parent.id = :parentId " +
           "AND EXISTS (" +
           "  SELECT 1 FROM Session s WHERE s.offering.id = :offeringId " +
           "  AND bs.session.startTime < s.endTime " +
           "  AND bs.session.endTime > s.startTime" +
           ")")
    long countOverlappingWithOffering(@Param("parentId") String parentId,
                                      @Param("offeringId") Long offeringId);
}
