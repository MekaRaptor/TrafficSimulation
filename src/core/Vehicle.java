package core;

public class Vehicle extends Thread {
    private final String id;
    private int position;
    private boolean active;
    private final Road road;
    private final TrafficLight trafficLight;
    private final Intersection intersection; 

    public Vehicle(String id, Road road, TrafficLight trafficLight, Intersection intersection) {
        this.id = id;
        this.road = road;
        this.trafficLight = trafficLight;
        this.intersection = intersection;
        this.position = 0;
        this.active = true;
    }
    

    @Override
    public void run() {
        while (active) {
            try {
                // TRAFİK IŞIĞI KONTROLÜ
                if (trafficLight.getTrafficLightState() == TrafficLight.State.GREEN) {
                    if (road.addVehicle(this)) {
                        intersection.enter(id);  // kavşağa giriş
                        move();
                        intersection.exit(id);  // kavşaktan çıkış
                        road.removeVehicle(this);
                    } else {
                        System.out.println("Vehicle " + id + " is waiting: road " + road.getId() + " is full.");
                    }
                }
                else {
                    System.out.println("Vehicle " + id + " is waiting at light: " + trafficLight.getTrafficLightState());
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
