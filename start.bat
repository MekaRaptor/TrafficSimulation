@echo off
cls
echo.
echo 🚀 Traffic Simulation 2.0 - Modern Stack Launcher
echo =====================================================
echo Backend: Java Simulation Engine + Node.js API Layer
echo Frontend: React + TypeScript + GraphQL + WebSockets  
echo =====================================================
echo.

:: Check if Node.js is installed
node --version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Node.js is not installed. Please install Node.js first.
    echo 📦 Download from: https://nodejs.org/
    pause
    exit /b 1
)

:: Check if Java is installed
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ❌ Java is not installed. Please install Java JDK first.
    echo 📦 Download from: https://www.oracle.com/java/technologies/javase-downloads.html
    pause
    exit /b 1
)

echo ✅ Node.js and Java are installed
echo.

:: Compile Java code
echo 📦 Compiling Java simulation engine...
if not exist "compiled" mkdir compiled
javac -cp "src" src/core/*.java -d compiled
if %errorlevel% neq 0 (
    echo ❌ Java compilation failed
    pause
    exit /b 1
)
echo ✅ Java code compiled successfully
echo.

:: Install Node.js dependencies
echo 📦 Installing Node.js dependencies...
cd api-server
if not exist "node_modules" (
    echo Installing API server dependencies...
    call npm install
    if %errorlevel% neq 0 (
        echo ❌ Failed to install API server dependencies
        pause
        exit /b 1
    )
)
cd ..

cd traffic-sim-frontend
if not exist "node_modules" (
    echo Installing frontend dependencies...
    call npm install
    if %errorlevel% neq 0 (
        echo ❌ Failed to install frontend dependencies
        pause
        exit /b 1
    )
)
cd ..

echo ✅ All dependencies installed
echo.

:: Start services
echo 🚀 Starting Traffic Simulation 2.0...
echo.
echo Starting services in order:
echo 1. Java Simulation Engine (Background)
echo 2. Node.js API Server (Background) 
echo 3. React Frontend (Will open browser)
echo.

:: Start Java simulation engine in background
echo 🏭 Starting Java Simulation Engine...
start "Java Simulation Engine" /min cmd /c "cd . && java -cp compiled src.core.SimulationServer"
timeout /t 3 /nobreak >nul

:: Start Node.js API server in background  
echo 🌐 Starting Node.js API Server...
start "Node.js API Server" /min cmd /c "cd api-server && npm start"
timeout /t 5 /nobreak >nul

:: Start React frontend (this will open browser)
echo ⚛️ Starting React Frontend...
echo.
echo 🌟 Your browser will open automatically in a few seconds...
echo.
echo 📡 Services will be available at:
echo    Frontend: http://localhost:5173
echo    API Server: http://localhost:4000
echo    GraphQL Playground: http://localhost:4000/graphql
echo.
echo ⚠️ Keep this window open to keep services running
echo ⚠️ Press Ctrl+C to stop all services
echo.

cd traffic-sim-frontend
call npm run dev

echo.
echo 🛑 Shutting down all services...
taskkill /f /im node.exe >nul 2>&1
taskkill /f /im java.exe >nul 2>&1
echo ✅ All services stopped
pause 