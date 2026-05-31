package simply.simply_study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import simply.simply_study.model.Booking;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByOfferingId(Long offeringId);

    boolean existsByParentIdAndOfferingId(String parentId, Long offeringId);

    @Query("SELECT b FROM Booking b " +
           "JOIN FETCH b.offering o " +
           "JOIN FETCH o.course c " +
           "JOIN FETCH o.teacher t " +
           "LEFT JOIN FETCH o.sessions s " +
           "WHERE b.parent.id = :parentId")
    List<Booking> findByParentIdWithDetails(@Param("parentId") String parentId);
}
