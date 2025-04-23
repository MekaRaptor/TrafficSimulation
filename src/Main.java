import core.Road;
import core.Vehicle;

public class Main {
    public static void main(String[] args) {
        Road road1 = new Road("R1", 1);  // Maksimum 1 araç
        Vehicle car1 = new Vehicle("A1", road1);
        Vehicle car2 = new Vehicle("B2", road1);

        car1.start();
        car2.start();
    }
}
