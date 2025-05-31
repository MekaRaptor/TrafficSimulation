import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class Vehicle implements Runnable {
    private final String id;
    private final VehicleType type;
    private final AtomicBoolean active;
    private final TrafficController controller;
    private Road currentRoad;
    private volatile boolean reachedB;
    private long startTime;
    private long totalTravelTime;
    
    public Vehicle(String id, TrafficController controller) {
        this.id = id;
        this.type = VehicleType.getRandomType();
        this.active = new AtomicBoolean(true);
        this.controller = controller;
        this.reachedB = false;
        this.startTime = System.currentTimeMillis();
    }
    
    @Override
    public void run() {
        try {
            System.out.println("🚗 Vehicle " + id + " (" + type.getDisplayName() + ") started journey from A");
            
            // Step 1: Travel from A to K1 (no road, just time delay)
            Thread.sleep(1000); // 1 second from A to K1
            
            // Step 2: Enter K1 intersection (critical section)
            Road assignedRoad = controller.getK1Manager().enterIntersection(this);
            if (assignedRoad == null || !active.get()) {
                return; // Vehicle stopped or couldn't get a road
            }
            
            currentRoad = assignedRoad;
            currentRoad.addVehicle(this);
            
            // Step 3: Travel on the assigned road (K1 to K2)
            System.out.println("🛣️ Vehicle " + id + " traveling on " + currentRoad.getName() + " for " + type.getRoadTravelTime() + "ms");
            Thread.sleep(type.getRoadTravelTime());
            
            // Step 4: Exit current road and enter K2 intersection
            currentRoad.removeVehicle(this);
            controller.getK2Manager().enterIntersection(this);
            
            // Step 5: Travel from K2 to B
            Thread.sleep(1000); // 1 second from K2 to B
            
            reachedB = true;
            totalTravelTime = System.currentTimeMillis() - startTime;
            System.out.println("🏁 Vehicle " + id + " reached B! Total time: " + totalTravelTime + "ms");
            
            // Notify controller that vehicle reached B
            controller.vehicleReachedB(this);
            
        } catch (InterruptedException e) {
            System.out.println("🛑 Vehicle " + id + " was interrupted");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ Error in vehicle " + id + ": " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cleanup
            if (currentRoad != null) {
                currentRoad.removeVehicle(this);
            }
            active.set(false);
        }
    }
    
    public void stop() {
        active.set(false);
    }
    
    public String getId() {
        return id;
    }
    
    public VehicleType getType() {
        return type;
    }
    
    public boolean isActive() {
        return active.get();
    }
    
    public boolean hasReachedB() {
        return reachedB;
    }
    
    public Road getCurrentRoad() {
        return currentRoad;
    }
    
    public long getTotalTravelTime() {
        return totalTravelTime;
    }
}

