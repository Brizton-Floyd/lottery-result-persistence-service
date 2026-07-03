# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a multi-module Spring Boot application for lottery result persistence and aggregation. The system polls state lottery websites, processes CSV and PDF files containing historical draw results, and provides REST APIs to access the data.

### Architecture

The project follows a modular Maven structure with 3 main modules:

- **lottery-result-persistence-server**: Main application entry point and REST controllers (port 8001)
- **lottery-result-aggregator**: Core processing logic for polling lottery data and file processing  
- **lottery-result-persistence-service-models**: Shared data models and DTOs

### Key Technologies

- Spring Boot 3.3.0 with Java 8
- Spring WebFlux for reactive programming
- Apache PDFBox for PDF processing
- OpenCSV for CSV parsing
- File-based persistence using Java serialization (.ser files), written atomically (temp file + fsync + atomic rename)
- Resilience4j for retry and rate limiting of outbound lottery-source fetches
- Spring Cache (in-memory `ConcurrentMapCacheManager`) for the read/catalog endpoints

## Development Commands

### Building the Application
```bash
mvn clean install
```

### Running the Application
```bash
# Run the main application
mvn spring-boot:run -pl lottery-result-persistence-server

# Or run the built JAR
java -jar lottery-result-persistence-server/target/result-persistence-service.jar
```

### Running Tests
```bash
mvn test
```

### Docker Commands
```bash
# Build Docker image
docker build -t lotto-persistence-service .

# Run with Docker Compose
docker-compose up
```

## Data Flow Architecture

1. **Application Startup**: `LotteryResultPersistenceServiceApplication` starts polling service
2. **Polling**: `LotteryResultPollingService` fetches lottery data from configured URLs in application.yml. Games are polled in a bounded thread pool (`lottery.polling.pool-size`), each fetch wrapped with Resilience4j retry and rate limiting; a single game's failure is isolated and logged rather than aborting the batch
3. **Processing**: State-specific processors (e.g., `TexasLotteryHistoryProcessor`) parse CSV files and create `LotteryGame` objects
4. **Persistence**: `LotteryGameSerializer` saves processed data as .ser files in `{base-dir}/{STATE}/` directories via a crash-safe atomic write (temp file → fsync → atomic rename)
5. **Cache invalidation**: when the poll cycle finishes it publishes a `PollCompletedEvent`; `CatalogCacheEvictor` listens for it and evicts all read/catalog caches so freshly-polled data becomes visible on the next request
6. **API Access**: REST controllers serve the persisted (and cached) data via `/api/v1` endpoints

### File Structure Pattern
- Processed lottery data is stored in `{base-dir}/{STATE_NAME}/{GAME_NAME}.ser` format
- The storage root is configurable via `lottery.storage.base-dir` (default `tmp`), which also lets tests point the real code at a temp directory
- Example: `tmp/TEXAS/Powerball.ser`

## Key Components

### Configuration
- `application.yml` contains lottery URLs and state region mappings
- URLs are organized by state with game-specific endpoints
- Currently supports Texas lottery games (CSV format)
- Operational settings (with defaults):
  - `lottery.storage.base-dir` (`tmp`): base directory for the .ser draw store, relative to the process working directory
  - `lottery.polling.pool-size` (`4`): bounded parallelism for the draw-result poll
  - `resilience4j.retry.instances.lotteryFetch`: retry policy for outbound fetches (max 3 attempts, exponential backoff, retries on `IOException`)
  - `resilience4j.ratelimiter.instances.lotteryFetch`: caps outbound download rate to the upstream lottery source

### Processors
- `HistoryProcessor` interface with state-specific implementations
- `LotteryHistoryProcessorFactory` creates appropriate processor based on state
- Processors handle different game types (bonus numbers, fireballs, etc.)

### Data Models
- `LotteryGame`: Contains game metadata and historical draws
- `LotteryDraw`: Individual draw with date and winning numbers
- Response models for API endpoints

## API Endpoints

- `GET /api/v1/states` - Get all available states
- `GET /api/v1/states/{stateName}/games` - Get games for a specific state
- `GET /api/v1/all/state-games` - Get all state lottery games (v1)
- `GET /api/v1/all/v2/state-games` - Get all state lottery games (v2 with nested structure)
- `GET /api/v1/state-games/{state}` - Get game names for a specific state
- `POST /api/v1/state/games` - Get detailed game data with StateGameAnalysisRequest; returns `404 Not Found` when the requested state/game has no persisted data

### Error Handling
- Failures are handled centrally by `GlobalExceptionHandler` (`@RestControllerAdvice`), which returns a structured `ApiError` JSON body (`timestamp`, `status`, `error`, `message`, `path`)
- `ResourceNotFoundException` → `404`; `IllegalArgumentException` → `400`; any other unhandled exception → `500`
- This replaces the previous behavior of catching exceptions and returning a `null` body with HTTP `400` for every failure; the catalog endpoints (`/all/state-games`, `/all/v2/state-games`, `/state-games/{state}`) now return an empty response rather than `400` when no data is present

## Development Notes

- The application uses file-based persistence instead of a database
- Read/catalog endpoints are cached in memory; the cache is invalidated whenever a poll completes, so staleness is bounded to a single poll cycle
- Lottery data is limited to the most recent 8000 draw results per game
- The system currently only supports Texas lottery data processing
- Louisiana lottery support is commented out in configuration
- Automated tests:
  - `lottery-result-persistence-server/src/test` — `LotteryDataServiceTest` exercises the real `LotteryDataService` against a temp `base-dir`, covering the `drawPositionCount` catalog path (v1 and v2), full game reads, and missing-game handling
  - `lottery-result-aggregator/src/test` — `LotteryGameSerializerTest` covers the atomic .ser write (readable output, in-place overwrite, no orphaned temp files)
  - Run them with `mvn test`

## File Processing Logic

The system processes different lottery game types with specific rules:
- **Standard games** (Lotto Texas, Cash Five): Regular number sorting
- **Bonus games** (Powerball, Mega Millions): Include bonus/powerball numbers
- **Pick games** (Pick 3, Daily 4): Include fireball numbers, no sorting

Each processor handles CSV parsing, date formatting, and number extraction based on the specific lottery format.