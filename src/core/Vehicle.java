package src.core;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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
    
    // Yeni gerçekçi sistem için eklenenler
    private CityMap.Zone startZone;
    private CityMap.Zone endZone;
    private CityMap.Zone currentZone;
    private List<Road> alternativeRoute = new ArrayList<>();
    private boolean useAlternativeRoute = false;
    private long lastRouteUpdate = System.currentTimeMillis();
    private int routeRecalculationCount = 0;
    private double fuelLevel = 100.0; // Yakıt seviyesi
    private String purpose = ""; // Seyahat amacı
    
    public enum VehicleType {
        CAR(1.0, "Şahsi Araç"),
        TRUCK(0.7, "Kamyon"),
        MOTORCYCLE(1.3, "Motosiklet"),
        BUS(0.8, "Otobüs"),
        TAXI(1.1, "Taksi"),
        DELIVERY(0.9, "Kargo Aracı");
        
        private final double speedFactor;
        private final String displayName;
        
        VehicleType(double speedFactor, String displayName) {
            this.speedFactor = speedFactor;
            this.displayName = displayName;
        }
        
        public double getSpeedFactor() {
            return speedFactor;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public static VehicleType getRandomType() {
            VehicleType[] types = VehicleType.values();
            return types[random.nextInt(types.length)];
        }
        
        // Zone'a göre araç türü dağılımı
        public static VehicleType getRandomTypeForZone(CityMap.ZoneType zoneType) {
            switch (zoneType) {
                case RESIDENTIAL:
                    // Konut bölgelerinde daha çok şahsi araç
                    if (random.nextDouble() < 0.7) return CAR;
                    if (random.nextDouble() < 0.9) return MOTORCYCLE;
                    return BUS;
                    
                case COMMERCIAL:
                    // Ticaret bölgelerinde karışık
                    if (random.nextDouble() < 0.4) return CAR;
                    if (random.nextDouble() < 0.6) return TAXI;
                    if (random.nextDouble() < 0.8) return DELIVERY;
                    return TRUCK;
                    
                case INDUSTRIAL:
                    // Sanayi bölgelerinde ağır araçlar
                    if (random.nextDouble() < 0.5) return TRUCK;
                    if (random.nextDouble() < 0.7) return DELIVERY;
                    return CAR;
                    
                case DOWNTOWN:
                    // Şehir merkezinde taksi ve otobüs
                    if (random.nextDouble() < 0.3) return TAXI;
                    if (random.nextDouble() < 0.5) return BUS;
                    if (random.nextDouble() < 0.8) return CAR;
                    return MOTORCYCLE;
                    
                default:
                    return getRandomType();
            }
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
        
        // Zone tabanlı araç türü ve amaç belirleme
        if (startZone != null) {
            this.type = VehicleType.getRandomTypeForZone(startZone.getType());
            this.purpose = generatePurpose();
        }
        
        if (!route.isEmpty()) {
            this.currentRoad = route.get(0);
        }
    }

    @Override
    public void run() {
        System.out.println("🚗 Vehicle " + id + " (" + type.getDisplayName() + ") starting journey: " + 
                         getZoneName(startZone) + " → " + getZoneName(endZone) + " (" + purpose + ")");
        
        for (int i = 0; i < route.size() && active; i++) {
            Road currentRoad = route.get(i);
            TrafficLight currentLight = null;
            Intersection currentIntersection = null;
            
            // Trafik ışığı ve kavşak bulma
            if (i < lights.size()) currentLight = lights.get(i);
            if (i < intersections.size()) currentIntersection = intersections.get(i);
            
            this.currentRoad = currentRoad;
            this.progress = 0.0;
            
            // Zone güncelleme
            updateCurrentZone();
            
            // Dinamik rota kontrolü
            if (shouldRecalculateRoute()) {
                recalculateRoute(i);
            }
            
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
            
            // Yakıt tüketimi
            consumeFuel();
        }
        
        System.out.println("✅ Vehicle " + id + " completed journey to " + getZoneName(endZone) + 
                         " (Total wait: " + (totalWaitTime / 1000) + "s, Fuel: " + String.format("%.1f%%", fuelLevel) + ")");
    }

    private boolean tryToMoveThrough(Road road, TrafficLight light, Intersection intersection) throws InterruptedException {
        // GÜÇLÜ Trafik ışığı kontrolü
        if (light != null) {
            System.out.println("🚦 Vehicle " + id + " checking traffic light for road " + road.getId());
            waitStartTime = System.currentTimeMillis();
            
            int lightWaitCount = 0;
            final int MAX_LIGHT_WAIT = 50; // Maksimum 5 saniye bekle (50 * 100ms)
            
            while (active && light.getTrafficLightState() != TrafficLight.State.GREEN && lightWaitCount < MAX_LIGHT_WAIT) {
                TrafficLight.State currentState = light.getTrafficLightState();
                System.out.println("🔴 Vehicle " + id + " waiting for GREEN light, current: " + currentState);
                Thread.sleep(100);
                lightWaitCount++;
            }
            
            if (lightWaitCount >= MAX_LIGHT_WAIT) {
                System.out.println("⚠️ Vehicle " + id + " timed out waiting for traffic light");
                totalWaitTime += System.currentTimeMillis() - waitStartTime;
                return false;
            }
            
            totalWaitTime += System.currentTimeMillis() - waitStartTime;
            System.out.println("🟢 Vehicle " + id + " got GREEN light, proceeding");
        }
        
        // Yola giriş denemesi
        if (!road.addVehicle(this)) {
            System.out.println("🚫 Vehicle " + id + " cannot enter road " + road.getId() + " (full)");
            return false;
        }
        
        System.out.println("✅ Vehicle " + id + " entered road " + road.getId());
        
        // Kavşak kontrolü (varsa)
        if (intersection != null) {
            waitStartTime = System.currentTimeMillis();
            if (!intersection.enter(id)) {
                road.removeVehicle(this);
                totalWaitTime += System.currentTimeMillis() - waitStartTime;
                System.out.println("🚫 Vehicle " + id + " cannot enter intersection " + intersection.getId());
                return false;
            }
            totalWaitTime += System.currentTimeMillis() - waitStartTime;
            System.out.println("✅ Vehicle " + id + " entered intersection " + intersection.getId());
        }
        
        // Yolda hareket (burada progress takip edilir)
        moveOnRoad(road);
        
        // Kavşaktan çıkış
        if (intersection != null) {
            intersection.exit(id);
            System.out.println("✅ Vehicle " + id + " exited intersection " + intersection.getId());
        }
        
        road.removeVehicle(this);
        System.out.println("✅ Vehicle " + id + " exited road " + road.getId());
        
        return true;
    }

    private void handleFailure(int currentRouteIndex) {
        // Gelişmiş hata yönetimi
        System.out.println("🚨 Vehicle " + id + " handling failure at route index " + currentRouteIndex);
        
        if (alternativeRoute.isEmpty()) {
            // Alternatif rota bulunamadı, geri dön
            if (currentRouteIndex > 0) {
                System.out.println("Vehicle " + id + " is returning to previous road");
                position--;
                if (currentRouteIndex - 1 < route.size()) {
                    currentRoad = route.get(currentRouteIndex - 1);
                }
            } else {
                System.out.println("Vehicle " + id + " cancelling route");
                active = false;
            }
        } else {
            // Alternatif rotaya geç
            useAlternativeRoute = true;
            System.out.println("Vehicle " + id + " switching to alternative route");
        }
    }

    private void moveOnRoad(Road road) {
        position++;
        
        // Yol türüne ve trafik durumuna göre hız ayarlaması
        double baseSpeed = speed;
        
        // Yol türü faktörü
        if (road.getRoadType() != null) {
            switch (road.getRoadType()) {
                case HIGHWAY:
                    baseSpeed *= 1.5; // Otoyolda hızlı
                    break;
                case MAIN_ROAD:
                    baseSpeed *= 1.2;
                    break;
                case RESIDENTIAL_STREET:
                    baseSpeed *= 0.7; // Konut sokağında yavaş
                    break;
            }
        }
        
        // Trafik sıkışıklığı faktörü
        double congestion = road.getCongestionLevel();
        double congestionFactor = 1.0 - (congestion * 0.8);
        baseSpeed *= congestionFactor;
        
        // Zone faktörü
        if (currentZone != null) {
            baseSpeed *= currentZone.getType().getTrafficDensityFactor();
        }
        
        // Araç türü faktörü
        if (type == VehicleType.MOTORCYCLE && congestion > 0.5) {
            baseSpeed *= 1.3; // Motosikletler trafikte daha hızlı
        }
        
        // DÜZELTME: Hareket simülasyonu - yolda kalma garantisi
        int steps = 50; // Daha hassas hareket için step sayısını artırdık
        double stepDelay = 100 / Math.max(0.1, baseSpeed);
        
        for (int i = 0; i < steps && active; i++) {
            try {
                // Progress yol sınırları içinde kalmalı (0.0 - 1.0)
                progress = Math.min(1.0, Math.max(0.0, i / (double)steps));
                
                // Mevcut road değişmemeli
                if (currentRoad == null || !currentRoad.equals(road)) {
                    System.out.println("⚠️ Vehicle " + id + " road reference lost, stopping movement");
                    break;
                }
                
                Thread.sleep((long)stepDelay);
                
                // Rastgele duraklamalar (gerçekçi hareket için) - azaltıldı
                if (random.nextDouble() < 0.02) { // %2'ye düşürüldü
                    Thread.sleep(random.nextInt(100) + 50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        progress = 1.0; // Yol tamamlandı
        
        System.out.println("✅ Vehicle " + id + " completed road " + road.getId() + 
                         " (" + road.getRoadType().getDisplayName() + ")" + 
                         " Speed: " + String.format("%.1f", baseSpeed) + 
                         " Congestion: " + String.format("%.1f%%", congestion * 100));
    }
    
    private void updateCurrentZone() {
        // Mevcut yola göre zone güncelle (basit versiyon)
        if (currentRoad != null && startZone != null && endZone != null) {
            double routeProgress = (double) position / route.size();
            if (routeProgress < 0.3) {
                currentZone = startZone;
            } else if (routeProgress > 0.7) {
                currentZone = endZone;
            } else {
                // Ara bölgede, rastgele zone seç veya aynı kalsın
            }
        }
    }
    
    private boolean shouldRecalculateRoute() {
        // 30 saniyede bir rota kontrolü
        return (System.currentTimeMillis() - lastRouteUpdate > 30000) && 
               (routeRecalculationCount < 3); // Maksimum 3 kez yeniden hesapla
    }
    
    private void recalculateRoute(int currentIndex) {
        // Basit rota yeniden hesaplama
        if (currentRoad != null && currentRoad.getCongestionLevel() > 0.7) {
            System.out.println("🔄 Vehicle " + id + " is recalculating route due to heavy traffic");
            routeRecalculationCount++;
            lastRouteUpdate = System.currentTimeMillis();
            
            // TODO: Gerçek A* pathfinding algoritması eklenecek
            // Şimdilik sadece mesaj göster
        }
    }
    
    private void consumeFuel() {
        // Yakıt tüketimi simülasyonu
        double consumption = 0.5; // Temel tüketim
        
        if (currentRoad != null) {
            // Yol türüne göre tüketim
            switch (currentRoad.getRoadType()) {
                case HIGHWAY:
                    consumption *= 0.8; // Otoyolda verimli
                    break;
                case RESIDENTIAL_STREET:
                    consumption *= 1.2; // Dur-kalk fazla yakıt
                    break;
            }
            
            // Sıkışıklıkta daha fazla yakıt
            consumption *= (1.0 + currentRoad.getCongestionLevel() * 0.5);
        }
        
        // Araç türüne göre tüketim
        switch (type) {
            case TRUCK:
                consumption *= 2.0;
                break;
            case BUS:
                consumption *= 1.8;
                break;
            case MOTORCYCLE:
                consumption *= 0.5;
                break;
        }
        
        fuelLevel = Math.max(0, fuelLevel - consumption);
        
        if (fuelLevel < 10) {
            System.out.println("⛽ Vehicle " + id + " is low on fuel (" + String.format("%.1f%%", fuelLevel) + ")");
        }
    }
    
    private String generatePurpose() {
        if (startZone == null || endZone == null) return "Seyahat";
        
        CityMap.ZoneType startType = startZone.getType();
        CityMap.ZoneType endType = endZone.getType();
        
        if (startType == CityMap.ZoneType.RESIDENTIAL && endType == CityMap.ZoneType.COMMERCIAL) {
            return "Alışveriş";
        } else if (startType == CityMap.ZoneType.RESIDENTIAL && endType == CityMap.ZoneType.DOWNTOWN) {
            return "İş";
        } else if (endType == CityMap.ZoneType.INDUSTRIAL) {
            return "Kargo/İş";
        } else if (endType == CityMap.ZoneType.PARK) {
            return "Rekreasyon";
        } else if (startType == endType) {
            return "Yerel Seyahat";
        }
        
        return "Genel Seyahat";
    }
    
    private String getZoneName(CityMap.Zone zone) {
        return zone != null ? zone.getType().getDisplayName() + " (" + zone.getId() + ")" : "Bilinmeyen";
    }

    // Getter metodları
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
        // Thread'i interrupt et ki döngüden çıksın
        this.interrupt();
    }
    
    // Yeni getter/setter metodları
    public void setStartZone(CityMap.Zone startZone) {
        this.startZone = startZone;
        if (startZone != null) {
            this.type = VehicleType.getRandomTypeForZone(startZone.getType());
            this.purpose = generatePurpose();
        }
    }
    
    public void setEndZone(CityMap.Zone endZone) {
        this.endZone = endZone;
        this.purpose = generatePurpose();
    }
    
    public CityMap.Zone getStartZone() {
        return startZone;
    }
    
    public CityMap.Zone getEndZone() {
        return endZone;
    }
    
    public CityMap.Zone getCurrentZone() {
        return currentZone;
    }
    
    public String getPurpose() {
        return purpose;
    }
    
    public double getFuelLevel() {
        return fuelLevel;
    }
    
    public int getRouteRecalculationCount() {
        return routeRecalculationCount;
    }
    
    @Override
    public String toString() {
        return String.format("Vehicle[%s, %s, %s→%s, Fuel: %.1f%%, Wait: %ds]", 
                           id, type.getDisplayName(), 
                           getZoneName(startZone), getZoneName(endZone),
                           fuelLevel, totalWaitTime / 1000);
    }
}
