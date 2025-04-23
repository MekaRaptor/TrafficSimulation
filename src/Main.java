// src/Main.java
import core.Vehicle;

public class Main {
    public static void main(String[] args) {
        Vehicle car1 = new Vehicle("A1");
        Vehicle car2 = new Vehicle("B2");

        car1.start();
        car2.start();
    }
}
