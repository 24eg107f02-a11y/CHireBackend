package app.campushire.repository;

import app.campushire.entity.Application;
import app.campushire.entity.ApplicationStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findByApplicationOrderByCreatedAtAsc(Application application);
    List<ApplicationStatusHistory> findByApplicationIdOrderByCreatedAtAsc(Long applicationId);
}
