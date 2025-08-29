# IntelliJ Setup Guide - Lottery Persistence Service on Port 8001

## ✅ **WORKING CONFIGURATION** 
The application requires explicit H2 profile activation in IntelliJ Run Configuration to enable both services.

## 🚀 **REQUIRED IntelliJ Setup** ⚠️

### **CRITICAL: Must Configure H2 Profile**
The `application.yml` change alone is NOT sufficient. You MUST configure the H2 profile in IntelliJ:

1. **Run** → **Edit Configurations...**
2. **Find/Select** your `LotteryResultPersistenceServiceApplication` configuration
3. **In "Active profiles" field**, enter: `h2`
4. **Click "Apply"** and **"OK"**
5. **Run the application**

### **Alternative Methods** (if Active profiles field not visible):

#### **Option A: Program Arguments**
- **In "Program arguments"** field: `--spring.profiles.active=h2`

#### **Option B: Environment Variables**  
- **Add Environment Variable**: `SPRING_PROFILES_ACTIVE=h2`

### **✅ VERIFIED WORKING CONFIGURATION:**
```
Name: LotteryResultPersistenceServiceApplication
Main class: com.floyd.lottoptions.server.LotteryResultPersistenceServiceApplication  
Active profiles: h2
Working directory: /Users/briztonfloyd/Desktop/Lotto Application/lottery-result-persistence-service
Module: lottery-result-persistence-server
```

## 🎯 **What You Get on Port 8001**

### **Historical Lottery Data APIs** (.ser files):
```http
GET http://localhost:8001/api/v1/states
GET http://localhost:8001/api/v1/states/Texas/games  
GET http://localhost:8001/api/v1/all/state-games
```

### **New Targeting System APIs** (H2 database):
```http
GET http://localhost:8001/api/v1/lottery-targeting/configurations
GET http://localhost:8001/api/v1/lottery-targeting/summary
GET http://localhost:8001/api/v1/lottery-targeting/patterns/powerball
POST http://localhost:8001/api/v1/lottery-targeting/sessions
```

### **H2 Database Console**:
```http
GET http://localhost:8001/h2-console
```
**Login credentials:**
- JDBC URL: `jdbc:h2:mem:lottery_targeting`
- Username: `sa`
- Password: (empty)

## 🔄 **Different Run Modes**

### **Full System (Both Services)** - Default:
```
Active Profile: h2
Port: 8001
Services: Historical Data + Targeting System
```

### **Historical Data Only** (if needed):
```
Active Profile: (none)
Port: 8001  
Services: Historical Data Only
```

## ✅ **VERIFICATION STEPS** - REQUIRED

After configuring H2 profile and starting from IntelliJ, verify both systems are running:

### **1. Check Console Logs - MUST See:**
```
The following 1 profile is active: "h2"
H2 console available at '/h2-console'  
Found 3 JPA repository interfaces
Tomcat started on port 8001 (http)
```

### **2. Test Endpoints - Both Must Work:**
- **Historical**: `http://localhost:8001/api/v1/states` ✅ Should return JSON
- **Targeting**: `http://localhost:8001/api/v1/lottery-targeting/summary` ✅ Should return JSON

### **3. Test H2 Console Access:**
- **URL**: `http://localhost:8001/h2-console` ✅ Should load H2 login page

### **⚠️ TROUBLESHOOTING - If Targeting APIs Return 404:**
1. **Check console logs** - must see "h2" profile active
2. **Verify H2 console loads** - if 404, profile not active
3. **Re-check Run Configuration** - ensure "Active profiles: h2" is set
4. **Restart application** after configuration changes

## 🚀 **Ready for Microservices**
Your persistence service on port 8001 is now ready to serve as the database API for your future generation service microservice on a different port (e.g., 8002).