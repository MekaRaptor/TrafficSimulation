package src.core;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Core Traffic Simulation Engine
 * Pure Java simulation logic - No web dependencies
 */
public class SimulationEngine {
    private final CityMap cityMap;
    private final AtomicLong simulationTime = new AtomicLong(0);
    private volatile boolean running = false;
    private final List<SimulationListener> listeners = new CopyOnWriteArrayList<>();
    
    // Performance metrics
    private final Map<String, Double> metrics = new ConcurrentHashMap<>();
    private long lastUpdateTime = System.currentTimeMillis();
    
    public SimulationEngine() {
        this.cityMap = new CityMap();
        initializeMetrics();
    }
    
    /**
     * Initialize simulation with realistic city layout
     */
    public void initialize(int vehicleCount) {
        System.out.println("🏭 Initializing Simulation Engine...");
        cityMap.setupRealisticCity(vehicleCount);
        updateMetrics();
        notifyListeners(SimulationEvent.INITIALIZED);
        System.out.println("✅ Simulation Engine ready!");
    }
    
    /**
     * Start the simulation
     */
    public synchronized void start() {
        if (running) return;
        
        try {
            running = true;
            simulationTime.set(0);
            System.err.println("🔧 Starting cityMap simulation...");
            cityMap.startSimulation();
            System.err.println("✅ CityMap started successfully");
            
            // Start metrics update thread
            Thread metricsThread = new Thread(this::metricsUpdateLoop);
            metricsThread.setDaemon(true);
            metricsThread.start();
            System.err.println("✅ Metrics thread started");
            
            notifyListeners(SimulationEvent.STARTED);
            System.err.println("🚀 Simulation Engine started! Running = " + running);
            
        } catch (Exception e) {
            System.err.println("❌ Error starting simulation: " + e.getMessage());
            e.printStackTrace();
            running = false;
        }
    }
    
    /**
     * Stop the simulation
     */
    public synchronized void stop() {
        if (!running) return;
        
        running = false;
        cityMap.stopSimulation();
        notifyListeners(SimulationEvent.STOPPED);
        System.out.println("🛑 Simulation Engine stopped!");
    }
    
    /**
     * Reset simulation
     */
    public synchronized void reset(int vehicleCount) {
        boolean wasRunning = running;
        stop();
        
        try {
            Thread.sleep(1000); // Give time for cleanup
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        initialize(vehicleCount);
        
        if (wasRunning) {
            start();
        }
        
        notifyListeners(SimulationEvent.RESET);
        System.out.println("🔄 Simulation Engine reset!");
    }
    
    /**
     * Get current simulation state
     */
    public SimulationState getState() {
        return new SimulationState(
            running,
            simulationTime.get(),
            cityMap.getVehicles(),
            cityMap.getRoads(),
            cityMap.getIntersections(),
            cityMap.getLights(),
            cityMap.getZones(),
            new HashMap<>(metrics),
            cityMap
        );
    }
    
    /**
     * Add simulation event listener
     */
    public void addListener(SimulationListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove simulation event listener
     */
    public void removeListener(SimulationListener listener) {
        listeners.remove(listener);
    }
    
    private void metricsUpdateLoop() {
        while (running) {
            try {
                updateMetrics();
                notifyListeners(SimulationEvent.METRICS_UPDATED);
                Thread.sleep(1000); // Update every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void updateMetrics() {
        long currentTime = System.currentTimeMillis();
        simulationTime.addAndGet(currentTime - lastUpdateTime);
        lastUpdateTime = currentTime;
        
        List<Vehicle> vehicles = cityMap.getVehicles();
        List<Road> roads = cityMap.getRoads();
        
        // Vehicle metrics
        long activeVehicles = vehicles.stream().mapToLong(v -> v.isActive() ? 1 : 0).sum();
        double avgSpeed = vehicles.stream()
            .filter(Vehicle::isActive)
            .mapToDouble(Vehicle::getSpeed)
            .average().orElse(0.0);
        
        // Road metrics
        double totalCongestion = roads.stream()
            .mapToDouble(Road::getCongestionLevel)
            .sum();
        double avgCongestion = roads.isEmpty() ? 0 : totalCongestion / roads.size();
        
        // Traffic light metrics
        long redLights = cityMap.getLights().stream()
            .mapToLong(light -> light.getCurrentState() == TrafficLight.State.RED ? 1 : 0)
            .sum();
        
        // Update metrics map
        metrics.put("activeVehicles", (double) activeVehicles);
        metrics.put("totalVehicles", (double) vehicles.size());
        metrics.put("averageSpeed", avgSpeed);
        metrics.put("totalRoads", (double) roads.size());
        metrics.put("averageCongestion", avgCongestion * 100); // Percentage
        metrics.put("redLightsCount", (double) redLights);
        metrics.put("totalLights", (double) cityMap.getLights().size());
        metrics.put("totalZones", (double) cityMap.getZones().size());
        metrics.put("totalIntersections", (double) cityMap.getIntersections().size());
        metrics.put("simulationTime", (double) simulationTime.get());
    }
    
    private void initializeMetrics() {
        metrics.put("activeVehicles", 0.0);
        metrics.put("totalVehicles", 0.0);
        metrics.put("averageSpeed", 0.0);
        metrics.put("totalRoads", 0.0);
        metrics.put("averageCongestion", 0.0);
        metrics.put("redLightsCount", 0.0);
        metrics.put("totalLights", 0.0);
        metrics.put("totalZones", 0.0);
        metrics.put("totalIntersections", 0.0);
        metrics.put("simulationTime", 0.0);
    }
    
    private void notifyListeners(SimulationEvent event) {
        for (SimulationListener listener : listeners) {
            try {
                listener.onSimulationEvent(event, getState());
            } catch (Exception e) {
                System.err.println("Error notifying listener: " + e.getMessage());
            }
        }
    }
    
    // Simulation event types
    public enum SimulationEvent {
        INITIALIZED, STARTED, STOPPED, RESET, METRICS_UPDATED, VEHICLE_SPAWNED, VEHICLE_FINISHED
    }
    
    // Simulation listener interface
    public interface SimulationListener {
        void onSimulationEvent(SimulationEvent event, SimulationState state);
    }
    
    // Simulation state container
    public static class SimulationState {
        private final boolean running;
        private final long simulationTime;
        private final List<Vehicle> vehicles;
        private final List<Road> roads;
        private final List<Intersection> intersections;
        private final List<TrafficLight> lights;
        private final Map<String, CityMap.Zone> zones;
        private final Map<String, Double> metrics;
        private final CityMap cityMap;
        
        public SimulationState(boolean running, long simulationTime, 
                             List<Vehicle> vehicles, List<Road> roads,
                             List<Intersection> intersections, List<TrafficLight> lights,
                             Map<String, CityMap.Zone> zones, Map<String, Double> metrics) {
            this.running = running;
            this.simulationTime = simulationTime;
            this.vehicles = new ArrayList<>(vehicles);
            this.roads = new ArrayList<>(roads);
            this.intersections = new ArrayList<>(intersections);
            this.lights = new ArrayList<>(lights);
            this.zones = new HashMap<>(zones);
            this.metrics = new HashMap<>(metrics);
            this.cityMap = null; // Will be set separately
        }
        
        public SimulationState(boolean running, long simulationTime, 
                             List<Vehicle> vehicles, List<Road> roads,
                             List<Intersection> intersections, List<TrafficLight> lights,
                             Map<String, CityMap.Zone> zones, Map<String, Double> metrics,
                             CityMap cityMap) {
            this.running = running;
            this.simulationTime = simulationTime;
            this.vehicles = new ArrayList<>(vehicles);
            this.roads = new ArrayList<>(roads);
            this.intersections = new ArrayList<>(intersections);
            this.lights = new ArrayList<>(lights);
            this.zones = new HashMap<>(zones);
            this.metrics = new HashMap<>(metrics);
            this.cityMap = cityMap;
        }
        
        // Getters
        public boolean isRunning() { return running; }
        public long getSimulationTime() { return simulationTime; }
        public List<Vehicle> getVehicles() { return vehicles; }
        public List<Road> getRoads() { return roads; }
        public List<Intersection> getIntersections() { return intersections; }
        public List<TrafficLight> getLights() { return lights; }
        public Map<String, CityMap.Zone> getZones() { return zones; }
        public Map<String, Double> getMetrics() { return metrics; }
        public CityMap getCityMap() { return cityMap; }
    }
} 