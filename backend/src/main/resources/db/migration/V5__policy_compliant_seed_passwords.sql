-- Seeded accounts get passwords that meet the registration policy
-- (12+ characters, upper and lower case, digit, special character).
-- New passwords: Demo-Client-2026, Admin-Review-2026, Second-Review-2026
UPDATE client
SET password_hash = '$2a$10$/9rUtTcJ5ZtGW8MCPl6LH.V3iOnsDoLeZnyeX6URd9iGVnX3582Q6'
WHERE email = 'demo@crego.test';

UPDATE bo_user
SET password_hash = '$2a$10$siHIEs3J3Ksj7/FLxlLIiuim5bixLH0jRwiHMEYWBJSasP3mpmJI.'
WHERE email = 'admin@crego.test';

UPDATE bo_user
SET password_hash = '$2a$10$vM/ZVS6khzI2f0XYReWTe.2iVkiWhpgNyOgGoxMZsy8R1d.bHUPWq'
WHERE email = 'reviewer@crego.test';
