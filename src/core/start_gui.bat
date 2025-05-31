@echo off
echo Traffic Simulation GUI Launcher
javac -cp "C:\Program Files\javafx-sdk-24.0.1\lib\*" TrafficSimulationGUI.java TrafficController.java Vehicle.java VehicleType.java Road.java IntersectionManager.java
java --module-path "C:\Program Files\javafx-sdk-24.0.1\lib" --add-modules javafx.controls,javafx.fxml TrafficSimulationGUI
pause
