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
- File-based persistence using Java serialization (.ser files)
- Resilience4j for rate limiting and fault tolerance

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
2. **Polling**: `LotteryResultPollingService` fetches lottery data from configured URLs in application.yml
3. **Processing**: State-specific processors (e.g., `TexasLotteryHistoryProcessor`) parse CSV files and create `LotteryGame` objects
4. **Persistence**: `LotteryGameSerializer` saves processed data as .ser files in `tmp/{STATE}/` directories
5. **API Access**: REST controllers serve the persisted data via `/api/v1` endpoints

### File Structure Pattern
- Processed lottery data is stored in `tmp/{STATE_NAME}/{GAME_NAME}.ser` format
- Example: `tmp/TEXAS/Powerball.ser`

## Key Components

### Configuration
- `application.yml` contains lottery URLs and state region mappings
- URLs are organized by state with game-specific endpoints
- Currently supports Texas lottery games (CSV format)

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
- `POST /api/v1/state/games` - Get detailed game data with StateGameAnalysisRequest

## Development Notes

- The application uses file-based persistence instead of a database
- Lottery data is limited to the most recent 8000 draw results per game
- The system currently only supports Texas lottery data processing
- Louisiana lottery support is commented out in configuration
- No automated tests are currently present in the codebase

## File Processing Logic

The system processes different lottery game types with specific rules:
- **Standard games** (Lotto Texas, Cash Five): Regular number sorting
- **Bonus games** (Powerball, Mega Millions): Include bonus/powerball numbers
- **Pick games** (Pick 3, Daily 4): Include fireball numbers, no sorting

Each processor handles CSV parsing, date formatting, and number extraction based on the specific lottery format.