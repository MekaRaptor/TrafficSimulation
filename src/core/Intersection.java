package src.core;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Intersection {
    private final String id;
    private Semaphore access;
    private final ConcurrentHashMap<String, Long> waitingVehicles;
    private static final long TIMEOUT = 5000; // 5 saniye timeout
    private static final long WAIT_TIME = 100; // 100ms bekleme süresi
    
    // Yeni gerçekçi sistem için eklenenler
    private CityMap.IntersectionType type = CityMap.IntersectionType.UNCONTROLLED;
    private double x, y; // Kavşak koordinatları
    private int priority = 1; // Ana yol kavşakları daha yüksek öncelik
    private boolean isRoundabout = false;
    private long roundaboutWaitTime = 300; // Dönel kavşak için daha kısa bekleme

    public Intersection(String id) {
        this.id = id;
        this.access = new Semaphore(1);  // aynı anda sadece 1 araç girebilir
        this.waitingVehicles = new ConcurrentHashMap<>();
    }

    public boolean enter(String vehicleId) {
        try {
            System.out.println("Vehicle " + vehicleId + " is waiting to enter intersection " + id + 
                             " (" + type.getDisplayName() + ")");
            
            // Aracın bekleme süresini kaydet
            waitingVehicles.put(vehicleId, System.currentTimeMillis());
            
            // Kavşak türüne göre farklı davranış
            boolean acquired = false;
            switch (type) {
                case TRAFFIC_LIGHT:
                    acquired = handleTrafficLightIntersection(vehicleId);
                    break;
                case ROUNDABOUT:
                    acquired = handleRoundabout(vehicleId);
                    break;
                case STOP_SIGN:
                    acquired = handleStopSign(vehicleId);
                    break;
                case UNCONTROLLED:
                default:
                    acquired = handleUncontrolledIntersection(vehicleId);
                    break;
            }
            
            if (acquired) {
                waitingVehicles.remove(vehicleId);
                System.out.println("Vehicle " + vehicleId + " entered intersection " + id);
                return true;
            } else {
                waitingVehicles.remove(vehicleId);
                return false;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            waitingVehicles.remove(vehicleId);
            return false;
        }
    }
    
    private boolean handleTrafficLightIntersection(String vehicleId) throws InterruptedException {
        // Işıklı kavşak - trafik ışığı kontrolü başka yerde yapılıyor
        return handleStandardEntry(vehicleId, WAIT_TIME);
    }
    
    private boolean handleRoundabout(String vehicleId) throws InterruptedException {
        // Dönel kavşak - daha hızlı geçiş
        boolean acquired = false;
        int attempts = 0;
        
        while (!acquired && attempts < 20) { // Dönel kavşaklarda daha az deneme
            acquired = access.tryAcquire(roundaboutWaitTime, TimeUnit.MILLISECONDS);
            if (!acquired) {
                attempts++;
                // Dönel kavşakta kısa bekleme
                Thread.sleep(50);
            }
        }
        
        return acquired;
    }
    
    private boolean handleStopSign(String vehicleId) throws InterruptedException {
        // Stop işareti - FIFO (ilk gelen ilk geçer)
        Thread.sleep(500); // Stop işaretinde zorunlu durma
        
        String oldestVehicle = findOldestWaitingVehicle();
        if (oldestVehicle != null && oldestVehicle.equals(vehicleId)) {
            // En uzun bekleyen araç önceliği alır
            return access.tryAcquire(2000, TimeUnit.MILLISECONDS);
        } else {
            // Biraz daha bekle
            return access.tryAcquire(1000, TimeUnit.MILLISECONDS);
        }
    }
    
    private boolean handleUncontrolledIntersection(String vehicleId) throws InterruptedException {
        // Kontrolsüz kavşak - hızlıca geç veya bekle
        return access.tryAcquire(500, TimeUnit.MILLISECONDS);
    }
    
    private boolean handleStandardEntry(String vehicleId, long waitTime) throws InterruptedException {
        // Orijinal algoritma
        boolean acquired = false;
        while (!acquired) {
            acquired = access.tryAcquire(waitTime, TimeUnit.MILLISECONDS);
            if (!acquired) {
                // Timeout kontrolü
                long totalWaitTime = System.currentTimeMillis() - waitingVehicles.get(vehicleId);
                if (totalWaitTime > TIMEOUT) {
                    System.out.println("Vehicle " + vehicleId + " timeout at intersection " + id + ", backing off");
                    return false;
                }
                
                // Öncelik sistemi
                String oldestVehicle = findOldestWaitingVehicle();
                if (oldestVehicle != null && oldestVehicle.equals(vehicleId)) {
                    acquired = access.tryAcquire(waitTime, TimeUnit.MILLISECONDS);
                }
            }
        }
        return true;
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
    
    // Kavşak maliyetini hesapla (pathfinding için)
    public double getTraversalCost() {
        double baseCost = 10.0; // Temel kavşak maliyeti
        
        switch (type) {
            case TRAFFIC_LIGHT:
                baseCost += 20.0; // Işıklı kavşaklar daha uzun sürer
                break;
            case ROUNDABOUT:
                baseCost += 5.0; // Dönel kavşaklar hızlı
                break;
            case STOP_SIGN:
                baseCost += 15.0; // Stop işareti orta düzeyde gecikme
                break;
            case UNCONTROLLED:
                baseCost += 8.0; // Kontrolsüz kavşaklar hızlı ama riskli
                break;
        }
        
        // Bekleyen araç sayısına göre ek maliyet
        baseCost += waitingVehicles.size() * 5.0;
        
        return baseCost;
    }

    public String getId() {
        return id;
    }

    public boolean isAvailable() {
        return access.availablePermits() > 0;
    }
    
    // Yeni getter/setter metodları
    public CityMap.IntersectionType getType() {
        return type;
    }
    
    public void setType(CityMap.IntersectionType type) {
        this.type = type;
        
        // Türe göre özel ayarlar
        switch (type) {
            case ROUNDABOUT:
                this.isRoundabout = true;
                this.access = new Semaphore(2); // Dönel kavşakta 2 araç
                break;
            case TRAFFIC_LIGHT:
                this.priority = 3; // En yüksek öncelik
                break;
            case STOP_SIGN:
                this.priority = 2;
                break;
            case UNCONTROLLED:
                this.priority = 1;
                break;
        }
    }
    
    public void setCoordinates(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public double getX() { return x; }
    public double getY() { return y; }
    
    public int getPriority() {
        return priority;
    }
    
    public boolean isRoundabout() {
        return isRoundabout;
    }
    
    public int getWaitingVehicleCount() {
        return waitingVehicles.size();
    }
    
    @Override
    public String toString() {
        return String.format("Intersection[%s, %s, %d waiting vehicles]", 
                           id, type.getDisplayName(), waitingVehicles.size());
    }
}
