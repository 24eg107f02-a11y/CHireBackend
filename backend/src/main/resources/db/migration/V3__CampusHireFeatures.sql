-- Flyway Migration V3: Comprehensive Placement Management Features
-- Adds student profile fields, job qualification, interview enhancements,
-- application status history, placement records, and notifications.

DROP PROCEDURE IF EXISTS campushire_add_column;

DELIMITER //
CREATE PROCEDURE campushire_add_column(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 1. Student Profiles additions
CALL campushire_add_column('student_profiles', 'college_id', 'VARCHAR(50)');
CALL campushire_add_column('student_profiles', 'phone', 'VARCHAR(30)');
CALL campushire_add_column('student_profiles', 'qualification', 'VARCHAR(100)');
CALL campushire_add_column('student_profiles', 'interests', 'VARCHAR(2000)');

-- 2. Job additions
CALL campushire_add_column('jobs', 'qualification', 'VARCHAR(100)');

-- 3. Interview additions
CALL campushire_add_column('interviews', 'interview_mode', 'VARCHAR(50) DEFAULT \'ONLINE\'');
CALL campushire_add_column('interviews', 'round', 'VARCHAR(100) DEFAULT \'Technical Round\'');
CALL campushire_add_column('interviews', 'status', 'VARCHAR(30) DEFAULT \'SCHEDULED\'');

DROP PROCEDURE IF EXISTS campushire_add_column;

-- 4. Application Status History Table
CREATE TABLE IF NOT EXISTS application_status_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    job_application_id BIGINT NOT NULL,
    from_status VARCHAR(30),
    to_status VARCHAR(30) NOT NULL,
    changed_by_user_id BIGINT,
    comments VARCHAR(2000),
    PRIMARY KEY (id),
    CONSTRAINT fk_status_history_application FOREIGN KEY (job_application_id) REFERENCES job_applications (id) ON DELETE CASCADE,
    CONSTRAINT fk_status_history_user FOREIGN KEY (changed_by_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- 5. Placement Records Table
CREATE TABLE IF NOT EXISTS placement_records (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    student_profile_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    package_ctc DECIMAL(12,2),
    selection_date DATE NOT NULL,
    placement_status VARCHAR(50) NOT NULL DEFAULT 'OFFERED',
    PRIMARY KEY (id),
    CONSTRAINT uk_placement_student_job UNIQUE (student_profile_id, job_id),
    CONSTRAINT fk_placement_student FOREIGN KEY (student_profile_id) REFERENCES student_profiles (id) ON DELETE CASCADE,
    CONSTRAINT fk_placement_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE CASCADE,
    CONSTRAINT fk_placement_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- 6. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    is_read BIT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB;
