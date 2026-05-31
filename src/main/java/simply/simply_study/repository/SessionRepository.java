package simply.simply_study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import simply.simply_study.model.Session;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
}
