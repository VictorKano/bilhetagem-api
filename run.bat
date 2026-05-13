@echo off
REM ============================================================
REM run.bat — Build and run api-cobranca-bilhetagem (Windows)
REM ============================================================

REM --------------- Configurable environment variables --------
IF NOT DEFINED DB_HOST          SET DB_HOST=localhost
IF NOT DEFINED DB_PORT          SET DB_PORT=5432
IF NOT DEFINED DB_NAME          SET DB_NAME=cobranca
IF NOT DEFINED DB_USER          SET DB_USER=postgres
IF NOT DEFINED DB_PASSWORD      SET DB_PASSWORD=postgres

IF NOT DEFINED REDIS_HOST       SET REDIS_HOST=localhost
IF NOT DEFINED REDIS_PORT       SET REDIS_PORT=6379

IF NOT DEFINED KAFKA_BOOTSTRAP_SERVERS SET KAFKA_BOOTSTRAP_SERVERS=localhost:9092

IF NOT DEFINED SERVER_PORT      SET SERVER_PORT=8080
REM -----------------------------------------------------------

SET JAR=target\api-cobranca-bilhetagem-1.0.0-SNAPSHOT.jar

echo ==================================================
echo   api-cobranca-bilhetagem
echo ==================================================
echo   DB   : jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%
echo   Redis: %REDIS_HOST%:%REDIS_PORT%
echo   Kafka: %KAFKA_BOOTSTRAP_SERVERS%
echo   Port : %SERVER_PORT%
echo ==================================================

REM Build if JAR is missing or --build flag is passed
IF NOT EXIST "%JAR%" GOTO build
IF "%1"=="--build" GOTO build
GOTO run

:build
echo.
echo [1/2] Building with Maven (skipping tests)...
IF EXIST "mvnw.cmd" (
    call mvnw.cmd clean package -DskipTests
) ELSE (
    where mvn >nul 2>&1
    IF ERRORLEVEL 1 (
        echo ERROR: Neither mvnw.cmd nor mvn were found.
        echo Install Maven: winget install Apache.Maven
        echo Then run: mvn -N wrapper:wrapper
        exit /b 1
    )
    call mvn clean package -DskipTests
)
IF ERRORLEVEL 1 (
    echo Build failed. Aborting.
    exit /b 1
)
echo [1/2] Build complete.

:run
echo.
echo [2/2] Starting application...
java -jar "%JAR%" --server.port=%SERVER_PORT%
