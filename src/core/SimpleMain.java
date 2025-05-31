public class SimpleMain {
    public static void main(String[] args) {
        
        // traffic controller
        TrafficController controller = new TrafficController();
        
        // Start 
        controller.startSimulation();
        
        // Let it run for 30 seconds
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Stop simulation
        controller.stopSimulation();
        
        
    }
} 