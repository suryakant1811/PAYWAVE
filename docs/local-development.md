# Local development

Java and Maven were not available on the machine that authored this repository. Compile and run it where JDK 21 and Maven 3.9 are installed.

This repository does not contain Dockerfiles or a Compose file. Container images are yours to write. `docker/mysql/init.sh` only creates the four databases and can be mounted into a MySQL 8 container when you build that image setup.

## Databases

Create these schemas before starting the matching service:

- `account_db` for account-service, port 8081
- `wallet_db` for wallet-service, port 8082
- `payment_db` for payment-service, port 8083
- `settlement_db` for settlement-service, port 8084

```bash
cp .env.example .env
set -a && source .env && set +a
export DB_URL='jdbc:mysql://localhost:3306/wallet_db?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC'
mvn -pl wallet-service -am spring-boot:run
```

Start account-service, wallet-service, payment-service, and settlement-service, then the gateway on port 8080. The gateway is the URL the client should use. Open `http://localhost:8080/` for the embedded demo UI (no extra UI process).

## EC2: fat JAR check

If `java -jar api-gateway/target/api-gateway-1.0.0.jar` exits immediately and `curl` cannot connect to 8080, the JAR is probably too small (thin JAR, often ~15 KiB). A runnable gateway JAR is tens of megabytes and contains `BOOT-INF/lib/`.

```bash
./scripts/build-fat-jars.sh
# or only the gateway:
mvn -pl api-gateway -am clean package -DskipTests
mvn -pl api-gateway spring-boot:repackage
./scripts/verify-fat-jar.sh api-gateway/target/api-gateway-1.0.0.jar
ls -lh api-gateway/target/api-gateway-1.0.0.jar
```

If the JAR is only ~25 KiB, `spring-boot:repackage` did not run. This repo does not use `spring-boot-starter-parent`, so each service module must bind the repackage goal (see parent `pom.xml`). After upgrading the POM, run `git pull` and `build-fat-jars.sh` again.

After a code change on your laptop, **commit and push**, then `git pull` on EC2. If `git pull` says “Already up to date” but the UI still fails, the laptop changes are not on GitHub yet.

Leave `PAYWAVE_MESSAGING_MODE` unset to log domain events. Set it to `http` on wallet-service, payment-service, and settlement-service when you want those events stored as notifications. Set it to `kafka` only after a broker exists and the topic `paywave.domain-events` is created. Account-service listens on that topic in kafka mode.

## Swagger

Open `http://localhost:8081/swagger-ui.html` for account-service. Wallet is 8082, payment is 8083, settlement is 8084, and the gateway is 8080. Call the gateway when you want the same path a mobile app uses.

## Tests

```bash
mvn test
mvn -pl wallet-service -am test -Pintegration
```

The integration test starts MySQL with Testcontainers, so that command needs Docker even though the application images are not defined here.
