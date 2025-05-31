public class SimpleMain {
    public static void main(String[] args) {
        System.out.println("🚀 Starting Traffic Simulation (Console Version)...");
        
        // Create traffic controller
        TrafficController controller = new TrafficController();
        
        // Start simulation
        controller.startSimulation();
        
        // Let it run for 30 seconds
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop simulation
        System.out.println("🛑 Stopping simulation...");
        controller.stopSimulation();
        
        System.out.println("✅ Simulation completed!");
    }
} 