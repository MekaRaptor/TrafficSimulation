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
import javafx.scene.image.Image;
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
    private AssetManager assetManager;
    private String assetMode = "AUTO"; // AUTO, FORCE, DISABLE
    
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
        // Initialize AssetManager
        assetManager = AssetManager.getInstance();
        
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
        
        Button nightModeButton = new Button("Night Mode: OFF");
        nightModeButton.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;");
        
        Button showRoutesButton = new Button("Routes: ON");
        showRoutesButton.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white;");
        
        Button assetModeButton = new Button("Assets: AUTO");
        assetModeButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        
        // Asset status button
        Button assetStatusButton = new Button("Check Assets");
        assetStatusButton.setStyle("-fx-background-color: #795548; -fx-text-fill: white;");
        assetStatusButton.setOnAction(e -> {
            showAssetStatus();
        });
        
        HBox assetControls = new HBox(5, assetModeButton, assetStatusButton);
        assetControls.setAlignment(Pos.CENTER);
        
        HBox toggleButtons = new HBox(5, nightModeButton, showRoutesButton);
        toggleButtons.setAlignment(Pos.CENTER);
        
        nightModeButton.setOnAction(e -> {
            nightMode = !nightMode;
            nightModeButton.setText("Night Mode: " + (nightMode ? "ON" : "OFF"));
            nightModeButton.setStyle(nightMode ? 
                "-fx-background-color: #3F51B5; -fx-text-fill: white;" : 
                "-fx-background-color: #9C27B0; -fx-text-fill: white;");
        });
        
        showRoutesButton.setOnAction(e -> {
            showRoutes = !showRoutes;
            showRoutesButton.setText("Routes: " + (showRoutes ? "ON" : "OFF"));
            showRoutesButton.setStyle(showRoutes ? 
                "-fx-background-color: #4CAF50; -fx-text-fill: white;" : 
                "-fx-background-color: #607D8B; -fx-text-fill: white;");
        });
        
        assetModeButton.setOnAction(e -> {
            // Toggle through different asset modes: AUTO -> FORCE -> DISABLE -> AUTO
            String currentText = assetModeButton.getText();
            if (currentText.contains("AUTO")) {
                assetMode = "FORCE";
                assetModeButton.setText("Assets: FORCE");
                assetModeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
            } else if (currentText.contains("FORCE")) {
                assetMode = "DISABLE";
                assetModeButton.setText("Assets: DISABLE");
                assetModeButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            } else {
                assetMode = "AUTO";
                assetModeButton.setText("Assets: AUTO");
                assetModeButton.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
            }
        });
        
        visualOptions.getChildren().addAll(routesButton, toggleButtons, assetControls);
        
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
        
        // Draw environment background (buildings, trees, grass)
        drawEnvironment();
        
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
        
        // Draw intersections with enhanced visuals
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                double x = j * CELL_SIZE * 2 + CELL_SIZE - PADDING;
                double y = i * CELL_SIZE * 2 + CELL_SIZE - PADDING;
                
                // Light intersection color to match roads
                gc.setFill(nightMode ? Color.rgb(65, 65, 70) : Color.rgb(185, 185, 190));
                gc.fillRect(x, y, PADDING * 2, PADDING * 2);
                
                // Add subtle crosswalk markings
                gc.setStroke(nightMode ? Color.rgb(180, 180, 180, 0.5) : Color.rgb(255, 255, 255, 0.6));
                gc.setLineWidth(1);
                
                // Horizontal crosswalk stripes
                for (int stripe = 0; stripe < 3; stripe++) {
                    double stripeY = y + 3 + stripe * 4;
                    gc.strokeLine(x, stripeY, x + PADDING * 2, stripeY);
                }
                
                // Vertical crosswalk stripes
                for (int stripe = 0; stripe < 3; stripe++) {
                    double stripeX = x + 3 + stripe * 4;
                    gc.strokeLine(stripeX, y, stripeX, y + PADDING * 2);
                }
            }
        }
        
        // Draw vehicle routes first (behind vehicles)
        if (showRoutes) {
            drawVehicleRoutes();
        }
        
        // Draw vehicles
        drawVehicles();
    }
    
    private void drawEnvironment() {
        // Draw grass background in empty areas (less overwhelming)
        Image grassImage = assetManager.getImage("grass");
        if (grassImage != null && assetManager.hasImage("grass")) {
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    double x = j * CELL_SIZE * 2;
                    double y = i * CELL_SIZE * 2;
                    
                    // Draw grass in corners of each cell with some spacing
                    double grassSize = CELL_SIZE - 15;
                    double offset = 8;
                    
                    // Only draw grass in specific corners to avoid clutter
                    if ((i + j) % 2 == 0) {
                        gc.drawImage(grassImage, x + offset, y + offset, grassSize, grassSize);
                        gc.drawImage(grassImage, x + CELL_SIZE + offset, y + CELL_SIZE + offset, grassSize, grassSize);
                    } else {
                        gc.drawImage(grassImage, x + CELL_SIZE + offset, y + offset, grassSize, grassSize);
                        gc.drawImage(grassImage, x + offset, y + CELL_SIZE + offset, grassSize, grassSize);
                    }
                }
            }
        }
        
        // Draw buildings around the edges (more organized)
        Image buildingImage = assetManager.getImage("building");
        Image apartmentsImage = assetManager.getImage("apartments");
        
        if (buildingImage != null || apartmentsImage != null) {
            // Buildings on left and right sides only
            for (int i = 0; i < gridSize; i += 2) { // Skip every other position
                // Left side buildings
                if (buildingImage != null && assetManager.hasImage("building")) {
                    gc.drawImage(buildingImage, -35, i * CELL_SIZE * 2 + 15, 30, 45);
                }
                
                // Right side buildings  
                if (apartmentsImage != null && assetManager.hasImage("apartments")) {
                    gc.drawImage(apartmentsImage, gridSize * CELL_SIZE * 2 + 5, i * CELL_SIZE * 2 + 15, 30, 45);
                }
            }
        }
        
        // Draw trees more selectively
        Image treeImage = assetManager.getImage("tree");
        if (treeImage != null && assetManager.hasImage("tree")) {
            // Add trees only in specific locations
            for (int i = 0; i < gridSize; i++) {
                for (int j = 0; j < gridSize; j++) {
                    // Much more selective tree placement
                    if ((i + j * 2) % 6 == 0) { 
                        double x = j * CELL_SIZE * 2 + 12;
                        double y = i * CELL_SIZE * 2 + 12;
                        gc.drawImage(treeImage, x, y, 16, 20);
                    }
                }
            }
        }
        
        // Draw skyline background (less prominent)
        Image skylineImage = assetManager.getImage("skyline");
        if (skylineImage != null && assetManager.hasImage("skyline")) {
            // Draw skyline as subtle background
            double skylineY = nightMode ? -45 : -35;
            double skylineOpacity = nightMode ? 0.6 : 0.4;
            
            gc.setGlobalAlpha(skylineOpacity);
            for (int i = 0; i < 2; i++) {
                gc.drawImage(skylineImage, i * 150, skylineY, 160, 30);
            }
            gc.setGlobalAlpha(1.0); // Reset opacity
        }
    }
    
    private void drawRoad(String roadId, double x, double y, double width, double height, boolean isHorizontal) {
        Road road = cityMap.getRoadById(roadId);
        if (road != null) {
            // Try to use road assets first
            String assetName = isHorizontal ? "road_horizontal" : "road_vertical";
            Image roadImage = assetManager.getImage(assetName);
            
            if (roadImage != null && assetManager.hasImage(assetName)) {
                // Draw using road asset
                drawRoadWithAsset(roadImage, x, y, width, height, road);
            } else {
                // Fallback to enhanced shape-based drawing
                drawRoadWithShapes(x, y, width, height, isHorizontal, road);
            }
        }
    }
    
    private void drawRoadWithAsset(Image roadImage, double x, double y, double width, double height, Road road) {
        // Calculate congestion overlay
        double congestion = (double) road.getVehicleCount() / road.getCapacity();
        
        // Draw the road asset
        gc.drawImage(roadImage, x, y, width, height);
        
        // Add congestion overlay if needed
        if (congestion > 0.2) {
            Color congestionColor = getCongestionColor(congestion);
            gc.setFill(congestionColor);
            gc.fillRect(x, y, width, height);
        }
    }
    
    private void drawRoadWithShapes(double x, double y, double width, double height, boolean isHorizontal, Road road) {
        // Enhanced version of original road drawing with lighter colors for better vehicle contrast
        double congestion = (double) road.getVehicleCount() / road.getCapacity();
        
        // Lighter road base color so vehicles stand out more
        Color baseRoadColor = nightMode ? Color.rgb(60, 60, 65) : Color.rgb(180, 180, 185);
        gc.setFill(baseRoadColor);
        gc.fillRect(x, y, width, height);
        
        // Add congestion overlay if needed
        if (congestion > 0.2) {
            Color congestionColor = getCongestionColor(congestion);
            gc.setFill(congestionColor);
            gc.fillRect(x, y, width, height);
        }
        
        // Draw road markings with subtle contrast
        gc.setStroke(nightMode ? Color.rgb(200, 200, 200, 0.7) : Color.rgb(255, 255, 255, 0.8));
        gc.setLineWidth(2);
        
        if (isHorizontal) {
            // Enhanced dashed line in the middle of horizontal road
            double dashLength = 8;
            double gapLength = 6;
            double startX = x;
            double middleY = y + height / 2;
            
            for (double i = 0; i < width; i += dashLength + gapLength) {
                gc.strokeLine(startX + i, middleY, startX + i + dashLength, middleY);
            }
            
            // Add subtle side borders
            gc.setStroke(nightMode ? Color.rgb(200, 200, 200, 0.3) : Color.rgb(255, 255, 255, 0.4));
            gc.setLineWidth(1);
            gc.strokeLine(x, y + 2, x + width, y + 2);
            gc.strokeLine(x, y + height - 2, x + width, y + height - 2);
            
        } else {
            // Enhanced dashed line in the middle of vertical road
            double dashLength = 8;
            double gapLength = 6;
            double middleX = x + width / 2;
            double startY = y;
            
            for (double i = 0; i < height; i += dashLength + gapLength) {
                gc.strokeLine(middleX, startY + i, middleX, startY + i + dashLength);
            }
            
            // Add subtle side borders
            gc.setStroke(nightMode ? Color.rgb(200, 200, 200, 0.3) : Color.rgb(255, 255, 255, 0.4));
            gc.setLineWidth(1);
            gc.strokeLine(x + 2, y, x + 2, y + height);
            gc.strokeLine(x + width - 2, y, x + width - 2, y + height);
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
                // Get vehicle type and corresponding asset
                Vehicle.VehicleType type = vehicle.getVehicleType();
                String assetName = getAssetNameForVehicleType(type);
                Image vehicleImage = assetManager.getImage(assetName);
                
                if (shouldUseAssets(assetName) && vehicleImage != null) {
                    // Draw vehicle using asset
                    drawVehicleWithAsset(info, vehicleImage, type, vehicle);
                } else {
                    // Fallback to original shape-based drawing with enhanced visuals
                    drawVehicleWithShapes(info, type, vehicle);
                }
                
                // Draw speed indicator
                if (vehicle.getSpeed() > 1.1) {
                    drawSpeedIndicator(info, vehicle);
                }
            }
        }
    }
    
    private String getAssetNameForVehicleType(Vehicle.VehicleType type) {
        switch (type) {
            case CAR: return "car";
            case TRUCK: return "truck";
            case MOTORCYCLE: return "motorcycle";
            case BUS: return "bus";
            default: return "car";
        }
    }
    
    private void drawVehicleWithAsset(VehicleInfo info, Image vehicleImage, Vehicle.VehicleType type, Vehicle vehicle) {
        // Calculate vehicle size based on type
        double size = VEHICLE_SIZE;
        switch (type) {
            case TRUCK: size *= 1.4; break;
            case MOTORCYCLE: size *= 0.8; break;
            case BUS: size *= 1.6; break;
            default: size *= 1.0; break;
        }
        
        // Add glow effect
        DropShadow glow = new DropShadow();
        glow.setColor(info.color);
        glow.setRadius(size * 0.8);
        gc.setEffect(glow);
        
        // Calculate rotation based on road direction
        Road currentRoad = vehicle.getCurrentRoad();
        double rotation = 0;
        if (currentRoad != null) {
            String roadId = currentRoad.getId();
            if (roadId.startsWith("R")) {
                rotation = 0; // Horizontal road
            } else {
                rotation = 90; // Vertical road
            }
        }
        
        // Save graphics context
        gc.save();
        
        // Apply rotation
        gc.translate(info.x, info.y);
        gc.rotate(rotation);
        
        // Draw the vehicle image
        gc.drawImage(vehicleImage, -size/2, -size/2, size, size);
        
        // Restore graphics context
        gc.restore();
        gc.setEffect(null);
    }
    
    private void drawVehicleWithShapes(VehicleInfo info, Vehicle.VehicleType type, Vehicle vehicle) {
        // Enhanced version of original shape-based drawing
        DropShadow glow = new DropShadow();
        glow.setColor(info.color);
        glow.setRadius(VEHICLE_SIZE);
        
        gc.setEffect(glow);
        gc.setFill(info.color);
        
        double size = VEHICLE_SIZE;
        
        switch (type) {
            case CAR:
                // Enhanced car with direction indicator
                gc.fillOval(info.x - size/2, info.y - size/2, size, size);
                // Add direction indicator
                gc.setFill(Color.WHITE);
                Road currentRoad = vehicle.getCurrentRoad();
                if (currentRoad != null && currentRoad.getId().startsWith("R")) {
                    // Horizontal direction indicator
                    gc.fillRect(info.x + size/4, info.y - 1, size/4, 2);
                } else {
                    // Vertical direction indicator  
                    gc.fillRect(info.x - 1, info.y + size/4, 2, size/4);
                }
                break;
            case TRUCK:
                size *= 1.4;
                // Enhanced truck with cab and trailer
                gc.fillRect(info.x - size/2, info.y - size/3, size, size*0.7);
                // Cab detail
                gc.setFill(info.color.darker());
                gc.fillRect(info.x + size/3, info.y - size/4, size/6, size/2);
                break;
            case MOTORCYCLE:
                size *= 0.8;
                // Enhanced motorcycle with rider silhouette
                double[] xPoints = {info.x, info.x + size/2, info.x, info.x - size/2};
                double[] yPoints = {info.y - size/2, info.y, info.y + size/2, info.y};
                gc.fillPolygon(xPoints, yPoints, 4);
                // Rider detail
                gc.setFill(Color.DARKGRAY);
                gc.fillOval(info.x - size/6, info.y - size/3, size/3, size/3);
                break;
            case BUS:
                size *= 1.6;
                // Enhanced bus with windows
                gc.fillRoundRect(info.x - size/2, info.y - size/3, size, size*0.6, 4, 4);
                // Windows
                gc.setFill(Color.LIGHTBLUE);
                for (int i = 0; i < 3; i++) {
                    double windowX = info.x - size/3 + i * size/4;
                    gc.fillRect(windowX, info.y - size/6, size/6, size/4);
                }
                break;
        }
        
        gc.setEffect(null);
    }
    
    private void drawSpeedIndicator(VehicleInfo info, Vehicle vehicle) {
        // Enhanced speed lines with particle effect
        gc.setStroke(info.color);
        gc.setLineWidth(1);
        double speedLineLength = vehicle.getSpeed() * 5;
        
        Road currentRoad = vehicle.getCurrentRoad();
        if (currentRoad != null) {
            String roadId = currentRoad.getId();
            boolean isHorizontal = roadId.startsWith("R");
            
            // Multiple speed lines for better effect
            for (int i = 0; i < 3; i++) {
                double offset = i * 2;
                double opacity = 1.0 - (i * 0.3);
                
                Color speedColor = Color.rgb(
                    (int)(info.color.getRed() * 255),
                    (int)(info.color.getGreen() * 255),
                    (int)(info.color.getBlue() * 255),
                    opacity
                );
                gc.setStroke(speedColor);
                
                if (isHorizontal) {
                    gc.strokeLine(
                        info.x - speedLineLength - offset, 
                        info.y + (i-1), 
                        info.x - speedLineLength/2 - offset, 
                        info.y + (i-1)
                    );
                } else {
                    gc.strokeLine(
                        info.x + (i-1), 
                        info.y - speedLineLength - offset, 
                        info.x + (i-1), 
                        info.y - speedLineLength/2 - offset
                    );
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
                // Get appropriate traffic light asset
                String assetName = getTrafficLightAssetName(light.getTrafficLightState());
                Image lightImage = assetManager.getImage(assetName);
                
                if (lightImage != null && assetManager.hasImage(assetName)) {
                    // Draw using asset
                    drawTrafficLightWithAsset(lightImage, x, y, light);
                } else {
                    // Fallback to enhanced shape-based drawing
                    drawTrafficLightWithShapes(x, y, light);
                }
            }
        }
    }
    
    private String getTrafficLightAssetName(TrafficLight.State state) {
        switch (state) {
            case GREEN: return "traffic_light_green";
            case YELLOW: return "traffic_light_yellow";
            case RED: return "traffic_light_red";
            default: return "traffic_light_red";
        }
    }
    
    private void drawTrafficLightWithAsset(Image lightImage, double x, double y, TrafficLight light) {
        // Apply brightness effect
        double brightness = light.getBrightness();
        
        // Create glow effect
        Glow glow = new Glow();
        glow.setLevel(0.8 * brightness);
        
        // Add emergency effect if needed
        if (light.isEmergencyMode()) {
            DropShadow emergencyEffect = new DropShadow();
            emergencyEffect.setColor(Color.YELLOW);
            emergencyEffect.setRadius(PADDING * 2);
            emergencyEffect.setSpread(0.7);
            gc.setEffect(emergencyEffect);
        } else {
            gc.setEffect(glow);
        }
        
        // Draw the traffic light image
        double size = PADDING * 2;
        gc.drawImage(lightImage, x - size/2, y - size/2, size, size);
        gc.setEffect(null);
    }
    
    private void drawTrafficLightWithShapes(double x, double y, TrafficLight light) {
        // Enhanced shape-based traffic light with housing
        Color color;
        switch (light.getTrafficLightState()) {
            case GREEN -> color = Color.rgb(0, 255, 0);
            case YELLOW -> color = Color.rgb(255, 255, 0);
            default -> color = Color.rgb(255, 0, 0);
        }
        
        // Apply brightness adjustment for blinking effect
        double brightness = light.getBrightness();
        color = color.deriveColor(0, 1.0, brightness, 1.0);
        
        // Draw traffic light housing (background)
        gc.setFill(nightMode ? Color.rgb(30, 30, 30) : Color.rgb(60, 60, 60));
        gc.fillRoundRect(x - PADDING, y - PADDING * 1.5, PADDING * 2, PADDING * 3, 4, 4);
        
        // Draw all three light positions
        double lightRadius = PADDING * 0.3;
        
        // Red light position (top)
        gc.setFill(light.getTrafficLightState() == TrafficLight.State.RED ? 
            color : Color.rgb(80, 0, 0));
        gc.fillOval(x - lightRadius, y - PADDING, lightRadius * 2, lightRadius * 2);
        
        // Yellow light position (middle)
        gc.setFill(light.getTrafficLightState() == TrafficLight.State.YELLOW ? 
            color : Color.rgb(80, 80, 0));
        gc.fillOval(x - lightRadius, y - lightRadius, lightRadius * 2, lightRadius * 2);
        
        // Green light position (bottom)
        gc.setFill(light.getTrafficLightState() == TrafficLight.State.GREEN ? 
            color : Color.rgb(0, 80, 0));
        gc.fillOval(x - lightRadius, y, lightRadius * 2, lightRadius * 2);
        
        // Create glow effect for active light
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
        
        // Redraw the active light with glow
        gc.setFill(color);
        switch (light.getTrafficLightState()) {
            case RED -> gc.fillOval(x - lightRadius, y - PADDING, lightRadius * 2, lightRadius * 2);
            case YELLOW -> gc.fillOval(x - lightRadius, y - lightRadius, lightRadius * 2, lightRadius * 2);
            case GREEN -> gc.fillOval(x - lightRadius, y, lightRadius * 2, lightRadius * 2);
        }
        
        gc.setEffect(null);
        
        // Draw housing outline
        gc.setStroke(nightMode ? Color.rgb(100, 100, 100) : Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x - PADDING, y - PADDING * 1.5, PADDING * 2, PADDING * 3, 4, 4);
    }
    
    private void showAssetStatus() {
        StringBuilder status = new StringBuilder();
        status.append("🎨 ASSET STATUS REPORT\n");
        status.append("========================\n\n");
        
        // Vehicle assets
        status.append("🚗 VEHICLES:\n");
        String[] vehicles = {"car", "truck", "motorcycle", "bus"};
        for (String vehicle : vehicles) {
            boolean hasAsset = assetManager.hasImage(vehicle);
            status.append(String.format("  %s %s\n", 
                hasAsset ? "✅" : "❌", 
                vehicle + (hasAsset ? " - LOADED" : " - MISSING (using shapes)")));
        }
        
        status.append("\n🛣️ ROADS:\n");
        String[] roads = {"road_horizontal", "road_vertical", "intersection"};
        for (String road : roads) {
            boolean hasAsset = assetManager.hasImage(road);
            status.append(String.format("  %s %s\n", 
                hasAsset ? "✅" : "❌", 
                road + (hasAsset ? " - LOADED" : " - MISSING (using shapes)")));
        }
        
        status.append("\n🚦 TRAFFIC LIGHTS:\n");
        String[] lights = {"traffic_light_red", "traffic_light_yellow", "traffic_light_green"};
        for (String light : lights) {
            boolean hasAsset = assetManager.hasImage(light);
            status.append(String.format("  %s %s\n", 
                hasAsset ? "✅" : "❌", 
                light + (hasAsset ? " - LOADED" : " - MISSING (using enhanced shapes)")));
        }
        
        // Count total loaded assets
        int totalLoaded = 0;
        int totalAssets = vehicles.length + roads.length + lights.length;
        for (String asset : vehicles) if (assetManager.hasImage(asset)) totalLoaded++;
        for (String asset : roads) if (assetManager.hasImage(asset)) totalLoaded++;
        for (String asset : lights) if (assetManager.hasImage(asset)) totalLoaded++;
        
        status.append(String.format("\n📊 SUMMARY: %d/%d assets loaded (%.1f%%)\n", 
            totalLoaded, totalAssets, (totalLoaded * 100.0 / totalAssets)));
        
        if (totalLoaded > 0) {
            status.append("\n🎮 Assets are working! You should see improved graphics in the simulation.");
        } else {
            status.append("\n💡 No assets loaded. Using enhanced shape-based graphics.");
            status.append("\n   To use assets, check that files are in the assets/ folders with correct names.");
        }
        
        // Show in a simple dialog (using console for now)
        System.out.println("\n" + status.toString());
        
        // Update stats label to show asset status briefly
        String originalText = statsLabel.getText();
        statsLabel.setText("Asset Status: " + totalLoaded + "/" + totalAssets + " loaded - Check console for details");
        
        // Restore original text after 3 seconds
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                Platform.runLater(() -> statsLabel.setText(originalText));
            } catch (InterruptedException ex) {}
        }).start();
    }
    
    private boolean shouldUseAssets(String assetName) {
        switch (assetMode) {
            case "DISABLE":
                return false;
            case "FORCE":
                return true; // Force even if asset is missing, will use enhanced shapes
            case "AUTO":
            default:
                return assetManager.hasImage(assetName);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 