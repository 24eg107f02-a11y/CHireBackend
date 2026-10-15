package com.campushire.repository;

import com.campushire.entity.PlacementDrive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {

    List<PlacementDrive> findAllByOrderByStartDateAsc();

    boolean existsByCreatedById(Long userId);
}
