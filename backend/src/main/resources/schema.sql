CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `username` VARCHAR(64) NOT NULL UNIQUE,
  `password` VARCHAR(256) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `role` VARCHAR(32) NOT NULL,
  `phone` VARCHAR(20),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `artifact` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `artifact_code` VARCHAR(64) NOT NULL UNIQUE,
  `name` VARCHAR(128) NOT NULL,
  `era` VARCHAR(64),
  `material` VARCHAR(64),
  `dimensions` VARCHAR(128),
  `source` VARCHAR(256),
  `acquisition_date` DATE,
  `status` VARCHAR(32) NOT NULL DEFAULT 'REGISTERED',
  `description` TEXT,
  `photo_url` VARCHAR(512),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `artifact_restriction` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `artifact_id` BIGINT NOT NULL,
  `no_exhibition` BOOLEAN DEFAULT FALSE,
  `no_exhibition_reason` TEXT,
  `no_loan` BOOLEAN DEFAULT FALSE,
  `no_loan_reason` TEXT,
  `min_loan_interval_days` INT DEFAULT 0,
  `max_loan_days` INT DEFAULT 180,
  `loan_conditions` TEXT,
  FOREIGN KEY (`artifact_id`) REFERENCES `artifact`(`id`)
);

CREATE TABLE IF NOT EXISTS `disease` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `artifact_id` BIGINT NOT NULL,
  `disease_type` VARCHAR(32) NOT NULL,
  `severity` VARCHAR(16) NOT NULL,
  `description` TEXT,
  `location` VARCHAR(256),
  `photo_url` VARCHAR(512),
  `reported_by` BIGINT NOT NULL,
  `reported_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`artifact_id`) REFERENCES `artifact`(`id`),
  FOREIGN KEY (`reported_by`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `restoration` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `artifact_id` BIGINT NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `plan` TEXT,
  `cleaning_method` VARCHAR(128),
  `reinforcement_material` VARCHAR(128),
  `status` VARCHAR(32) NOT NULL DEFAULT 'PROPOSED',
  `proposed_by` BIGINT NOT NULL,
  `approved_by` BIGINT,
  `proposed_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `approved_at` DATETIME,
  `completed_at` DATETIME,
  FOREIGN KEY (`artifact_id`) REFERENCES `artifact`(`id`),
  FOREIGN KEY (`proposed_by`) REFERENCES `user`(`id`),
  FOREIGN KEY (`approved_by`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `restoration_step` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `restoration_id` BIGINT NOT NULL,
  `step_order` INT NOT NULL,
  `action` VARCHAR(128) NOT NULL,
  `description` TEXT,
  `material_used` VARCHAR(128),
  `before_photo_url` VARCHAR(512),
  `after_photo_url` VARCHAR(512),
  `operator_id` BIGINT NOT NULL,
  `operated_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`restoration_id`) REFERENCES `restoration`(`id`),
  FOREIGN KEY (`operator_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `loan` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `artifact_id` BIGINT NOT NULL,
  `applicant_id` BIGINT NOT NULL,
  `borrowing_institution` VARCHAR(128) NOT NULL,
  `exhibition_name` VARCHAR(128) NOT NULL,
  `loan_start_date` DATE NOT NULL,
  `loan_end_date` DATE NOT NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `rejection_reason` TEXT,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `approved_at` DATETIME,
  FOREIGN KEY (`artifact_id`) REFERENCES `artifact`(`id`),
  FOREIGN KEY (`applicant_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `loan_environment` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `loan_id` BIGINT NOT NULL,
  `temperature_min` DECIMAL(5,2),
  `temperature_max` DECIMAL(5,2),
  `humidity_min` DECIMAL(5,2),
  `humidity_max` DECIMAL(5,2),
  `illuminance_max` DECIMAL(8,2),
  `vibration_max` DECIMAL(8,4),
  `security_route` TEXT,
  `setup_date` DATE,
  FOREIGN KEY (`loan_id`) REFERENCES `loan`(`id`)
);

CREATE TABLE IF NOT EXISTS `insurance` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `loan_id` BIGINT NOT NULL,
  `appraised_value` DECIMAL(14,2) NOT NULL,
  `deductible` DECIMAL(14,2),
  `deductible_clause` TEXT,
  `transport_liability` TEXT,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `verified_by` BIGINT,
  `effective_date` DATE,
  `expiry_date` DATE,
  `verified_at` DATETIME,
  FOREIGN KEY (`loan_id`) REFERENCES `loan`(`id`),
  FOREIGN KEY (`verified_by`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `transport` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `loan_id` BIGINT NOT NULL,
  `packaging_standard` VARCHAR(128),
  `box_code` VARCHAR(64),
  `box_spec` VARCHAR(128),
  `inner_material` VARCHAR(128),
  `seal_code` VARCHAR(64),
  `sealed_at` DATETIME,
  `sealed_by` BIGINT,
  `escort_id` BIGINT,
  `route` VARCHAR(512),
  `status` VARCHAR(32) NOT NULL DEFAULT 'PREPARING',
  `departed_at` DATETIME,
  `arrived_at` DATETIME,
  FOREIGN KEY (`loan_id`) REFERENCES `loan`(`id`),
  FOREIGN KEY (`sealed_by`) REFERENCES `user`(`id`),
  FOREIGN KEY (`escort_id`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `transport_monitor` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `transport_id` BIGINT NOT NULL,
  `temperature` DECIMAL(5,2),
  `humidity` DECIMAL(5,2),
  `vibration` DECIMAL(8,4),
  `gps_location` VARCHAR(128),
  `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`transport_id`) REFERENCES `transport`(`id`)
);

CREATE TABLE IF NOT EXISTS `exhibition` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `loan_id` BIGINT NOT NULL,
  `venue` VARCHAR(128),
  `showcase_code` VARCHAR(64),
  `setup_at` DATETIME,
  `setup_confirmed_by` BIGINT,
  `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  FOREIGN KEY (`loan_id`) REFERENCES `loan`(`id`),
  FOREIGN KEY (`setup_confirmed_by`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `environment_monitor` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `exhibition_id` BIGINT NOT NULL,
  `temperature` DECIMAL(5,2),
  `humidity` DECIMAL(5,2),
  `illuminance` DECIMAL(8,2),
  `vibration` DECIMAL(8,4),
  `is_alert` BOOLEAN DEFAULT FALSE,
  `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`exhibition_id`) REFERENCES `exhibition`(`id`)
);

CREATE TABLE IF NOT EXISTS `return_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `loan_id` BIGINT NOT NULL,
  `return_transport_id` BIGINT,
  `overall_status` VARCHAR(32) NOT NULL DEFAULT 'PENDING',
  `return_notes` TEXT,
  `received_by` BIGINT,
  `received_at` DATETIME,
  FOREIGN KEY (`loan_id`) REFERENCES `loan`(`id`),
  FOREIGN KEY (`return_transport_id`) REFERENCES `transport`(`id`),
  FOREIGN KEY (`received_by`) REFERENCES `user`(`id`)
);

CREATE TABLE IF NOT EXISTS `damage_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `return_id` BIGINT NOT NULL,
  `damage_type` VARCHAR(32) NOT NULL,
  `description` TEXT,
  `photo_url` VARCHAR(512),
  `responsibility` VARCHAR(256),
  `recorded_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`return_id`) REFERENCES `return_record`(`id`)
);

CREATE TABLE IF NOT EXISTS `evidence_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `artifact_id` BIGINT NOT NULL,
  `action` VARCHAR(64) NOT NULL,
  `from_status` VARCHAR(32),
  `to_status` VARCHAR(32),
  `operator_id` BIGINT NOT NULL,
  `photo_url` VARCHAR(512),
  `description` TEXT,
  `responsibility_impact` VARCHAR(256),
  `related_entity_id` BIGINT,
  `related_entity_type` VARCHAR(32),
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (`artifact_id`) REFERENCES `artifact`(`id`),
  FOREIGN KEY (`operator_id`) REFERENCES `user`(`id`)
);
