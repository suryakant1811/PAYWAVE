#!/bin/bash
set -euo pipefail
mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE DATABASE IF NOT EXISTS account_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS wallet_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS payment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS settlement_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL PRIVILEGES ON account_db.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON wallet_db.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO '${MYSQL_USER}'@'%';
GRANT ALL PRIVILEGES ON settlement_db.* TO '${MYSQL_USER}'@'%';
FLUSH PRIVILEGES;
SQL
