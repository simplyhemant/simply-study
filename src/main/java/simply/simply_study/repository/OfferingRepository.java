package simply.simply_study.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import simply.simply_study.model.Offering;

import java.util.List;
import java.util.Optional;

@Repository
public interface OfferingRepository extends JpaRepository<Offering, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offering o LEFT JOIN FETCH o.sessions JOIN FETCH o.course JOIN FETCH o.teacher WHERE o.id = :id")
    Optional<Offering> findByIdWithWriteLock(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Offering o LEFT JOIN FETCH o.sessions JOIN FETCH o.course JOIN FETCH o.teacher WHERE o.teacher.id = :teacherId")
    List<Offering> findByTeacherIdWithDetails(@Param("teacherId") String teacherId);

    @Query("SELECT DISTINCT o FROM Offering o LEFT JOIN FETCH o.sessions JOIN FETCH o.course JOIN FETCH o.teacher")
    List<Offering> findAllWithSessionsAndCourse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Offering o SET o.currentEnrollment = o.currentEnrollment + 1 WHERE o.id = :id AND o.currentEnrollment < o.maxCapacity")
    int incrementEnrollment(@Param("id") Long id);
}
