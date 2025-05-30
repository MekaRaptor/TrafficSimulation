package src.core;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class TrafficLight extends Thread {
    public enum State { GREEN, YELLOW, RED, BLINKING }
    private State state = State.RED;
    private final Road road;
    private static final AtomicInteger activeGreenLights = new AtomicInteger(0);
    private static final int MAX_CONCURRENT_GREEN = 2;
    private static final Random random = new Random();
    
    // Timing constants
    private static final int MIN_GREEN_TIME = 3000;  // 3 seconds
    private static final int MAX_GREEN_TIME = 8000;  // 8 seconds
    private static final int YELLOW_TIME = 2000;     // 2 seconds
    private static final int MIN_RED_TIME = 3000;    // 3 seconds
    private static final int MAX_RED_TIME = 7000;    // 7 seconds
    
    // Visual properties
    private double brightness = 1.0;
    private boolean isBlinking = false;
    private long lastStateChangeTime;
    private long blinkInterval = 500; // milliseconds
    private boolean emergencyMode = false;

    public TrafficLight(Road road) {
        this.road = road;
        this.lastStateChangeTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Check for emergency mode (rare random event)
                if (random.nextDouble() < 0.01 && !emergencyMode) {
                    emergencyMode = true;
                    handleEmergencyMode();
                    emergencyMode = false;
                    continue;
                }
                
                // Calculate green light duration based on traffic density
                int greenTime = calculateGreenTime();
                
                // Wait if too many green lights are active
                while (activeGreenLights.get() >= MAX_CONCURRENT_GREEN) {
                    Thread.sleep(500);
                    if (Thread.currentThread().isInterrupted()) return;
                }

                // Switch to green
                activeGreenLights.incrementAndGet();
                setState(State.GREEN);
                Thread.sleep(greenTime);
                if (Thread.currentThread().isInterrupted()) return;

                // Switch to yellow
                setState(State.YELLOW);
                Thread.sleep(YELLOW_TIME);
                if (Thread.currentThread().isInterrupted()) return;

                // Switch to red
                setState(State.RED);
                activeGreenLights.decrementAndGet();
                Thread.sleep(calculateRedTime());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void handleEmergencyMode() throws InterruptedException {
        System.out.println("Emergency mode activated for traffic light on road " + road.getId());
        
        // Blink yellow for emergency
        for (int i = 0; i < 10; i++) {
            if (Thread.currentThread().isInterrupted()) return;
            setState(State.YELLOW);
            Thread.sleep(300);
            if (Thread.currentThread().isInterrupted()) return;
            setState(State.BLINKING);
            Thread.sleep(300);
        }
    }

    private int calculateGreenTime() {
        int vehicleCount = road.getVehicleCount();
        int baseTime = MIN_GREEN_TIME;
        
        // Increase time based on vehicle count
        baseTime += Math.min(vehicleCount * 500, MAX_GREEN_TIME - MIN_GREEN_TIME);
        
        // Add some randomness for realism
        baseTime += random.nextInt(1000) - 500;
        
        return Math.max(MIN_GREEN_TIME, Math.min(baseTime, MAX_GREEN_TIME));
    }

    private int calculateRedTime() {
        int baseTime = MIN_RED_TIME;
        
        // Add traffic-dependent variation
        int vehicleCount = road.getVehicleCount();
        if (vehicleCount > 3) {
            // Shorter red time for busy roads
            baseTime = MIN_RED_TIME;
        } else {
            // Longer red time for less busy roads
            baseTime = (MIN_RED_TIME + MAX_RED_TIME) / 2;
        }
        
        // Add some randomness
        baseTime += random.nextInt(2000) - 1000;
        
        return Math.max(MIN_RED_TIME, Math.min(baseTime, MAX_RED_TIME));
    }

    public synchronized State getTrafficLightState() {
        // Handle blinking effect
        if (isBlinking) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastStateChangeTime > blinkInterval) {
                brightness = (brightness < 0.5) ? 1.0 : 0.3;
                lastStateChangeTime = currentTime;
            }
            return state == State.BLINKING ? State.YELLOW : state;
        }
        return state;
    }
    
    public synchronized double getBrightness() {
        return brightness;
    }
    
    public synchronized boolean isEmergencyMode() {
        return emergencyMode;
    }

    private synchronized void setState(State newState) {
        this.state = newState;
        this.lastStateChangeTime = System.currentTimeMillis();
        
        // Set blinking state
        isBlinking = (newState == State.BLINKING);
        
        System.out.println("Traffic light on road " + road.getId() + " changed to: " + newState);
    }

    public String getLightId() {
        return road.getId();
    }
}
