# IntelliJ Setup Guide - Lottery Persistence Service on Port 8001

## Overview
The application serves historical lottery data (parsed from `.ser` files) over a REST API on port 8001. No database or profile activation is required - running the main class is sufficient.

## IntelliJ Run Configuration

1. **Run** → **Edit Configurations...**
2. **Add / select** an *Application* configuration:
   ```
   Name: LotteryResultPersistenceServiceApplication
   Main class: com.floyd.lottoptions.server.LotteryResultPersistenceServiceApplication
   Module: lottery-result-persistence-server
   Working directory: <repository root>
   ```
3. **Click "Apply"** and **"OK"**
4. **Run the application**

No active profiles or program arguments are needed.

## What You Get on Port 8001

### Historical Lottery Data APIs (`.ser` files)
```http
GET  http://localhost:8001/api/v1/states
GET  http://localhost:8001/api/v1/states/{stateName}/games
GET  http://localhost:8001/api/v1/all/state-games
GET  http://localhost:8001/api/v1/all/v2/state-games
GET  http://localhost:8001/api/v1/state-games/{state}
POST http://localhost:8001/api/v1/state/games
```

## Verification Steps

After starting the application from IntelliJ, verify it is running:

### 1. Check Console Logs - Should See:
```
Tomcat started on port 8001 (http)
Started LotteryResultPersistenceServiceApplication
```

### 2. Test an Endpoint:
- `http://localhost:8001/api/v1/states` should return JSON
