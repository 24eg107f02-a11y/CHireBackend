package app.campushire.repository;

import app.campushire.entity.StudentProfile;
import app.campushire.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);
    Optional<StudentProfile> findByUserId(Long userId);
    Optional<StudentProfile> findByCollegeId(String collegeId);
}
