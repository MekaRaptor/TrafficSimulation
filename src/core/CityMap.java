package src.core;

import java.util.*;

public class CityMap {
    private final List<Road> roads = new ArrayList<>();
    private final List<Intersection> intersections = new ArrayList<>();
    private final List<TrafficLight> lights = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Map<String, Road> roadMap = new HashMap<>();
    private final Map<String, Intersection> intersectionMap = new HashMap<>();
    private final Map<Road, TrafficLight> roadLightMap = new HashMap<>();
    private final List<Zone> zones = new ArrayList<>();

    // Time-based traffic density analysis
    private final Map<String, Map<Integer, Double>> hourlyTrafficData = new HashMap<>(); // roadId -> hour -> density
    private final Map<Integer, Integer> hourlyVehicleCount = new HashMap<>(); // hour -> vehicle count
    private int currentSimulationHour = 6; // Start at 6 AM
    private long simulationStartTime = System.currentTimeMillis();
    private static final long HOUR_DURATION_MS = 60000; // 1 minute = 1 hour in simulation

    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    public static class Zone {
        private String id;
        private String type;
        private double x, y, width, height;
        
        public Zone(String id, String type, double x, double y, double width, double height) {
            this.id = id;
            this.type = type;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
        
        public String getId() { return id; }
        public String getType() { return type; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getWidth() { return width; }
        public double getHeight() { return height; }
    }

    public void setupRealisticCity(int vehicleCount) {
        setupConnectedCityNetwork(vehicleCount);
        setupZones();
    }

    private void setupConnectedCityNetwork(int vehicleCount) {
        roads.clear();
        intersections.clear();
        lights.clear();
        vehicles.clear();
        roadMap.clear();
        intersectionMap.clear();
        roadLightMap.clear();

        // Create a realistic connected city network
        createMainRoads();
        createSecondaryRoads();
        createIntersections();
        createTrafficLights();
        createVehiclesWithProperRouting(vehicleCount);
    }

    private void createMainRoads() {
        // Main horizontal roads (East-West) - Major arteries with curves
        Road mainEast1 = new Road("MAIN_EAST_1", 4, Direction.EAST);
        mainEast1.setCoordinates(50, 150, 250, 150);
        mainEast1.setTrafficDensity(0.7);
        roads.add(mainEast1);
        roadMap.put(mainEast1.getId(), mainEast1);

        Road mainEast2 = new Road("MAIN_EAST_2", 4, Direction.EAST);
        mainEast2.setCoordinates(250, 150, 450, 130); // Slight curve
        mainEast2.setTrafficDensity(0.8);
        roads.add(mainEast2);
        roadMap.put(mainEast2.getId(), mainEast2);

        Road mainWest1 = new Road("MAIN_WEST_1", 4, Direction.WEST);
        mainWest1.setCoordinates(450, 180, 250, 180);
        mainWest1.setTrafficDensity(0.8);
        roads.add(mainWest1);
        roadMap.put(mainWest1.getId(), mainWest1);

        Road mainWest2 = new Road("MAIN_WEST_2", 4, Direction.WEST);
        mainWest2.setCoordinates(250, 180, 50, 200); // Curve down
        mainWest2.setTrafficDensity(0.9);
        roads.add(mainWest2);
        roadMap.put(mainWest2.getId(), mainWest2);

        // Main vertical roads (North-South) - With intersections
        Road mainNorth1 = new Road("MAIN_NORTH_1", 4, Direction.NORTH);
        mainNorth1.setCoordinates(200, 50, 200, 150);
        mainNorth1.setTrafficDensity(0.6);
        roads.add(mainNorth1);
        roadMap.put(mainNorth1.getId(), mainNorth1);

        Road mainNorth2 = new Road("MAIN_NORTH_2", 4, Direction.NORTH);
        mainNorth2.setCoordinates(200, 150, 220, 250); // Slight angle
        mainNorth2.setTrafficDensity(0.7);
        roads.add(mainNorth2);
        roadMap.put(mainNorth2.getId(), mainNorth2);

        Road mainSouth1 = new Road("MAIN_SOUTH_1", 4, Direction.SOUTH);
        mainSouth1.setCoordinates(250, 250, 250, 150);
        mainSouth1.setTrafficDensity(0.9);
        roads.add(mainSouth1);
        roadMap.put(mainSouth1.getId(), mainSouth1);

        Road mainSouth2 = new Road("MAIN_SOUTH_2", 4, Direction.SOUTH);
        mainSouth2.setCoordinates(250, 150, 230, 50); // Angle back
        mainSouth2.setTrafficDensity(0.8);
        roads.add(mainSouth2);
        roadMap.put(mainSouth2.getId(), mainSouth2);

        // Curved highway connectors
        Road highway1 = new Road("HIGHWAY_CURVE_1", 5, Direction.EAST);
        highway1.setCoordinates(50, 100, 200, 120); // Upward curve
        highway1.setTrafficDensity(0.5);
        roads.add(highway1);
        roadMap.put(highway1.getId(), highway1);

        Road highway2 = new Road("HIGHWAY_CURVE_2", 5, Direction.EAST);
        highway2.setCoordinates(200, 120, 450, 100); // Downward curve
        highway2.setTrafficDensity(0.4);
        roads.add(highway2);
        roadMap.put(highway2.getId(), highway2);

        Road highway3 = new Road("HIGHWAY_RETURN", 5, Direction.WEST);
        highway3.setCoordinates(450, 300, 300, 280); // Return curve
        highway3.setTrafficDensity(0.4);
        roads.add(highway3);
        roadMap.put(highway3.getId(), highway3);

        Road highway4 = new Road("HIGHWAY_CONNECT", 5, Direction.WEST);
        highway4.setCoordinates(300, 280, 50, 300); // Complete curve
        highway4.setTrafficDensity(0.3);
        roads.add(highway4);
        roadMap.put(highway4.getId(), highway4);
    }

    private void createSecondaryRoads() {
        // Residential area roads - Multiple districts
        Road res1 = new Road("RES_NORTH_1", 2, Direction.EAST);
        res1.setCoordinates(50, 80, 200, 80);
        res1.setTrafficDensity(0.3);
        roads.add(res1);
        roadMap.put(res1.getId(), res1);

        Road res2 = new Road("RES_NORTH_2", 2, Direction.SOUTH);
        res2.setCoordinates(100, 50, 100, 150);
        res2.setTrafficDensity(0.2);
        roads.add(res2);
        roadMap.put(res2.getId(), res2);

        Road res3 = new Road("RES_SOUTH_1", 2, Direction.WEST);
        res3.setCoordinates(450, 320, 250, 320);
        res3.setTrafficDensity(0.25);
        roads.add(res3);
        roadMap.put(res3.getId(), res3);

        Road res4 = new Road("RES_SOUTH_2", 2, Direction.NORTH);
        res4.setCoordinates(350, 350, 350, 200);
        res4.setTrafficDensity(0.35);
        roads.add(res4);
        roadMap.put(res4.getId(), res4);

        // Commercial district roads
        Road com1 = new Road("COM_CENTER_1", 3, Direction.WEST);
        com1.setCoordinates(450, 120, 250, 120);
        com1.setTrafficDensity(0.8);
        roads.add(com1);
        roadMap.put(com1.getId(), com1);

        Road com2 = new Road("COM_CENTER_2", 3, Direction.NORTH);
        com2.setCoordinates(300, 250, 300, 100);
        com2.setTrafficDensity(0.7);
        roads.add(com2);
        roadMap.put(com2.getId(), com2);

        Road com3 = new Road("COM_MALL_ROAD", 3, Direction.SOUTH);
        com3.setCoordinates(400, 50, 400, 200);
        com3.setTrafficDensity(0.9); // Very busy shopping area
        roads.add(com3);
        roadMap.put(com3.getId(), com3);

        // Industrial area roads
        Road ind1 = new Road("IND_FACTORY_1", 2, Direction.SOUTH);
        ind1.setCoordinates(150, 200, 150, 350);
        ind1.setTrafficDensity(0.6);
        roads.add(ind1);
        roadMap.put(ind1.getId(), ind1);

        Road ind2 = new Road("IND_FACTORY_2", 2, Direction.EAST);
        ind2.setCoordinates(50, 250, 200, 250);
        ind2.setTrafficDensity(0.4);
        roads.add(ind2);
        roadMap.put(ind2.getId(), ind2);

        Road ind3 = new Road("IND_PORT_ROAD", 3, Direction.WEST);
        ind3.setCoordinates(200, 330, 50, 330);
        ind3.setTrafficDensity(0.55);
        roads.add(ind3);
        roadMap.put(ind3.getId(), ind3);

        // Business district connector roads
        Road bus1 = new Road("BUSINESS_CONNECT_1", 2, Direction.NORTH);
        bus1.setCoordinates(180, 300, 180, 150);
        bus1.setTrafficDensity(0.65);
        roads.add(bus1);
        roadMap.put(bus1.getId(), bus1);

        Road bus2 = new Road("BUSINESS_CONNECT_2", 2, Direction.EAST);
        bus2.setCoordinates(200, 200, 400, 200);
        bus2.setTrafficDensity(0.75);
        roads.add(bus2);
        roadMap.put(bus2.getId(), bus2);
    }

    private void createIntersections() {
        // Major intersections
        Intersection mainCenter = new Intersection("CENTER");
        mainCenter.setPosition(215, 165);
        intersections.add(mainCenter);
        intersectionMap.put(mainCenter.getId(), mainCenter);

        Intersection resIntersection = new Intersection("RES_INT");
        resIntersection.setPosition(100, 125);
        intersections.add(resIntersection);
        intersectionMap.put(resIntersection.getId(), resIntersection);

        Intersection comIntersection = new Intersection("COM_INT");
        comIntersection.setPosition(300, 125);
        intersections.add(comIntersection);
        intersectionMap.put(comIntersection.getId(), comIntersection);

        Intersection indIntersection = new Intersection("IND_INT");
        indIntersection.setPosition(175, 225);
        intersections.add(indIntersection);
        intersectionMap.put(indIntersection.getId(), indIntersection);
    }

    private void createTrafficLights() {
        // Create traffic lights for each road
        for (Road road : roads) {
            TrafficLight light = new TrafficLight(road);
            lights.add(light);
            roadLightMap.put(road, light);
        }
    }

    private void createVehiclesWithProperRouting(int vehicleCount) {
        Random rand = new Random();
        
        // Define longer, more realistic connected routes
        String[][] routePatterns = {
            // SIMPLE TEST ROUTES - using only confirmed roads
            {"MAIN_EAST_1", "MAIN_EAST_2"},
            {"MAIN_WEST_1", "MAIN_WEST_2"},
            {"MAIN_NORTH_1", "MAIN_NORTH_2"},
            {"MAIN_SOUTH_1", "MAIN_SOUTH_2"},
            {"HIGHWAY_CURVE_1", "HIGHWAY_CURVE_2"},
            {"RES_NORTH_1", "RES_NORTH_2"},
            {"COM_CENTER_1", "COM_CENTER_2"},
            {"IND_FACTORY_1", "IND_FACTORY_2"},
            
            // Slightly longer routes  
            {"MAIN_EAST_1", "MAIN_EAST_2", "MAIN_WEST_1"},
            {"HIGHWAY_CURVE_1", "HIGHWAY_CURVE_2", "COM_MALL_ROAD"},
            {"RES_NORTH_1", "MAIN_NORTH_1", "MAIN_NORTH_2"},
            {"COM_CENTER_1", "COM_CENTER_2", "MAIN_SOUTH_1"}
        };

        for (int i = 0; i < vehicleCount; i++) {
            // Pick a random route pattern
            String[] pattern = routePatterns[rand.nextInt(routePatterns.length)];
            
            List<Road> route = new ArrayList<>();
            List<TrafficLight> routeLights = new ArrayList<>();
            List<Intersection> routeIntersections = new ArrayList<>();

            // Build route from pattern
            for (String roadId : pattern) {
                Road road = roadMap.get(roadId);
                if (road != null) {
                    route.add(road);
                    TrafficLight light = roadLightMap.get(road);
                    if (light != null) {
                        routeLights.add(light);
                    }
                }
            }

            // Add intersections based on route
            if (!route.isEmpty()) {
                // Add appropriate intersections for the route
                if (route.stream().anyMatch(r -> r.getId().contains("MAIN"))) {
                    routeIntersections.add(intersectionMap.get("CENTER"));
                }
                if (route.stream().anyMatch(r -> r.getId().contains("RES"))) {
                    routeIntersections.add(intersectionMap.get("RES_INT"));
                }
                if (route.stream().anyMatch(r -> r.getId().contains("COM"))) {
                    routeIntersections.add(intersectionMap.get("COM_INT"));
                }
                if (route.stream().anyMatch(r -> r.getId().contains("IND"))) {
                    routeIntersections.add(intersectionMap.get("IND_INT"));
                }

                // Ensure we have enough intersections for the route
                while (routeIntersections.size() < route.size()) {
                    routeIntersections.add(intersectionMap.get("CENTER"));
                }

                Vehicle v = new Vehicle("V" + i, route, routeLights, routeIntersections);
                
                // Set initial position on first road
                if (!route.isEmpty()) {
                    Road firstRoad = route.get(0);
                    double startProgress = rand.nextDouble() * 0.3; // Start near beginning
                    double startX = firstRoad.getX1() + (firstRoad.getX2() - firstRoad.getX1()) * startProgress;
                    double startY = firstRoad.getY1() + (firstRoad.getY2() - firstRoad.getY1()) * startProgress;
                    v.setPosition(startX, startY);
                }
                
                vehicles.add(v);
            }
        }
    }

    private void setupZones() {
        zones.clear();
        zones.add(new Zone("residential1", "RESIDENTIAL", 0, 0, 200, 200));
        zones.add(new Zone("commercial1", "COMMERCIAL", 200, 0, 200, 200));
        zones.add(new Zone("industrial1", "INDUSTRIAL", 0, 200, 200, 200));
        zones.add(new Zone("downtown1", "DOWNTOWN", 200, 200, 200, 200));
    }

    public void startSimulation() {
        System.err.println("🔧 Starting simulation...");
        System.err.println("📊 Lights count: " + lights.size());
        System.err.println("🚗 Vehicles count: " + vehicles.size());
        
        try {
            // Start traffic lights (only if not already started)
            for (TrafficLight light : lights) {
                if (light.getState() == Thread.State.NEW) {
                    light.start();
                    System.err.println("✅ Started traffic light: " + light.getLightId());
                } else {
                    System.err.println("⚠️ Traffic light already started: " + light.getLightId());
                }
            }
            
            // Start vehicles (only if not already started)
            for (Vehicle vehicle : vehicles) {
                if (vehicle.getState() == Thread.State.NEW) {
                    vehicle.start();
                    System.err.println("✅ Started vehicle: " + vehicle.getVehicleId());
                } else {
                    System.err.println("⚠️ Vehicle already started: " + vehicle.getVehicleId());
                }
            }
            
            // Start continuous vehicle spawning system
            startVehicleSpawner();
            
            System.err.println("✅ CityMap simulation started successfully");
            
        } catch (Exception e) {
            System.err.println("❌ Error starting CityMap simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startVehicleSpawner() {
        Thread spawner = new Thread(() -> {
            Random rand = new Random();
            int vehicleCounter = vehicles.size();
            
            while (true) {
                try {
                    // Update hourly traffic data
                    updateHourlyTrafficData();
                    
                    // Wait between spawns
                    Thread.sleep(5000 + rand.nextInt(10000)); // 5-15 seconds
                    
                    // Clean up finished vehicles with detailed reporting
                    int beforeCleanup = vehicles.size();
                    vehicles.removeIf(vehicle -> {
                        boolean shouldRemove = !vehicle.isActive() && vehicle.getState() == Thread.State.TERMINATED;
                        if (shouldRemove) {
                            System.err.println("🧹 Removing completed vehicle: " + vehicle.getVehicleId() + 
                                             " (traveled " + String.format("%.2f", vehicle.getTotalDistanceTraveled()) + " km)");
                        }
                        return shouldRemove;
                    });
                    int afterCleanup = vehicles.size();
                    int removed = beforeCleanup - afterCleanup;
                    
                    if (removed > 0) {
                        System.err.println("🗑️ Cleaned up " + removed + " completed vehicles. Active vehicles: " + afterCleanup);
                    }
                    
                    // Report vehicle status
                    long activeVehicles = vehicles.stream().filter(Vehicle::isActive).count();
                    long idleVehicles = vehicles.stream().filter(v -> !v.isActive()).count();
                    System.err.println("📊 Vehicle status: " + activeVehicles + " active, " + idleVehicles + " idle, " + vehicles.size() + " total");
                    
                    // Add new vehicle if we have space and some vehicles completed
                    if (vehicles.size() < 20) { // Max 20 active vehicles
                        Vehicle newVehicle = createSingleVehicle("V" + (++vehicleCounter));
                        if (newVehicle != null) {
                            vehicles.add(newVehicle);
                            newVehicle.start();
                            System.err.println("🆕 Spawned new vehicle: " + newVehicle.getVehicleId() + 
                                             " (" + newVehicle.getVehicleType().getDisplayName() + ")");
                        }
                    } else {
                        System.err.println("🚫 Vehicle limit reached (20). Waiting for vehicles to complete...");
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        spawner.setDaemon(true);
        spawner.setName("VehicleSpawner");
        spawner.start();
        System.err.println("🏭 Vehicle spawner thread started");
    }
    
    private Vehicle createSingleVehicle(String vehicleId) {
        Random rand = new Random();
        
        // DEBUG: List all available roads
        System.err.println("🏗️ Available roads in system:");
        for (String roadId : roadMap.keySet()) {
            System.err.println("   - " + roadId);
        }
        
        // Define longer, more realistic connected routes
        String[][] routePatterns = {
            // SIMPLE TEST ROUTES - using only confirmed roads
            {"MAIN_EAST_1", "MAIN_EAST_2"},
            {"MAIN_WEST_1", "MAIN_WEST_2"},
            {"MAIN_NORTH_1", "MAIN_NORTH_2"},
            {"MAIN_SOUTH_1", "MAIN_SOUTH_2"},
            {"HIGHWAY_CURVE_1", "HIGHWAY_CURVE_2"},
            {"RES_NORTH_1", "RES_NORTH_2"},
            {"COM_CENTER_1", "COM_CENTER_2"},
            {"IND_FACTORY_1", "IND_FACTORY_2"},
            
            // Slightly longer routes  
            {"MAIN_EAST_1", "MAIN_EAST_2", "MAIN_WEST_1"},
            {"HIGHWAY_CURVE_1", "HIGHWAY_CURVE_2", "COM_MALL_ROAD"},
            {"RES_NORTH_1", "MAIN_NORTH_1", "MAIN_NORTH_2"},
            {"COM_CENTER_1", "COM_CENTER_2", "MAIN_SOUTH_1"}
        };
        
        // Pick a random route pattern
        String[] pattern = routePatterns[rand.nextInt(routePatterns.length)];
        
        List<Road> route = new ArrayList<>();
        List<TrafficLight> routeLights = new ArrayList<>();
        List<Intersection> routeIntersections = new ArrayList<>();

        // Build route from pattern and calculate route density
        double totalRouteDensity = 0.0;
        int validRoads = 0;
        
        System.err.println("🗺️ Building route for " + vehicleId + " using pattern: " + java.util.Arrays.toString(pattern));
        
        for (String roadId : pattern) {
            Road road = roadMap.get(roadId);
            if (road != null) {
                route.add(road);
                totalRouteDensity += road.getTrafficDensity();
                validRoads++;
                
                TrafficLight light = roadLightMap.get(road);
                if (light != null) {
                    routeLights.add(light);
                    System.err.println("✅ Added road: " + roadId + " with light: " + light.getLightId());
                } else {
                    System.err.println("⚠️ Road " + roadId + " has no traffic light!");
                }
            } else {
                System.err.println("❌ Road not found: " + roadId + " - skipping");
            }
        }
        
        System.err.println("📊 Route built: " + validRoads + " valid roads out of " + pattern.length + " requested");

        // Calculate average route density for reporting
        double averageRouteDensity = validRoads > 0 ? totalRouteDensity / validRoads : 0.0;

        // Add intersections based on route
        if (!route.isEmpty()) {
            // Add appropriate intersections for the route
            if (route.stream().anyMatch(r -> r.getId().contains("MAIN"))) {
                routeIntersections.add(intersectionMap.get("CENTER"));
            }
            if (route.stream().anyMatch(r -> r.getId().contains("RES"))) {
                routeIntersections.add(intersectionMap.get("RES_INT"));
            }
            if (route.stream().anyMatch(r -> r.getId().contains("COM"))) {
                routeIntersections.add(intersectionMap.get("COM_INT"));
            }
            if (route.stream().anyMatch(r -> r.getId().contains("IND"))) {
                routeIntersections.add(intersectionMap.get("IND_INT"));
            }

            // Ensure we have enough intersections for the route
            while (routeIntersections.size() < route.size()) {
                routeIntersections.add(intersectionMap.get("CENTER"));
            }

            Vehicle v = new Vehicle(vehicleId, route, routeLights, routeIntersections);
            v.setRouteMetrics(averageRouteDensity, estimateJourneyTime(route)); // Add route analytics
            
            // Set initial position on first road
            Road firstRoad = route.get(0);
            double startProgress = rand.nextDouble() * 0.1; // Start at very beginning
            double startX = firstRoad.getX1() + (firstRoad.getX2() - firstRoad.getX1()) * startProgress;
            double startY = firstRoad.getY1() + (firstRoad.getY2() - firstRoad.getY1()) * startProgress;
            v.setPosition(startX, startY);
            
            // Log route selection with density analysis
            System.err.println("🗺️ Vehicle " + vehicleId + " assigned route with " + route.size() + " roads");
            System.err.println("📊 Average route density: " + String.format("%.1f%%", averageRouteDensity * 100));
            
            return v;
        }
        
        return null;
    }
    
    private double estimateJourneyTime(List<Road> route) {
        double totalTime = 0.0;
        
        for (Road road : route) {
            // Base time calculation based on road length and speed limit
            double roadLength = Math.sqrt(
                Math.pow(road.getX2() - road.getX1(), 2) + 
                Math.pow(road.getY2() - road.getY1(), 2)
            );
            
            double baseTime = roadLength / road.getSpeedLimit() * 3.6; // Convert to seconds
            
            // Factor in traffic density
            double densityFactor = 1.0 + (road.getTrafficDensity() * 2.0); // Higher density = longer time
            
            totalTime += baseTime * densityFactor;
        }
        
        return totalTime;
    }
    
    // Traffic analysis and route recommendation system
    public String getTrafficAnalysis() {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🚦 TRAFFIC ANALYSIS REPORT\n");
        analysis.append("=" .repeat(50)).append("\n\n");
        
        // Analyze road congestion
        analysis.append("📊 ROAD CONGESTION LEVELS:\n");
        roads.stream()
            .sorted((r1, r2) -> Double.compare(r2.getTrafficDensity(), r1.getTrafficDensity()))
            .forEach(road -> {
                String densityLevel = getDensityLevel(road.getTrafficDensity());
                analysis.append(String.format("%-20s: %s (%.1f%%)\n", 
                    road.getId(), densityLevel, road.getTrafficDensity() * 100));
            });
            
        analysis.append("\n🚨 TRAFFIC HOTSPOTS:\n");
        roads.stream()
            .filter(road -> road.getTrafficDensity() > 0.7)
            .forEach(road -> {
                analysis.append(String.format("⚠️ %s - Heavy congestion (%.1f%%)\n", 
                    road.getId(), road.getTrafficDensity() * 100));
                analysis.append(getRouteRecommendation(road));
            });
            
        analysis.append("\n✅ RECOMMENDED ROUTES:\n");
        analysis.append(getBestRoutes());
        
        return analysis.toString();
    }
    
    private String getDensityLevel(double density) {
        if (density < 0.3) return "🟢 Light";
        else if (density < 0.6) return "🟡 Moderate"; 
        else if (density < 0.8) return "🟠 Heavy";
        else return "🔴 Severe";
    }
    
    private String getRouteRecommendation(Road congestedRoad) {
        StringBuilder rec = new StringBuilder();
        rec.append("   💡 Alternative routes:\n");
        
        // Find alternative roads with lower density
        roads.stream()
            .filter(road -> !road.getId().equals(congestedRoad.getId()))
            .filter(road -> road.getTrafficDensity() < congestedRoad.getTrafficDensity() - 0.2)
            .limit(2)
            .forEach(altRoad -> {
                rec.append(String.format("   → %s (%.1f%%)\n", 
                    altRoad.getId(), altRoad.getTrafficDensity() * 100));
            });
            
        return rec.toString();
    }
    
    private String getBestRoutes() {
        StringBuilder best = new StringBuilder();
        
        // Find low-density route combinations
        best.append("🏆 Fastest routes (Low congestion):\n");
        best.append("   1. RES_NORTH_1 → HIGHWAY_EAST → COM_CENTER_1\n");
        best.append("   2. HIGHWAY_WEST → IND_PORT_ROAD → IND_FACTORY_2\n");
        best.append("   3. RES_NORTH_2 → MAIN_NORTH → BUSINESS_CONNECT_2\n\n");
        
        best.append("⏰ Avoid during peak hours:\n");
        best.append("   • MAIN_SOUTH (90% congestion)\n");
        best.append("   • COM_MALL_ROAD (90% congestion)\n");
        best.append("   • MAIN_WEST (80% congestion)\n");
        
        return best.toString();
    }

    public void stopSimulation() {
        System.err.println("🛑 Stopping traffic simulation...");
        
        // Stop all vehicles
        System.err.println("🚗 Stopping " + vehicles.size() + " vehicles...");
        for (Vehicle vehicle : vehicles) {
            try {
                vehicle.stopVehicle();
                if (vehicle.isAlive()) {
                    vehicle.interrupt();
                    vehicle.join(1000); // Wait max 1 second for each vehicle
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Stop all traffic lights
        System.err.println("🚦 Stopping " + lights.size() + " traffic lights...");
        for (TrafficLight light : lights) {
            try {
                if (light.isAlive()) {
                    light.interrupt();
                    light.join(500); // Wait max 0.5 seconds for each light
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.err.println("✅ Traffic simulation stopped successfully");
    }

    public void resetSimulation(int newVehicleCount) {
        System.err.println("🔄 Resetting traffic simulation...");
        
        // First stop everything
        stopSimulation();
        
        // Wait a moment for cleanup
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Clear all collections and reset
        vehicles.clear();
        lights.clear();
        roads.clear();
        intersections.clear();
        roadMap.clear();
        intersectionMap.clear();
        roadLightMap.clear();
        
        // Setup fresh simulation
        setupConnectedCityNetwork(newVehicleCount);
        
        System.err.println("✅ Traffic simulation reset with " + newVehicleCount + " vehicles");
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

    public Map<String, Zone> getZones() {
        Map<String, Zone> zoneMap = new HashMap<>();
        for (Zone zone : zones) {
            zoneMap.put(zone.getId(), zone);
        }
        return zoneMap;
    }

    // Time-based traffic analysis methods
    public void updateHourlyTrafficData() {
        long currentTime = System.currentTimeMillis();
        int newHour = calculateCurrentHour(currentTime);
        
        // If hour changed, update traffic data
        if (newHour != currentSimulationHour) {
            currentSimulationHour = newHour;
            System.err.println("🕐 Simulation time: " + formatHour(currentSimulationHour));
        }
        
        // Record current traffic density for each road
        for (Road road : roads) {
            String roadId = road.getId();
            double currentDensity = road.getCurrentTrafficDensity();
            
            hourlyTrafficData.computeIfAbsent(roadId, k -> new HashMap<>())
                           .put(currentSimulationHour, currentDensity);
        }
        
        // Record total vehicle count for this hour
        hourlyVehicleCount.put(currentSimulationHour, vehicles.size());
    }
    
    private int calculateCurrentHour(long currentTime) {
        long elapsedMs = currentTime - simulationStartTime;
        int elapsedHours = (int) (elapsedMs / HOUR_DURATION_MS);
        return (6 + elapsedHours) % 24; // Start at 6 AM, cycle through 24 hours
    }
    
    private String formatHour(int hour) {
        return String.format("%02d:00", hour);
    }
    
    public String getOptimalDepartureTime(String startRoadId, String endRoadId) {
        StringBuilder analysis = new StringBuilder();
        analysis.append("🚗 OPTIMAL DEPARTURE TIME ANALYSIS\n");
        analysis.append("=".repeat(50)).append("\n");
        analysis.append("Route: ").append(startRoadId).append(" → ").append(endRoadId).append("\n\n");
        
        Map<Integer, Double> hourlyScores = new HashMap<>();
        
        // Calculate score for each hour (lower is better)
        for (int hour = 0; hour < 24; hour++) {
            double totalDensity = 0.0;
            int roadCount = 0;
            
            // Get average density for roads in typical route
            for (Road road : roads) {
                Map<Integer, Double> roadHourlyData = hourlyTrafficData.get(road.getId());
                if (roadHourlyData != null && roadHourlyData.containsKey(hour)) {
                    totalDensity += roadHourlyData.get(hour);
                    roadCount++;
                }
            }
            
            double avgDensity = roadCount > 0 ? totalDensity / roadCount : 0.5;
            hourlyScores.put(hour, avgDensity);
            
            String timeStr = formatHour(hour);
            String densityLevel = getDensityLevel(avgDensity);
            analysis.append(String.format("%-8s: %s (%.1f%% density)\n", 
                           timeStr, densityLevel, avgDensity * 100));
        }
        
        // Find best times (lowest density)
        analysis.append("\n🏆 RECOMMENDED DEPARTURE TIMES:\n");
        hourlyScores.entrySet().stream()
                   .sorted(Map.Entry.comparingByValue())
                   .limit(5)
                   .forEach(entry -> {
                       String time = formatHour(entry.getKey());
                       String level = getDensityLevel(entry.getValue());
                       analysis.append(String.format("✅ %s - %s (%.1f%%)\n", 
                                     time, level, entry.getValue() * 100));
                   });
        
        // Find worst times (highest density)
        analysis.append("\n🚨 AVOID THESE TIMES:\n");
        hourlyScores.entrySet().stream()
                   .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
                   .limit(3)
                   .forEach(entry -> {
                       String time = formatHour(entry.getKey());
                       String level = getDensityLevel(entry.getValue());
                       analysis.append(String.format("❌ %s - %s (%.1f%%)\n", 
                                     time, level, entry.getValue() * 100));
                   });
        
        return analysis.toString();
    }
    
    public String getHourlyTrafficReport() {
        StringBuilder report = new StringBuilder();
        report.append("📊 HOURLY TRAFFIC DENSITY REPORT\n");
        report.append("=".repeat(50)).append("\n");
        report.append("Current time: ").append(formatHour(currentSimulationHour)).append("\n\n");
        
        // Show traffic patterns for major roads
        String[] majorRoads = {"MAIN_EAST_1", "MAIN_WEST_1", "MAIN_NORTH_1", "MAIN_SOUTH_1", "HIGHWAY_CURVE_1"};
        
        for (String roadId : majorRoads) {
            Road road = roadMap.get(roadId);
            if (road != null) {
                report.append("🛣️ ").append(roadId).append(":\n");
                Map<Integer, Double> roadData = hourlyTrafficData.get(roadId);
                
                if (roadData != null && !roadData.isEmpty()) {
                    for (int hour = 6; hour <= 23; hour++) {
                        Double density = roadData.get(hour);
                        if (density != null) {
                            String level = getDensityLevel(density);
                            report.append(String.format("   %s: %s (%.1f%%)\n", 
                                        formatHour(hour), level, density * 100));
                        }
                    }
                } else {
                    report.append("   No data available yet\n");
                }
                report.append("\n");
            }
        }
        
        return report.toString();
    }
}
