package core;

public class Vehicle extends Thread {
    private final String id;
    private int position;
    private boolean active;
    private final Road road;

    public Vehicle(String id, Road road) {
        this.id = id;
        this.road = road;
        this.position = 0;
        this.active = true;
    }

    @Override
    public void run() {
        while (active) {
            try {
                // Yol müsaitse hareket et, değilse bekle
                if (road.addVehicle(this)) {
                    move();
                    road.removeVehicle(this); // hareketten sonra yoldan çık
                } else {
                    System.out.println("Vehicle " + id + " is waiting: road " + road.getId() + " is full.");
                }

                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void move() {
        position++;
        System.out.println("Vehicle " + id + " moved to position " + position + " on road " + road.getId());
    }

    public String getVehicleId() {
        return id;
    }

    public void stopVehicle() {
        active = false;
    }
}
