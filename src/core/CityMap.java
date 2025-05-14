import java.util.*;

public class CityMap {
    private final List<Road> roads = new ArrayList<>();
    private final List<Intersection> intersections = new ArrayList<>();
    private final List<TrafficLight> lights = new ArrayList<>();
    private final List<Vehicle> vehicles = new ArrayList<>();
    private final Map<String, Road> roadMap = new HashMap<>();
    private final Map<String, Intersection> intersectionMap = new HashMap<>();
    private final Map<Road, TrafficLight> roadLightMap = new HashMap<>();

    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    }

    public void setupGridCity(int gridSize, int vehicleCount) {
        roads.clear();
        intersections.clear();
        lights.clear();
        vehicles.clear();
        roadMap.clear();
        intersectionMap.clear();
        roadLightMap.clear();

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                Road hRoad = new Road("R" + i + "-" + j, 2, Direction.EAST);
                TrafficLight hLight = new TrafficLight(hRoad);
                roads.add(hRoad);
                lights.add(hLight);
                roadMap.put(hRoad.getId(), hRoad);
                roadLightMap.put(hRoad, hLight);

                Road vRoad = new Road("C" + i + "-" + j, 2, Direction.SOUTH);
                TrafficLight vLight = new TrafficLight(vRoad);
                roads.add(vRoad);
                lights.add(vLight);
                roadMap.put(vRoad.getId(), vRoad);
                roadLightMap.put(vRoad, vLight);

                Intersection intersection = new Intersection("X" + i + "-" + j);
                intersections.add(intersection);
                intersectionMap.put(intersection.getId(), intersection);
            }
        }

        Random rand = new Random();

        for (int i = 0; i < vehicleCount; i++) {
            int startX = rand.nextInt(gridSize);
            int startY = rand.nextInt(gridSize);
            List<Road> route = new ArrayList<>();
            List<TrafficLight> routeLights = new ArrayList<>();
            List<Intersection> routeIntersections = new ArrayList<>();

            int x = startX;
            int y = startY;

            for (int step = 0; step < 3; step++) {
                boolean horizontal = rand.nextBoolean();
                String roadId;
                if (horizontal && y + 1 < gridSize) {
                    roadId = "R" + x + "-" + y;
                    y++;
                } else if (!horizontal && x + 1 < gridSize) {
                    roadId = "C" + x + "-" + y;
                    x++;
                } else {
                    continue;
                }

                Road road = roadMap.get(roadId);
                if (road != null) route.add(road);

                for (TrafficLight light : lights) {
                    if (light.getLightId().equals(roadId)) {
                        routeLights.add(light);
                        break;
                    }
                }

                String intersectionId = "X" + x + "-" + y;
                Intersection intersection = intersectionMap.get(intersectionId);
                if (intersection != null) {
                    routeIntersections.add(intersection);
                }
            }

            if (!route.isEmpty()) {
                Vehicle v = new Vehicle("V" + i, route, routeLights, routeIntersections);
                vehicles.add(v);
            }
        }
    }

    public void startSimulation() {
        lights.forEach(Thread::start);
        vehicles.forEach(Thread::start);
    }

    public void stopSimulation() {
        vehicles.forEach(Vehicle::stopVehicle);
        lights.forEach(Thread::interrupt);
    }

    public Road getRoadById(String roadId) {
        return roadMap.get(roadId);
    }

    public TrafficLight getTrafficLightForRoad(Road road) {
        return roadLightMap.get(road);
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public List<Road> getRoads() {
        return roads;
    }

    public List<Intersection> getIntersections() {
        return intersections;
    }

    public List<TrafficLight> getLights() {
        return lights;
    }
}
