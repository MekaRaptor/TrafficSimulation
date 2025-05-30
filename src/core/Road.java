package src.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Road {
    private final String id;
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final int capacity;
    private final CityMap.Direction direction;
    
    // Yeni gerçekçi sistem için eklenenler
    private CityMap.RoadType roadType = CityMap.RoadType.LOCAL_STREET; // Varsayılan
    private double x1, y1, x2, y2; // Yol koordinatları
    private int speedLimit = 50; // km/h
    private double congestionLevel = 0.0; // 0.0 - 1.0
    private long lastTrafficUpdate = System.currentTimeMillis();

    public Road(String id, int capacity, CityMap.Direction direction) {
        this.id = id;
        this.capacity = capacity;
        this.direction = direction;
    }

    public synchronized boolean addVehicle(Vehicle v) {
        if (vehicles.size() < capacity) {
            vehicles.add(v);
            updateCongestion();
            System.out.println("Vehicle " + v.getVehicleId() + " entered road " + id + 
                             " (" + roadType.getDisplayName() + ")");
            return true;
        } else {
            System.out.println("Road " + id + " is full. (" + roadType.getDisplayName() + 
                             " - Capacity: " + capacity + ")");
            return false;
        }
    }

    public synchronized void removeVehicle(Vehicle v) {
        vehicles.remove(v);
        updateCongestion();
        System.out.println("Vehicle " + v.getVehicleId() + " left road " + id);
    }
    
    private void updateCongestion() {
        // Trafik sıkışıklığını gerçek zamanlı güncelle
        double rawCongestion = (double) vehicles.size() / capacity;
        
        // Yol türüne göre sıkışıklık faktörü
        double typeMultiplier = 1.0;
        switch (roadType) {
            case HIGHWAY:
                typeMultiplier = 0.7; // Otoyollar daha az sıkışır
                break;
            case MAIN_ROAD:
                typeMultiplier = 0.8;
                break;
            case SECONDARY_ROAD:
                typeMultiplier = 1.0;
                break;
            case LOCAL_STREET:
                typeMultiplier = 1.2; // Yerel yollar daha çok sıkışır
                break;
            case RESIDENTIAL_STREET:
                typeMultiplier = 1.5;
                break;
        }
        
        this.congestionLevel = Math.min(1.0, rawCongestion * typeMultiplier);
        this.lastTrafficUpdate = System.currentTimeMillis();
    }
    
    // Trafik durumuna göre etkin hızı hesapla
    public int getEffectiveSpeedLimit() {
        double congestionFactor = 1.0 - (congestionLevel * 0.6); // %60'a kadar yavaşlama
        return (int) (speedLimit * congestionFactor);
    }
    
    // Yolun ne kadar "çekici" olduğunu hesapla (pathfinding için)
    public double getRouteAttractiveness() {
        double baseCost = getLength(); // Mesafe
        double congestionPenalty = congestionLevel * 100; // Sıkışıklık cezası
        double speedBonus = speedLimit / 10.0; // Hız bonusu
        
        return baseCost + congestionPenalty - speedBonus;
    }
    
    public double getLength() {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
    
    public boolean isHighPriorityRoad() {
        return roadType == CityMap.RoadType.HIGHWAY || roadType == CityMap.RoadType.MAIN_ROAD;
    }

    // Getter metodları
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
    
    // Yeni getter/setter metodları
    public CityMap.RoadType getRoadType() {
        return roadType;
    }
    
    public void setRoadType(CityMap.RoadType roadType) {
        this.roadType = roadType;
        this.speedLimit = roadType.getSpeedLimit();
    }
    
    public void setCoordinates(double x1, double y1, double x2, double y2) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }
    
    public double getX1() { return x1; }
    public double getY1() { return y1; }
    public double getX2() { return x2; }
    public double getY2() { return y2; }
    
    public int getSpeedLimit() {
        return speedLimit;
    }
    
    public void setSpeedLimit(int speedLimit) {
        this.speedLimit = speedLimit;
    }
    
    public double getCongestionLevel() {
        return congestionLevel;
    }
    
    public synchronized List<Vehicle> getVehicles() {
        return new ArrayList<>(vehicles); // Güvenli kopya
    }
    
    @Override
    public String toString() {
        return String.format("Road[%s, %s, %d/%d vehicles, %.1f%% congestion]", 
                           id, roadType.getDisplayName(), vehicles.size(), capacity, congestionLevel * 100);
    }
}
