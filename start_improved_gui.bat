@echo off
echo ===============================================
echo    IMPROVED TRAFFIC SIMULATION GUI
echo ===============================================
echo.

echo ✨ NEW FEATURES:
echo   • Starts in REALISTIC mode by default
echo   • 15 vehicles (instead of 10)
echo   • Larger canvas (800x700)
echo   • Better default settings
echo.

echo 🎮 CONTROLS:
echo   • "Start Simulation" = Begin traffic movement
echo   • "Mode: REALISTIC/GRID" = Toggle city types
echo   • Vehicle Count slider = Change number of cars
echo   • "Night Mode" = Toggle dark theme
echo   • "Routes: ON/OFF" = Show/hide vehicle paths
echo.

echo 🚀 Starting enhanced GUI...
echo.

java --module-path javafx\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics -cp compiled TrafficSimulationGUI

echo.
echo GUI closed.
pause 