package app.campushire.repository;

import app.campushire.entity.Application;
import app.campushire.entity.ApplicationStatus;
import app.campushire.entity.Job;
import app.campushire.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByStudent(User student);
    List<Application> findByJob(Job job);
    List<Application> findByStatus(ApplicationStatus status);
    Optional<Application> findByStudentAndJob(User student, Job job);
    List<Application> findByJobId(Long jobId);
    List<Application> findByStudentId(Long studentId);
}
