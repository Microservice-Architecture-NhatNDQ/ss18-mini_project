-- Initialize distinct databases for Database-Per-Service pattern
CREATE DATABASE IF NOT EXISTS rikkeibank_identity CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rikkeibank_customer CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rikkeibank_account CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rikkeibank_transaction CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rikkeibank_notification CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Grant all privileges to root user
GRANT ALL PRIVILEGES ON rikkeibank_identity.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON rikkeibank_customer.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON rikkeibank_account.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON rikkeibank_transaction.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON rikkeibank_notification.* TO 'root'@'%';
FLUSH PRIVILEGES;
