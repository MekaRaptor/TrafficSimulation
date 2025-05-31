import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Road {
    private final int roadId; // 1, 2, or 3 for the three parallel roads
    private final String name;
    private final ConcurrentLinkedQueue<Vehicle> vehiclesOnRoad;
    private final AtomicInteger vehicleCount;
    
    public Road(int roadId, String name) {
        this.roadId = roadId;
        this.name = name;
        this.vehiclesOnRoad = new ConcurrentLinkedQueue<>();
        this.vehicleCount = new AtomicInteger(0);
    }
    
    public void addVehicle(Vehicle vehicle) {
        vehiclesOnRoad.offer(vehicle);
        vehicleCount.incrementAndGet();
        System.out.println("Vehicle " + vehicle.getId() + " entered " + name + " (Road " + roadId + "). Current count: " + vehicleCount.get());
    }
    
    public void removeVehicle(Vehicle vehicle) {
        vehiclesOnRoad.remove(vehicle);
        vehicleCount.decrementAndGet();
        System.out.println("Vehicle " + vehicle.getId() + " left " + name + " (Road " + roadId + "). Current count: " + vehicleCount.get());
    }
    
    public int getVehicleCount() {
        return vehicleCount.get();
    }
    
    public int getRoadId() {
        return roadId;
    }
    
    public String getName() {
        return name;
    }
    
    public ConcurrentLinkedQueue<Vehicle> getVehiclesOnRoad() {
        return vehiclesOnRoad;
    }
}
