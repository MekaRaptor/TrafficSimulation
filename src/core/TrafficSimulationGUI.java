import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TrafficSimulationGUI extends Application {
    private TrafficController trafficController;
    private Canvas canvas;
    private GraphicsContext gc;
    private AnimationTimer animationTimer;
    private Button startButton, stopButton;
    private Label statusLabel, vehicleCountLabel, statsLabel;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    
    private static final double CANVAS_WIDTH = 800;
    private static final double CANVAS_HEIGHT = 600;
    
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("🚗 Traffic Simulation - JavaFX GUI");
        
        // Initialize the traffic controller
        trafficController = new TrafficController();
        
        // Create the main layout
        BorderPane root = new BorderPane();
        
        // Create control panel
        VBox controlPanel = createControlPanel();
        root.setLeft(controlPanel);
        
        // Create canvas for animation
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        root.setCenter(canvas);
        
        // Create scene
        Scene scene = new Scene(root, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Start animation loop
        setupAnimationTimer();
        
        // Initial draw
        drawScene();
    }
    
    private VBox createControlPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(350);
        panel.setStyle("-fx-background-color: #f0f0f0;");
        
        // Title
        Label titleLabel = new Label("🚦 Traffic Control Panel");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Status
        statusLabel = new Label("Status: ⏹️ Stopped");
        statusLabel.setStyle("-fx-font-size: 14px;");
        
        // Vehicle count
        vehicleCountLabel = new Label("Active Vehicles: 0");
        vehicleCountLabel.setStyle("-fx-font-size: 12px;");
        
        // Statistics
        statsLabel = new Label("Statistics: 0 total spawned");
        statsLabel.setStyle("-fx-font-size: 12px;");
        
        // Control buttons
        HBox buttonBox = new HBox(10);
        startButton = new Button("▶️ Start");
        stopButton = new Button("⏹️ Stop");
        
        startButton.setOnAction(e -> startSimulation());
        stopButton.setOnAction(e -> stopSimulation());
        
        buttonBox.getChildren().addAll(startButton, stopButton);
        
        // Add all components
        panel.getChildren().addAll(
            titleLabel, new Separator(),
            statusLabel, vehicleCountLabel, statsLabel,
            new Separator(), buttonBox
        );
        
        return panel;
    }
    
    private void startSimulation() {
        if (!isRunning.get()) {
            isRunning.set(true);
            startButton.setDisable(true);
            stopButton.setDisable(false);
            statusLabel.setText("Status: ▶️ Running");
            
            trafficController.startSimulation();
        }
    }
    
    private void stopSimulation() {
        isRunning.set(false);
        stopButton.setDisable(true);
        startButton.setDisable(false);
        statusLabel.setText("Status: ⏹️ Stopped");
        
        trafficController.stopSimulation();
    }
    
    private void setupAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isRunning.get()) {
                    updateSimulation();
                }
                drawScene();
            }
        };
        animationTimer.start();
    }
    
    private void updateSimulation() {
        Platform.runLater(() -> {
            // Update vehicle count and statistics
            int activeVehicles = trafficController.getActiveVehicleCount();
            vehicleCountLabel.setText("Active Vehicles: " + activeVehicles);
            
            int totalSpawned = trafficController.getTotalVehiclesSpawned();
            statsLabel.setText("Statistics: " + totalSpawned + " total spawned");
        });
    }
    
    private void drawScene() {
        // Clear canvas
        gc.setFill(Color.LIGHTGRAY);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        
        // Draw road network
        drawRoadNetwork();
        
        // Draw vehicles
        drawVehicles();
        
        // Draw legend
        drawLegend();
    }
    
    private void drawRoadNetwork() {
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(3);
        
        // City A (left)
        gc.setFill(Color.LIGHTBLUE);
        gc.fillRoundRect(50, 250, 100, 100, 10, 10);
        gc.setFill(Color.BLACK);
        gc.fillText("City A", 85, 305);
        
        // City B (right)
        gc.setFill(Color.LIGHTGREEN);
        gc.fillRoundRect(650, 250, 100, 100, 10, 10);
        gc.setFill(Color.BLACK);
        gc.fillText("City B", 685, 305);
        
        // K1 Intersection
        gc.setFill(Color.ORANGE);
        gc.fillOval(270, 280, 60, 40);
        gc.setFill(Color.BLACK);
        gc.fillText("K1", 295, 305);
        
        // K2 Intersection  
        gc.setFill(Color.ORANGE);
        gc.fillOval(470, 280, 60, 40);
        gc.setFill(Color.BLACK);
        gc.fillText("K2", 495, 305);
        
        // Roads A to K1
        gc.setStroke(Color.DARKBLUE);
        gc.setLineWidth(8);
        gc.strokeLine(150, 300, 270, 300);
        
        // Three parallel roads K1 to K2
        gc.setStroke(Color.DARKRED);
        gc.strokeLine(330, 270, 470, 270); // Road 1 (top)
        gc.strokeLine(330, 300, 470, 300); // Road 2 (middle)  
        gc.strokeLine(330, 330, 470, 330); // Road 3 (bottom)
        
        // Roads K2 to B
        gc.setStroke(Color.DARKGREEN);
        gc.strokeLine(530, 300, 650, 300);
        
        // Draw road labels
        gc.setFill(Color.WHITE);
        gc.fillText("Road 1", 385, 265);
        gc.fillText("Road 2", 385, 295); 
        gc.fillText("Road 3", 385, 325);
        
        // Show road congestion
        drawRoadCongestion();
    }
    
    private void drawRoadCongestion() {
        // Get congestion data from traffic controller
        Map<Integer, Integer> congestion = trafficController.getRoadCongestion();
        
        // Road 1 congestion
        int road1Count = congestion.getOrDefault(1, 0);
        drawCongestionBar(335, 250, road1Count);
        
        // Road 2 congestion
        int road2Count = congestion.getOrDefault(2, 0);
        drawCongestionBar(335, 280, road2Count);
        
        // Road 3 congestion
        int road3Count = congestion.getOrDefault(3, 0);
        drawCongestionBar(335, 310, road3Count);
    }
    
    private void drawCongestionBar(double x, double y, int vehicleCount) {
        // Background bar
        gc.setFill(Color.WHITE);
        gc.fillRect(x, y, 120, 10);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x, y, 120, 10);
        
        // Congestion level (max 5 vehicles per road)
        double congestionLevel = Math.min(1.0, vehicleCount / 5.0);
        Color congestionColor = congestionLevel < 0.3 ? Color.GREEN : 
                              congestionLevel < 0.7 ? Color.ORANGE : Color.RED;
        
        gc.setFill(congestionColor);
        gc.fillRect(x + 1, y + 1, (120 - 2) * congestionLevel, 8);
        
        // Vehicle count text
        gc.setFill(Color.BLACK);
        gc.fillText(String.valueOf(vehicleCount), x + 125, y + 8);
    }
    
    private void drawVehicles() {
        // Simplified vehicle drawing - show animated vehicles moving along roads
        Random rand = new Random(System.currentTimeMillis() / 1000); // Update every second
        
        int activeVehicles = trafficController.getActiveVehicleCount();
        for (int i = 0; i < activeVehicles && i < 15; i++) {
            // Simulate vehicle positions along different road segments
            double progress = (rand.nextDouble() + i * 0.1) % 1.0;
            
            if (progress < 0.2) {
                // A to K1 segment
                double x = 150 + progress * 5 * 120;
                drawVehicle(x, 300, VehicleType.values()[i % 4]);
            } else if (progress < 0.8) {
                // K1 to K2 segment (on one of three roads)
                int roadIndex = i % 3;
                double roadY = 270 + roadIndex * 30;
                double x = 330 + (progress - 0.2) * (140.0 / 0.6);
                drawVehicle(x, roadY, VehicleType.values()[i % 4]);
            } else {
                // K2 to B segment
                double x = 530 + (progress - 0.8) * 5 * 120;
                drawVehicle(x, 300, VehicleType.values()[i % 4]);
            }
        }
    }
    
    private void drawVehicle(double x, double y, VehicleType type) {
        Color color = getVehicleColor(type);
        gc.setFill(color);
        
        switch (type) {
            case CAR:
                gc.fillOval(x-8, y-5, 16, 10);
                break;
            case TRUCK:
                gc.fillRect(x-12, y-6, 24, 12);
                break;
            case MOTORCYCLE:
                gc.fillOval(x-4, y-3, 8, 6);
                break;
            case BUS:
                gc.fillRoundRect(x-15, y-7, 30, 14, 4, 4);
                break;
        }
        
        // Add a small border
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        gc.strokeOval(x-8, y-5, 16, 10);
    }
    
    private Color getVehicleColor(VehicleType type) {
        switch (type) {
            case CAR: return Color.BLUE;
            case TRUCK: return Color.RED;
            case MOTORCYCLE: return Color.YELLOW;
            case BUS: return Color.PURPLE;
            default: return Color.BLACK;
        }
    }
    
    private void drawLegend() {
        gc.setFill(Color.WHITE);
        gc.fillRoundRect(20, 20, 220, 180, 10, 10);
        gc.setStroke(Color.BLACK);
        gc.strokeRoundRect(20, 20, 220, 180, 10, 10);
        
        gc.setFill(Color.BLACK);
        gc.fillText("🚗 Vehicle Legend:", 30, 40);
        
        VehicleType[] types = VehicleType.values();
        for (int i = 0; i < types.length; i++) {
            VehicleType type = types[i];
            Color color = getVehicleColor(type);
            
            gc.setFill(color);
            gc.fillOval(35, 55 + i * 25, 15, 10);
            
            gc.setFill(Color.BLACK);
            gc.fillText(type.name() + " (" + (type.getRoadTravelTime() / 1000) + "s)", 55, 65 + i * 25);
        }
        
        // Add road congestion legend
        gc.fillText("📊 Road Congestion:", 30, 160);
        gc.setFill(Color.GREEN); gc.fillRect(35, 170, 15, 8);
        gc.setFill(Color.BLACK); gc.fillText("Low", 55, 178);
        gc.setFill(Color.ORANGE); gc.fillRect(90, 170, 15, 8);
        gc.setFill(Color.BLACK); gc.fillText("Medium", 110, 178);
        gc.setFill(Color.RED); gc.fillRect(160, 170, 15, 8);
        gc.setFill(Color.BLACK); gc.fillText("High", 180, 178);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
} 