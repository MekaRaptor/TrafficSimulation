package src.core;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Intersection {
    private final String id;
    private final Semaphore access;
    private final ConcurrentHashMap<String, Long> waitingVehicles;
    private static final long TIMEOUT = 5000; // 5 saniye timeout
    private static final long WAIT_TIME = 100; // 100ms bekleme süresi
    
    // Position fields for visualization
    private double x = 0.0, y = 0.0;

    public Intersection(String id) {
        this.id = id;
        this.access = new Semaphore(1);  // aynı anda sadece 1 araç girebilir
        this.waitingVehicles = new ConcurrentHashMap<>();
    }

    public boolean enter(String vehicleId) {
        try {
            System.out.println("Vehicle " + vehicleId + " is waiting to enter intersection " + id);
            
            // Aracın bekleme süresini kaydet
            waitingVehicles.put(vehicleId, System.currentTimeMillis());
            
            // Deadlock önleme: Timeout ile birlikte izin almaya çalış
            boolean acquired = false;
            while (!acquired) {
                acquired = access.tryAcquire(WAIT_TIME, TimeUnit.MILLISECONDS);
                if (!acquired) {
                    // Timeout kontrolü
                    long waitTime = System.currentTimeMillis() - waitingVehicles.get(vehicleId);
                    if (waitTime > TIMEOUT) {
                        // Timeout oldu, aracı geri çek
                        waitingVehicles.remove(vehicleId);
                        System.out.println("Vehicle " + vehicleId + " timeout at intersection " + id + ", backing off");
                        return false;
                    }
                    
                    // En uzun bekleyen araç bu mu kontrol et
                    String oldestVehicle = findOldestWaitingVehicle();
                    if (oldestVehicle != null && oldestVehicle.equals(vehicleId)) {
                        // En uzun bekleyen araç ise öncelik ver
                        acquired = access.tryAcquire(WAIT_TIME, TimeUnit.MILLISECONDS);
                    }
                }
            }
            
            // Başarılı giriş
            waitingVehicles.remove(vehicleId);
            System.out.println("Vehicle " + vehicleId + " entered intersection " + id);
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            waitingVehicles.remove(vehicleId);
            return false;
        }
    }

    private String findOldestWaitingVehicle() {
        long oldestTime = Long.MAX_VALUE;
        String oldestVehicle = null;
        
        for (var entry : waitingVehicles.entrySet()) {
            if (entry.getValue() < oldestTime) {
                oldestTime = entry.getValue();
                oldestVehicle = entry.getKey();
            }
        }
        
        return oldestVehicle;
    }

    public void exit(String vehicleId) {
        System.out.println("Vehicle " + vehicleId + " exited intersection " + id);
        access.release();  // çıkışta izin serbest bırakılır
    }

    public String getId() {
        return id;
    }

    public boolean isAvailable() {
        return access.availablePermits() > 0;
    }
    
    // Position methods for visualization
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    // Additional methods for GraphQL
    public IntersectionType getType() {
        return IntersectionType.TRAFFIC_LIGHT;
    }
    
    public java.util.List<String> getConnectedRoads() {
        return new java.util.ArrayList<>(); // Placeholder
    }
    
    public int getWaitingVehicles() {
        return waitingVehicles.size();
    }
    
    public enum IntersectionType {
        TRAFFIC_LIGHT, STOP_SIGN, ROUNDABOUT, UNCONTROLLED;
        
        public String getDisplayName() {
            return this.name().replace("_", " ");
        }
    }
}
