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
        for (int i = 0; i < route.size() && active; i++) {
            Road currentRoad = route.get(i);
            TrafficLight currentLight = lights.get(i);
            Intersection currentIntersection = intersections.get(i);
            
            this.currentRoad = currentRoad;
            this.progress = 0.0;
            
            int retryCount = 0;
            boolean success = false;
            
            while (!success && retryCount < MAX_RETRY_COUNT && active) {
                try {
                    waitStartTime = System.currentTimeMillis();
                    success = tryToMoveThrough(currentRoad, currentLight, currentIntersection);
                    if (!success) {
                        retryCount++;
                        // Add random wait time to prevent deadlocks
                        Thread.sleep(random.nextInt(1000) + 500);
                        
                        if (retryCount == MAX_RETRY_COUNT) {
                            // Try alternative route or turn back
                            System.out.println("Vehicle " + id + " failed to proceed after " + MAX_RETRY_COUNT + " attempts");
                            handleFailure(i);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        System.out.println("Vehicle " + id + " completed its route");
    }

    private boolean tryToMoveThrough(Road road, TrafficLight light, Intersection intersection) throws InterruptedException {
        // Wait for green light
        waitStartTime = System.currentTimeMillis();
        while (active && light.getTrafficLightState() != TrafficLight.State.GREEN) {
            Thread.sleep(100);
        }
        totalWaitTime += System.currentTimeMillis() - waitStartTime;
        
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

    private void handleFailure(int currentRouteIndex) {
        // Simple fallback strategy: return to previous road
        if (currentRouteIndex > 0) {
            System.out.println("Vehicle " + id + " is returning to previous road");
            position--;
            if (currentRouteIndex - 1 < route.size()) {
                currentRoad = route.get(currentRouteIndex - 1);
            }
        } else {
            // Cancel route if at starting point
            System.out.println("Vehicle " + id + " cancelling route");
            active = false;
        }
    }

    private void move(Road road) {
        position++;
        
        // Calculate movement steps based on vehicle type and speed
        int steps = 20;
        double stepDelay = 50 / speed;
        
        // Simulate traffic conditions affecting speed
        double congestion = (double) road.getVehicleCount() / road.getCapacity();
        double congestionFactor = 1.0 - (congestion * 0.7); // Slow down in congestion
        stepDelay /= congestionFactor;
        
        // Smooth animation with variable speed
        for (int i = 0; i < steps; i++) {
            try {
                progress = i / (double)steps;
                
                // Add slight randomness to movement for realism
                if (random.nextDouble() < 0.1) {
                    Thread.sleep((long)(stepDelay * (0.8 + random.nextDouble() * 0.4)));
                } else {
                    Thread.sleep((long)stepDelay);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        progress = 1.0;
        System.out.println("Vehicle " + id + " moved to position " + position + " on road " + road.getId()
                + " [" + road.getDirection() + "]");
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
}
