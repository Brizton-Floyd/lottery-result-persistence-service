-- Sample data for testing the lottery targeting system

-- Insert sample lottery configurations
INSERT INTO lottery_configurations (id, name, min_number, max_number, draw_size, pattern_length) VALUES 
('powerball', 'Powerball', 1, 69, 5, 5),
('mega_millions', 'Mega Millions', 1, 70, 5, 5),
('lotto_texas', 'Lotto Texas', 1, 54, 6, 6),
('cash_five', 'Cash Five', 1, 35, 5, 5);

-- Insert prize structures (dynamic tiers)
INSERT INTO prize_structures (lottery_config_id, tier_name, match_count, description, is_active) VALUES
-- Powerball prize structure
('powerball', 'tier3', 3, 'Match 3 numbers', true),
('powerball', 'tier4', 4, 'Match 4 numbers', true),
('powerball', 'tier5', 5, 'Match 5 numbers', true),
('powerball', 'tier6', 6, 'Match 5 + Powerball', true),

-- Mega Millions prize structure  
('mega_millions', 'tier3', 3, 'Match 3 numbers', true),
('mega_millions', 'tier4', 4, 'Match 4 numbers', true),
('mega_millions', 'tier5', 5, 'Match 5 numbers', true),
('mega_millions', 'tier6', 6, 'Match 5 + Mega Ball', true),

-- Lotto Texas prize structure
('lotto_texas', 'tier3', 3, 'Match 3 numbers', true),
('lotto_texas', 'tier4', 4, 'Match 4 numbers', true),
('lotto_texas', 'tier5', 5, 'Match 5 numbers', true),
('lotto_texas', 'tier6', 6, 'Match 6 numbers', true),

-- Cash Five prize structure
('cash_five', 'tier2', 2, 'Match 2 numbers', true),
('cash_five', 'tier3', 3, 'Match 3 numbers', true),
('cash_five', 'tier4', 4, 'Match 4 numbers', true),
('cash_five', 'tier5', 5, 'Match 5 numbers', true);

-- Insert pattern groups
INSERT INTO pattern_groups (lottery_config_id, pattern_type, efficiency_multiplier, total_analyzed_draws, last_analysis_date) VALUES
('powerball', 'HOT', 1.2500, 1000, CURRENT_TIMESTAMP),
('powerball', 'WARM', 1.0000, 1000, CURRENT_TIMESTAMP), 
('powerball', 'COLD', 0.7500, 1000, CURRENT_TIMESTAMP),

('mega_millions', 'HOT', 1.1800, 950, CURRENT_TIMESTAMP),
('mega_millions', 'WARM', 1.0000, 950, CURRENT_TIMESTAMP),
('mega_millions', 'COLD', 0.8200, 950, CURRENT_TIMESTAMP),

('lotto_texas', 'HOT', 1.3000, 1200, CURRENT_TIMESTAMP),
('lotto_texas', 'WARM', 1.0000, 1200, CURRENT_TIMESTAMP),
('lotto_texas', 'COLD', 0.7000, 1200, CURRENT_TIMESTAMP),

('cash_five', 'HOT', 1.4000, 800, CURRENT_TIMESTAMP),
('cash_five', 'WARM', 1.0000, 800, CURRENT_TIMESTAMP),
('cash_five', 'COLD', 0.6000, 800, CURRENT_TIMESTAMP);

-- Insert sample hot numbers for each pattern group
INSERT INTO hot_numbers (pattern_group_id, number_pattern, frequency_count) VALUES
-- Powerball hot numbers
(1, '07,14,21,35,69', 45),
(1, '02,11,29,43,67', 42),
(1, '05,17,33,44,58', 38),

-- Powerball warm numbers  
(2, '08,19,26,41,55', 25),
(2, '03,22,37,49,62', 23),

-- Powerball cold numbers
(3, '01,13,28,46,66', 8),
(3, '09,24,39,52,61', 6),

-- Mega Millions hot numbers
(4, '03,17,31,45,70', 48),
(4, '07,22,38,51,64', 44),

-- Continue with other lottery games...
(7, '12,23,34,45,54', 52),
(7, '06,18,29,41,53', 47),

(10, '05,15,25,30,35', 65),
(10, '02,12,22,27,32', 58);

-- Sample proven combinations
INSERT INTO proven_combinations (lottery_config_id, combination_numbers, tier_achieved, draw_date, confidence_score, times_appeared) VALUES
('powerball', '07,14,21,35,69', 'tier5', '2024-01-15', 0.8500, 1),
('mega_millions', '03,17,31,45,70', 'tier4', '2024-02-03', 0.7200, 1),
('lotto_texas', '12,23,34,45,54', 'tier5', '2024-01-28', 0.9100, 1),
('cash_five', '05,15,25,30,35', 'tier5', '2024-02-10', 0.9500, 1);