package src.core;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class CityMap {
    private final List<Road> roads = new ArrayList<>();
    private final List<Intersection> intersections = new ArrayList<>();
    private final List<TrafficLight> lights = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Map<String, Road> roadMap = new HashMap<>();
    private final Map<String, Intersection> intersectionMap = new HashMap<>();
    private final Map<Road, TrafficLight> roadLightMap = new HashMap<>();
    
    // Yeni gerçekçi şehir sistemi için eklenen özellikler
    private final Map<String, Zone> zones = new HashMap<>();
    private final Map<String, List<Road>> roadNetwork = new HashMap<>(); // Kavşak ID -> Bağlı yollar
    private final Random random = new Random();
    
    // Zone türleri
    public enum ZoneType {
        RESIDENTIAL("Konut Bölgesi", 0.3),
        COMMERCIAL("Ticaret Bölgesi", 0.8), 
        INDUSTRIAL("Sanayi Bölgesi", 0.6),
        DOWNTOWN("Şehir Merkezi", 1.0),
        PARK("Park Alanı", 0.1);
        
        private final String displayName;
        private final double trafficDensityFactor;
        
        ZoneType(String displayName, double trafficDensityFactor) {
            this.displayName = displayName;
            this.trafficDensityFactor = trafficDensityFactor;
        }
        
        public String getDisplayName() { return displayName; }
        public double getTrafficDensityFactor() { return trafficDensityFactor; }
    }
    
    // Yol türleri
    public enum RoadType {
        HIGHWAY("Otoyol", 6, 120, true),
        MAIN_ROAD("Ana Yol", 4, 60, true),
        SECONDARY_ROAD("Tali Yol", 2, 40, true),
        LOCAL_STREET("Mahalle Yolu", 2, 30, false),
        RESIDENTIAL_STREET("Konut Sokağı", 1, 20, false);
        
        private final String displayName;
        private final int capacity;
        private final int speedLimit;
        private final boolean hasTrafficLights;
        
        RoadType(String displayName, int capacity, int speedLimit, boolean hasTrafficLights) {
            this.displayName = displayName;
            this.capacity = capacity;
            this.speedLimit = speedLimit;
            this.hasTrafficLights = hasTrafficLights;
        }
        
        public String getDisplayName() { return displayName; }
        public int getCapacity() { return capacity; }
        public int getSpeedLimit() { return speedLimit; }
        public boolean hasTrafficLights() { return hasTrafficLights; }
    }
    
    // Kavşak türleri
    public enum IntersectionType {
        TRAFFIC_LIGHT("Işıklı Kavşak"),
        STOP_SIGN("Stop Levhası"),
        ROUNDABOUT("Dönel Kavşak"), 
        UNCONTROLLED("Kontrolsüz Kavşak");
        
        private final String displayName;
        
        IntersectionType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    // Zone sınıfı
    public static class Zone {
        private final String id;
        private final ZoneType type;
        private final double x, y, width, height;
        private final List<String> roadIds = new ArrayList<>();
        
        public Zone(String id, ZoneType type, double x, double y, double width, double height) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        // Getters
        public String getId() { return id; }
        public ZoneType getType() { return type; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
        public List<String> getRoadIds() { return roadIds; }
        
        public void addRoad(String roadId) {
            roadIds.add(roadId);
        }
    }

    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    // Eski grid sistemi - geriye uyumluluk için korundu
    public void setupGridCity(int gridSize, int vehicleCount) {
        roads.clear();
        intersections.clear();
        lights.clear();
        vehicles.clear();
        roadMap.clear();
        intersectionMap.clear();
        roadLightMap.clear();

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Road hRoad = new Road("R" + i + "-" + j, 2, Direction.EAST);
                TrafficLight hLight = new TrafficLight(hRoad);
                roads.add(hRoad);
                lights.add(hLight);
                roadMap.put(hRoad.getId(), hRoad);
                roadLightMap.put(hRoad, hLight);

                Road vRoad = new Road("C" + i + "-" + j, 2, Direction.SOUTH);
                TrafficLight vLight = new TrafficLight(vRoad);
                roads.add(vRoad);
                lights.add(vLight);
                roadMap.put(vRoad.getId(), vRoad);
                roadLightMap.put(vRoad, vLight);

                Intersection intersection = new Intersection("X" + i + "-" + j);
                intersections.add(intersection);
                intersectionMap.put(intersection.getId(), intersection);
            }
        }

        Random rand = new Random();

        for (int i = 0; i < vehicleCount; i++) {
            int startX = rand.nextInt(gridSize);
            int startY = rand.nextInt(gridSize);
            List<Road> route = new ArrayList<>();
            List<TrafficLight> routeLights = new ArrayList<>();
            List<Intersection> routeIntersections = new ArrayList<>();

            int x = startX;
            int y = startY;

            for (int step = 0; step < 3; step++) {
                boolean horizontal = rand.nextBoolean();
                String roadId;
                if (horizontal && y + 1 < gridSize) {
                    roadId = "R" + x + "-" + y;
                    y++;
                } else if (!horizontal && x + 1 < gridSize) {
                    roadId = "C" + x + "-" + y;
                    x++;
                } else {
                    continue;
                }

                Road road = roadMap.get(roadId);
                if (road != null) route.add(road);

                for (TrafficLight light : lights) {
                    if (light.getLightId().equals(roadId)) {
                        routeLights.add(light);
                        break;
                    }
                }

                String intersectionId = "X" + x + "-" + y;
                Intersection intersection = intersectionMap.get(intersectionId);
                if (intersection != null) {
                    routeIntersections.add(intersection);
                }
            }

            if (!route.isEmpty()) {
                Vehicle v = new Vehicle("V" + i, route, routeLights, routeIntersections);
                vehicles.add(v);
            }
        }
    }
    
    // YENİ: Gerçekçi şehir kurulumu
    public void setupRealisticCity(int vehicleCount) {
        // Önce mevcut simülasyonu tamamen durdur
        forceStopSimulation();
        
        clearAll();
        
        System.out.println("🏙️ Gerçekçi şehir oluşturuluyor...");
        
        // 1. Zone'ları oluştur
        createZones();
        
        // 2. Yol ağını oluştur
        createRoadNetwork();
        
        // 3. Kavşakları oluştur
        createIntersections();
        
        // 4. Trafik ışıklarını yerleştir
        placeTra‌fficLights();
        
        // 5. Araçları oluştur (akıllı pathfinding ile)
        createSmartVehicles(vehicleCount);
        
        System.out.println("✅ Gerçekçi şehir kurulumu tamamlandı!");
        System.out.println("📊 Zone sayısı: " + zones.size());
        System.out.println("🛣️ Yol sayısı: " + roads.size());
        System.out.println("🚦 Kavşak sayısı: " + intersections.size());
        System.out.println("🚗 Araç sayısı: " + vehicles.size());
    }
    
    private void forceStopSimulation() {
        simulationRunning = false;
        
        // Simülasyon manager'ı durdur
        if (simulationManager != null && simulationManager.isAlive()) {
            simulationManager.interrupt();
            try {
                simulationManager.join(2000); // 2 saniye bekle
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Tüm vehicle thread'leri durdur
        for (Vehicle vehicle : vehicles) {
            if (vehicle.isAlive()) {
                vehicle.stopVehicle();
                vehicle.interrupt();
            }
        }
        
        // Tüm traffic light thread'leri durdur
        for (TrafficLight light : lights) {
            if (light.isAlive()) {
                light.interrupt();
            }
        }
        
        // Thread'lerin durmasını bekle
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void clearAll() {
        roads.clear();
        intersections.clear();
        lights.clear();
        vehicles.clear();
        roadMap.clear();
        intersectionMap.clear();
        roadLightMap.clear();
        zones.clear();
        roadNetwork.clear();
    }
    
    private void createZones() {
        // STRATEJİK BÖLGE DÜZENLEMESİ - Yollarla kesişmeyen optimal konumlar
        
        // KUZEY-BATI köşesi (Üst sol)
        zones.put("RES_NW1", new Zone("RES_NW1", ZoneType.RESIDENTIAL, 10, 10, 80, 130));
        zones.put("COM_NW", new Zone("COM_NW", ZoneType.COMMERCIAL, 160, 10, 80, 130));
        zones.put("RES_NW2", new Zone("RES_NW2", ZoneType.RESIDENTIAL, 260, 10, 130, 130));
        
        // KUZEY-DOĞU köşesi (Üst sağ)
        zones.put("DOWNTOWN", new Zone("DOWNTOWN", ZoneType.DOWNTOWN, 420, 10, 120, 80));
        zones.put("RES_NE1", new Zone("RES_NE1", ZoneType.RESIDENTIAL, 560, 10, 120, 80));
        zones.put("COM_NE", new Zone("COM_NE", ZoneType.COMMERCIAL, 700, 10, 90, 130));
        
        // GÜNEY-BATI köşesi (Alt sol)
        zones.put("IND_SW", new Zone("IND_SW", ZoneType.INDUSTRIAL, 10, 370, 80, 120));
        zones.put("RES_SW1", new Zone("RES_SW1", ZoneType.RESIDENTIAL, 10, 500, 130, 80));
        zones.put("RES_SW2", new Zone("RES_SW2", ZoneType.RESIDENTIAL, 160, 520, 80, 100));
        zones.put("COM_SW", new Zone("COM_SW", ZoneType.COMMERCIAL, 260, 520, 130, 100));
        
        // GÜNEY-DOĞU köşesi (Alt sağ) 
        zones.put("PARK_SE", new Zone("PARK_SE", ZoneType.PARK, 420, 370, 120, 120));
        zones.put("COM_SE", new Zone("COM_SE", ZoneType.COMMERCIAL, 560, 370, 120, 80));
        zones.put("RES_SE", new Zone("RES_SE", ZoneType.RESIDENTIAL, 700, 370, 90, 180));
        zones.put("IND_SE", new Zone("IND_SE", ZoneType.INDUSTRIAL, 420, 520, 230, 100));
        
        // DOĞU UZANTISI (Sağ kenar)
        zones.put("RES_EAST", new Zone("RES_EAST", ZoneType.RESIDENTIAL, 820, 10, 120, 200));
        zones.put("PARK_EAST", new Zone("PARK_EAST", ZoneType.PARK, 820, 220, 120, 120));
        zones.put("COM_EAST", new Zone("COM_EAST", ZoneType.COMMERCIAL, 820, 350, 120, 200));
        
        System.out.println("✅ " + zones.size() + " stratejik bölge oluşturuldu (optimize yerleşim)");
    }
    
    private void createRoadNetwork() {
        int roadCounter = 0;
        
        // CANVAS İÇİN OPTİMİZE YOL AĞI (1000x800)
        
        // ANA OMURGA: Çapraz otoyollar (şehri 4'e böler)
        createRoad("HW_MAIN_EW", RoadType.HIGHWAY, 0, 350, 950, 350, roadCounter++);  // Yatay otoyol (genişletildi)
        createRoad("HW_MAIN_NS", RoadType.HIGHWAY, 400, 0, 400, 750, roadCounter++);  // Dikey otoyol (uzatıldı)
        
        // KUZEY BÖLGE YOLLARI (Canvas üst yarısı için optimize)
        createRoad("NORTH_1", RoadType.MAIN_ROAD, 100, 0, 100, 350, roadCounter++);   // Sol dikey
        createRoad("NORTH_2", RoadType.MAIN_ROAD, 200, 0, 200, 350, roadCounter++);   // Orta dikey
        createRoad("NORTH_3", RoadType.MAIN_ROAD, 300, 0, 300, 350, roadCounter++);   // Sağ dikey
        createRoad("NORTH_4", RoadType.MAIN_ROAD, 550, 0, 550, 350, roadCounter++);   // Doğu dikey
        createRoad("NORTH_5", RoadType.MAIN_ROAD, 700, 0, 700, 350, roadCounter++);   // Uzak doğu dikey
        createRoad("NORTH_H1", RoadType.SECONDARY_ROAD, 0, 150, 950, 150, roadCounter++); // Üst yatay (uzatıldı)
        createRoad("NORTH_H2", RoadType.SECONDARY_ROAD, 0, 250, 950, 250, roadCounter++); // Alt yatay (uzatıldı)
        
        // GÜNEY BÖLGE YOLLARI (Canvas alt yarısı için optimize)  
        createRoad("SOUTH_1", RoadType.MAIN_ROAD, 100, 350, 100, 750, roadCounter++);  // Sol dikey
        createRoad("SOUTH_2", RoadType.MAIN_ROAD, 200, 350, 200, 750, roadCounter++);  // Orta dikey
        createRoad("SOUTH_3", RoadType.MAIN_ROAD, 300, 350, 300, 750, roadCounter++);  // Sağ dikey
        createRoad("SOUTH_4", RoadType.MAIN_ROAD, 550, 350, 550, 750, roadCounter++);  // Doğu dikey
        createRoad("SOUTH_5", RoadType.MAIN_ROAD, 700, 350, 700, 750, roadCounter++);  // Uzak doğu dikey
        createRoad("SOUTH_H1", RoadType.SECONDARY_ROAD, 0, 500, 950, 500, roadCounter++); // Üst yatay (uzatıldı)
        createRoad("SOUTH_H2", RoadType.SECONDARY_ROAD, 0, 650, 950, 650, roadCounter++); // Alt yatay (uzatıldı)
        
        // DOĞU UZANTISI YOLLARI (Canvas sağ kenarı için)
        createRoad("EAST_1", RoadType.MAIN_ROAD, 800, 0, 800, 750, roadCounter++);     // Ana dikey
        createRoad("EAST_2", RoadType.MAIN_ROAD, 900, 0, 900, 750, roadCounter++);     // Kenar dikey
        createRoad("EAST_H1", RoadType.SECONDARY_ROAD, 400, 100, 950, 100, roadCounter++); // Üst bağlantı
        createRoad("EAST_H2", RoadType.SECONDARY_ROAD, 400, 300, 950, 300, roadCounter++); // Alt bağlantı
        
        // MERKEZ BAĞLANTI YOLLARI - Tüm bölgeleri birbirine bağlar
        createRoad("CONNECT_1", RoadType.LOCAL_STREET, 0, 400, 950, 400, roadCounter++); // Yatay bağlantı
        createRoad("CONNECT_2", RoadType.LOCAL_STREET, 0, 600, 950, 600, roadCounter++); // Alt yatay bağlantı
        createRoad("CONNECT_3", RoadType.LOCAL_STREET, 600, 0, 600, 750, roadCounter++); // Dikey bağlantı
        
        System.out.println("✅ " + roads.size() + " canvas-optimized yol oluşturuldu (1000x800)");
    }
    
    private void createRoad(String id, RoadType type, double x1, double y1, double x2, double y2, int counter) {
        Direction direction = (Math.abs(x2 - x1) > Math.abs(y2 - y1)) ? Direction.EAST : Direction.SOUTH;
        Road road = new Road(id, type.getCapacity(), direction);
        road.setRoadType(type);
        road.setCoordinates(x1, y1, x2, y2);
        
        roads.add(road);
        roadMap.put(id, road);
        
        // Zone'lara yol ataması
        assignRoadToZones(road);
    }
    
    private void assignRoadToZones(Road road) {
        double roadX = (road.getX1() + road.getX2()) / 2;
        double roadY = (road.getY1() + road.getY2()) / 2;
        
        for (Zone zone : zones.values()) {
            if (roadX >= zone.getX() && roadX <= zone.getX() + zone.getWidth() &&
                roadY >= zone.getY() && roadY <= zone.getY() + zone.getHeight()) {
                zone.addRoad(road.getId());
                break;
            }
        }
    }
    
    private void createIntersections() {
        // Otomatik kavşak tespiti - yolların kesişim noktalarında
        Map<String, List<Road>> intersectionPoints = new HashMap<>();
        
        for (int i = 0; i < roads.size(); i++) {
            for (int j = i + 1; j < roads.size(); j++) {
                Road road1 = roads.get(i);
                Road road2 = roads.get(j);
                
                // Yolların kesişim noktasını kontrol et
                double[] intersection = findIntersection(road1, road2);
                if (intersection != null) {
                    String intersectionId = "INT_" + road1.getId() + "_" + road2.getId();
                    
                    // Kavşak türünü belirle
                    IntersectionType type = determineIntersectionType(road1, road2);
                    
                    Intersection inter = new Intersection(intersectionId);
                    inter.setType(type);
                    inter.setCoordinates(intersection[0], intersection[1]);
                    
                    intersections.add(inter);
                    intersectionMap.put(intersectionId, inter);
                    
                    // Yol ağı bağlantısını kur
                    roadNetwork.computeIfAbsent(intersectionId, k -> new ArrayList<>()).add(road1);
                    roadNetwork.computeIfAbsent(intersectionId, k -> new ArrayList<>()).add(road2);
                }
            }
        }
        
        System.out.println("✅ " + intersections.size() + " kavşak oluşturuldu");
    }
    
    private double[] findIntersection(Road road1, Road road2) {
        // Basit çizgi kesişim algoritması
        double x1 = road1.getX1(), y1 = road1.getY1(), x2 = road1.getX2(), y2 = road1.getY2();
        double x3 = road2.getX1(), y3 = road2.getY1(), x4 = road2.getX2(), y4 = road2.getY2();
        
        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 0.001) return null; // Paralel çizgiler
        
        double t = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom;
        
        if (t >= 0 && t <= 1 && u >= 0 && u <= 1) {
            double intersectionX = x1 + t * (x2 - x1);
            double intersectionY = y1 + t * (y2 - y1);
            return new double[]{intersectionX, intersectionY};
        }
        
        return null;
    }
    
    private IntersectionType determineIntersectionType(Road road1, Road road2) {
        RoadType type1 = road1.getRoadType();
        RoadType type2 = road2.getRoadType();
        
        // Otoyol kavşakları her zaman ışıklı
        if (type1 == RoadType.HIGHWAY || type2 == RoadType.HIGHWAY) {
            return IntersectionType.TRAFFIC_LIGHT;
        }
        
        // Ana yol kavşakları çoğunlukla ışıklı
        if ((type1 == RoadType.MAIN_ROAD || type2 == RoadType.MAIN_ROAD) && random.nextDouble() < 0.8) {
            return IntersectionType.TRAFFIC_LIGHT;
        }
        
        // Tali yollar için karışık türler
        if (random.nextDouble() < 0.3) {
            return IntersectionType.ROUNDABOUT;
        } else if (random.nextDouble() < 0.5) {
            return IntersectionType.STOP_SIGN;
        } else {
            return IntersectionType.UNCONTROLLED;
        }
    }
    
    private void placeTra‌fficLights() {
        for (Intersection intersection : intersections) {
            if (intersection.getType() == IntersectionType.TRAFFIC_LIGHT) {
                List<Road> connectedRoads = roadNetwork.get(intersection.getId());
                if (connectedRoads != null) {
                    for (Road road : connectedRoads) {
                        if (roadLightMap.get(road) == null) { // Henüz ışık yok
                            TrafficLight light = new TrafficLight(road);
                            lights.add(light);
                            roadLightMap.put(road, light);
                        }
                    }
                }
            }
        }
        
        System.out.println("✅ " + lights.size() + " trafik ışığı yerleştirildi");
    }
    
    private void createSmartVehicles(int vehicleCount) {
        for (int i = 0; i < vehicleCount; i++) {
            // Rastgele başlangıç ve hedef zone'ları seç
            Zone startZone = getRandomZone();
            Zone endZone = getRandomZone();
            
            if (startZone != null && endZone != null && !startZone.equals(endZone)) {
                // Pathfinding ile en iyi rotayı bul
                List<Road> route = findSmartRoute(startZone, endZone);
                
                if (!route.isEmpty()) {
                    List<TrafficLight> routeLights = new ArrayList<>();
                    List<Intersection> routeIntersections = new ArrayList<>();
                    
                    // ENTEGRE: Rota boyunca ışık ve kavşakları topla
                    for (Road road : route) {
                        // Trafik ışığı kontrolü
                        TrafficLight light = roadLightMap.get(road);
                        if (light != null) {
                            routeLights.add(light);
                            System.out.println("🚦 Vehicle V" + i + " route includes traffic light on " + road.getId());
                        }
                        
                        // Kavşak kontrolü - bu yolun bağlı olduğu kavşakları bul
                        for (Intersection intersection : intersections) {
                            List<Road> connectedRoads = roadNetwork.get(intersection.getId());
                            if (connectedRoads != null && connectedRoads.contains(road)) {
                                if (!routeIntersections.contains(intersection)) {
                                    routeIntersections.add(intersection);
                                    System.out.println("🔀 Vehicle V" + i + " route includes intersection " + intersection.getId() + 
                                                     " (" + intersection.getType().getDisplayName() + ")");
                                }
                            }
                        }
                    }
                    
                    Vehicle vehicle = new Vehicle("V" + i, route, routeLights, routeIntersections);
                    vehicle.setStartZone(startZone);
                    vehicle.setEndZone(endZone);
                    vehicles.add(vehicle);
                    
                    System.out.println("🚗 Created vehicle V" + i + " with " + route.size() + " roads, " + 
                                     routeLights.size() + " lights, " + routeIntersections.size() + " intersections");
                }
            }
        }
        
        System.out.println("✅ " + vehicles.size() + " akıllı araç oluşturuldu (TAM ENTEGRE)");
    }
    
    private Zone getRandomZone() {
        List<Zone> zoneList = new ArrayList<>(zones.values());
        return zoneList.isEmpty() ? null : zoneList.get(random.nextInt(zoneList.size()));
    }
    
    // TAM BAĞLANTILI pathfinding - araçlar bağlantılı yolları takip eder
    private List<Road> findSmartRoute(Zone startZone, Zone endZone) {
        List<Road> route = new ArrayList<>();
        
        // Başlangıç ve hedef zone'a en yakın yolları bul
        Road startRoad = findNearestRoad(startZone);
        Road endRoad = findNearestRoad(endZone);
        
        if (startRoad == null || endRoad == null) {
            return route; // Boş rota
        }
        
        // A* Benzeri basit pathfinding
        Set<Road> visited = new HashSet<>();
        List<Road> currentPath = new ArrayList<>();
        
        if (findPathBFS(startRoad, endRoad, visited, currentPath, route)) {
            System.out.println("✅ Smart route found: " + route.size() + " roads from " + 
                             startZone.getType().getDisplayName() + " to " + endZone.getType().getDisplayName());
            
            // Rota debug bilgisi
            route.forEach(road -> System.out.println("  → " + road.getId() + " (" + road.getRoadType().getDisplayName() + ")"));
        } else {
            // Pathfinding başarısız, direct connection dene
            route.add(startRoad);
            if (!startRoad.equals(endRoad)) {
                route.add(endRoad);
            }
            System.out.println("⚠️ Fallback route used: " + route.size() + " roads");
        }
        
        return route;
    }
    
    // BFS ile yol bulma algoritması
    private boolean findPathBFS(Road start, Road end, Set<Road> visited, List<Road> currentPath, List<Road> result) {
        if (start.equals(end)) {
            result.addAll(currentPath);
            result.add(end);
            return true;
        }
        
        if (visited.contains(start) || currentPath.size() > 5) { // Max 5 yol uzunluğu
            return false;
        }
        
        visited.add(start);
        currentPath.add(start);
        
        // Bağlı yolları bul
        List<Road> connectedRoads = findConnectedRoads(start);
        
        for (Road connectedRoad : connectedRoads) {
            if (findPathBFS(connectedRoad, end, new HashSet<>(visited), new ArrayList<>(currentPath), result)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Bir yola bağlı diğer yolları bul (kesişim noktalarında)
    private List<Road> findConnectedRoads(Road road) {
        List<Road> connected = new ArrayList<>();
        
        for (Road otherRoad : roads) {
            if (!otherRoad.equals(road) && roadsIntersect(road, otherRoad)) {
                connected.add(otherRoad);
            }
        }
        
        return connected;
    }
    
    // İki yolun kesişip kesişmediğini kontrol et
    private boolean roadsIntersect(Road road1, Road road2) {
        // Basit kesişim kontrolü - çizgiler kesişiyor mu?
        double[] intersection = findIntersection(road1, road2);
        return intersection != null;
    }
    
    // Zone'a en yakın yolu bul
    private Road findNearestRoad(Zone zone) {
        Road nearest = null;
        double minDistance = Double.MAX_VALUE;
        
        double zoneCenterX = zone.getX() + zone.getWidth() / 2.0;
        double zoneCenterY = zone.getY() + zone.getHeight() / 2.0;
        
        for (Road road : roads) {
            // Yolun orta noktasına olan mesafe
            double roadCenterX = (road.getX1() + road.getX2()) / 2.0;
            double roadCenterY = (road.getY1() + road.getY2()) / 2.0;
            
            double distance = Math.sqrt(Math.pow(zoneCenterX - roadCenterX, 2) + 
                                      Math.pow(zoneCenterY - roadCenterY, 2));
            
            if (distance < minDistance) {
                minDistance = distance;
                nearest = road;
            }
        }
        
        System.out.println("🎯 Zone " + zone.getId() + " → nearest road: " + 
                         (nearest != null ? nearest.getId() : "none") + 
                         " (distance: " + String.format("%.1f", minDistance) + ")");
        
        return nearest;
    }

    // Simülasyon kontrolü için eklenen değişkenler
    private volatile boolean simulationRunning = false;
    private Thread simulationManager;
    private final int maxActiveVehicles = 15;
    private final long vehicleSpawnInterval = 3000; // 3 saniye

    public void startSimulation() {
        if (simulationRunning) return;
        
        // Önceki thread'lerin temizlendiğinden emin ol
        forceStopSimulation();
        
        simulationRunning = true;
        
        // YENİ: Thread'ler restart edilemez, yeni instance'lar oluştur
        recreateThreads();
        
        // Simülasyon yöneticisini başlat
        simulationManager = new Thread(this::runSimulationLoop);
        simulationManager.setDaemon(true);
        simulationManager.start();
        
        System.out.println("🚀 Simulation started with " + vehicles.size() + " vehicles");
    }

    public void stopSimulation() {
        simulationRunning = false;
        
        // Mevcut araçları durdur
        vehicles.forEach(Vehicle::stopVehicle);
        
        // Trafik ışıklarını durdur
        lights.forEach(Thread::interrupt);
        
        // Simülasyon yöneticisini durdur
        if (simulationManager != null) {
            simulationManager.interrupt();
        }
    }
    
    private void runSimulationLoop() {
        while (simulationRunning) {
            try {
                // Bitmiş araçları temizle
                cleanupFinishedVehicles();
                
                // Yeni araç üret (gerekirse)
                if (getActiveVehicleCount() < maxActiveVehicles) {
                    generateNewVehicle();
                }
                
                // Araç spawning interval'ı bekle
                Thread.sleep(vehicleSpawnInterval);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void cleanupFinishedVehicles() {
        synchronized (vehicles) {
            vehicles.removeIf(vehicle -> !vehicle.isActive());
        }
    }
    
    private int getActiveVehicleCount() {
        return (int) vehicles.stream().mapToLong(vehicle -> vehicle.isActive() ? 1 : 0).sum();
    }
    
    private void generateNewVehicle() {
        // Rastgele başlangıç ve hedef zone'ları seç
        Zone startZone = getRandomZone();
        Zone endZone = getRandomZone();
        
        if (startZone != null && endZone != null && !startZone.equals(endZone)) {
            // Pathfinding ile en iyi rotayı bul
            List<Road> route = findSmartRoute(startZone, endZone);
            
            if (!route.isEmpty()) {
                List<TrafficLight> routeLights = new ArrayList<>();
                List<Intersection> routeIntersections = new ArrayList<>();
                
                // Rota boyunca ışık ve kavşakları topla
                for (Road road : route) {
                    TrafficLight light = roadLightMap.get(road);
                    if (light != null) routeLights.add(light);
                    
                    // Bu yolun bağlı olduğu kavşakları bul
                    for (String intersectionId : roadNetwork.keySet()) {
                        if (roadNetwork.get(intersectionId).contains(road)) {
                            Intersection intersection = intersectionMap.get(intersectionId);
                            if (intersection != null && !routeIntersections.contains(intersection)) {
                                routeIntersections.add(intersection);
                            }
                        }
                    }
                }
                
                String vehicleId = "V" + System.currentTimeMillis() % 10000; // Unique ID
                Vehicle vehicle = new Vehicle(vehicleId, route, routeLights, routeIntersections);
                vehicle.setStartZone(startZone);
                vehicle.setEndZone(endZone);
                
                synchronized (vehicles) {
                    vehicles.add(vehicle);
                }
                
                // Yeni aracı hemen başlat
                vehicle.start();
                
                System.out.println("🚗 New vehicle " + vehicleId + " spawned: " + 
                                 startZone.getType().getDisplayName() + " → " + endZone.getType().getDisplayName());
            }
        }
    }

    public Road getRoadById(String roadId) {
        return roadMap.get(roadId);
    }

    public TrafficLight getTrafficLightForRoad(Road road) {
        return roadLightMap.get(road);
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Road> getRoads() {
        return roads;
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }

    public List<TrafficLight> getLights() {
        return lights;
    }
    
    // Yeni getter metodları
    public Map<String, Zone> getZones() {
        return zones;
    }
    
    public Map<String, List<Road>> getRoadNetwork() {
        return roadNetwork;
    }
    
    public Zone getZoneById(String zoneId) {
        return zones.get(zoneId);
    }

    private void recreateThreads() {
        // Yeni TrafficLight thread'leri oluştur
        List<TrafficLight> newLights = new ArrayList<>();
        Map<Road, TrafficLight> newRoadLightMap = new HashMap<>();
        
        for (Map.Entry<Road, TrafficLight> entry : roadLightMap.entrySet()) {
            Road road = entry.getKey();
            TrafficLight newLight = new TrafficLight(road);
            newLights.add(newLight);
            newRoadLightMap.put(road, newLight);
            newLight.start();
        }
        
        lights.clear();
        lights.addAll(newLights);
        roadLightMap.clear();
        roadLightMap.putAll(newRoadLightMap);
        
        // Mevcut araçları temizle - yeni araçlar simülasyon döngüsü tarafından üretilecek
        // Çünkü eski Vehicle thread'leri restart edilemez
        vehicles.clear();
        
        System.out.println("🔄 Recreated " + lights.size() + " traffic lights");
    }
}
