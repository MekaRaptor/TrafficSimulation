public enum VehicleType {
    CAR(5000, "Car"),           // 5 seconds base time
    TRUCK(8000, "Truck"),       // 8 seconds base time
    MOTORCYCLE(3000, "Motorcycle"), // 3 seconds base time
    BUS(7000, "Bus");          // 7 seconds base time
    
    private final long roadTravelTime; // milliseconds
    private final String displayName;
    
    VehicleType(long roadTravelTime, String displayName) {
        this.roadTravelTime = roadTravelTime;
        this.displayName = displayName;
    }
    
    public long getRoadTravelTime() {
        return roadTravelTime;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static VehicleType getRandomType() {
        VehicleType[] types = values();
        return types[(int) (Math.random() * types.length)];
    }
} 