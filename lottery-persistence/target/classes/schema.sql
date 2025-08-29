-- Lottery Targeting System Database Schema

-- Lottery Configurations Table
CREATE TABLE IF NOT EXISTS lottery_configurations (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    min_number INTEGER NOT NULL,
    max_number INTEGER NOT NULL,
    draw_size INTEGER NOT NULL,
    pattern_length INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Prize Structure Table (dynamic tiers)
CREATE TABLE IF NOT EXISTS prize_structures (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lottery_config_id VARCHAR(50) NOT NULL,
    tier_name VARCHAR(20) NOT NULL,
    match_count INTEGER NOT NULL,
    description VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (lottery_config_id) REFERENCES lottery_configurations(id),
    UNIQUE(lottery_config_id, tier_name)
);

-- Pattern Groups Table
CREATE TABLE IF NOT EXISTS pattern_groups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lottery_config_id VARCHAR(50) NOT NULL,
    pattern_type VARCHAR(10) NOT NULL CHECK (pattern_type IN ('HOT', 'WARM', 'COLD')),
    efficiency_multiplier DECIMAL(5,4) DEFAULT 1.0000,
    total_analyzed_draws INTEGER DEFAULT 0,
    last_analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lottery_config_id) REFERENCES lottery_configurations(id),
    UNIQUE(lottery_config_id, pattern_type)
);

-- Hot Numbers Table
CREATE TABLE IF NOT EXISTS hot_numbers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pattern_group_id BIGINT NOT NULL,
    number_pattern VARCHAR(100) NOT NULL,
    frequency_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pattern_group_id) REFERENCES pattern_groups(id)
);

-- Proven Combinations Table
CREATE TABLE IF NOT EXISTS proven_combinations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lottery_config_id VARCHAR(50) NOT NULL,
    combination_numbers VARCHAR(255) NOT NULL,
    tier_achieved VARCHAR(20) NOT NULL,
    draw_date DATE,
    confidence_score DECIMAL(5,4) DEFAULT 0.0000,
    times_appeared INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lottery_config_id) REFERENCES lottery_configurations(id)
);

-- User Sessions Table
CREATE TABLE IF NOT EXISTS user_sessions (
    session_id VARCHAR(100) PRIMARY KEY,
    lottery_config_id VARCHAR(50) NOT NULL,
    target_tier VARCHAR(20) NOT NULL,
    number_of_tickets INTEGER NOT NULL,
    generation_strategy VARCHAR(50) NOT NULL,
    budget DECIMAL(10,2),
    preferences TEXT, -- JSON string
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (lottery_config_id) REFERENCES lottery_configurations(id)
);

-- Generated Ticket Sets Table
CREATE TABLE IF NOT EXISTS generated_ticket_sets (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    diversity_score DECIMAL(5,4) DEFAULT 0.0000,
    pattern_coverage_score DECIMAL(5,4) DEFAULT 0.0000,
    expected_hit_rate DECIMAL(5,4) DEFAULT 0.0000,
    overall_confidence DECIMAL(5,4) DEFAULT 0.0000,
    recommendation_level VARCHAR(20),
    patterns_used TEXT, -- JSON string
    tier_probabilities TEXT, -- JSON string
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES user_sessions(session_id)
);

-- Lottery Tickets Table
CREATE TABLE IF NOT EXISTS lottery_tickets (
    ticket_id VARCHAR(100) PRIMARY KEY,
    ticket_set_id BIGINT NOT NULL,
    numbers VARCHAR(255) NOT NULL, -- Comma-separated numbers
    pattern_used VARCHAR(100),
    confidence_score DECIMAL(5,4) DEFAULT 0.0000,
    metadata TEXT, -- JSON string
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (ticket_set_id) REFERENCES generated_ticket_sets(id)
);

-- Indexes for better performance
CREATE INDEX IF NOT EXISTS idx_pattern_groups_lottery_config ON pattern_groups(lottery_config_id);
CREATE INDEX IF NOT EXISTS idx_hot_numbers_pattern_group ON hot_numbers(pattern_group_id);
CREATE INDEX IF NOT EXISTS idx_proven_combinations_lottery_config ON proven_combinations(lottery_config_id);
CREATE INDEX IF NOT EXISTS idx_proven_combinations_tier ON proven_combinations(tier_achieved);
CREATE INDEX IF NOT EXISTS idx_user_sessions_lottery_config ON user_sessions(lottery_config_id);
CREATE INDEX IF NOT EXISTS idx_generated_ticket_sets_session ON generated_ticket_sets(session_id);
CREATE INDEX IF NOT EXISTS idx_lottery_tickets_set ON lottery_tickets(ticket_set_id);