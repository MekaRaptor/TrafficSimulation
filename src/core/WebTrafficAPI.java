package src.core;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple REST API for Traffic Simulation
 * Converts Java objects to JSON-like format for web frontend
 */
public class WebTrafficAPI {
    private final CityMap cityMap;
    private boolean isRunning = false;
    
    public WebTrafficAPI() {
        this.cityMap = new CityMap();
        // Start with realistic city
        cityMap.setupRealisticCity(15);
    }
    
    /**
     * Start the simulation
     */
    public String startSimulation() {
        if (!isRunning) {
            cityMap.startSimulation();
            isRunning = true;
            return "{\"status\":\"started\",\"message\":\"Traffic simulation started\"}";
        }
        return "{\"status\":\"already_running\",\"message\":\"Simulation is already running\"}";
    }
    
    /**
     * Stop the simulation
     */
    public String stopSimulation() {
        if (isRunning) {
            cityMap.stopSimulation();
            isRunning = false;
            
            // Thread'lerin durmasını bekle
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            return "{\"status\":\"stopped\",\"message\":\"Traffic simulation stopped\"}";
        }
        return "{\"status\":\"already_stopped\",\"message\":\"Simulation is not running\"}";
    }
    
    /**
     * Get current vehicles with positions - TAM ENTEGRE
     */
    public String getVehicles() {
        StringBuilder json = new StringBuilder();
        json.append("{\"vehicles\":[");
        
        List<Vehicle> vehicles = cityMap.getVehicles();
        for (int i = 0; i < vehicles.size(); i++) {
            Vehicle v = vehicles.get(i);
            Road currentRoad = v.getCurrentRoad();
            
            // TAM ENTEGRE koordinat hesaplama
            double x = 100; // default position
            double y = 100;
            String roadId = "none";
            boolean onRoad = false;
            
            if (currentRoad != null && v.getProgress() >= 0) {
                // Yol üzerindeki gerçek pozisyonu hesapla
                double progress = Math.max(0.0, Math.min(1.0, v.getProgress()));
                x = currentRoad.getX1() + (currentRoad.getX2() - currentRoad.getX1()) * progress;
                y = currentRoad.getY1() + (currentRoad.getY2() - currentRoad.getY1()) * progress;
                roadId = currentRoad.getId();
                onRoad = true;
                
                // Yol yönüne göre araç pozisyonunu hafifçe kaydır (çakışmayı önle)
                if (Math.abs(currentRoad.getX2() - currentRoad.getX1()) > Math.abs(currentRoad.getY2() - currentRoad.getY1())) {
                    // Yatay yol - Y koordinatını kaydır
                    y += (v.getVehicleId().hashCode() % 3 - 1) * 5; // -5, 0, +5 kaydırma
                } else {
                    // Dikey yol - X koordinatını kaydır
                    x += (v.getVehicleId().hashCode() % 3 - 1) * 5; // -5, 0, +5 kaydırma
                }
            } else {
                // Araç henüz yola girmemiş - başlangıç zone'unda
                CityMap.Zone originZone = v.getStartZone();
                if (originZone != null) {
                    x = originZone.getX() + originZone.getWidth() / 2.0;
                    y = originZone.getY() + originZone.getHeight() / 2.0;
                    // Zone içinde rastgele pozisyon
                    x += (Math.random() - 0.5) * 40;
                    y += (Math.random() - 0.5) * 40;
                }
            }
            
            json.append("{");
            json.append("\"id\":\"").append(v.getVehicleId()).append("\",");
            json.append("\"type\":\"").append(v.getVehicleType().getDisplayName()).append("\",");
            json.append("\"x\":").append(String.format("%.1f", x)).append(",");
            json.append("\"y\":").append(String.format("%.1f", y)).append(",");
            json.append("\"speed\":").append(String.format("%.2f", v.getSpeed())).append(",");
            json.append("\"fuel\":").append(String.format("%.1f", v.getFuelLevel())).append(",");
            json.append("\"roadId\":\"").append(roadId).append("\",");
            json.append("\"purpose\":\"").append(v.getPurpose()).append("\",");
            json.append("\"progress\":").append(String.format("%.2f", v.getProgress())).append(",");
            json.append("\"onRoad\":").append(onRoad).append(",");
            json.append("\"active\":").append(v.isActive());
            json.append("}");
            
            if (i < vehicles.size() - 1) json.append(",");
        }
        
        json.append("]}");
        return json.toString();
    }
    
    /**
     * Get all roads with traffic info
     */
    public String getRoads() {
        StringBuilder json = new StringBuilder();
        json.append("{\"roads\":[");
        
        List<Road> roads = cityMap.getRoads();
        for (int i = 0; i < roads.size(); i++) {
            Road r = roads.get(i);
            
            json.append("{");
            json.append("\"id\":\"").append(r.getId()).append("\",");
            json.append("\"type\":\"").append(r.getRoadType().getDisplayName()).append("\",");
            json.append("\"x1\":").append(r.getX1()).append(",");
            json.append("\"y1\":").append(r.getY1()).append(",");
            json.append("\"x2\":").append(r.getX2()).append(",");
            json.append("\"y2\":").append(r.getY2()).append(",");
            json.append("\"capacity\":").append(r.getCapacity()).append(",");
            json.append("\"currentVehicles\":").append(r.getVehicleCount()).append(",");
            json.append("\"congestion\":").append(String.format("%.2f", r.getCongestionLevel())).append(",");
            json.append("\"speedLimit\":").append(r.getSpeedLimit());
            json.append("}");
            
            if (i < roads.size() - 1) json.append(",");
        }
        
        json.append("]}");
        return json.toString();
    }
    
    /**
     * Get zones information
     */
    public String getZones() {
        StringBuilder json = new StringBuilder();
        json.append("{\"zones\":[");
        
        Collection<CityMap.Zone> zones = cityMap.getZones().values();
        int count = 0;
        for (CityMap.Zone zone : zones) {
            json.append("{");
            json.append("\"id\":\"").append(zone.getId()).append("\",");
            json.append("\"type\":\"").append(zone.getType().getDisplayName()).append("\",");
            json.append("\"x\":").append(zone.getX()).append(",");
            json.append("\"y\":").append(zone.getY()).append(",");
            json.append("\"width\":").append(zone.getWidth()).append(",");
            json.append("\"height\":").append(zone.getHeight());
            json.append("}");
            
            count++;
            if (count < zones.size()) json.append(",");
        }
        
        json.append("]}");
        return json.toString();
    }
    
    /**
     * Get simulation statistics
     */
    public String getStats() {
        int activeVehicles = 0;
        long totalWaitTime = 0;
        double totalCongestion = 0;
        
        // Calculate active vehicles and wait time
        for (Vehicle v : cityMap.getVehicles()) {
            if (v.isActive()) {
                activeVehicles++;
                totalWaitTime += v.getTotalWaitTime();
            }
        }
        
        // Calculate average congestion
        List<Road> roads = cityMap.getRoads();
        for (Road road : roads) {
            totalCongestion += road.getCongestionLevel();
        }
        double avgCongestion = roads.isEmpty() ? 0 : totalCongestion / roads.size();
        
        double avgWaitTime = activeVehicles > 0 ? (totalWaitTime / 1000.0) / activeVehicles : 0;
        
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"activeVehicles\":").append(activeVehicles).append(",");
        json.append("\"totalVehicles\":").append(cityMap.getVehicles().size()).append(",");
        json.append("\"totalRoads\":").append(roads.size()).append(",");
        json.append("\"totalIntersections\":").append(cityMap.getIntersections().size()).append(",");
        json.append("\"avgWaitTime\":").append(String.format("%.1f", avgWaitTime)).append(",");
        json.append("\"avgCongestion\":").append(String.format("%.2f", avgCongestion * 100)).append(",");
        json.append("\"isRunning\":").append(isRunning);
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Reset simulation with new vehicle count
     */
    public String resetSimulation(int vehicleCount) {
        // Önce simülasyonu tamamen durdur
        stopSimulation();
        
        // Thread'lerin tamamen durmasını bekle
        try {
            Thread.sleep(1000); // 1 saniye bekle
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Yeni şehir kur
        cityMap.setupRealisticCity(Math.max(5, Math.min(50, vehicleCount)));
        
        // isRunning flag'ini reset et
        isRunning = false;
        
        return "{\"status\":\"reset\",\"message\":\"Simulation reset with " + vehicleCount + " vehicles. Use start to begin.\"}";
    }
    
    /**
     * Get traffic lights with current states
     */
    public String getLights() {
        StringBuilder json = new StringBuilder();
        json.append("{\"lights\":[");
        
        List<TrafficLight> lights = cityMap.getLights();
        for (int i = 0; i < lights.size(); i++) {
            TrafficLight light = lights.get(i);
            
            json.append("{");
            json.append("\"id\":\"").append(light.getLightId()).append("\",");
            json.append("\"state\":\"").append(light.getTrafficLightState().toString()).append("\",");
            json.append("\"brightness\":").append(String.format("%.2f", light.getBrightness())).append(",");
            json.append("\"isEmergency\":").append(light.isEmergencyMode());
            json.append("}");
            
            if (i < lights.size() - 1) json.append(",");
        }
        
        json.append("]}");
        return json.toString();
    }
    
    /**
     * Simple HTTP server simulation
     * In real implementation, this would be Spring Boot
     */
    public String handleRequest(String endpoint, String method, Map<String, String> params) {
        switch (endpoint) {
            case "/api/start":
                return startSimulation();
            case "/api/stop":
                return stopSimulation();
            case "/api/vehicles":
                return getVehicles();
            case "/api/roads":
                return getRoads();
            case "/api/zones":
                return getZones();
            case "/api/stats":
                return getStats();
            case "/api/reset":
                int vehicleCount = params.containsKey("vehicles") ? 
                    Integer.parseInt(params.get("vehicles")) : 15;
                return resetSimulation(vehicleCount);
            case "/api/lights":
                return getLights();
            default:
                return "{\"error\":\"Endpoint not found\"}";
        }
    }
    
    // Test the API
    public static void main(String[] args) {
        WebTrafficAPI api = new WebTrafficAPI();
        
        System.out.println("🌐 Traffic Simulation API Test");
        System.out.println("==============================");
        
        System.out.println("\n📊 Initial Stats:");
        System.out.println(api.getStats());
        
        System.out.println("\n🛣️ Roads:");
        System.out.println(api.getRoads());
        
        System.out.println("\n🏙️ Zones:");
        System.out.println(api.getZones());
        
        System.out.println("\n🚀 Starting simulation...");
        System.out.println(api.startSimulation());
        
        try {
            Thread.sleep(5000); // 5 saniye bekle
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        System.out.println("\n🚗 Vehicles after 5 seconds:");
        System.out.println(api.getVehicles());
        
        System.out.println("\n📈 Final Stats:");
        System.out.println(api.getStats());
        
        System.out.println("\n🛑 Stopping simulation...");
        System.out.println(api.stopSimulation());
    }
} 