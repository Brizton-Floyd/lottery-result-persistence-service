# Lottery Prize Tier Targeting System - Phase 1 Implementation Plan

## Project Overview
Implementation of core foundation and data persistence for a lottery number generation system targeting specific prize tiers (3, 4, 5+). This system will integrate with the existing multi-module Spring Boot lottery result persistence service.

## Phase 1: Core Foundation and Data Persistence

### Architecture Decision
- **Database**: H2 embedded database (development/testing ready, PostgreSQL migration path available)
- **Integration**: Two new Maven modules added to existing project structure
- **Data Strategy**: Hybrid approach - keep existing .ser files for historical data, use H2 for new targeting system

### New Modules to Create

#### 1. lottery-data-models (Domain/Model Layer)
**Location**: `lottery-data-models/src/main/java/com/floyd/lottoptions/datamodels/`

**Core Interfaces & Classes:**
- `LotteryConfiguration` - Interface defining lottery game rules
  - Fields: id, name, numberRange (min/max), drawSize, patternLength, prizeStructure (dynamic tiers)
- `PatternGroupDefinition` - Interface for lottery number pattern classification
  - Fields: lotteryConfigId, groups (hot/warm/cold), patterns, frequency, efficiency multiplier, lastUpdated
- `UserRequest` - User input representation
  - Fields: sessionId, targetTier (dynamic), numberOfTickets, generationStrategy, budget, lotteryConfigId, preferences, timestamp
- `GeneratedTicketSet` - Primary system output
  - Fields: sessionId, generated tickets list, generation metadata, quality metrics, patterns used, expectedPerformance

#### 2. lottery-persistence (Data Access Layer)  
**Location**: `lottery-persistence/src/main/java/com/floyd/lottoptions/persistence/`

**Components:**
- H2 database schema (DDL)
- `LotteryDataRepository` class with Spring Data JPA
- Database tables: lottery_configurations, pattern_groups, hot_numbers, proven_combinations, user_sessions, generated_ticket_sets, lottery_tickets

### Implementation Tasks

#### Database Setup
- [ ] Configure H2 database in application.yml
- [ ] Create DDL schema for all required tables
- [ ] Set up Spring Data JPA configuration
- [ ] Enable H2 console for development

#### Data Models Module
- [ ] Create Maven module structure
- [ ] Implement LotteryConfiguration interface
- [ ] Implement PatternGroupDefinition interface  
- [ ] Implement UserRequest class
- [ ] Implement GeneratedTicketSet class
- [ ] Add supporting value objects and enums

#### Persistence Module
- [ ] Create Maven module structure
- [ ] Create database schema DDL
- [ ] Implement LotteryDataRepository
- [ ] Create JPA entities for all tables
- [ ] Implement data access methods
- [ ] Add database initialization scripts

#### Integration Tasks
- [ ] Update parent pom.xml with new modules
- [ ] Configure module dependencies
- [ ] Add H2 and Spring Data JPA dependencies
- [ ] Update application configuration
- [ ] Test database connectivity

### Key Design Decisions

#### Dynamic Tier Targeting
- Target tier field accepts any tier number (not limited to 5)
- Prize structure in LotteryConfiguration supports variable tier definitions
- System designed to handle different lottery games with different tier structures

#### Data Integration Strategy
- Existing historical lottery data remains in .ser files
- New targeting system uses H2 for relational queries
- Pattern analysis bridges both data sources
- No disruption to existing polling/processing system

#### Technology Stack Alignment
- Spring Boot 3.3.0 (matches existing)
- Java 8 (matches existing)
- Spring Data JPA for database operations
- H2 embedded database
- Maven multi-module structure (matches existing)

### Success Criteria
- [ ] New modules compile and integrate with existing system
- [ ] H2 database schema created and accessible
- [ ] All core data models implemented with proper relationships
- [ ] Repository layer functional with basic CRUD operations
- [ ] No disruption to existing lottery result persistence functionality
- [ ] Database supports dynamic tier targeting (not limited to tier 5)

### Next Phase Preview
Phase 2 will implement the business logic layer using these foundation components to perform pattern analysis and ticket generation.