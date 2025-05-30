package src.core;

public class Main {
    public static void main(String[] args) {
        System.out.println("===============================================");
        System.out.println("       TRAFFIC SIMULATION DEMO");
        System.out.println("===============================================");
        System.out.println();
        
        // Ask user which simulation to run
        System.out.println("Choose simulation type:");
        System.out.println("1. Console Demo (No GUI needed)");
        System.out.println("2. Full GUI (Requires JavaFX)");
        System.out.println();
        
        // For now, run console demo
        System.out.println("Running Console Demo...");
        System.out.println();
        
        runConsoleDemo();
    }
    
    private static void runConsoleDemo() {
        try {
            System.out.println("🏗️ Creating realistic city...");
            CityMap cityMap = new CityMap();
            
            // Setup realistic city with 15 vehicles
            cityMap.setupRealisticCity(15);
            
            System.out.println("✅ City created successfully!");
            System.out.println("📊 Statistics:");
            System.out.println("   - Roads: " + cityMap.getRoads().size());
            System.out.println("   - Intersections: " + cityMap.getIntersections().size());
            System.out.println("   - Traffic Lights: " + cityMap.getLights().size());
            System.out.println("   - Vehicles: " + cityMap.getVehicles().size());
            System.out.println("   - Zones: " + cityMap.getZones().size());
            
            System.out.println();
            System.out.println("🚀 Starting traffic simulation...");
            cityMap.startSimulation();
            
            System.out.println("⏱️ Simulation running for 30 seconds...");
            System.out.println("   (Watch the console for vehicle movements)");
            System.out.println();
            
            // Let simulation run for 30 seconds
            Thread.sleep(30000);
            
            System.out.println();
            System.out.println("🛑 Stopping simulation...");
            cityMap.stopSimulation();
            
            // Show final statistics
            System.out.println();
            System.out.println("📈 Final Statistics:");
            int activeVehicles = 0;
            long totalWaitTime = 0;
            
            for (Vehicle vehicle : cityMap.getVehicles()) {
                if (vehicle.isActive()) {
                    activeVehicles++;
                    totalWaitTime += vehicle.getTotalWaitTime();
                }
            }
            
            System.out.println("   - Active vehicles: " + activeVehicles);
            System.out.println("   - Average wait time: " + (totalWaitTime / Math.max(1, activeVehicles) / 1000) + " seconds");
            
            // Show road congestion
            System.out.println("   - Road congestion levels:");
            for (Road road : cityMap.getRoads()) {
                if (road.getVehicleCount() > 0) {
                    double congestion = (double) road.getVehicleCount() / road.getCapacity() * 100;
                    System.out.println("     * " + road.getId() + " (" + road.getRoadType().getDisplayName() + "): " + 
                                     String.format("%.1f%%", congestion));
                }
            }
            
            System.out.println();
            System.out.println("✅ Console demo completed!");
            System.out.println();
            System.out.println("💡 To run the full GUI version:");
            System.out.println("   1. Use 'auto_download_and_run.bat' to automatically download JavaFX");
            System.out.println("   2. Or manually download JavaFX and use 'run_traffic_simulation.bat'");
            
        } catch (Exception e) {
            System.err.println("❌ Error during simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 