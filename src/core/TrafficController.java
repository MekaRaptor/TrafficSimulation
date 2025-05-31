import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

public class TrafficController {
    // Configuration
    private static final int MAX_VEHICLES = 15;
    private static final long VEHICLE_SPAWN_INTERVAL = 500; // 1 second
    private static final int THREAD_POOL_SIZE = 15;
    
    // Threadler
    private final ThreadPoolExecutor vehicleThreadPool;
    private final Thread k1ManagerThread;
    private final Thread k2ManagerThread;
    private final Thread vehicleSpawnerThread;
    private final Thread guiUpdaterThread;
    
    // Roads and Intersections
    private final List<Road> roads;
    private final IntersectionManager k1Manager;
    private final IntersectionManager k2Manager;
    
    // Vehicle Management
    private final List<Vehicle> activeVehicles;
    private final List<Vehicle> completedVehicles;
    private final AtomicInteger vehicleIdCounter;
    private volatile boolean running;
    
    // GUI Interface
    private Object gui; // Changed from TrafficSimulationGUI to Object to avoid JavaFX dependency
    
    public TrafficController() {
        // Initialize roads (3 parallel roads from K1 to K2)
        this.roads = Arrays.asList(
            new Road(1, "North Road"),
            new Road(2, "Middle Road"), 
            new Road(3, "South Road")
        );
        
        // Initialize intersection managers
        this.k1Manager = new IntersectionManager("K1", roads, true);  // K1 is entry point
        this.k2Manager = new IntersectionManager("K2", roads, false); // K2 is exit point
        
        // Initialize vehicle management
        this.activeVehicles = new ArrayList<>();
        this.completedVehicles = new ArrayList<>();
        this.vehicleIdCounter = new AtomicInteger(0);
        
        // Initialize thread pool for vehicles
        this.vehicleThreadPool = new ThreadPoolExecutor(
            THREAD_POOL_SIZE, THREAD_POOL_SIZE,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "Vehicle-Thread-" + threadNumber.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            }
        );
        
        // Initialize manager threads
        this.k1ManagerThread = new Thread(k1Manager, "K1-Manager");
        this.k2ManagerThread = new Thread(k2Manager, "K2-Manager");
        
        // Initialize spawner and GUI updater threads
        this.vehicleSpawnerThread = new Thread(this::vehicleSpawnerLoop, "Vehicle-Spawner");
        this.guiUpdaterThread = new Thread(this::guiUpdaterLoop, "GUI-Updater");
        
        this.running = false;
        
        System.out.println("🚀 TrafficController initialized");
        System.out.println("   📍 Roads: " + roads.size());
        System.out.println("   🚦 Intersections: K1 (entry), K2 (exit)");
        System.out.println("   🧵 Thread Pool Size: " + THREAD_POOL_SIZE);
    }
    
    /**
     * Start the simulation
     */
    public void startSimulation() {
        if (running) {
            System.out.println("⚠️ Simulation is already running");
            return;
        }
        
        running = true;
        System.out.println("🚀 Starting Traffic Simulation...");
        
        // Start intersection managers
        k1ManagerThread.start();
        k2ManagerThread.start();
        
        // Start vehicle spawner
        vehicleSpawnerThread.start();
        
        // Start GUI updater
        guiUpdaterThread.start();
        
        System.out.println("✅ Simulation started successfully");
    }
    
    /**
     * Stop the simulation
     */
    public void stopSimulation() {
        if (!running) {
            System.out.println("⚠️ Simulation is not running");
            return;
        }
        
        System.out.println("🛑 Stopping Traffic Simulation...");
        running = false;
        
        // Stop intersection managers
        k1Manager.stop();
        k2Manager.stop();
        
        // Stop all active vehicles
        synchronized (activeVehicles) {
            for (Vehicle vehicle : activeVehicles) {
                vehicle.stop();
            }
        }
        
        // Shutdown thread pool
        vehicleThreadPool.shutdown();
        try {
            if (!vehicleThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                vehicleThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            vehicleThreadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Interrupt manager threads
        k1ManagerThread.interrupt();
        k2ManagerThread.interrupt();
        vehicleSpawnerThread.interrupt();
        guiUpdaterThread.interrupt();
        
        System.out.println("✅ Simulation stopped successfully");
        printFinalStatistics();
    }
    
    /**
     * Vehicle spawner loop - creates new vehicles every second
     */
    private void vehicleSpawnerLoop() {
        System.out.println("🚗 Vehicle spawner started");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                synchronized (activeVehicles) {
                    if (activeVehicles.size() < MAX_VEHICLES) {
                        // Create new vehicle
                        String vehicleId = "V" + vehicleIdCounter.getAndIncrement();
                        Vehicle newVehicle = new Vehicle(vehicleId, this);
                        
                        // Submit to thread pool
                        vehicleThreadPool.submit(newVehicle);
                        activeVehicles.add(newVehicle);
                        
                        System.out.println("🚗 Spawned " + vehicleId + " (" + newVehicle.getType().getDisplayName() + 
                                         "). Active vehicles: " + activeVehicles.size());
                    }
                }
                
                Thread.sleep(VEHICLE_SPAWN_INTERVAL);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("🚗 Vehicle spawner stopped");
    }
    
    /**
     * GUI updater loop - updates GUI every second
     */
    private void guiUpdaterLoop() {
        System.out.println("🖥️ GUI updater started");
        
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Update GUI if available (skip for console version)
                // if (gui != null) {
                //     gui.updateSimulation();
                // }
                
                Thread.sleep(1000); // Update every second
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("🖥️ GUI updater stopped");
    }
    
    /**
     * Called when a vehicle reaches destination B
     */
    public void vehicleReachedB(Vehicle vehicle) {
        synchronized (activeVehicles) {
            boolean removed = activeVehicles.remove(vehicle);
            if (removed) {
                completedVehicles.add(vehicle);
            }
        }
        
        System.out.println("🏁 Vehicle " + vehicle.getId() + " completed journey. " +
                          "Active: " + activeVehicles.size() + ", Completed: " + completedVehicles.size());
    }
    
    /**
     * Print final statistics
     */
    private void printFinalStatistics() {
        System.out.println("\n📊 FINAL SIMULATION STATISTICS:");
        System.out.println("   🚗 Total vehicles spawned: " + (vehicleIdCounter.get() - 1));
        System.out.println("   ✅ Vehicles completed: " + completedVehicles.size());
        System.out.println("   🔄 Vehicles still active: " + activeVehicles.size());
        
        if (!completedVehicles.isEmpty()) {
            double avgTravelTime = completedVehicles.stream()
                .mapToLong(Vehicle::getTotalTravelTime)
                .average()
                .orElse(0.0);
            System.out.println("   ⏱️ Average travel time: " + String.format("%.2f", avgTravelTime) + "ms");
        }
        
        System.out.println("   🛣️ Road final status:");
        for (Road road : roads) {
            System.out.println("      " + road.getName() + ": " + road.getVehicleCount() + " vehicles");
        }
    }
    
    // Getters for simulation state
    public IntersectionManager getK1Manager() { return k1Manager; }
    public IntersectionManager getK2Manager() { return k2Manager; }
    public List<Road> getRoads() { return roads; }
    public List<Vehicle> getActiveVehicles() { 
        synchronized (activeVehicles) {
            return new ArrayList<>(activeVehicles); 
        }
    }
    public List<Vehicle> getCompletedVehicles() { return new ArrayList<>(completedVehicles); }
    public boolean isRunning() { return running; }
    
    // GUI interface
    public void setGUI(Object gui) {
        this.gui = gui;
    }
    
    // GUI Interface Methods
    public int getActiveVehicleCount() {
        return activeVehicles.size();
    }
    
    public int getTotalVehiclesSpawned() {
        return vehicleIdCounter.get() - 1;
    }
    
    public Map<Integer, Integer> getRoadCongestion() {
        Map<Integer, Integer> congestion = new HashMap<>();
        for (Road road : roads) {
            congestion.put(road.getRoadId(), road.getVehicleCount());
        }
        return congestion;
    }

    public static void main(String[] args) {
        TrafficController controller = new TrafficController();
        controller.startSimulation();

        try {
            Thread.sleep(30000); // Run for 30 seconds
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        controller.stopSimulation();
    }
} 