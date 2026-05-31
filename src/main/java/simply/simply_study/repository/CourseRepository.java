package simply.simply_study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import simply.simply_study.model.Course;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query("SELECT DISTINCT c FROM Course c " +
           "LEFT JOIN FETCH c.offerings o " +
           "LEFT JOIN FETCH o.teacher t " +
           "WHERE c.id = :id")
    Optional<Course> findByIdWithDetails(@Param("id") Long id);
}
