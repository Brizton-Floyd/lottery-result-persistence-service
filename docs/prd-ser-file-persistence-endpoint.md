# PRD: .ser File Persistence Endpoint

## Overview
Create a REST API endpoint that allows external clients to push lottery draw data and persist it to `.ser` files, making the data available to existing clients that consume data via `/api/v1/state/games`.

## Goals
- Enable external clients to push lottery draw data programmatically
- Persist data using existing serialization infrastructure
- Maintain compatibility with existing file-based read operations
- Provide proper error handling and validation

## Problem Statement
Currently, `.ser` files are only created by the internal polling service. External clients need the ability to push lottery draw data that should be persisted to `.ser` files so existing clients can consume this data alongside Texas-based games.

## Target Users
- **Primary:** External client services pushing lottery draw results
- **Secondary:** Existing clients reading from `/api/v1/state/games` endpoint

## Scope

### In Scope
- New POST endpoint to accept lottery draw data
- Serialize data to `.ser` files in `tmp/{STATE}/{GAME}.ser` format
- Validate incoming data structure (state, game name, draw data)
- Handle file I/O errors gracefully with appropriate HTTP status codes
- Logging for successful/failed serialization attempts

### Out of Scope
- Modifying existing polling service
- Database persistence (separate concern)
- Authentication/authorization
- Batch operations
- Duplicate detection/prevention
- File locking mechanisms

## Technical Specifications

### Target Systems
- **Repository:** `lottery-result-persistence-service`
- **Module:** `lottery-result-persistence-server` (controller)
- **Dependencies:** `lottery-result-aggregator` (LotteryGameSerializer)

### Constraints
- Must use existing `LotteryGameSerializer` component
- Must follow existing `.ser` file structure (`tmp/{STATE}/{GAME}.ser`)
- Must accept `LotteryGame` model from `lottery-result-persistence-service-models`
- Tech stack: Spring Boot, Java, Maven

### Dependencies
- **Existing:** `LotteryGameSerializer` class
- **Existing:** `LotteryGame` model
- **Required:** File system write permissions to `tmp/` directory

## Functional Requirements

| ID | Description | Status | User Story | Expected Outcome |
|:---|:---|:---|:---|:---|
| FR001 | Create POST endpoint `/api/v1/lottery/persist` | ⬜ | As an external client, I want to POST lottery game data so it can be persisted to .ser files | Endpoint accepts LotteryGame JSON and returns 200 OK on success |
| FR002 | Validate required fields in request body | ⬜ | As a developer, I want to ensure data integrity by validating required fields | Return 400 Bad Request if state, game name, or draw data is missing |
| FR003 | Serialize LotteryGame to .ser file | ⬜ | As a system, I need to persist the data using existing serialization | LotteryGame object serialized to `tmp/{STATE}/{GAME}.ser` |
| FR004 | Handle file I/O errors gracefully | ⬜ | As a developer, I want proper error handling for file operations | Return 500 Internal Server Error with error message on I/O failure |
| FR005 | Log serialization operations | ⬜ | As an operator, I want to monitor serialization success/failure | Log INFO on success, ERROR on failure with details |
| FR006 | Return appropriate response with serialized game info | ⬜ | As a client, I want confirmation of successful persistence | Return 200 OK with LotteryGame details on success |

## Non-Functional Requirements

### Logging
- Log successful serialization with state and game name
- Log failures with full stack trace
- Use SLF4J logger

### Error Handling
- 400 Bad Request: Missing required fields
- 500 Internal Server Error: File I/O errors
- Include error messages in response body

### Validation
- State name must not be null or empty
- Game name must not be null or empty
- Draw data (lottery draws list) must not be null

### Performance
- No specific performance requirements (file I/O is inherently slower)
- Note: No concurrency control exists in current implementation

## API Specification

### Endpoint
```
POST /api/v1/lottery/persist
```

### Request Body
```json
{
  "fullName": "Cash Five",
  "stateGameBelongsTo": "Texas",
  "lotteryDraws": [
    {
      "drawDate": "2026-02-14",
      "drawResults": [5, 15, 25, 30, 35],
      "bonusNumber": null
    }
  ],
  "drawHistoryCount": 1
}
```

### Success Response (200 OK)
```json
{
  "fullName": "Cash Five",
  "stateGameBelongsTo": "Texas",
  "drawHistoryCount": 1,
  "message": "Successfully persisted to tmp/TEXAS/Cash Five.ser"
}
```

### Error Responses

**400 Bad Request**
```json
{
  "error": "Validation failed",
  "message": "State name and game name are required"
}
```

**500 Internal Server Error**
```json
{
  "error": "Serialization failed",
  "message": "IOException: Permission denied"
}
```

## Implementation Plan

1. **FR001:** Add POST endpoint to `PersistenceServiceController`
2. **FR002:** Add validation logic for required fields
3. **FR003:** Inject `LotteryGameSerializer` and call serialize method
4. **FR004:** Add try-catch for IOException with proper error response
5. **FR005:** Add logging statements for success/failure
6. **FR006:** Return ResponseEntity with appropriate status and body

## Testing Strategy

### Unit Tests
- Test endpoint with valid LotteryGame data
- Test validation with missing state name
- Test validation with missing game name
- Test validation with null draw data
- Test IOException handling
- Mock LotteryGameSerializer to verify interactions

### Integration Tests
- Test end-to-end serialization with actual file creation
- Verify .ser file exists in correct location
- Verify existing `/api/v1/state/games` can read the persisted data

## Success Criteria
- [ ] All functional requirements marked as ✅
- [ ] Project compiles successfully with `./mvnw compile`
- [ ] Unit tests pass
- [ ] Manual testing confirms .ser file creation
- [ ] Existing clients can read the persisted data

## Traceability
Each completed requirement will include:
- Path to implementation file
- Path to test file
- Confirmation of successful build output

---

**Status:** Ready for Implementation
**Created:** 2026-02-15
**Branch:** `feat/ser-file-persistence-endpoint`
