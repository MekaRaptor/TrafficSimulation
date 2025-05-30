@echo off
echo ===============================================
echo      WEB-BASED TRAFFIC SIMULATION  
echo ===============================================
echo.

echo 🌐 Starting Web-Based Traffic Simulation...
echo.

echo 📁 Opening web interface in your default browser...
echo.

REM Open the HTML file in default browser
start "" "web\index.html"

echo ✅ Web interface opened!
echo.
echo 🎮 Instructions:
echo   1. Click "Start Simulation" to begin
echo   2. Watch vehicles move on real maps
echo   3. Toggle between map modes
echo   4. View live statistics
echo.
echo 🔧 Developer Notes:
echo   - Current version uses simulated data
echo   - To connect to Java backend, implement HTTP server
echo   - Uses OpenStreetMap for realistic city view
echo.

pause 