-- =============================================================================
-- HP CARE AUTHORISED SERVICE DESK — MYSQL DATABASE SCHEMA
-- Partner: HOME COMFORTS · 89 ZONE 2 M.P. NAGAR BHOPAL
-- GST NO: 23ABCPA4215G1ZJ | PAN: ABCPA4215G | Helpline: 8962194727 / 8962524727
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `hp_care_db` 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `hp_care_db`;

-- 1. Users / Service Engineers & Administrators Table
CREATE TABLE IF NOT EXISTS `users` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `full_name` VARCHAR(100) NOT NULL,
    `role` VARCHAR(50) NOT NULL DEFAULT 'SERVICE_ENGINEER', -- SUPER_ADMIN, ADMIN, SERVICE_ENGINEER
    `phone` VARCHAR(20),
    `email` VARCHAR(100),
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Administrators & Engineers
INSERT INTO `users` (`username`, `password`, `full_name`, `role`, `phone`, `email`)
VALUES 
    ('anchit', 'anchitsir', 'Anchit Sir (Owner / Super Admin)', 'SUPER_ADMIN', '8962194727', 'hpcarebhopal@gmail.com'),
    ('ayush', 'ayush', 'Ayush Sharma (Admin Desk)', 'ADMIN', '8962524727', 'hpcarebhopal@gmail.com'),
    ('vibhor', 'vibhor', 'Vibhor (Service Engineer)', 'SERVICE_ENGINEER', '8962194727', 'hpcarebhopal@gmail.com'),
    ('danish', 'danish', 'Danish (Service Engineer)', 'SERVICE_ENGINEER', '8962524727', 'hpcarebhopal@gmail.com')
ON DUPLICATE KEY UPDATE `role` = VALUES(`role`), `full_name` = VALUES(`full_name`);

-- 2. Owner Security & Connected Client Sessions Table (Exclusive to Anchit Sir)
CREATE TABLE IF NOT EXISTS `connected_clients` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `client_ip` VARCHAR(50) NOT NULL,
    `device_info` VARCHAR(255),
    `username` VARCHAR(50),
    `api_key` VARCHAR(100),
    `action` VARCHAR(100) DEFAULT 'CONNECTED',
    `last_active` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Service Cases Table (Supports UW, OOW, and Quotations)
CREATE TABLE IF NOT EXISTS `cases` (
    `id` VARCHAR(50) PRIMARY KEY,
    `case_no` VARCHAR(50) NOT NULL,
    `quote_no` VARCHAR(50),
    `quote_date` VARCHAR(50),
    `obligation` VARCHAR(50) NOT NULL DEFAULT 'Under Warranty', -- 'Under Warranty', 'Out of Warranty', 'Quotation'
    `case_type` VARCHAR(50) NOT NULL DEFAULT 'Under Warranty',
    `partner_name` VARCHAR(100) DEFAULT 'HOME COMFORTS',
    `branch_address` VARCHAR(255) DEFAULT '89 ZONE 2 M.P. NAGAR BHOPAL',
    `phone` VARCHAR(50) DEFAULT '8962194727 / 8962524727',
    `email_id` VARCHAR(100) DEFAULT 'hpcarebhopal1@gmail.com',
    `receiving_date_time` VARCHAR(100),
    `purchase_date` VARCHAR(50),
    
    -- Customer Details
    `customer_name` VARCHAR(150) NOT NULL,
    `company` VARCHAR(150),
    `mobile` VARCHAR(50) NOT NULL,
    `alt_contact` VARCHAR(50),
    `email` VARCHAR(150),
    `address` TEXT,
    `city` VARCHAR(100) DEFAULT 'BHOPAL',
    `state` VARCHAR(100) DEFAULT 'Madhya Pradesh',
    `pincode` VARCHAR(20) DEFAULT '462022',
    `cust_gst_no` VARCHAR(50),
    
    -- Device Specs
    `model` VARCHAR(150) NOT NULL,
    `product_id` VARCHAR(100),
    `serial` VARCHAR(100) NOT NULL,
    `password` VARCHAR(100),
    `os` VARCHAR(100) DEFAULT 'WINDOWS 11',
    
    -- Service Complaint, Diagnosis & Condition
    `complaint` TEXT NOT NULL,
    `cust_remarks` TEXT,
    `resolution_summary` TEXT,
    `scratches_condition` TEXT,
    `damages_condition` TEXT,
    `physical_condition` TEXT,
    `accessories` TEXT,
    `recommendations` TEXT,
    
    -- Financials & Payment
    `amount` VARCHAR(50) DEFAULT 'NA',
    `payment_mode` VARCHAR(50) DEFAULT 'NA',
    `payment_remarks` TEXT,
    
    -- Post-Checklist & Diagnostic Data (JSON stringified)
    `post_checklist` TEXT,
    `hardware` TEXT,
    `parts_used_returned` TEXT,
    
    -- Tracking & Status
    `status` VARCHAR(50) DEFAULT 'LOGGED',
    `priority` VARCHAR(20) DEFAULT 'Medium',
    `engineer` VARCHAR(100) DEFAULT 'Danish',
    `date_opened` VARCHAR(50),
    `last_updated` VARCHAR(50),
    `actual_date` VARCHAR(50),
    `escalation` VARCHAR(10) DEFAULT 'No',
    
    -- Intake Times
    `in_time` VARCHAR(20) DEFAULT '11:30 AM',
    `demo_st` VARCHAR(20) DEFAULT '12:00 PM',
    `demo_finish` VARCHAR(20) DEFAULT '12:30 PM',
    `log_st` VARCHAR(20) DEFAULT '12:45 PM',
    `log_finish` VARCHAR(20) DEFAULT '01:00 PM',
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX `idx_case_no` (`case_no`),
    INDEX `idx_serial` (`serial`),
    INDEX `idx_mobile` (`mobile`),
    INDEX `idx_obligation` (`obligation`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Quotation Spare Parts & Labor Items
CREATE TABLE IF NOT EXISTS `case_parts` (
    `id` INT AUTO_INCREMENT PRIMARY KEY,
    `case_id` VARCHAR(50) NOT NULL,
    `part_no` VARCHAR(100),
    `description` TEXT,
    `rate` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `gst_rate` DECIMAL(6, 2) NOT NULL DEFAULT 18.00,
    `qty` INT NOT NULL DEFAULT 1,
    `amount` DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_case_parts` FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Device Inspection Photos (Camera & File Uploads)
CREATE TABLE IF NOT EXISTS `case_photos` (
    `id` VARCHAR(100) PRIMARY KEY,
    `case_id` VARCHAR(50) NOT NULL,
    `url` TEXT NOT NULL,
    `label` VARCHAR(100) DEFAULT 'Visual Record',
    `timestamp` VARCHAR(100),
    `is_primary` BOOLEAN DEFAULT FALSE,
    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_case_photos` FOREIGN KEY (`case_id`) REFERENCES `cases` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed Initial Baseline Cases (Starting at HC BPL 1800)
INSERT INTO `cases` (
    `id`, `case_no`, `quote_no`, `quote_date`, `obligation`, `case_type`, `partner_name`, `branch_address`,
    `phone`, `email_id`, `receiving_date_time`, `purchase_date`, `customer_name`, `company`, `mobile`, `alt_contact`,
    `email`, `address`, `city`, `state`, `pincode`, `model`, `product_id`, `serial`, `password`, `os`,
    `complaint`, `cust_remarks`, `resolution_summary`, `scratches_condition`, `damages_condition`, `accessories`, `recommendations`,
    `amount`, `payment_mode`, `payment_remarks`, `status`, `priority`, `engineer`, `date_opened`, `last_updated`
) VALUES 
(
    'HC BPL 1800', 'HC BPL 1800', '', '', 'Under Warranty', 'Under Warranty', 'HOME COMFORTS', '89 ZONE 2 M.P. NAGAR BHOPAL',
    '8962194727 / 8962524727', 'hpcarebhopal1@gmail.com', '12-Aug-2026 10:30:00 AM', '10-Jan-25', 'SANJAY VERMA', 'SANJAY VERMA', '9826112233', '9826001122',
    'SANJAY.VERMA@GMAIL.COM', 'M.P. NAGAR ZONE 1, BHOPAL', 'BHOPAL', 'Madhya Pradesh', '462011', '15-EG2009TU / 67U34PA', '67U34PA', '5CD4120891', '1234', 'WINDOWS 11',
    'KEYBOARD KEYS NOT WORKING (SPACE & ENTER KEYS STUCK)', 'Keyboard top assembly inspection in progress.', 'REPLACED TOP COVER WITH KEYBOARD UNDER WARRANTY.',
    'Minor Hairline Scratches (Normal Wear)', 'No Physical Damage / Intact Body', '[\"AC Adapter / Charger\"]', 'Completed warranty replacement.',
    'NA', 'NA', 'Under Warranty Repair - Free', 'CLOSED', 'Medium', 'Danish', '12-Aug-2026', '12-Aug-2026'
),
(
    'HC BPL 1803', 'HC BPL 1803', '', '', 'Under Warranty', 'Under Warranty', 'HOME COMFORTS', '89 ZONE 2 M.P. NAGAR BHOPAL',
    '8962194727 / 8962524727', 'hpcarebhopal1@gmail.com', '14-Aug-2026 12:00:00 AM', '01-Jul-24', 'VISHAL', 'VISHAL', '9015561917', '',
    'OFFICIALVISHALKUMAR8@GMAIL.COM', 'BHOPAL', 'BHOPAL', 'Madhya Pradesh', '462022', '16-S0095AX/8R1E6PA', '8R1E6PA', 'CND33606C5', '', 'WINDOWS 11',
    'LAPTOP SUDDENLY OFF', 'CHECK AND FOUND THAT THERE WAS ISSUE WITH SPS-PCA IR-SENSOR , INITIAL ISSUE RESOLVED AFTER REPLACEMENT , BUT FLICKERING ISSUE WAS OBSERVED WHILE CHECKING . SUSPECTED PART IS MOTHERBOARD , BUT DAMAGE IS FOUND ON BOARD CONNECTORS.',
    'CHECK AND FOUND THAT THERE WAS ISSUE WITH SPS-PCA IR-SENSOR , INITIAL ISSUE RESOLVED AFTER REPLACEMENT , BUT FLICKERING ISSUE WAS OBSERVED WHILE CHECKING . SUSPECTED PART IS MOTHERBOARD , BUT DAMAGE IS FOUND ON BOARD CONNECTORS.',
    'Minor hairline scratches on top A-cover', 'Internal board connector damage observed; casing intact', '[\"AC Adapter / Charger\", \"Power Cord / AC Cable\"]',
    'Board replacement and ultrasonic connector treatment recommended.', 'NA', 'NA', 'Under Warranty Service',
    'IN_REPAIR', 'High', 'Danish', '14-Aug-2026 12:00:00 AM', '14-Aug-2026 04:30 PM'
),
(
    'HC BPL 1804', 'HC BPL 1804', '', '', 'Out of Warranty', 'Out of Warranty', 'HOME COMFORTS', '89 ZONE 2 M.P. NAGAR BHOPAL',
    '8962194727 / 8962524727', 'hpcarebhopal1@gmail.com', '16-Aug-2026 03:15:00 PM', '', 'ROHIT GUPTA', 'ROHIT GUPTA', '9893012345', '',
    'ROHIT.GUPTA@YAHOO.COM', 'ARERA COLONY, BHOPAL', 'BHOPAL', 'Madhya Pradesh', '462016', '14S-DQ2606TU', '50M71PA', '5CD2094KLL', '9988', 'WINDOWS 11',
    'OS CRASH & FAN NOISE [OVERHEATING]', 'Thermal paste dry, CPU fan clogged with dust.', 'HEATSINK CLEANED & THERMAL GREASE APPLIED.',
    'A-Cover & Palmrest Scratches', 'No Physical Damage', '[\"AC Adapter / Charger\"]', 'Thermal servicing completed.',
    '1180', 'UPI', 'Standard OOW Inspection & Thermal Service: ₹1,000 + 18% GST = ₹1,180', 'CLOSED', 'Medium', 'Ayush', '16-Aug-2026', '16-Aug-2026'
),
(
    '20254', '20254', '20254', '8/9/2026', 'Quotation', 'Quotation', 'HOME COMFORTS', '89 Zone 2 M.P. Nagar Bhopal-462011',
    '8962194727', 'hpcarebhopal@gmail.com', '08-Sep-2026 11:30:00 AM', '', 'RUKMANI LODHI', 'RUKMANI LODHI', '924412702090', '',
    'LVUKMANI96@GMAIL.COM', 'BHOPAL ( M.P )', 'BHOPAL', 'Madhya Pradesh', '462011', '15S-FQ5185TU', '7Q6Z7PA', '5CD3333C1Z', '', 'WINDOWS 11',
    'DISPLAY PANEL CRACKED / WHITE LINES ON SCREEN', 'Customer requested official quotation for 15.6 FHD AG SVA Display replacement.',
    'Quotation provided for display replacement.', 'Minor Hairline Scratches (Normal Wear)', 'Internal LCD matrix cracked; outer bezel intact', '[\"AC Adapter / Charger\"]',
    'Replace 15.6 FHD raw panel and complete display hinge servicing.', '12273', 'Advance Cash / DD', 'Payment is 100% Advance by Cash/DD in favour of HOME COMFORTS',
    'AWAITING_APPROVAL', 'Medium', 'Danish', '8/9/2026', '8/9/2026'
)
ON DUPLICATE KEY UPDATE `customer_name` = VALUES(`customer_name`);

-- Seed parts for quote 20254
INSERT INTO `case_parts` (`case_id`, `part_no`, `description`, `rate`, `gst_rate`, `qty`, `amount`)
VALUES 
('20254', 'M14025-001', 'SPS-LCD RAWPNL 15.6 FHD AG SVA 250 uslim', 9400.76, 18.00, 1, 11092.90),
('20254', 'HPSERVICECHARGE', 'HP Service & Installation Labor (SAC 998713)', 1000.00, 18.00, 1, 1180.00);
