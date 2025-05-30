package src.core;

import java.io.*;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Java Simulation Server
 * Communicates with Node.js API layer via stdin/stdout
 */
public class SimulationServer {
    private final SimulationEngine engine;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = true;
    
    public SimulationServer() {
        this.engine = new SimulationEngine();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        // Add listener to broadcast state changes
        this.engine.addListener(this::onSimulationEvent);
    }
    
    public void start() {
        System.err.println("🏭 Java Simulation Server starting...");
        
        try {
            // Initialize simulation
            engine.initialize(15);
            
            // Start command listener
            startCommandListener();
            
            // Start periodic state broadcast
            startStateBroadcast();
            
            System.err.println("✅ Java Simulation Server ready!");
            
            // Keep server alive
            while (running) {
                Thread.sleep(1000);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Error in simulation server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    private void startCommandListener() {
        Thread commandThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            
            while (running && scanner.hasNextLine()) {
                try {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        handleCommand(line);
                    }
                } catch (Exception e) {
                    System.err.println("Error processing command: " + e.getMessage());
                }
            }
            
            scanner.close();
        });
        
        commandThread.setDaemon(true);
        commandThread.start();
    }
    
    private void startStateBroadcast() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                broadcastState();
            } catch (Exception e) {
                System.err.println("Error broadcasting state: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    
    private void handleCommand(String jsonCommand) {
        // Parse JSON command (simplified - in production use proper JSON library)
        System.err.println("🔧 Received command: " + jsonCommand); // DEBUG
        try {
            if (jsonCommand.contains("\"command\":\"start\"")) {
                System.err.println("🚀 Starting simulation..."); // DEBUG
                engine.start();
                sendResponse("start", true, "Simulation started");
                
            } else if (jsonCommand.contains("\"command\":\"stop\"")) {
                System.err.println("🛑 Stopping simulation..."); // DEBUG
                engine.stop();
                sendResponse("stop", true, "Simulation stopped");
                
            } else if (jsonCommand.contains("\"command\":\"reset\"")) {
                // Extract vehicle count from JSON (simplified)
                int vehicleCount = extractVehicleCount(jsonCommand);
                System.err.println("🔄 Resetting simulation with " + vehicleCount + " vehicles..."); // DEBUG
                engine.reset(vehicleCount);
                sendResponse("reset", true, "Simulation reset with " + vehicleCount + " vehicles");
                
            } else if (jsonCommand.contains("\"command\":\"addVehicle\"")) {
                // For now, just reset with one more vehicle
                // In production, implement proper vehicle addition
                sendResponse("addVehicle", true, "Vehicle added (mock response)");
                
            } else if (jsonCommand.contains("\"command\":\"getState\"")) {
                broadcastState();
                
            } else if ("STATUS".equalsIgnoreCase(jsonCommand)) {
                boolean isRunning = engine.getState().isRunning();
                sendResponse("status", true, isRunning ? "running" : "stopped");
            } else if ("STATS".equalsIgnoreCase(jsonCommand)) {
                sendResponse("stats", true, getSimulationStats());
            } else if ("ANALYSIS".equalsIgnoreCase(jsonCommand)) {
                sendResponse("analysis", true, engine.getState().getCityMap().getTrafficAnalysis());
            } else if (jsonCommand.contains("\"command\":\"getOptimalTime\"")) {
                // Extract start and end roads from JSON
                String startRoad = extractParameter(jsonCommand, "startRoad", "MAIN_EAST_1");
                String endRoad = extractParameter(jsonCommand, "endRoad", "MAIN_WEST_1");
                sendResponse("optimalTime", true, engine.getState().getCityMap().getOptimalDepartureTime(startRoad, endRoad));
            } else if ("HOURLY_REPORT".equalsIgnoreCase(jsonCommand)) {
                sendResponse("hourlyReport", true, engine.getState().getCityMap().getHourlyTrafficReport());
            } else {
                System.err.println("❌ Unknown command: " + jsonCommand);
            }
            
        } catch (Exception e) {
            System.err.println("Error handling command: " + e.getMessage());
            sendResponse("error", false, e.getMessage());
        }
    }
    
    private int extractVehicleCount(String jsonCommand) {
        // Simplified JSON parsing - extract vehicleCount
        try {
            int startIndex = jsonCommand.indexOf("\"vehicleCount\":") + 15;
            int endIndex = jsonCommand.indexOf("}", startIndex);
            if (endIndex == -1) endIndex = jsonCommand.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = jsonCommand.length();
            
            String countStr = jsonCommand.substring(startIndex, endIndex).trim();
            return Integer.parseInt(countStr);
        } catch (Exception e) {
            return 15; // Default
        }
    }
    
    private String extractParameter(String jsonCommand, String parameterName, String defaultValue) {
        // Simplified JSON parsing - extract a parameter
        try {
            int startIndex = jsonCommand.indexOf("\"" + parameterName + "\":\"") + parameterName.length() + 3;
            int endIndex = jsonCommand.indexOf("\"", startIndex);
            if (endIndex == -1) endIndex = jsonCommand.length();
            
            String parameterValue = jsonCommand.substring(startIndex, endIndex).trim();
            return parameterValue.isEmpty() ? defaultValue : parameterValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private void sendResponse(String command, boolean success, String message) {
        try {
            String response = String.format(
                "{\"type\":\"response\",\"command\":\"%s\",\"success\":%b,\"message\":\"%s\"}",
                command, success, message
            );
            System.out.println(response);
            System.out.flush();
        } catch (Exception e) {
            System.err.println("Error sending response: " + e.getMessage());
        }
    }
    
    private String getSimulationStats() {
        try {
            SimulationEngine.SimulationState state = engine.getState();
            StringBuilder stats = new StringBuilder();
            
            stats.append("📊 SIMULATION STATISTICS\\n");
            stats.append("=".repeat(30)).append("\\n");
            stats.append("Status: ").append(state.isRunning() ? "Running" : "Stopped").append("\\n");
            stats.append("Simulation Time: ").append(state.getSimulationTime()).append("s\\n");
            stats.append("Active Vehicles: ").append(state.getVehicles().size()).append("\\n");
            stats.append("Total Roads: ").append(state.getRoads().size()).append("\\n");
            stats.append("Traffic Lights: ").append(state.getLights().size()).append("\\n");
            stats.append("Zones: ").append(state.getZones().size()).append("\\n");
            
            // Vehicle type breakdown
            stats.append("\\nVehicle Types:\\n");
            long cars = state.getVehicles().stream().filter(v -> v.getVehicleType() == Vehicle.VehicleType.CAR).count();
            long trucks = state.getVehicles().stream().filter(v -> v.getVehicleType() == Vehicle.VehicleType.TRUCK).count();
            long motorcycles = state.getVehicles().stream().filter(v -> v.getVehicleType() == Vehicle.VehicleType.MOTORCYCLE).count();
            long buses = state.getVehicles().stream().filter(v -> v.getVehicleType() == Vehicle.VehicleType.BUS).count();
            
            stats.append("- Cars: ").append(cars).append("\\n");
            stats.append("- Trucks: ").append(trucks).append("\\n");
            stats.append("- Motorcycles: ").append(motorcycles).append("\\n");
            stats.append("- Buses: ").append(buses).append("\\n");
            
            return stats.toString();
            
        } catch (Exception e) {
            return "Error generating stats: " + e.getMessage();
        }
    }
    
    private void broadcastState() {
        try {
            SimulationEngine.SimulationState state = engine.getState();
            String jsonState = convertStateToJSON(state);
            
            String broadcast = String.format(
                "{\"type\":\"state\",\"payload\":%s}",
                jsonState
            );
            
            System.out.println(broadcast);
            System.out.flush();
            
        } catch (Exception e) {
            System.err.println("Error broadcasting state: " + e.getMessage());
        }
    }
    
    private String convertStateToJSON(SimulationEngine.SimulationState state) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // Basic state
        json.append("\"running\":").append(state.isRunning()).append(",");
        json.append("\"simulationTime\":").append(state.getSimulationTime()).append(",");
        
        // Vehicles
        json.append("\"vehicles\":[");
        boolean first = true;
        for (Vehicle vehicle : state.getVehicles()) {
            if (!first) json.append(",");
            json.append(vehicleToJSON(vehicle));
            first = false;
        }
        json.append("],");
        
        // Roads
        json.append("\"roads\":[");
        first = true;
        for (Road road : state.getRoads()) {
            if (!first) json.append(",");
            json.append(roadToJSON(road));
            first = false;
        }
        json.append("],");
        
        // Zones
        json.append("\"zones\":[");
        first = true;
        for (CityMap.Zone zone : state.getZones().values()) {
            if (!first) json.append(",");
            json.append(zoneToJSON(zone));
            first = false;
        }
        json.append("],");
        
        // Traffic Lights
        json.append("\"lights\":[");
        first = true;
        for (TrafficLight light : state.getLights()) {
            if (!first) json.append(",");
            json.append(lightToJSON(light));
            first = false;
        }
        json.append("],");
        
        // Metrics
        json.append("\"metrics\":{");
        boolean firstMetric = true;
        for (String key : state.getMetrics().keySet()) {
            if (!firstMetric) json.append(",");
            json.append("\"").append(key).append("\":").append(state.getMetrics().get(key));
            firstMetric = false;
        }
        json.append("}");
        
        json.append("}");
        return json.toString();
    }
    
    private String vehicleToJSON(Vehicle vehicle) {
        String threadName = "Vehicle-" + vehicle.getVehicleId() + "-" + 
                           (vehicle.getVehicleType() != null ? vehicle.getVehicleType().getDisplayName() : "Unknown");
        
        String threadState = vehicle.isActive() ? "ACTIVE" : "COMPLETED";
        
        return String.format(
            "{\"id\":\"%s\",\"threadName\":\"%s\",\"threadState\":\"%s\",\"type\":\"%s\"," +
            "\"x\":%.2f,\"y\":%.2f,\"speed\":%.2f,\"active\":%b,\"onRoad\":%b," +
            "\"roadId\":\"%s\",\"progress\":%.2f,\"startZone\":\"%s\",\"endZone\":\"%s\"," +
            "\"routeDensity\":%.2f,\"estimatedTime\":%.1f,\"distanceTraveled\":%.2f," +
            "\"journeyStartTime\":\"%s\",\"roadsVisited\":%d}",
            vehicle.getVehicleId(),
            threadName,
            threadState,
            vehicle.getVehicleType() != null ? vehicle.getVehicleType().getDisplayName() : "Unknown",
            vehicle.getX(),
            vehicle.getY(),
            vehicle.getSpeed(),
            vehicle.isActive(),
            vehicle.isOnRoad(),
            vehicle.getCurrentRoadId() != null ? vehicle.getCurrentRoadId() : "",
            vehicle.getProgress(),
            vehicle.getStartZone() != null ? vehicle.getStartZone().getId() : "",
            vehicle.getEndZone() != null ? vehicle.getEndZone().getId() : "",
            vehicle.getAverageRouteDensity(),
            vehicle.getEstimatedJourneyTime(),
            vehicle.getTotalDistanceTraveled(),
            vehicle.getJourneyStartTime() != null ? vehicle.getJourneyStartTime() : "",
            vehicle.getRoadsVisited().size()
        );
    }
    
    private String roadToJSON(Road road) {
        double currentDensity = road.getCurrentTrafficDensity();
        double baseDensity = road.getTrafficDensity();
        String densityLevel = getDensityLevel(currentDensity);
        String densityColor = getDensityColor(currentDensity);
        
        return String.format(
            "{\"id\":\"%s\",\"type\":\"%s\",\"x1\":%.2f,\"y1\":%.2f,\"x2\":%.2f,\"y2\":%.2f," +
            "\"capacity\":%d,\"speedLimit\":%.2f,\"congestionLevel\":%.2f,\"vehicleCount\":%d," +
            "\"hasTrafficLights\":%b,\"baseDensity\":%.2f,\"currentDensity\":%.2f," +
            "\"densityLevel\":\"%s\",\"densityColor\":\"%s\",\"efficiency\":%.2f}",
            road.getId(),
            road.getRoadType() != null ? road.getRoadType().getDisplayName() : "Unknown",
            road.getX1(),
            road.getY1(),
            road.getX2(),
            road.getY2(),
            road.getCapacity(),
            road.getSpeedLimit(),
            road.getCongestionLevel(),
            road.getVehicleCount(),
            road.hasTrafficLights(),
            baseDensity,
            currentDensity,
            densityLevel,
            densityColor,
            calculateRoadEfficiency(road)
        );
    }
    
    private String getDensityLevel(double density) {
        if (density < 0.3) return "Light";
        else if (density < 0.6) return "Moderate"; 
        else if (density < 0.8) return "Heavy";
        else return "Severe";
    }
    
    private String getDensityColor(double density) {
        if (density < 0.3) return "#00FF00";      // Green
        else if (density < 0.6) return "#FFFF00"; // Yellow
        else if (density < 0.8) return "#FFA500"; // Orange
        else return "#FF0000";                    // Red
    }
    
    private double calculateRoadEfficiency(Road road) {
        double maxEfficiency = 1.0;
        double congestion = road.getCongestionLevel();
        double density = road.getCurrentTrafficDensity();
        
        // Efficiency decreases with congestion and density
        return maxEfficiency * (1.0 - (congestion * 0.4 + density * 0.6));
    }
    
    private String zoneToJSON(CityMap.Zone zone) {
        return String.format(
            "{\"id\":\"%s\",\"type\":\"%s\",\"x\":%.2f,\"y\":%.2f,\"width\":%.2f,\"height\":%.2f}",
            zone.getId(),
            zone.getType(),
            zone.getX(),
            zone.getY(),
            zone.getWidth(),
            zone.getHeight()
        );
    }
    
    private String lightToJSON(TrafficLight light) {
        String lightStatus = light.getCurrentState().name();
        String roadId = light.getRoadId();
        
        // Get road information for positioning
        Road road = null;
        try {
            // Find the road this light belongs to
            for (Road r : engine.getState().getRoads()) {
                if (r.getId().equals(roadId)) {
                    road = r;
                    break;
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        
        double lightX = road != null ? (road.getX1() + road.getX2()) / 2 : 0;
        double lightY = road != null ? (road.getY1() + road.getY2()) / 2 : 0;
        
        return String.format(
            "{\"id\":\"%s\",\"roadId\":\"%s\",\"state\":\"%s\",\"timeRemaining\":%d," +
            "\"isEmergency\":%b,\"brightness\":%.2f,\"displayName\":\"%s\",\"active\":%b," +
            "\"x\":%.2f,\"y\":%.2f,\"visible\":%b,\"stateColor\":\"%s\"}",
            light.getLightId(),
            roadId,
            lightStatus,
            light.getTimeRemaining(),
            light.isEmergencyMode(),
            light.getBrightness(),
            "TrafficLight-" + roadId,
            light.isAlive(),
            lightX,
            lightY,
            true,
            getTrafficLightColor(lightStatus)
        );
    }
    
    private String getTrafficLightColor(String state) {
        switch (state) {
            case "GREEN": return "#00FF00";
            case "YELLOW": return "#FFFF00";
            case "RED": return "#FF0000";
            case "BLINKING": return "#FFA500";
            default: return "#808080";
        }
    }
    
    private void onSimulationEvent(SimulationEngine.SimulationEvent event, SimulationEngine.SimulationState state) {
        // Broadcast significant events
        if (event == SimulationEngine.SimulationEvent.STARTED || 
            event == SimulationEngine.SimulationEvent.STOPPED ||
            event == SimulationEngine.SimulationEvent.RESET) {
            
            try {
                String eventJson = String.format(
                    "{\"type\":\"event\",\"event\":\"%s\",\"timestamp\":%d}",
                    event.name(),
                    System.currentTimeMillis()
                );
                System.out.println(eventJson);
                System.out.flush();
            } catch (Exception e) {
                System.err.println("Error sending event: " + e.getMessage());
            }
        }
    }
    
    private void cleanup() {
        System.err.println("🧹 Cleaning up Java Simulation Server...");
        
        running = false;
        
        if (engine != null) {
            engine.stop();
        }
        
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        System.err.println("✅ Java Simulation Server stopped");
    }
    
    public static void main(String[] args) {
        SimulationServer server = new SimulationServer();
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.err.println("🛑 Shutdown signal received");
            server.cleanup();
        }));
        
        server.start();
    }
} 