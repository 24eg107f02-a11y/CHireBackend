package app.campushire.controller;

import app.campushire.entity.Company;
import app.campushire.exception.ResourceNotFoundException;
import app.campushire.repository.CompanyRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CompanyController {

    private final CompanyRepository companyRepository;

    public CompanyController(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @GetMapping("/companies")
    public ResponseEntity<List<Company>> getCompanies() {
        return ResponseEntity.ok(companyRepository.findAll());
    }

    @GetMapping("/companies/{companyId}")
    public ResponseEntity<Company> getCompany(@PathVariable Long companyId) {
        return ResponseEntity.ok(companyRepository.findById(companyId)
            .orElseThrow(() -> new ResourceNotFoundException("Company not found")));
    }

    @PostMapping("/companies")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECRUITER')")
    public ResponseEntity<Company> createCompany(@Valid @RequestBody Company company) {
        return ResponseEntity.ok(companyRepository.save(company));
    }
}
