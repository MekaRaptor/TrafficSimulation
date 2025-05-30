@echo off
echo Compiling Java files...
javac -d compiled src\core\CityMap.java src\core\Road.java src\core\Intersection.java src\core\TrafficLight.java src\core\Vehicle.java src\core\WebTrafficAPI.java src\core\SimpleHTTPServer.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Starting HTTP server...
java -cp compiled SimpleHTTPServer 