package core;

import java.util.concurrent.Semaphore;

public class Intersection {
    private final String id;
    private final Semaphore access;

    public Intersection(String id) {
        this.id = id;
        this.access = new Semaphore(1);  // aynı anda sadece 1 araç girebilsin
    }

    public void enter(String vehicleId) {
        try {
            System.out.println("Vehicle " + vehicleId + " is waiting to enter intersection " + id);
            access.acquire();  // erişim izni alır (beklerse bloklanır)
            System.out.println("Vehicle " + vehicleId + " entered intersection " + id);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void exit(String vehicleId) {
        System.out.println("Vehicle " + vehicleId + " exited intersection " + id);
        access.release();  // erişim izni bırakılır
    }

    public String getId() {
        return id;
    }
}
