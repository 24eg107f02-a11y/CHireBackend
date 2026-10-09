package app.campushire.controller;

import app.campushire.entity.Application;
import app.campushire.entity.PlacementRecord;
import app.campushire.entity.PlacementStatus;
import app.campushire.exception.ResourceNotFoundException;
import app.campushire.repository.ApplicationRepository;
import app.campushire.repository.PlacementRecordRepository;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PlacementController {

    private final PlacementRecordRepository placementRecordRepository;
    private final ApplicationRepository applicationRepository;

    public PlacementController(PlacementRecordRepository placementRecordRepository, ApplicationRepository applicationRepository) {
        this.placementRecordRepository = placementRecordRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/applications/{applicationId}/placements")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<List<PlacementRecord>> getPlacementRecords(@PathVariable Long applicationId) {
        return ResponseEntity.ok(placementRecordRepository.findByApplicationId(applicationId));
    }

    @PostMapping("/applications/{applicationId}/placements")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<PlacementRecord> createPlacementRecord(@PathVariable Long applicationId, @RequestBody PlacementRecord record) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new ResourceNotFoundException("Application not found"));
        record.setApplication(application);
        record.setCompany(application.getJob().getCompany());
        return ResponseEntity.ok(placementRecordRepository.save(record));
    }

    @PutMapping("/placements/{placementId}/status")
    @PreAuthorize("hasRole('RECRUITER') or hasRole('ADMIN')")
    public ResponseEntity<PlacementRecord> updatePlacementStatus(@PathVariable Long placementId, @RequestBody PlacementStatus status) {
        PlacementRecord record = placementRecordRepository.findById(placementId)
            .orElseThrow(() -> new ResourceNotFoundException("Placement record not found"));
        record.setStatus(status);
        return ResponseEntity.ok(placementRecordRepository.save(record));
    }
}
