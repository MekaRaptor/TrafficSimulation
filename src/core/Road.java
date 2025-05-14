import java.util.ArrayList;
import java.util.List;

public class Road {
    private final String id;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final int capacity;
    private final CityMap.Direction direction;

    public Road(String id, int capacity, CityMap.Direction direction) {
        this.id = id;
        this.capacity = capacity;
        this.direction = direction;
    }

    public synchronized boolean addVehicle(Vehicle v) {
        if (vehicles.size() < capacity) {
            vehicles.add(v);
            System.out.println("Vehicle " + v.getVehicleId() + " entered road " + id);
            return true;
        } else {
            System.out.println("Road " + id + " is full.");
            return false;
        }
    }

    public synchronized void removeVehicle(Vehicle v) {
        vehicles.remove(v);
        System.out.println("Vehicle " + v.getVehicleId() + " left road " + id);
    }

    public String getId() {
        return id;
    }

    public synchronized int getVehicleCount() {
        return vehicles.size();
    }

    public int getCapacity() {
        return capacity;
    }

    public CityMap.Direction getDirection() {
        return direction;
    }
}
