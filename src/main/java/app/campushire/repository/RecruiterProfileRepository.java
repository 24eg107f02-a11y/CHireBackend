package app.campushire.repository;

import app.campushire.entity.RecruiterProfile;
import app.campushire.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecruiterProfileRepository extends JpaRepository<RecruiterProfile, Long> {
    Optional<RecruiterProfile> findByUser(User user);
    Optional<RecruiterProfile> findByUserId(Long userId);
}
