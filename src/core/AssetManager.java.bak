import javafx.scene.image.Image;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AssetManager {
    private static AssetManager instance;
    private Map<String, Image> images = new HashMap<>();
    
    private AssetManager() {
        loadAssets();
    }
    
    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }
    
    private void loadAssets() {
        System.out.println("Working directory: " + System.getProperty("user.dir"));
        System.out.println("Loading assets...");
        
        // Vehicle assets
        loadImage("car", "vehichles/car.png");
        loadImage("truck", "vehichles/truck.png");
        loadImage("motorcycle", "vehichles/motorcycle.png");
        loadImage("bus", "vehichles/bus.png");
        
        // Road assets - DISABLED for better visual consistency
        // We'll use enhanced shape-based roads instead
        // loadImage("road_horizontal", "roads/road_horizontal.png");
        // loadImage("road_vertical", "roads/road_vertical.png");
        // loadImage("intersection", "roads/intersection.png");
        // loadImage("zebra_crossing", "roads/intersection.png");
        
        // Traffic light assets
        loadImage("traffic_light_red", "lights/traffic_light_red.png");
        loadImage("traffic_light_yellow", "lights/traffic_light_yellow.png");
        loadImage("traffic_light_green", "lights/traffic_light_green.png");
        
        // Environment assets
        loadImage("building", "environment/apartment.png");
        loadImage("apartments", "environment/apartments.png");
        loadImage("skyline", "environment/skyline.png");
        loadImage("tree", "environment/tree.png");
        loadImage("grass", "environment/grass.png");
        
        System.out.println("Asset loading completed.");
    }
    
    private void loadImage(String name, String path) {
        try {
            // Try multiple possible paths
            String[] possiblePaths = {
                "TrafficSimulation/assets/" + path,      // Full path from project root
                "assets/" + path,                        // From current directory
                path,                                    // Original path
                "../TrafficSimulation/assets/" + path,   // One level up
                "./" + path                             // Current directory
            };
            
            for (String tryPath : possiblePaths) {
                java.io.File file = new java.io.File(tryPath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    images.put(name, image);
                    System.out.println("✅ Asset yüklendi: " + name + " (from " + tryPath + ")");
                    return;
                }
            }
            
            // Fallback to classpath
            InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream != null) {
                Image image = new Image(stream);
                images.put(name, image);
                System.out.println("✅ Asset yüklendi: " + name + " (from classpath)");
                return;
            }
            
            System.out.println("❌ Asset bulunamadı: " + name + " (varsayılan görünüm kullanılacak)");
            
        } catch (Exception e) {
            System.out.println("❌ Asset yüklenirken hata: " + path + " - " + e.getMessage());
        }
    }
    
    public Image getImage(String name) {
        return images.get(name);
    }
    
    public boolean hasImage(String name) {
        return images.containsKey(name) && images.get(name) != null;
    }
    
    // Fallback image generator for missing assets
    public static Image createPlaceholderImage(int width, int height, String color) {
        // This would create a simple colored rectangle as placeholder
        // For now, return null to use original drawing methods
        return null;
    }
} 