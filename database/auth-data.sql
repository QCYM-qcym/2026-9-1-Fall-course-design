-- DEMO ONLY / NOT FOR PRODUCTION. Public course demonstration accounts.
-- Import into a NEW demonstration database after schema.sql then data.sql.
-- Public account credentials are documented in the project README.
-- BCrypt cost 10; this file never alters meteorological records or resets existing users.
USE shandong_weather;
SET NAMES utf8mb4;

INSERT INTO sys_user (username, password_hash, role, enabled) VALUES
('demo_user', '$2a$10$3vlG6zdlWsp4zHbwGcrxoOao51TrTpEhQsO3iZui4L2G.9IlNCUdC', 'USER', 1),
('demo_admin', '$2a$10$k2FaPePvThyKgqQj3vZslezywbTtgDXamvuW7G/Cu80qhNINEN/Oi', 'ADMIN', 1);
