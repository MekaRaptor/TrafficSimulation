import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class TrafficSimulationGUI extends Application {
    private static final int CELL_SIZE = 60;
    private static final int PADDING = 10;
    private static final int VEHICLE_SIZE = 10;
    private Canvas canvas;
    private GraphicsContext gc;
    private CityMap cityMap;
    private int gridSize = 5;
    private int vehicleCount = 10;
    private double simulationSpeed = 1.0;
    private Map<String, VehicleInfo> vehicleInfoMap = new HashMap<>();
    private Label statsLabel;
    private boolean showRoutes = true;
    private boolean nightMode = false;
    
    private static class VehicleInfo {
        double x, y;
        double targetX, targetY;
        double velocityX, velocityY;
        Color color;
        List<double[]> routePoints = new ArrayList<>();
        
        VehicleInfo(double x, double y) {
            this.x = x;
            this.y = y;
            this.targetX = x;
            this.targetY = y;
            this.color = Color.rgb(
                (int)(Math.random() * 200 + 55),
                (int)(Math.random() * 200 + 55),
                (int)(Math.random() * 200 + 55)
            );
        }
        
        void updatePosition(double elapsedTime, double speed) {
            // Smooth movement with easing
            double dx = targetX - x;
            double dy = targetY - y;
            
            // Calculate velocity with easing
            double easing = 5.0 * speed;
            velocityX = dx * easing * elapsedTime;
            velocityY = dy * easing * elapsedTime;
            
            // Apply velocity
            x += velocityX;
            y += velocityY;
        }
        
        void setTarget(double x, double y) {
            this.targetX = x;
            this.targetY = y;
        }
        
        void addRoutePoint(double x, double y) {
            routePoints.add(new double[]{x, y});
            // Keep only last 5 points
            if (routePoints.size() > 5) {
                routePoints.remove(0);
            }
        }
    }
    
    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // Control panel
        VBox controlPanel = createControlPanel();
        root.setRight(controlPanel);
        
        // Stats panel
        VBox statsPanel = createStatsPanel();
        root.setBottom(statsPanel);
        
        // Create canvas
        canvas = new Canvas(gridSize * CELL_SIZE * 2, gridSize * CELL_SIZE * 2);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);
        
        // Start simulation
        cityMap = new CityMap();
        cityMap.setupGridCity(gridSize, vehicleCount);
        
        // Start animation timer
        startAnimation();
        
        Scene scene = new Scene(root);
        primaryStage.setTitle("Traffic Simulation");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Stop threads when window is closed
        primaryStage.setOnCloseRequest(e -> {
            cityMap.stopSimulation();
            Platform.exit();
            System.exit(0);
        });
    }
    
    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1px;");
        panel.setPrefWidth(200);
        
        // Title
        Label titleLabel = new Label("TRAFFIC CONTROL");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));
        titleLabel.setStyle("-fx-text-fill: #333333;");
        
        // Grid size control
        Label gridLabel = new Label("Grid Size: " + gridSize);
        Slider gridSlider = new Slider(3, 10, gridSize);
        gridSlider.setShowTickLabels(true);
        gridSlider.setShowTickMarks(true);
        gridSlider.setMajorTickUnit(1);
        gridSlider.setBlockIncrement(1);
        gridSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            gridSize = newVal.intValue();
            gridLabel.setText("Grid Size: " + gridSize);
            updateCanvasSize();
        });
        
        // Vehicle count control
        Label vehicleLabel = new Label("Vehicle Count: " + vehicleCount);
        Slider vehicleSlider = new Slider(5, 30, vehicleCount);
        vehicleSlider.setShowTickLabels(true);
        vehicleSlider.setShowTickMarks(true);
        vehicleSlider.setMajorTickUnit(5);
        vehicleSlider.setBlockIncrement(1);
        vehicleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            vehicleCount = newVal.intValue();
            vehicleLabel.setText("Vehicle Count: " + vehicleCount);
        });
        
        // Simulation speed control
        Label speedLabel = new Label("Simulation Speed: " + simulationSpeed + "x");
        Slider speedSlider = new Slider(0.1, 3.0, simulationSpeed);
        speedSlider.setShowTickLabels(true);
        speedSlider.setShowTickMarks(true);
        speedSlider.setMajorTickUnit(0.5);
        speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            simulationSpeed = newVal.doubleValue();
            speedLabel.setText(String.format("Simulation Speed: %.1fx", simulationSpeed));
        });
        
        // Control buttons
        Button startButton = new Button("Start Simulation");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        Button resetButton = new Button("Reset");
        resetButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        Button pauseButton = new Button("Pause");
        pauseButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        
        HBox buttons = new HBox(10, startButton, pauseButton, resetButton);
        buttons.setAlignment(Pos.CENTER);
        
        startButton.setOnAction(e -> {
            cityMap.stopSimulation();
            vehicleInfoMap.clear();
            cityMap = new CityMap();
            cityMap.setupGridCity(gridSize, vehicleCount);
            cityMap.startSimulation();
        });
        
        pauseButton.setOnAction(e -> {
            if (pauseButton.getText().equals("Pause")) {
                cityMap.stopSimulation();
                pauseButton.setText("Resume");
                pauseButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
            } else {
                cityMap.startSimulation();
                pauseButton.setText("Pause");
                pauseButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
            }
        });
        
        resetButton.setOnAction(e -> {
            cityMap.stopSimulation();
            vehicleInfoMap.clear();
            cityMap = new CityMap();
            cityMap.setupGridCity(gridSize, vehicleCount);
        });
        
        // Visual options
        HBox visualOptions = new HBox(10);
        visualOptions.setAlignment(Pos.CENTER);
        
        Button routesButton = new Button("Hide Routes");
        routesButton.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;");
        routesButton.setOnAction(e -> {
            showRoutes = !showRoutes;
            routesButton.setText(showRoutes ? "Hide Routes" : "Show Routes");
        });
        
        Button nightModeButton = new Button("Night Mode");
        nightModeButton.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white;");
        nightModeButton.setOnAction(e -> {
            nightMode = !nightMode;
            nightModeButton.setText(nightMode ? "Day Mode" : "Night Mode");
            if (nightMode) {
                panel.setStyle("-fx-background-color: #333333; -fx-border-color: #555555; -fx-border-width: 1px;");
                titleLabel.setStyle("-fx-text-fill: #ffffff;");
                gridLabel.setStyle("-fx-text-fill: #ffffff;");
                vehicleLabel.setStyle("-fx-text-fill: #ffffff;");
                speedLabel.setStyle("-fx-text-fill: #ffffff;");
                statsLabel.setStyle("-fx-text-fill: #ffffff; -fx-background-color: #333333;");
            } else {
                panel.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #cccccc; -fx-border-width: 1px;");
                titleLabel.setStyle("-fx-text-fill: #333333;");
                gridLabel.setStyle("");
                vehicleLabel.setStyle("");
                speedLabel.setStyle("");
                statsLabel.setStyle("-fx-background-color: #e0e0e0;");
            }
        });
        
        visualOptions.getChildren().addAll(routesButton, nightModeButton);
        
        panel.getChildren().addAll(
            titleLabel,
            new Separator(),
            gridLabel, gridSlider,
            vehicleLabel, vehicleSlider,
            speedLabel, speedSlider,
            new Separator(),
            buttons,
            new Separator(),
            visualOptions
        );
        
        return panel;
    }
    
    private class Separator extends HBox {
        public Separator() {
            super();
            this.setPrefHeight(10);
        }
    }
    
    private VBox createStatsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-background-color: #e0e0e0;");
        
        statsLabel = new Label("Simulation Statistics");
        statsLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
        panel.getChildren().add(statsLabel);
        
        return panel;
    }
    
    private void updateCanvasSize() {
        canvas.setWidth(gridSize * CELL_SIZE * 2);
        canvas.setHeight(gridSize * CELL_SIZE * 2);
    }
    
    private void startAnimation() {
        new AnimationTimer() {
            private long lastUpdate = 0;
            
            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }
                
                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                
                // Update vehicle positions with smooth animation
                updateVehiclePositions(elapsedSeconds);
                
                if (elapsedSeconds >= 0.016 / simulationSpeed) { // ~60 FPS
                    drawSimulation();
                    updateStats();
                    lastUpdate = now;
                }
            }
        }.start();
    }
    
    private void updateVehiclePositions(double elapsedSeconds) {
        for (Vehicle vehicle : cityMap.getVehicles()) {
            String vehicleId = vehicle.getVehicleId();
            VehicleInfo info = vehicleInfoMap.computeIfAbsent(vehicleId, 
                k -> new VehicleInfo(0, 0));
            
            Road currentRoad = vehicle.getCurrentRoad();
            if (currentRoad != null) {
                // Calculate vehicle position on the road
                double progress = vehicle.getProgress();
                String roadId = currentRoad.getId();
                boolean isHorizontal = roadId.startsWith("R");
                int row = Integer.parseInt(roadId.split("-")[0].substring(1));
                int col = Integer.parseInt(roadId.split("-")[1]);
                
                double targetX, targetY;
                if (isHorizontal) {
                    targetX = col * CELL_SIZE * 2 + progress * CELL_SIZE * 2;
                    targetY = row * CELL_SIZE * 2 + CELL_SIZE;
                } else {
                    targetX = col * CELL_SIZE * 2 + CELL_SIZE;
                    targetY = row * CELL_SIZE * 2 + progress * CELL_SIZE * 2;
                }
                
                // Set target position for smooth movement
                info.setTarget(targetX, targetY);
                
                // Update position with smooth animation
                info.updatePosition(elapsedSeconds, simulationSpeed);
                
                // Store route points for trail effect
                info.addRoutePoint(info.x, info.y);
            }
        }
    }
    
    private void updateStats() {
        int activeVehicles = cityMap.getVehicles().size();
        int totalCongestion = calculateTotalCongestion();
        double avgWaitTime = calculateAverageWaitTime();
        
        // Count vehicles by type
        int cars = 0, trucks = 0, motorcycles = 0, buses = 0;
        for (Vehicle vehicle : cityMap.getVehicles()) {
            if (vehicle.isActive()) {
                switch (vehicle.getVehicleType()) {
                    case CAR -> cars++;
                    case TRUCK -> trucks++;
                    case MOTORCYCLE -> motorcycles++;
                    case BUS -> buses++;
                }
            }
        }
        
        statsLabel.setText(String.format(
            "Active: %d | Congestion: %d%% | Wait Time: %.1f sec | Cars: %d | Trucks: %d | Motorcycles: %d | Buses: %d",
            activeVehicles, totalCongestion, avgWaitTime, cars, trucks, motorcycles, buses
        ));
    }
    
    private int calculateTotalCongestion() {
        int totalCapacity = 0;
        int totalVehicles = 0;
        
        for (Road road : cityMap.getRoads()) {
            totalCapacity += road.getCapacity();
            totalVehicles += road.getVehicleCount();
        }
        
        return totalCapacity > 0 ? (totalVehicles * 100) / totalCapacity : 0;
    }
    
    private double calculateAverageWaitTime() {
        double totalWaitTime = 0;
        int vehicleCount = 0;
        
        for (Vehicle vehicle : cityMap.getVehicles()) {
            if (vehicle.isActive()) {
                totalWaitTime += vehicle.getTotalWaitTime() / 1000.0;
                vehicleCount++;
            }
        }
        
        return vehicleCount > 0 ? totalWaitTime / vehicleCount : 0.0;
    }
    
    private void drawSimulation() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        
        // Set background color based on mode
        if (nightMode) {
            gc.setFill(Color.rgb(20, 20, 30));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        } else {
            gc.setFill(Color.rgb(240, 240, 240));
            gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        
        // Draw grid
        gc.setStroke(nightMode ? Color.rgb(70, 70, 80) : Color.GRAY);
        gc.setLineWidth(1);
        
        // Draw horizontal roads
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = j * CELL_SIZE * 2;
                double y = i * CELL_SIZE * 2;
                
                // Road
                drawRoad("R" + i + "-" + j, x, y + CELL_SIZE - PADDING, CELL_SIZE * 2, PADDING * 2, true);
                
                // Traffic light
                drawTrafficLight("R" + i + "-" + j, x + CELL_SIZE * 2 - PADDING * 2, y + CELL_SIZE);
            }
        }
        
        // Draw vertical roads
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = j * CELL_SIZE * 2;
                double y = i * CELL_SIZE * 2;
                
                // Road
                drawRoad("C" + i + "-" + j, x + CELL_SIZE - PADDING, y, PADDING * 2, CELL_SIZE * 2, false);
                
                // Traffic light
                drawTrafficLight("C" + i + "-" + j, x + CELL_SIZE, y + CELL_SIZE * 2 - PADDING * 2);
            }
        }
        
        // Draw intersections
        gc.setFill(nightMode ? Color.rgb(50, 50, 60) : Color.DARKGRAY);
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = j * CELL_SIZE * 2 + CELL_SIZE - PADDING;
                double y = i * CELL_SIZE * 2 + CELL_SIZE - PADDING;
                gc.fillRect(x, y, PADDING * 2, PADDING * 2);
            }
        }
        
        // Draw vehicle routes first (behind vehicles)
        if (showRoutes) {
            drawVehicleRoutes();
        }
        
        // Draw vehicles
        drawVehicles();
    }
    
    private void drawRoad(String roadId, double x, double y, double width, double height, boolean isHorizontal) {
        Road road = cityMap.getRoadById(roadId);
        if (road != null) {
            // Road color based on congestion
            double congestion = (double) road.getVehicleCount() / road.getCapacity();
            Color roadColor = getCongestionColor(congestion);
            gc.setFill(roadColor);
            gc.fillRect(x, y, width, height);
            
            // Draw road markings
            gc.setStroke(nightMode ? Color.rgb(180, 180, 180, 0.5) : Color.WHITE);
            gc.setLineWidth(1);
            
            if (isHorizontal) {
                // Dashed line in the middle of horizontal road
                double dashLength = 5;
                double gapLength = 5;
                double startX = x;
                double middleY = y + height / 2;
                
                for (double i = 0; i < width; i += dashLength + gapLength) {
                    gc.strokeLine(startX + i, middleY, startX + i + dashLength, middleY);
                }
            } else {
                // Dashed line in the middle of vertical road
                double dashLength = 5;
                double gapLength = 5;
                double middleX = x + width / 2;
                double startY = y;
                
                for (double i = 0; i < height; i += dashLength + gapLength) {
                    gc.strokeLine(middleX, startY + i, middleX, startY + i + dashLength);
                }
            }
        }
    }
    
    private Color getCongestionColor(double congestion) {
        if (nightMode) {
            if (congestion >= 0.8) return Color.rgb(200, 0, 0, 0.5); // Red - Heavy congestion
            if (congestion >= 0.5) return Color.rgb(200, 120, 0, 0.5); // Orange - Medium congestion
            if (congestion >= 0.2) return Color.rgb(200, 200, 0, 0.5); // Yellow - Light congestion
            return Color.rgb(50, 50, 60, 0.5); // Dark gray - Normal
        } else {
            if (congestion >= 0.8) return Color.rgb(255, 0, 0, 0.3); // Red - Heavy congestion
            if (congestion >= 0.5) return Color.rgb(255, 165, 0, 0.3); // Orange - Medium congestion
            if (congestion >= 0.2) return Color.rgb(255, 255, 0, 0.3); // Yellow - Light congestion
            return Color.rgb(200, 200, 200, 0.3); // Light gray - Normal
        }
    }
    
    private void drawVehicles() {
        for (Vehicle vehicle : cityMap.getVehicles()) {
            String vehicleId = vehicle.getVehicleId();
            VehicleInfo info = vehicleInfoMap.get(vehicleId);
            
            if (info != null && vehicle.getCurrentRoad() != null) {
                // Create glowing effect for vehicles
                DropShadow glow = new DropShadow();
                glow.setColor(info.color);
                glow.setRadius(VEHICLE_SIZE);
                
                // Draw vehicle with glow effect
                gc.setEffect(glow);
                gc.setFill(info.color);
                
                // Draw different vehicle types with distinct shapes
                Vehicle.VehicleType type = vehicle.getVehicleType();
                double size = VEHICLE_SIZE;
                
                switch (type) {
                    case CAR:
                        // Draw car (circle)
                        gc.fillOval(info.x - size/2, info.y - size/2, size, size);
                        break;
                    case TRUCK:
                        // Draw truck (rectangle)
                        size *= 1.4;
                        gc.fillRect(info.x - size/2, info.y - size/3, size, size*0.7);
                        break;
                    case MOTORCYCLE:
                        // Draw motorcycle (diamond)
                        size *= 0.8;
                        double[] xPoints = {info.x, info.x + size/2, info.x, info.x - size/2};
                        double[] yPoints = {info.y - size/2, info.y, info.y + size/2, info.y};
                        gc.fillPolygon(xPoints, yPoints, 4);
                        break;
                    case BUS:
                        // Draw bus (rounded rectangle)
                        size *= 1.6;
                        gc.fillRoundRect(info.x - size/2, info.y - size/3, size, size*0.6, 4, 4);
                        break;
                }
                
                // Reset effect
                gc.setEffect(null);
                
                // Draw speed indicator
                if (vehicle.getSpeed() > 1.1) {
                    // Fast vehicles have motion lines
                    gc.setStroke(info.color);
                    gc.setLineWidth(1);
                    double speedLineLength = vehicle.getSpeed() * 5;
                    
                    Road currentRoad = vehicle.getCurrentRoad();
                    if (currentRoad != null) {
                        String roadId = currentRoad.getId();
                        boolean isHorizontal = roadId.startsWith("R");
                        
                        if (isHorizontal) {
                            gc.strokeLine(info.x - speedLineLength, info.y, info.x - speedLineLength/2, info.y);
                        } else {
                            gc.strokeLine(info.x, info.y - speedLineLength, info.x, info.y - speedLineLength/2);
                        }
                    }
                }
            }
        }
    }
    
    private void drawVehicleRoutes() {
        for (VehicleInfo info : vehicleInfoMap.values()) {
            if (info.routePoints.size() > 1) {
                // Draw route trail with fading effect
                for (int i = 0; i < info.routePoints.size() - 1; i++) {
                    double opacity = (double)(i + 1) / info.routePoints.size();
                    gc.setStroke(Color.rgb(
                        (int)(info.color.getRed() * 255),
                        (int)(info.color.getGreen() * 255),
                        (int)(info.color.getBlue() * 255),
                        opacity * 0.5
                    ));
                    gc.setLineWidth(3);
                    
                    double[] point1 = info.routePoints.get(i);
                    double[] point2 = info.routePoints.get(i + 1);
                    gc.strokeLine(point1[0], point1[1], point2[0], point2[1]);
                }
            }
        }
    }
    
    private void drawTrafficLight(String roadId, double x, double y) {
        Road road = cityMap.getRoadById(roadId);
        if (road != null) {
            TrafficLight light = cityMap.getTrafficLightForRoad(road);
            if (light != null) {
                Color color;
                switch (light.getTrafficLightState()) {
                    case GREEN -> color = Color.rgb(0, 255, 0);
                    case YELLOW -> color = Color.rgb(255, 255, 0);
                    default -> color = Color.rgb(255, 0, 0);
                }
                
                // Apply brightness adjustment for blinking effect
                double brightness = light.getBrightness();
                color = color.deriveColor(0, 1.0, brightness, 1.0);
                
                // Create glow effect for traffic lights
                Glow glow = new Glow();
                glow.setLevel(0.8 * brightness);
                
                // Add emergency effect
                if (light.isEmergencyMode()) {
                    DropShadow emergencyEffect = new DropShadow();
                    emergencyEffect.setColor(Color.YELLOW);
                    emergencyEffect.setRadius(PADDING);
                    emergencyEffect.setSpread(0.7);
                    gc.setEffect(emergencyEffect);
                } else {
                    gc.setEffect(glow);
                }
                
                gc.setFill(color);
                gc.fillOval(x - PADDING / 2, y - PADDING / 2, PADDING, PADDING);
                gc.setEffect(null);
                
                // Draw light housing
                gc.setStroke(nightMode ? Color.rgb(100, 100, 100) : Color.BLACK);
                gc.setLineWidth(1);
                gc.strokeOval(x - PADDING / 2, y - PADDING / 2, PADDING, PADDING);
            }
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 