package app.campushire.repository;

import app.campushire.entity.Application;
import app.campushire.entity.PlacementRecord;
import app.campushire.entity.PlacementStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacementRecordRepository extends JpaRepository<PlacementRecord, Long> {
    List<PlacementRecord> findByApplication(Application application);
    List<PlacementRecord> findByStatus(PlacementStatus status);
    List<PlacementRecord> findByApplicationId(Long applicationId);
}
