import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.List;
import java.util.Arrays;

public class IntersectionManager implements Runnable {
    private final String intersectionId;
    private final Semaphore mutex; // Critical section control
    private final List<Road> roads;
    private final boolean isK1; // true for K1, false for K2
    
    // K1-K2 Communication: Queues to track vehicle types on each road
    private final ConcurrentLinkedQueue<VehicleType> road1Queue;
    private final ConcurrentLinkedQueue<VehicleType> road2Queue;
    private final ConcurrentLinkedQueue<VehicleType> road3Queue;
    
    private volatile boolean running;
    
    public IntersectionManager(String intersectionId, List<Road> roads, boolean isK1) {
        this.intersectionId = intersectionId;
        this.roads = roads;
        this.isK1 = isK1;
        this.mutex = new Semaphore(1); // Only one vehicle can enter intersection at a time
        this.road1Queue = new ConcurrentLinkedQueue<>();
        this.road2Queue = new ConcurrentLinkedQueue<>();
        this.road3Queue = new ConcurrentLinkedQueue<>();
        this.running = true;
        
        System.out.println("🚦 " + intersectionId + " IntersectionManager initialized");
    }
    
    @Override
    public void run() {
        System.out.println("🚦 " + intersectionId + " manager thread started");
        
        while (running) {
            try {
                // Monitor traffic and print statistics every 2 seconds
                Thread.sleep(2000);
                printTrafficStatistics();
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("🚦 " + intersectionId + " manager thread stopped");
    }
    
    /**
     * Critical section: Vehicle enters intersection
     * For K1: Assigns vehicle to least congested road
     * For K2: Just passes vehicle through to B
     */
    public Road enterIntersection(Vehicle vehicle) throws InterruptedException {
        // Acquire critical section
        mutex.acquire();
        
        try {
            System.out.println("🚦 Vehicle " + vehicle.getId() + " entered " + intersectionId + " critical section");
            
            if (isK1) {
                // K1: Assign to least congested road
                Road assignedRoad = assignToLeastCongestedRoad(vehicle);
                
                // Add vehicle type to appropriate queue for K1-K2 communication
                addVehicleToQueue(assignedRoad.getRoadId(), vehicle.getType());
                
                System.out.println("📊 " + intersectionId + " assigned Vehicle " + vehicle.getId() + 
                                 " (" + vehicle.getType().getDisplayName() + ") to " + assignedRoad.getName());
                return assignedRoad;
                
            } else {
                // K2: Remove vehicle from queue (it's leaving the road)
                removeVehicleFromQueue(vehicle);
                
                System.out.println("🚦 " + intersectionId + " processed Vehicle " + vehicle.getId() + 
                                 " from " + (vehicle.getCurrentRoad() != null ? vehicle.getCurrentRoad().getName() : "unknown road"));
                return null; // K2 doesn't assign roads, vehicle goes to B
            }
            
        } finally {
            // Release critical section
            mutex.release();
        }
    }
    
    /**
     * K1: Find the road with least congestion (fewest vehicles)
     */
    private Road assignToLeastCongestedRoad(Vehicle vehicle) {
        Road leastCongestedRoad = roads.get(0);
        int minVehicles = leastCongestedRoad.getVehicleCount();
        
        for (Road road : roads) {
            int vehicleCount = road.getVehicleCount();
            if (vehicleCount < minVehicles) {
                minVehicles = vehicleCount;
                leastCongestedRoad = road;
            }
        }
        
        System.out.println("📊 Traffic analysis: " + 
                          "Road1=" + roads.get(0).getVehicleCount() + ", " +
                          "Road2=" + roads.get(1).getVehicleCount() + ", " + 
                          "Road3=" + roads.get(2).getVehicleCount() + 
                          " → Assigned to " + leastCongestedRoad.getName());
        
        return leastCongestedRoad;
    }
    
    /**
     * Add vehicle type to appropriate queue for K1-K2 communication
     */
    private void addVehicleToQueue(int roadId, VehicleType vehicleType) {
        switch (roadId) {
            case 1:
                road1Queue.offer(vehicleType);
                break;
            case 2:
                road2Queue.offer(vehicleType);
                break;
            case 3:
                road3Queue.offer(vehicleType);
                break;
        }
    }
    
    /**
     * Remove vehicle from queue when it reaches K2
     */
    private void removeVehicleFromQueue(Vehicle vehicle) {
        if (vehicle.getCurrentRoad() != null) {
            int roadId = vehicle.getCurrentRoad().getRoadId();
            switch (roadId) {
                case 1:
                    road1Queue.poll();
                    break;
                case 2:
                    road2Queue.poll();
                    break;
                case 3:
                    road3Queue.poll();
                    break;
            }
        }
    }
    
    /**
     * Print traffic statistics
     */
    private void printTrafficStatistics() {
        if (isK1) {
            System.out.println("📊 " + intersectionId + " Traffic Status:");
            for (Road road : roads) {
                System.out.println("   " + road.getName() + ": " + road.getVehicleCount() + " vehicles");
            }
        }
        
        System.out.println("📊 " + intersectionId + " Queue Status: " +
                          "Road1=" + road1Queue.size() + ", " +
                          "Road2=" + road2Queue.size() + ", " + 
                          "Road3=" + road3Queue.size());
    }
    
    public void stop() {
        running = false;
    }
    
    public String getIntersectionId() {
        return intersectionId;
    }
    
    public boolean isK1() {
        return isK1;
    }
    
    // Getters for queues (for statistics)
    public int getRoad1QueueSize() { return road1Queue.size(); }
    public int getRoad2QueueSize() { return road2Queue.size(); }
    public int getRoad3QueueSize() { return road3Queue.size(); }
} 