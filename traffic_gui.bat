@echo off
title Traffic Simulation GUI
color 0A
echo.
echo ================================================
echo        TRAFFIC SIMULATION GUI LAUNCHER
echo ================================================
echo.

echo [INFO] Current directory: %CD%
echo.

echo [INFO] Checking for Java files...
if not exist TrafficSimulationGUI.java (
    echo [ERROR] TrafficSimulationGUI.java not found!
    echo [ERROR] Make sure you are in the correct directory.
    echo.
    pause
    exit /b 1
)
echo [OK] Java files found.
echo.

echo [INFO] Compiling Java files...
javac -cp "C:\Program Files\javafx-sdk-24.0.1\lib\*" TrafficSimulationGUI.java TrafficController.java Vehicle.java VehicleType.java Road.java IntersectionManager.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Compilation failed!
    echo [ERROR] Check Java and JavaFX installation.
    echo.
    pause
    exit /b 1
)

echo [OK] Compilation successful!
echo.

echo [INFO] Starting JavaFX GUI...
echo [INFO] Close the GUI window to return here.
echo.

java --module-path "C:\Program Files\javafx-sdk-24.0.1\lib" --add-modules javafx.controls,javafx.fxml TrafficSimulationGUI

echo.
echo [INFO] GUI closed successfully.
echo [INFO] Press any key to exit...
pause >nul 