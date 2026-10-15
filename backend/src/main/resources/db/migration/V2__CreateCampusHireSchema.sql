CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(190) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    enabled BIT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS student_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    branch VARCHAR(100),
    cgpa DECIMAL(4,2),
    graduation_year INT,
    skills VARCHAR(2000),
    projects VARCHAR(4000),
    resume_url VARCHAR(2048),
    PRIMARY KEY (id),
    CONSTRAINT uk_student_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_student_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS recruiter_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    job_title VARCHAR(100),
    phone VARCHAR(30),
    PRIMARY KEY (id),
    CONSTRAINT uk_recruiter_profiles_user UNIQUE (user_id),
    CONSTRAINT fk_recruiter_profiles_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS companies (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    recruiter_profile_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    website VARCHAR(255),
    headquarters VARCHAR(120),
    PRIMARY KEY (id),
    CONSTRAINT fk_companies_recruiter FOREIGN KEY (recruiter_profile_id) REFERENCES recruiter_profiles (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS placement_drives (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by_user_id BIGINT NOT NULL,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(1000),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    venue VARCHAR(120),
    PRIMARY KEY (id),
    CONSTRAINT fk_placement_drives_user FOREIGN KEY (created_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    company_id BIGINT NOT NULL,
    placement_drive_id BIGINT,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(8000) NOT NULL,
    location VARCHAR(120),
    employment_type VARCHAR(80),
    salary DECIMAL(12,2),
    eligible_branch VARCHAR(100),
    minimum_cgpa DECIMAL(4,2),
    application_deadline DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    reviewed_by_user_id BIGINT,
    reviewed_at DATETIME(6),
    review_notes VARCHAR(1000),
    PRIMARY KEY (id),
    CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_jobs_placement_drive FOREIGN KEY (placement_drive_id) REFERENCES placement_drives (id),
    CONSTRAINT fk_jobs_reviewer FOREIGN KEY (reviewed_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS job_required_skills (
    job_id BIGINT NOT NULL,
    skill VARCHAR(80) NOT NULL,
    INDEX ix_job_required_skills_job (job_id),
    CONSTRAINT fk_job_required_skills_job FOREIGN KEY (job_id) REFERENCES jobs (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS job_applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    job_id BIGINT NOT NULL,
    student_profile_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    recruiter_notes VARCHAR(2000),
    PRIMARY KEY (id),
    CONSTRAINT uk_job_application_student_job UNIQUE (job_id, student_profile_id),
    CONSTRAINT fk_job_applications_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_job_applications_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS interviews (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    job_application_id BIGINT NOT NULL,
    scheduled_by_user_id BIGINT NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    interview_type VARCHAR(30),
    location_or_meeting_url VARCHAR(500),
    notes VARCHAR(2000),
    PRIMARY KEY (id),
    CONSTRAINT fk_interviews_application FOREIGN KEY (job_application_id) REFERENCES job_applications (id),
    CONSTRAINT fk_interviews_scheduler FOREIGN KEY (scheduled_by_user_id) REFERENCES users (id)
) ENGINE=InnoDB;
