package core;

public class TrafficLight extends Thread {
    public static enum State { GREEN, YELLOW, RED }
    private State state = State.RED;

    @Override
    public void run() {
        while (true) {
            try {
                setState(State.GREEN);
                Thread.sleep(5000);
                setState(State.YELLOW);
                Thread.sleep(2000);
                setState(State.RED);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public synchronized State getTrafficLightState() {
        return state;
    }

    private synchronized void setState(State newState) {
        this.state = newState;
        System.out.println("Traffic light changed to: " + newState);
    }
}
