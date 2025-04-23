import core.Intersection;
import core.Road;
import core.TrafficLight;
import core.Vehicle;

public class Main {
    public static void main(String[] args) {
        Road road1 = new Road("R1", 1);  // Maksimum 1 araç
        TrafficLight light = new TrafficLight();
        Intersection intersection = new Intersection("X1");
        
        Vehicle car1 = new Vehicle("A1", road1, light, intersection);
        Vehicle car2 = new Vehicle("B2", road1, light, intersection);
        

        light.start();
        car1.start();
        car2.start();
    }
}
