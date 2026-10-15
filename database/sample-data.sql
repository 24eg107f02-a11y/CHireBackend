SET @demo_recruiter_email = 'demo.recruiter@example.edu';
SET @demo_recruiter_profile_id = (
    SELECT rp.id
    FROM recruiter_profiles rp
    JOIN users u ON u.id = rp.user_id
    WHERE LOWER(u.email) = LOWER(@demo_recruiter_email)
      AND u.role = 'RECRUITER'
    LIMIT 1
);

SELECT IF(
    @demo_recruiter_profile_id IS NULL,
    'No sample data inserted. Register a recruiter as demo.recruiter@example.edu, start the backend once, and run this script again.',
    'Adding sample company and approved job postings for demo.recruiter@example.edu.'
) AS seed_status;

START TRANSACTION;

INSERT INTO companies (
    recruiter_profile_id, name, description, website, headquarters, created_at, updated_at
)
SELECT
    @demo_recruiter_profile_id,
    'CampusHire Demo Systems',
    'A sample technology company for demonstrating campus recruitment workflows.',
    'https://example.com',
    'Bengaluru',
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
WHERE @demo_recruiter_profile_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM companies
      WHERE recruiter_profile_id = @demo_recruiter_profile_id
        AND name = 'CampusHire Demo Systems'
  );

INSERT INTO jobs (
    company_id, title, description, location, employment_type, salary,
    eligible_branch, minimum_cgpa, application_deadline, status, created_at, updated_at
)
SELECT c.id,
       'Graduate Software Engineer',
       'Build and test services with a supportive engineering team. Applicants should be comfortable with Java, Spring Boot, and SQL.',
       'Bengaluru',
       'FULL_TIME',
       900000.00,
       'Computer Science',
       7.00,
       CURRENT_DATE + INTERVAL 30 DAY,
       'APPROVED',
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6)
FROM companies c
WHERE c.recruiter_profile_id = @demo_recruiter_profile_id
  AND c.name = 'CampusHire Demo Systems'
  AND NOT EXISTS (
      SELECT 1 FROM jobs j
      WHERE j.company_id = c.id AND j.title = 'Graduate Software Engineer'
  );

INSERT INTO jobs (
    company_id, title, description, location, employment_type, salary,
    eligible_branch, minimum_cgpa, application_deadline, status, created_at, updated_at
)
SELECT c.id,
       'Data Analyst Intern',
       'Help teams turn operational data into clear reports and useful insights using SQL, Python, and spreadsheets.',
       'Remote',
       'INTERNSHIP',
       300000.00,
       NULL,
       6.50,
       CURRENT_DATE + INTERVAL 45 DAY,
       'APPROVED',
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6)
FROM companies c
WHERE c.recruiter_profile_id = @demo_recruiter_profile_id
  AND c.name = 'CampusHire Demo Systems'
  AND NOT EXISTS (
      SELECT 1 FROM jobs j
      WHERE j.company_id = c.id AND j.title = 'Data Analyst Intern'
  );

INSERT INTO jobs (
    company_id, title, description, location, employment_type, salary,
    eligible_branch, minimum_cgpa, application_deadline, status, created_at, updated_at
)
SELECT c.id,
       'Junior Web Developer',
       'Contribute to responsive web experiences and collaborate with designers and engineers. Familiarity with React, JavaScript, and Git is useful.',
       'Pune',
       'FULL_TIME',
       750000.00,
       'Information Technology',
       7.50,
       CURRENT_DATE + INTERVAL 60 DAY,
       'APPROVED',
       CURRENT_TIMESTAMP(6),
       CURRENT_TIMESTAMP(6)
FROM companies c
WHERE c.recruiter_profile_id = @demo_recruiter_profile_id
  AND c.name = 'CampusHire Demo Systems'
  AND NOT EXISTS (
      SELECT 1 FROM jobs j
      WHERE j.company_id = c.id AND j.title = 'Junior Web Developer'
  );

INSERT INTO job_required_skills (job_id, skill)
SELECT j.id, required.skill
FROM jobs j
JOIN companies c ON c.id = j.company_id
JOIN (
    SELECT 'Graduate Software Engineer' AS title, 'Java' AS skill
    UNION ALL SELECT 'Graduate Software Engineer', 'Spring Boot'
    UNION ALL SELECT 'Graduate Software Engineer', 'SQL'
    UNION ALL SELECT 'Data Analyst Intern', 'SQL'
    UNION ALL SELECT 'Data Analyst Intern', 'Python'
    UNION ALL SELECT 'Data Analyst Intern', 'Excel'
    UNION ALL SELECT 'Junior Web Developer', 'React'
    UNION ALL SELECT 'Junior Web Developer', 'JavaScript'
    UNION ALL SELECT 'Junior Web Developer', 'Git'
) required ON required.title = j.title
WHERE c.recruiter_profile_id = @demo_recruiter_profile_id
  AND c.name = 'CampusHire Demo Systems'
  AND NOT EXISTS (
      SELECT 1
      FROM job_required_skills existing
      WHERE existing.job_id = j.id AND existing.skill = required.skill
  );

COMMIT;

SELECT c.name AS company,
       j.title AS job_title,
       j.status,
       j.application_deadline,
       COUNT(skill.job_id) AS required_skill_count
FROM companies c
JOIN jobs j ON j.company_id = c.id
LEFT JOIN job_required_skills skill ON skill.job_id = j.id
WHERE c.recruiter_profile_id = @demo_recruiter_profile_id
  AND c.name = 'CampusHire Demo Systems'
GROUP BY c.id, c.name, j.id, j.title, j.status, j.application_deadline
ORDER BY j.title;
