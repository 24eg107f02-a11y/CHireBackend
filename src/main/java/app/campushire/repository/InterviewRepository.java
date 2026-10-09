package app.campushire.repository;

import app.campushire.entity.Application;
import app.campushire.entity.Interview;
import app.campushire.entity.InterviewStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByApplication(Application application);
    List<Interview> findByStatus(InterviewStatus status);
    List<Interview> findByApplicationId(Long applicationId);
}
