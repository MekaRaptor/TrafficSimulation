package src.core;

import java.util.List;
import java.util.Random;

public class Vehicle extends Thread {
    private final String id;
    private int position;
    private boolean active;
    private final List<Road> route;
    private final List<TrafficLight> lights;
    private final List<Intersection> intersections;
    private static final int MAX_RETRY_COUNT = 3;
    private static final Random random = new Random();
    private Road currentRoad;
    private double progress = 0.0;
    private long totalWaitTime = 0;
    private long waitStartTime = 0;
    private double speed;
    private VehicleType type;
    
    // Position fields for visualization
    private double x = 0.0, y = 0.0;
    
    // Route metrics for analysis
    private double averageRouteDensity = 0.0;
    private double estimatedJourneyTime = 0.0;
    
    // Journey tracking
    private double totalDistanceTraveled = 0.0;
    private String journeyStartTime;
    private String journeyEndTime;
    private java.util.List<String> roadsVisited = new java.util.ArrayList<>();
    private java.util.Map<String, Double> roadDistances = new java.util.HashMap<>();
    
    public enum VehicleType {
        CAR(1.0),
        TRUCK(0.7),
        MOTORCYCLE(1.3),
        BUS(0.8);
        
        private final double speedFactor;
        
        VehicleType(double speedFactor) {
            this.speedFactor = speedFactor;
        }
        
        public double getSpeedFactor() {
            return speedFactor;
        }

        public String getDisplayName() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase();
        }
        
        public static VehicleType getRandomType() {
            VehicleType[] types = VehicleType.values();
            return types[random.nextInt(types.length)];
        }
    }

    public Vehicle(String id, List<Road> route, List<TrafficLight> lights, List<Intersection> intersections) {
        this.id = id;
        this.route = route;
        this.lights = lights;
        this.intersections = intersections;
        this.position = 0;
        this.active = true;
        this.type = VehicleType.getRandomType();
        this.speed = type.getSpeedFactor() * (0.8 + random.nextDouble() * 0.4); // Random speed variation
        
        if (!route.isEmpty()) {
            this.currentRoad = route.get(0);
        }
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        journeyStartTime = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(startTime));
        
        String threadName = "Vehicle-" + id + "-" + type.getDisplayName();
        Thread.currentThread().setName(threadName);
        
        System.out.println("🚗 " + threadName + " STARTED JOURNEY at " + journeyStartTime);
        System.out.println("📍 Route: " + getRouteDescription());
        System.out.println("🎯 Estimated distance: " + calculateTotalRouteDistance() + " km");
        
        boolean routeCompleted = false;
        int roadsCompleted = 0;
        
        try {
            // Process all roads in route with minimal blocking
            for (int i = 0; i < route.size() && active; i++) {
                Road currentRoad = route.get(i);
                TrafficLight currentLight = lights.get(Math.min(i, lights.size() - 1));
                Intersection currentIntersection = intersections.get(Math.min(i, intersections.size() - 1));
                
                this.currentRoad = currentRoad;
                this.progress = 0.0;
                
                System.out.println("🛣️ " + threadName + " entering road " + (i+1) + "/" + route.size() + ": " + currentRoad.getId());
                
                // Calculate road distance
                double roadDistance = calculateRoadDistance(currentRoad);
                
                // Non-blocking approach with timeout
                boolean success = processRoadWithTimeout(currentRoad, currentLight, currentIntersection, 15000);
                
                if (!success) {
                    System.out.println("⚠️ " + threadName + " skipping congested road: " + currentRoad.getId());
                    continue; // Skip to next road instead of terminating
                }
                
                // Record successful road traversal
                roadsVisited.add(currentRoad.getId());
                roadDistances.put(currentRoad.getId(), roadDistance);
                totalDistanceTraveled += roadDistance;
                roadsCompleted++;
                
                System.out.println("✅ " + threadName + " completed road " + roadsCompleted + "/" + route.size() + 
                                 ": " + currentRoad.getId() + " (" + String.format("%.2f", roadDistance) + " km)");
                System.out.println("📊 " + threadName + " total distance so far: " + String.format("%.2f", totalDistanceTraveled) + " km");
            }
            
            routeCompleted = active && roadsCompleted > 0;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            active = false;
        } finally {
            // Clean up and report completion
            long endTime = System.currentTimeMillis();
            journeyEndTime = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(endTime));
            long journeyTime = endTime - startTime;
            
            active = false;
            currentRoad = null;
            progress = 1.0;
            
            // COMPREHENSIVE JOURNEY REPORT
            System.out.println("=" .repeat(80));
            if (routeCompleted) {
                System.out.println("🎉 ✅ " + threadName + " COMPLETED JOURNEY SUCCESSFULLY!");
                System.out.println("🏁 Status: ROUTE COMPLETED (" + roadsCompleted + "/" + route.size() + " roads)");
            } else {
                System.out.println("❌ " + threadName + " JOURNEY INCOMPLETE");
                System.out.println("🚫 Status: TERMINATED (" + roadsCompleted + "/" + route.size() + " roads completed)");
            }
            
            System.out.println("⏰ Journey time: " + journeyStartTime + " → " + journeyEndTime + 
                             " (" + String.format("%.1f", journeyTime / 1000.0) + " seconds)");
            System.out.println("🗺️ Total distance traveled: " + String.format("%.2f", totalDistanceTraveled) + " km");
            System.out.println("⏳ Total wait time: " + String.format("%.1f", totalWaitTime / 1000.0) + " seconds");
            System.out.println("🚗 Vehicle type: " + type.getDisplayName() + " (Speed factor: " + type.getSpeedFactor() + ")");
            System.out.println("🎯 Average speed: " + String.format("%.1f", speed) + " units/sec");
            
            if (!roadsVisited.isEmpty()) {
                System.out.println("📍 Roads visited:");
                for (int i = 0; i < roadsVisited.size(); i++) {
                    String roadId = roadsVisited.get(i);
                    double distance = roadDistances.getOrDefault(roadId, 0.0);
                    System.out.println("   " + (i+1) + ". " + roadId + " (" + String.format("%.2f", distance) + " km)");
                }
            }
            
            System.out.println("🏁 Thread " + threadName + " is now TERMINATED");
            System.out.println("=" .repeat(80));
        }
    }
    
    private String getRouteDescription() {
        if (route.isEmpty()) return "No route";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < route.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(route.get(i).getId());
        }
        return sb.toString();
    }

    private boolean processRoadWithTimeout(Road road, TrafficLight light, Intersection intersection, long timeoutMs) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        String threadName = Thread.currentThread().getName();
        
        System.out.println("🔍 " + threadName + " processing road: " + road.getId() + " with light: " + light.getLightId());
        
        // Quick traffic light check with minimal blocking
        while (active && System.currentTimeMillis() - startTime < timeoutMs) {
            TrafficLight.State lightState = light.getTrafficLightState();
            
            System.out.println("🚦 " + threadName + " sees light state: " + lightState + " on road: " + road.getId());
            
            // If GREEN, try to proceed immediately
            if (lightState == TrafficLight.State.GREEN) {
                System.out.println("✅ " + threadName + " GREEN light - attempting to move through road: " + road.getId());
                boolean result = tryToMoveThrough(road, light, intersection);
                System.out.println("🎯 " + threadName + " move result: " + result + " for road: " + road.getId());
                return result;
            }
            
            // If RED/YELLOW, wait briefly but don't block entire system
            System.out.println("⏳ " + threadName + " waiting for GREEN light on road: " + road.getId() + " (current: " + lightState + ")");
            Thread.sleep(50); // Very short wait to allow parallel processing
        }
        
        // Timeout reached
        System.out.println("⏰ " + threadName + " TIMEOUT reached for road: " + road.getId() + " after " + (System.currentTimeMillis() - startTime) + "ms");
        return false;
    }

    private boolean tryToMoveThrough(Road road, TrafficLight light, Intersection intersection) throws InterruptedException {
        // Check traffic light state
        TrafficLight.State lightState = light.getTrafficLightState();
        
        // RED or YELLOW - must stop and wait
        if (lightState == TrafficLight.State.RED || lightState == TrafficLight.State.YELLOW) {
            waitStartTime = System.currentTimeMillis();
            
            // Wait for GREEN light
            while (active && (light.getTrafficLightState() == TrafficLight.State.RED || 
                             light.getTrafficLightState() == TrafficLight.State.YELLOW)) {
                Thread.sleep(100);
                
                // Timeout check to prevent infinite waiting
                if (System.currentTimeMillis() - waitStartTime > 30000) { // 30 second timeout
                    return false;
                }
            }
            
            totalWaitTime += System.currentTimeMillis() - waitStartTime;
        }
        
        // Now light should be GREEN - proceed
        if (light.getTrafficLightState() != TrafficLight.State.GREEN) {
            return false; // Something went wrong
        }
        
        // Try to enter the road
        if (!road.addVehicle(this)) {
            return false;
        }
        
        // Try to enter the intersection
        waitStartTime = System.currentTimeMillis();
        if (!intersection.enter(id)) {
            road.removeVehicle(this);
            totalWaitTime += System.currentTimeMillis() - waitStartTime;
            return false;
        }
        totalWaitTime += System.currentTimeMillis() - waitStartTime;
        
        // Move through the road
        move(road);
        
        // Exit the intersection
        intersection.exit(id);
        road.removeVehicle(this);
        
        return true;
    }

    private void move(Road road) {
        position++;
        
        // Calculate movement steps based on vehicle type and speed
        int steps = 30; // More steps for smoother animation
        double stepDelay = 100 / speed; // Adjusted for better timing
        
        // Simulate traffic conditions affecting speed
        double congestion = (double) road.getVehicleCount() / road.getCapacity();
        double congestionFactor = 1.0 - (congestion * 0.5); // Reduce congestion impact
        stepDelay /= congestionFactor;
        
        // Smooth animation with variable speed
        for (int i = 0; i < steps; i++) {
            try {
                // Update progress continuously for smooth movement
                progress = (double) i / steps;
                
                // Update actual x,y coordinates in real-time
                if (currentRoad != null) {
                    double newX = currentRoad.getX1() + (currentRoad.getX2() - currentRoad.getX1()) * progress;
                    double newY = currentRoad.getY1() + (currentRoad.getY2() - currentRoad.getY1()) * progress;
                    this.x = newX;
                    this.y = newY;
                }
                
                // Add realistic speed variation
                double speedVariation = 0.8 + random.nextDouble() * 0.4;
                Thread.sleep((long)(stepDelay * speedVariation));
                
                // Break if vehicle should stop (e.g., interrupted)
                if (!active || Thread.currentThread().isInterrupted()) {
                    break;
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        progress = 1.0; // Ensure we end at 100%
        System.out.println("Vehicle " + id + " moved to position " + position + " on road " + road.getId()
                + " [" + road.getDirection() + "] Speed: " + String.format("%.1f", speed));
    }

    public String getVehicleId() {
        return id;
    }

    public boolean isActive() {
        return active;
    }

    public Road getCurrentRoad() {
        return currentRoad;
    }

    public double getProgress() {
        return progress;
    }

    public long getTotalWaitTime() {
        return totalWaitTime;
    }
    
    public VehicleType getVehicleType() {
        return type;
    }
    
    public double getSpeed() {
        return speed;
    }

    public void stopVehicle() {
        active = false;
    }
    
    // Set vehicle position (used for initialization)
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public double getX() {
        if (currentRoad != null) {
            // Calculate position along road based on progress
            double startX = currentRoad.getX1();
            double endX = currentRoad.getX2();
            return startX + (endX - startX) * progress;
        }
        return x;
    }

    public double getY() {
        if (currentRoad != null) {
            // Calculate position along road based on progress
            double startY = currentRoad.getY1();
            double endY = currentRoad.getY2();
            return startY + (endY - startY) * progress;
        }
        return y;
    }

    public boolean isOnRoad() {
        return currentRoad != null && active;
    }

    public String getCurrentRoadId() {
        return currentRoad != null ? currentRoad.getId() : null;
    }

    public CityMap.Zone getStartZone() {
        return null; // Placeholder - would need zone information
    }

    public CityMap.Zone getEndZone() {
        return null; // Placeholder - would need zone information
    }
    
    // Route metrics methods
    public void setRouteMetrics(double averageDensity, double estimatedTime) {
        this.averageRouteDensity = averageDensity;
        this.estimatedJourneyTime = estimatedTime;
    }
    
    public double getAverageRouteDensity() {
        return averageRouteDensity;
    }
    
    public double getEstimatedJourneyTime() {
        return estimatedJourneyTime;
    }
    
    public String getDisplayName() {
        return id + " (" + type.getDisplayName() + ")";
    }
    
    // Distance calculation methods
    private double calculateRoadDistance(Road road) {
        double deltaX = road.getX2() - road.getX1();
        double deltaY = road.getY2() - road.getY1();
        double pixelDistance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        // Convert pixels to km (assuming 1 pixel = 0.01 km for realism)
        return pixelDistance * 0.01;
    }
    
    private double calculateTotalRouteDistance() {
        double totalDistance = 0.0;
        for (Road road : route) {
            totalDistance += calculateRoadDistance(road);
        }
        return totalDistance;
    }
    
    // Journey tracking getters
    public double getTotalDistanceTraveled() {
        return totalDistanceTraveled;
    }
    
    public String getJourneyStartTime() {
        return journeyStartTime;
    }
    
    public String getJourneyEndTime() {
        return journeyEndTime;
    }
    
    public java.util.List<String> getRoadsVisited() {
        return new java.util.ArrayList<>(roadsVisited);
    }
    
    public java.util.Map<String, Double> getRoadDistances() {
        return new java.util.HashMap<>(roadDistances);
    }
}
