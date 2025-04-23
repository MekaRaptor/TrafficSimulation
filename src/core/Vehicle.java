// src/core/Vehicle.java
package core;

public class Vehicle extends Thread {
    private final String id;
    private int position;
    private boolean active;

    public Vehicle(String id) {
        this.id = id;
        this.position = 0;
        this.active = true;
    }

    @Override
    public void run() {
        while (active) {
            move();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void move() {
        position++;
        System.out.println("Vehicle " + id + " moved to position " + position);
    }

    public String getVehicleId() {
        return id;
    }

    public void stopVehicle() {
        active = false;
    }
}
