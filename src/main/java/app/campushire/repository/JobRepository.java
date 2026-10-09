package app.campushire.repository;

import app.campushire.entity.Job;
import app.campushire.entity.JobStatus;
import app.campushire.entity.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByRecruiter(User recruiter);
    List<Job> findByStatus(JobStatus status);
    List<Job> findByCompanyId(Long companyId);
}
