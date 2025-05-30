@echo off
echo ===============================================
echo       LIVE TRAFFIC SIMULATION SERVER
echo ===============================================
echo.

echo 🚀 Starting Live City Traffic Simulation...
echo.

echo [1/3] 🔨 Compiling Java backend...
javac -d compiled src\core\CityMap.java src\core\Road.java src\core\Intersection.java src\core\TrafficLight.java src\core\Vehicle.java src\core\WebTrafficAPI.java src\core\SimpleHTTPServer.java

if %errorlevel% neq 0 (
    echo ❌ Compilation failed!
    pause
    exit /b 1
)

echo ✅ Compilation successful!
echo.

echo [2/3] 🌐 Starting HTTP server...
echo.
echo 📍 Your custom city will be available at:
echo    http://localhost:8080/city
echo.
echo 🎮 Features:
echo    • Real Java backend integration
echo    • Live vehicle tracking  
echo    • Custom city with zones
echo    • Real-time statistics
echo    • Interactive controls
echo.

echo [3/3] 🚀 Launching server...
echo.
echo ⚠️  To stop the server, press Ctrl+C
echo.

REM Start the HTTP server
java -cp compiled SimpleHTTPServer

echo.
echo 🛑 Server stopped.
pause 