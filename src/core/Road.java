package src.core;

import java.util.ArrayList;
import java.util.List;

public class Road {
    private final String id;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final int capacity;
    private final CityMap.Direction direction;
    
    // Coordinate fields for visualization
    private double x1 = 0.0, y1 = 0.0, x2 = 100.0, y2 = 100.0;
    
    // Traffic density field for analysis (0.0 to 1.0)
    private double trafficDensity = 0.0;

    public Road(String id, int capacity, CityMap.Direction direction) {
        this.id = id;
        this.capacity = capacity;
        this.direction = direction;
    }
    
    // Set road coordinates for visualization
    public void setCoordinates(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
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

    public double getCongestionLevel() {
        return (double) getVehicleCount() / capacity;
    }

    // Position methods - now return actual coordinates
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }

    // Road type information
    public RoadType getRoadType() { 
        if (id.contains("MAIN")) {
            return RoadType.MAIN_ROAD;
        } else if (id.contains("RES")) {
            return RoadType.RESIDENTIAL_STREET;
        } else if (id.contains("COM")) {
            return RoadType.SECONDARY_ROAD;
        } else if (id.contains("IND")) {
            return RoadType.SECONDARY_ROAD;
        }
        return RoadType.LOCAL_STREET; 
    }

    public double getSpeedLimit() { 
        switch (getRoadType()) {
            case MAIN_ROAD: return 60.0;      // km/h
            case SECONDARY_ROAD: return 40.0;  // km/h  
            case RESIDENTIAL_STREET: return 30.0; // km/h
            default: return 50.0;              // km/h
        }
    }

    public boolean hasTrafficLights() { 
        return true; 
    }

    // Traffic density methods
    public double getTrafficDensity() {
        return trafficDensity;
    }
    
    public void setTrafficDensity(double density) {
        this.trafficDensity = Math.max(0.0, Math.min(1.0, density)); // Clamp between 0-1
    }
    
    // Dynamic traffic density based on current vehicle count
    public double getCurrentTrafficDensity() {
        double currentDensity = (double) getVehicleCount() / capacity;
        double baseDensity = trafficDensity;
        return Math.min(1.0, baseDensity + (currentDensity * 0.3)); // Combine base + current
    }

    public enum RoadType {
        HIGHWAY, MAIN_ROAD, SECONDARY_ROAD, LOCAL_STREET, RESIDENTIAL_STREET;
        
        public String getDisplayName() {
            return this.name().replace("_", " ");
        }
    }
}
