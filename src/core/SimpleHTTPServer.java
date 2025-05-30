package src.core;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Simple HTTP Server for Traffic Simulation Web API
 * Serves REST endpoints and static files
 */
public class SimpleHTTPServer {
    private final int port;
    private final WebTrafficAPI api;
    private ServerSocket serverSocket;
    private final ExecutorService threadPool;
    private volatile boolean isRunning = false;
    
    public SimpleHTTPServer(int port) {
        this.port = port;
        this.api = new WebTrafficAPI();
        this.threadPool = Executors.newFixedThreadPool(10);
    }
    
    /**
     * Start the HTTP server
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            
            System.out.println("🌐 HTTP Server started on port " + port);
            System.out.println("📡 Web API endpoints available:");
            System.out.println("   GET  http://localhost:" + port + "/api/stats");
            System.out.println("   GET  http://localhost:" + port + "/api/vehicles");
            System.out.println("   GET  http://localhost:" + port + "/api/roads");
            System.out.println("   GET  http://localhost:" + port + "/api/zones");
            System.out.println("   POST http://localhost:" + port + "/api/start");
            System.out.println("   POST http://localhost:" + port + "/api/stop");
            System.out.println("   POST http://localhost:" + port + "/api/reset");
            System.out.println("📄 Static files:");
            System.out.println("   http://localhost:" + port + "/city");
            System.out.println("");
            System.out.println("✅ Open your browser and go to: http://localhost:" + port + "/city");
            System.out.println("");
            
            while (isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    threadPool.submit(new ClientHandler(clientSocket));
                } catch (IOException e) {
                    if (isRunning) {
                        System.err.println("Error accepting connection: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
    
    /**
     * Stop the HTTP server
     */
    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            threadPool.shutdown();
            System.out.println("🛑 HTTP Server stopped");
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
    }
    
    /**
     * Client request handler
     */
    private class ClientHandler implements Runnable {
        private final Socket clientSocket;
        
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream());
                 BufferedOutputStream dataOut = new BufferedOutputStream(clientSocket.getOutputStream())) {
                
                // Read request line
                String input = in.readLine();
                if (input == null) return;
                
                StringTokenizer parse = new StringTokenizer(input);
                String method = parse.nextToken().toUpperCase();
                String fileRequested = parse.nextToken().toLowerCase();
                
                // Parse query parameters
                Map<String, String> params = new HashMap<>();
                if (fileRequested.contains("?")) {
                    String[] parts = fileRequested.split("\\?");
                    fileRequested = parts[0];
                    if (parts.length > 1) {
                        String[] paramPairs = parts[1].split("&");
                        for (String pair : paramPairs) {
                            String[] keyValue = pair.split("=");
                            if (keyValue.length == 2) {
                                params.put(keyValue[0], keyValue[1]);
                            }
                        }
                    }
                }
                
                System.out.println("📨 " + method + " " + fileRequested + " " + params);
                
                // Handle API requests
                if (fileRequested.startsWith("/api/")) {
                    handleApiRequest(out, method, fileRequested, params);
                }
                // Handle static file requests
                else if (fileRequested.equals("/") || fileRequested.equals("/city")) {
                    serveStaticFile(out, dataOut, "/realistic_simulation.html");
                }
                else if (fileRequested.equals("/test")) {
                    serveTestPage(out, dataOut);
                }
                else {
                    // 404 Not Found
                    String response = "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Type: text/html\r\n" +
                                    "\r\n" +
                                    "<html><body><h1>404 - Not Found</h1></body></html>";
                    out.print(response);
                    out.flush();
                }
                
            } catch (IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
        
        /**
         * Handle API requests
         */
        private void handleApiRequest(PrintWriter out, String method, String endpoint, Map<String, String> params) {
            String jsonResponse;
            int statusCode = 200;
            
            try {
                jsonResponse = api.handleRequest(endpoint, method, params);
            } catch (Exception e) {
                jsonResponse = "{\"error\":\"Internal server error: " + e.getMessage() + "\"}";
                statusCode = 500;
            }
            
            // Send HTTP response
            out.print("HTTP/1.1 " + statusCode + " OK\r\n");
            out.print("Content-Type: application/json\r\n");
            out.print("Access-Control-Allow-Origin: *\r\n");
            out.print("Access-Control-Allow-Methods: GET, POST, OPTIONS\r\n");
            out.print("Access-Control-Allow-Headers: Content-Type\r\n");
            out.print("Content-Length: " + jsonResponse.getBytes().length + "\r\n");
            out.print("\r\n");
            out.print(jsonResponse);
            out.flush();
        }
        
        /**
         * Serve static HTML file
         */
        private void serveStaticFile(PrintWriter out, BufferedOutputStream dataOut, String fileName) {
            try {
                // Generate dynamic HTML response
                String htmlContent = generateCityHTML();
                byte[] fileData = htmlContent.getBytes();
                
                // Send HTTP headers
                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Content-Type: text/html\r\n");
                out.print("Content-Length: " + fileData.length + "\r\n");
                out.print("\r\n");
                out.flush();
                
                // Send file data
                dataOut.write(fileData, 0, fileData.length);
                dataOut.flush();
                
            } catch (IOException e) {
                System.err.println("Error serving static file: " + e.getMessage());
            }
        }
        
        /**
         * Serve test page
         */
        private void serveTestPage(PrintWriter out, BufferedOutputStream dataOut) {
            try {
                // Read test file
                File testFile = new File("test_simple.html");
                byte[] fileData;
                
                if (testFile.exists()) {
                    fileData = java.nio.file.Files.readAllBytes(testFile.toPath());
                } else {
                    String content = "<!DOCTYPE html><html><body><h1>Test file not found</h1></body></html>";
                    fileData = content.getBytes();
                }
                
                // Send HTTP headers
                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Content-Type: text/html\r\n");
                out.print("Content-Length: " + fileData.length + "\r\n");
                out.print("\r\n");
                out.flush();
                
                // Send file data
                dataOut.write(fileData, 0, fileData.length);
                dataOut.flush();
                
            } catch (IOException e) {
                System.err.println("Error serving test page: " + e.getMessage());
            }
        }
        
        /**
         * Generate the city HTML with live backend integration
         */
        private String generateCityHTML() {
            return """
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>🏙️ Live Traffic Simulation</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #1a1a2e; color: white; overflow: hidden; }
        .container { display: flex; height: 100vh; }
        .sidebar { width: 300px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 20px; box-shadow: 2px 0 10px rgba(0,0,0,0.3); overflow-y: auto; }
        .main-content { flex: 1; background: #16213e; position: relative; overflow: hidden; }
        .controls { margin-bottom: 20px; }
        .btn { width: 100%; padding: 12px; margin: 6px 0; border: none; border-radius: 8px; font-weight: bold; cursor: pointer; transition: all 0.3s; font-size: 14px; }
        .btn-start { background: #4CAF50; color: white; }
        .btn-start:hover { background: #45a049; transform: translateY(-2px); }
        .btn-stop { background: #f44336; color: white; }
        .btn-stop:hover { background: #da190b; transform: translateY(-2px); }
        .btn-reset { background: #ff9800; color: white; }
        .btn-reset:hover { background: #e68900; transform: translateY(-2px); }
        .stats { background: rgba(255,255,255,0.1); padding: 12px; border-radius: 10px; margin: 8px 0; }
        .stat-item { display: flex; justify-content: space-between; margin: 6px 0; font-size: 14px; }
        .stat-value { font-weight: bold; color: #FFD700; }
        #cityCanvas { background: linear-gradient(135deg, #2C3E50 0%, #34495E 100%); cursor: grab; width: 100%; height: 100%; object-fit: contain; }
        #cityCanvas:active { cursor: grabbing; }
        .zone-label { position: absolute; color: white; font-weight: bold; text-shadow: 2px 2px 4px rgba(0,0,0,0.8); pointer-events: none; font-size: 12px; }
        .status-indicator { display: inline-block; width: 12px; height: 12px; border-radius: 50%; margin-right: 8px; }
        .running { background: #4CAF50; box-shadow: 0 0 10px #4CAF50; }
        .stopped { background: #f44336; }
        .traffic-light { position: absolute; width: 20px; height: 60px; background: #333; border-radius: 10px; border: 2px solid #666; }
        .light-bulb { width: 14px; height: 14px; border-radius: 50%; margin: 2px auto; border: 1px solid #555; }
        .light-red { background: #ff4444; box-shadow: 0 0 10px #ff4444; }
        .light-yellow { background: #ffff44; box-shadow: 0 0 10px #ffff44; }
        .light-green { background: #44ff44; box-shadow: 0 0 10px #44ff44; }
        .light-off { background: #333; }
        h2 { font-size: 18px; margin-bottom: 15px; text-align: center; }
        h3 { font-size: 14px; margin-bottom: 10px; margin-top: 10px; border-bottom: 1px solid rgba(255,255,255,0.2); padding-bottom: 5px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="sidebar">
            <h2>🏙️ Live Traffic Control</h2>
            <div class="stats">
                <h3>Backend Status</h3>
                <div class="stat-item">
                    <span>Status:</span>
                    <span id="status"><span class="status-indicator stopped"></span>Stopped</span>
                </div>
            </div>
            <div class="controls">
                <h3>🎮 Controls</h3>
                <button class="btn btn-start" onclick="startSimulation()">▶️ Start Traffic</button>
                <button class="btn btn-stop" onclick="stopSimulation()">⏹️ Stop Traffic</button>
                <button class="btn btn-reset" onclick="resetCity()">🔄 Reset City</button>
            </div>
            <div class="stats">
                <h3>📊 Live Statistics</h3>
                <div class="stat-item">
                    <span>🚗 Active Vehicles:</span>
                    <span class="stat-value" id="activeVehicles">0</span>
                </div>
                <div class="stat-item">
                    <span>🛣️ Total Roads:</span>
                    <span class="stat-value" id="totalRoads">0</span>
                </div>
                <div class="stat-item">
                    <span>⚠️ Congestion:</span>
                    <span class="stat-value" id="avgCongestion">0%</span>
                </div>
            </div>
            <div class="stats">
                <h3>🚗 Vehicle Types</h3>
                <div class="stat-item">
                    <span>🚗 Cars:</span><span class="stat-value" id="carCount">0</span>
                </div>
                <div class="stat-item">
                    <span>🚛 Trucks:</span><span class="stat-value" id="truckCount">0</span>
                </div>
                <div class="stat-item">
                    <span>🏍️ Motorcycles:</span><span class="stat-value" id="motorcycleCount">0</span>
                </div>
                <div class="stat-item">
                    <span>🚌 Buses:</span><span class="stat-value" id="busCount">0</span>
                </div>
            </div>
        </div>
        
        <div class="main-content">
            <canvas id="cityCanvas"></canvas>
        </div>
    </div>
    
    <script>
        let canvas, ctx;
        let vehicles = [];
        let roads = [];
        let zones = [];
        let trafficLights = [];
        let stats = {};
        let isRunning = false;
        let updateInterval;
        let animationId;

        // Enhanced city layout - STRATEJİK DÜZENLEME VE BÜYÜK CANVAS
        const CITY_CONFIG = {
            width: 0, // Dinamik olarak ayarlanacak
            height: 0,
            zones: [
                // STRATEJİK BÖLGE DÜZENLEMESİ - Backend ile uyumlu
                
                // KUZEY-BATI köşesi (Üst sol)
                { id: 'RES_NW1', type: 'residential', x: 10, y: 10, width: 80, height: 130, color: '#2E8B57' },
                { id: 'COM_NW', type: 'commercial', x: 160, y: 10, width: 80, height: 130, color: '#4169E1' },
                { id: 'RES_NW2', type: 'residential', x: 260, y: 10, width: 130, height: 130, color: '#2E8B57' },
                
                // KUZEY-DOĞU köşesi (Üst sağ)
                { id: 'DOWNTOWN', type: 'downtown', x: 420, y: 10, width: 120, height: 80, color: '#8A2BE2' },
                { id: 'RES_NE1', type: 'residential', x: 560, y: 10, width: 120, height: 80, color: '#2E8B57' },
                { id: 'COM_NE', type: 'commercial', x: 700, y: 10, width: 90, height: 130, color: '#4169E1' },
                
                // GÜNEY-BATI köşesi (Alt sol)
                { id: 'IND_SW', type: 'industrial', x: 10, y: 370, width: 80, height: 120, color: '#FF8C00' },
                { id: 'RES_SW1', type: 'residential', x: 10, y: 500, width: 130, height: 80, color: '#2E8B57' },
                { id: 'RES_SW2', type: 'residential', x: 160, y: 520, width: 80, height: 100, color: '#2E8B57' },
                { id: 'COM_SW', type: 'commercial', x: 260, y: 520, width: 130, height: 100, color: '#4169E1' },
                
                // GÜNEY-DOĞU köşesi (Alt sağ)
                { id: 'PARK_SE', type: 'park', x: 420, y: 370, width: 120, height: 120, color: '#98FB98' },
                { id: 'COM_SE', type: 'commercial', x: 560, y: 370, width: 120, height: 80, color: '#4169E1' },
                { id: 'RES_SE', type: 'residential', x: 700, y: 370, width: 90, height: 180, color: '#2E8B57' },
                { id: 'IND_SE', type: 'industrial', x: 420, y: 520, width: 230, height: 100, color: '#FF8C00' },
                
                // DOĞU UZANTISI (Sağ kenar - Canvas genişliği için)
                { id: 'RES_EAST', type: 'residential', x: 820, y: 10, width: 120, height: 200, color: '#2E8B57' },
                { id: 'PARK_EAST', type: 'park', x: 820, y: 220, width: 120, height: 120, color: '#98FB98' },
                { id: 'COM_EAST', type: 'commercial', x: 820, y: 350, width: 120, height: 200, color: '#4169E1' }
            ],
            roads: [
                // CANVAS İÇİN OPTİMİZE YOL AĞI (1000x800) - Backend ile AYNI
                
                // ANA OMURGA: Çapraz otoyollar
                { id: 'HW_MAIN_EW', type: 'highway', x1: 0, y1: 350, x2: 950, y2: 350, width: 8, color: '#FFD700', hasLights: true },
                { id: 'HW_MAIN_NS', type: 'highway', x1: 400, y1: 0, x2: 400, y2: 750, width: 8, color: '#FFD700', hasLights: true },
                
                // KUZEY BÖLGE YOLLARI
                { id: 'NORTH_1', type: 'main', x1: 100, y1: 0, x2: 100, y2: 350, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'NORTH_2', type: 'main', x1: 200, y1: 0, x2: 200, y2: 350, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'NORTH_3', type: 'main', x1: 300, y1: 0, x2: 300, y2: 350, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'NORTH_4', type: 'main', x1: 550, y1: 0, x2: 550, y2: 350, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'NORTH_5', type: 'main', x1: 700, y1: 0, x2: 700, y2: 350, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'NORTH_H1', type: 'secondary', x1: 0, y1: 150, x2: 950, y2: 150, width: 4, color: '#808080', hasLights: false },
                { id: 'NORTH_H2', type: 'secondary', x1: 0, y1: 250, x2: 950, y2: 250, width: 4, color: '#808080', hasLights: false },
                
                // GÜNEY BÖLGE YOLLARI
                { id: 'SOUTH_1', type: 'main', x1: 100, y1: 350, x2: 100, y2: 750, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'SOUTH_2', type: 'main', x1: 200, y1: 350, x2: 200, y2: 750, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'SOUTH_3', type: 'main', x1: 300, y1: 350, x2: 300, y2: 750, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'SOUTH_4', type: 'main', x1: 550, y1: 350, x2: 550, y2: 750, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'SOUTH_5', type: 'main', x1: 700, y1: 350, x2: 700, y2: 750, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'SOUTH_H1', type: 'secondary', x1: 0, y1: 500, x2: 950, y2: 500, width: 4, color: '#808080', hasLights: false },
                { id: 'SOUTH_H2', type: 'secondary', x1: 0, y1: 650, x2: 950, y2: 650, width: 4, color: '#808080', hasLights: false },
                
                // DOĞU UZANTISI YOLLARI
                { id: 'EAST_1', type: 'main', x1: 800, y1: 0, x2: 800, y2: 750, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'EAST_2', type: 'main', x1: 900, y1: 0, x2: 900, y2: 750, width: 5, color: '#C0C0C0', hasLights: true },
                { id: 'EAST_H1', type: 'secondary', x1: 400, y1: 100, x2: 950, y2: 100, width: 4, color: '#808080', hasLights: false },
                { id: 'EAST_H2', type: 'secondary', x1: 400, y1: 300, x2: 950, y2: 300, width: 4, color: '#808080', hasLights: false },
                
                // MERKEZ BAĞLANTI YOLLARI
                { id: 'CONNECT_1', type: 'local', x1: 0, y1: 400, x2: 950, y2: 400, width: 3, color: '#696969', hasLights: false },
                { id: 'CONNECT_2', type: 'local', x1: 0, y1: 600, x2: 950, y2: 600, width: 3, color: '#696969', hasLights: false },
                { id: 'CONNECT_3', type: 'local', x1: 600, y1: 0, x2: 600, y2: 750, width: 3, color: '#696969', hasLights: false }
            ]
        };

        // Backend'den gerçek yolları çek ve koordinatları kullan
        let backendRoads = [];
        let realTrafficLights = [];

        async function loadBackendData() {
            try {
                // Gerçek yol verilerini çek
                const roadsResponse = await fetch('/api/roads');
                const roadsData = await roadsResponse.json();
                backendRoads = roadsData.roads || [];
                
                // Trafik ışıklarını çek
                const lightsResponse = await fetch('/api/lights');
                const lightsData = await lightsResponse.json();
                realTrafficLights = lightsData.lights || [];
                
                console.log('Loaded', backendRoads.length, 'roads and', realTrafficLights.length, 'traffic lights from backend');
            } catch (error) {
                console.error('Failed to load backend data:', error);
            }
        }

        function initCanvas() {
            canvas = document.getElementById('cityCanvas');
            ctx = canvas.getContext('2d');
            resizeCanvas();
            window.addEventListener('resize', resizeCanvas);
            loadBackendData(); // Backend verilerini yükle
            drawCity();
        }

        function resizeCanvas() {
            const container = canvas.parentElement;
            canvas.width = Math.max(1000, container.clientWidth); // Daha geniş canvas
            canvas.height = Math.max(800, container.clientHeight); // Daha yüksek canvas
            CITY_CONFIG.width = canvas.width;
            CITY_CONFIG.height = canvas.height;
            drawCity();
        }

        function drawCity() {
            // Arkaplanı temizle
            ctx.fillStyle = '#1a1a2e';
            ctx.fillRect(0, 0, canvas.width, canvas.height);

            // Zone'ları çiz
            drawZones();

            // Yolları çiz
            drawRoads();

            // Trafik ışıklarını çiz
            drawTrafficLights();

            // Araçları çiz
            drawVehicles();

            // Zone etiketlerini çiz
            drawZoneLabels();
        }

        function drawZones() {
            CITY_CONFIG.zones.forEach(zone => {
                ctx.fillStyle = zone.color;
                ctx.globalAlpha = 0.3;
                ctx.fillRect(zone.x, zone.y, zone.width, zone.height);
                
                // Zone kenarlığı
                ctx.globalAlpha = 0.8;
                ctx.strokeStyle = zone.color;
                ctx.lineWidth = 2;
                ctx.strokeRect(zone.x, zone.y, zone.width, zone.height);
                ctx.globalAlpha = 1.0;
            });
        }

        function drawRoads() {
            // Backend'den gelen gerçek yolları çiz
            if (backendRoads.length > 0) {
                backendRoads.forEach(road => {
                    // Yol türüne göre renk ve kalınlık belirle
                    let color = '#C0C0C0'; // Default
                    let width = 4;
                    
                    switch(road.type) {
                        case 'Otoyol':
                            color = '#FFD700';
                            width = 8;
                            break;
                        case 'Ana Yol':
                            color = '#C0C0C0';
                            width = 5;
                            break;
                        case 'Tali Yol':
                            color = '#808080';
                            width = 4;
                            break;
                        case 'Mahalle Yolu':
                        case 'Konut Sokağı':
                            color = '#696969';
                            width = 3;
                            break;
                    }
                    
                    // Ana yol çizgisi
                    ctx.strokeStyle = color;
                    ctx.lineWidth = width;
                    ctx.lineCap = 'round';
                    
                    ctx.beginPath();
                    ctx.moveTo(road.x1, road.y1);
                    ctx.lineTo(road.x2, road.y2);
                    ctx.stroke();

                    // Yol çizgileri (beyaz kesikli) - sadece büyük yollar için
                    if (width > 4) {
                        ctx.strokeStyle = 'white';
                        ctx.lineWidth = 1;
                        ctx.setLineDash([10, 10]);
                        ctx.beginPath();
                        ctx.moveTo(road.x1, road.y1);
                        ctx.lineTo(road.x2, road.y2);
                        ctx.stroke();
                        ctx.setLineDash([]);
                    }
                    
                    // Trafik sıkışıklığı göstergesi
                    if (road.congestion > 0.3) {
                        ctx.strokeStyle = road.congestion > 0.7 ? '#FF4444' : '#FFAA44';
                        ctx.lineWidth = Math.max(1, width - 2);
                        ctx.globalAlpha = 0.6;
                        ctx.beginPath();
                        ctx.moveTo(road.x1, road.y1);
                        ctx.lineTo(road.x2, road.y2);
                        ctx.stroke();
                        ctx.globalAlpha = 1.0;
                    }
                });
            } else {
                // Fallback: Static yolları çiz
                CITY_CONFIG.roads.forEach(road => {
                    ctx.strokeStyle = road.color;
                    ctx.lineWidth = road.width;
                    ctx.lineCap = 'round';
                    
                    ctx.beginPath();
                    ctx.moveTo(road.x1, road.y1);
                    ctx.lineTo(road.x2, road.y2);
                    ctx.stroke();

                    // Yol çizgileri (beyaz kesikli)
                    if (road.width > 3) {
                        ctx.strokeStyle = 'white';
                        ctx.lineWidth = 1;
                        ctx.setLineDash([10, 10]);
                        ctx.beginPath();
                        ctx.moveTo(road.x1, road.y1);
                        ctx.lineTo(road.x2, road.y2);
                        ctx.stroke();
                        ctx.setLineDash([]);
                    }
                });
            }
        }

        function drawTrafficLights() {
            // Backend'den gelen gerçek trafik ışıklarını çiz
            realTrafficLights.forEach((lightData, index) => {
                // Trafik ışığının yol ID'sine göre konumunu bul
                const position = findTrafficLightPosition(lightData.id);
                if (!position) return;
                
                // Trafik ışığı kutusu
                ctx.fillStyle = lightData.isEmergency ? '#FF6B6B' : '#333';
                ctx.fillRect(position.x - 10, position.y - 30, 20, 60);
                ctx.strokeStyle = '#666';
                ctx.lineWidth = 2;
                ctx.strokeRect(position.x - 10, position.y - 30, 20, 60);

                // Gerçek backend state'ini kullan
                const lightState = lightData.state;
                const brightness = lightData.brightness;

                // Kırmızı ışık
                ctx.fillStyle = lightState === 'RED' ? `rgba(255, 68, 68, ${brightness})` : '#330000';
                ctx.beginPath();
                ctx.arc(position.x, position.y - 18, 6, 0, 2 * Math.PI);
                ctx.fill();
                if (lightState === 'RED') {
                    ctx.shadowColor = '#ff4444';
                    ctx.shadowBlur = 12;
                    ctx.fill();
                    ctx.shadowBlur = 0;
                }

                // Sarı ışık
                ctx.fillStyle = lightState === 'YELLOW' ? `rgba(255, 255, 68, ${brightness})` : '#333300';
                ctx.beginPath();
                ctx.arc(position.x, position.y, 6, 0, 2 * Math.PI);
                ctx.fill();
                if (lightState === 'YELLOW') {
                    ctx.shadowColor = '#ffff44';
                    ctx.shadowBlur = 12;
                    ctx.fill();
                    ctx.shadowBlur = 0;
                }

                // Yeşil ışık
                ctx.fillStyle = lightState === 'GREEN' ? `rgba(68, 255, 68, ${brightness})` : '#003300';
                ctx.beginPath();
                ctx.arc(position.x, position.y + 18, 6, 0, 2 * Math.PI);
                ctx.fill();
                if (lightState === 'GREEN') {
                    ctx.shadowColor = '#44ff44';
                    ctx.shadowBlur = 12;
                    ctx.fill();
                    ctx.shadowBlur = 0;
                }

                // Acil durum indicator
                if (lightData.isEmergency) {
                    ctx.fillStyle = 'rgba(255, 0, 0, 0.8)';
                    ctx.font = '10px Arial';
                    ctx.textAlign = 'center';
                    ctx.fillText('🚨', position.x, position.y - 35);
                }
            });
        }

        // Yol ID'sine göre trafik ışığı konumunu bul
        function findTrafficLightPosition(roadId) {
            // Backend yol ID'lerine göre kavşak konumları (AYNI koordinatlar)
            const lightPositions = {
                'HW_MAIN_EW': { x: 200, y: 350 },
                'HW_MAIN_NS': { x: 400, y: 350 },
                'MAIN_NORTH': { x: 200, y: 300 },
                'MAIN_SOUTH': { x: 300, y: 500 },
                'MAIN_EAST': { x: 500, y: 250 },
                'MAIN_WEST': { x: 200, y: 280 },
                'SEC_1': { x: 250, y: 200 },
                'SEC_2': { x: 450, y: 250 },
                'SEC_3': { x: 400, y: 450 },
                'LOCAL_1': { x: 175, y: 120 },
                'LOCAL_2': { x: 150, y: 130 },
                'LOCAL_3': { x: 280, y: 600 }
            };
            
            return lightPositions[roadId] || null;
        }

        function drawVehicles() {
            vehicles.forEach(vehicle => {
                if (!vehicle.active) return;

                // ENTEGRE: Backend koordinatlarını doğrudan kullan (scale olmadan)
                const x = parseFloat(vehicle.x);
                const y = parseFloat(vehicle.y);

                const size = getVehicleSize(vehicle.type);
                const emoji = getVehicleEmoji(vehicle.type);

                // Araç durumu göstergesi
                let vehicleColor = getVehicleColor(vehicle.type);
                if (!vehicle.onRoad) {
                    vehicleColor = 'rgba(200,200,200,0.7)'; // Zone'da bekleyen araçlar soluk
                }

                // Araç gölgesi
                ctx.fillStyle = 'rgba(0,0,0,0.3)';
                ctx.fillRect(x + 2, y + 2, size, size);

                // Araç gövdesi (gerçek koordinatlarda)
                ctx.fillStyle = vehicleColor;
                ctx.fillRect(x - size/2, y - size/2, size, size);

                // Araç emoji
                ctx.font = size + 'px Arial';
                ctx.textAlign = 'center';
                ctx.fillStyle = 'white';
                ctx.strokeStyle = 'black';
                ctx.lineWidth = 1;
                ctx.strokeText(emoji, x, y + size/3);
                ctx.fillText(emoji, x, y + size/3);

                // Gelişmiş durum göstergeleri
                ctx.font = 'bold 10px Arial';
                
                // Hız göstergesi
                if (vehicle.speed > 0 && vehicle.onRoad) {
                    ctx.fillStyle = 'rgba(0,255,0,0.9)';
                    ctx.strokeStyle = 'black';
                    ctx.lineWidth = 1;
                    const speedText = Math.round(vehicle.speed) + " km/h";
                    ctx.strokeText(speedText, x, y - size/2 - 5);
                    ctx.fillText(speedText, x, y - size/2 - 5);
                }

                // Araç ID ve durum
                ctx.fillStyle = vehicle.onRoad ? 'rgba(255,255,0,0.9)' : 'rgba(255,100,100,0.9)';
                const statusText = vehicle.onRoad ? vehicle.id + " (on " + vehicle.roadId + ")" : vehicle.id + " (waiting)";
                ctx.strokeText(statusText, x, y + size/2 + 15);
                ctx.fillText(statusText, x, y + size/2 + 15);

                // Progress göstergesi (yolda olan araçlar için)
                if (vehicle.onRoad && vehicle.progress !== undefined) {
                    const progressWidth = size;
                    const progressHeight = 3;
                    const progressX = x - progressWidth/2;
                    const progressY = y + size/2 + 25;
                    
                    // Progress bar arka plan
                    ctx.fillStyle = 'rgba(0,0,0,0.3)';
                    ctx.fillRect(progressX, progressY, progressWidth, progressHeight);
                    
                    // Progress bar dolum
                    ctx.fillStyle = 'rgba(0,255,0,0.8)';
                    ctx.fillRect(progressX, progressY, progressWidth * vehicle.progress, progressHeight);
                }
            });
        }

        function drawZoneLabels() {
            ctx.fillStyle = 'white';
            ctx.font = 'bold 12px Arial';
            ctx.textAlign = 'center';
            ctx.shadowColor = 'black';
            ctx.shadowBlur = 3;

            CITY_CONFIG.zones.forEach(zone => {
                const centerX = zone.x + zone.width / 2;
                const centerY = zone.y + zone.height / 2;
                const labels = getZoneLabel(zone.type);
                
                ctx.fillText(labels.title, centerX, centerY - 5);
                ctx.font = '10px Arial';
                ctx.fillText(labels.subtitle, centerX, centerY + 10);
                ctx.font = 'bold 12px Arial';
            });
            ctx.shadowBlur = 0;
        }

        function getZoneLabel(type) {
            const labels = {
                'residential': { title: 'Konut Bölgesi', subtitle: 'Residential' },
                'commercial': { title: 'Ticaret Bölgesi', subtitle: 'Commercial' },
                'downtown': { title: 'Şehir Merkezi', subtitle: 'Downtown' },
                'industrial': { title: 'Sanayi Bölgesi', subtitle: 'Industrial' },
                'park': { title: 'Park Alanı', subtitle: 'Park' }
            };
            return labels[type] || { title: 'Bilinmeyen', subtitle: 'Unknown' };
        }

        function getVehicleSize(type) {
            const sizes = {
                'Şahsi Araç': 12,
                'Kamyon': 18,
                'Motosiklet': 8,
                'Otobüs': 20,
                'Taksi': 12,
                'Kargo Aracı': 15
            };
            return sizes[type] || 12;
        }

        function getVehicleEmoji(type) {
            const emojis = {
                'Şahsi Araç': '🚗',
                'Kamyon': '🚛',
                'Motosiklet': '🏍️',
                'Otobüs': '🚌',
                'Taksi': '🚕',
                'Kargo Aracı': '🚐'
            };
            return emojis[type] || '🚗';
        }

        function getVehicleColor(type) {
            const colors = {
                'Şahsi Araç': '#3498db',
                'Kamyon': '#e74c3c',
                'Motosiklet': '#f39c12',
                'Otobüs': '#2ecc71',
                'Taksi': '#f1c40f',
                'Kargo Aracı': '#9b59b6'
            };
            return colors[type] || '#3498db';
        }

        async function updateVehicles() {
            if (!isRunning) return;

            try {
                const response = await fetch('/api/vehicles');
                const data = await response.json();
                vehicles = data.vehicles || [];

                // Araç tiplerini say
                updateVehicleCounts();

                console.log(`Updated ${vehicles.length} vehicles`);
            } catch (error) {
                console.error('Failed to fetch vehicles:', error);
            }
        }

        async function updateStats() {
            try {
                const response = await fetch('/api/stats');
                stats = await response.json();

                document.getElementById('activeVehicles').textContent = stats.activeVehicles || 0;
                document.getElementById('totalRoads').textContent = stats.totalRoads || 0;
                document.getElementById('avgCongestion').textContent = (stats.avgCongestion || 0) + '%';

                // Status güncelle
                const statusElement = document.getElementById('status');
                if (stats.isRunning) {
                    statusElement.innerHTML = '<span class="status-indicator running"></span>Running';
                    isRunning = true;
                } else {
                    statusElement.innerHTML = '<span class="status-indicator stopped"></span>Stopped';
                    isRunning = false;
                }
            } catch (error) {
                console.error('Failed to fetch stats:', error);
            }
        }

        function updateVehicleCounts() {
            const counts = {
                'Şahsi Araç': 0,
                'Kamyon': 0,
                'Motosiklet': 0,
                'Otobüs': 0
            };

            vehicles.forEach(vehicle => {
                if (vehicle.active && counts.hasOwnProperty(vehicle.type)) {
                    counts[vehicle.type]++;
                }
            });

            document.getElementById('carCount').textContent = counts['Şahsi Araç'];
            document.getElementById('truckCount').textContent = counts['Kamyon'];
            document.getElementById('motorcycleCount').textContent = counts['Motosiklet'];
            document.getElementById('busCount').textContent = counts['Otobüs'];
        }

        async function startSimulation() {
            try {
                const response = await fetch('/api/start', { method: 'POST' });
                const result = await response.json();
                console.log(result.message);

                if (result.status === 'started') {
                    isRunning = true;
                    updateInterval = setInterval(() => {
                        updateVehicles();
                        updateStats();
                        updateTrafficLights(); // Gerçek trafik ışığı durumlarını güncelle
                        updateRoads(); // Yol verilerini güncelle (trafik sıkışıklığı için)
                    }, 1000);

                    // Animasyon döngüsü
                    function animate() {
                        if (isRunning) {
                            drawCity();
                            animationId = requestAnimationFrame(animate);
                        }
                    }
                    animate();
                }
            } catch (error) {
                console.error('Failed to start simulation:', error);
            }
        }

        async function stopSimulation() {
            try {
                const response = await fetch('/api/stop', { method: 'POST' });
                const result = await response.json();
                console.log(result.message);

                isRunning = false;
                if (updateInterval) clearInterval(updateInterval);
                if (animationId) cancelAnimationFrame(animationId);
            } catch (error) {
                console.error('Failed to stop simulation:', error);
            }
        }

        async function resetCity() {
            try {
                const response = await fetch('/api/reset?vehicles=15', { method: 'POST' });
                const result = await response.json();
                console.log(result.message);

                vehicles = [];
                drawCity();
                updateStats();
            } catch (error) {
                console.error('Failed to reset city:', error);
            }
        }

        async function updateTrafficLights() {
            try {
                const response = await fetch('/api/lights');
                const data = await response.json();
                realTrafficLights = data.lights || [];
            } catch (error) {
                console.error('Failed to update traffic lights:', error);
            }
        }

        async function updateRoads() {
            try {
                const response = await fetch('/api/roads');
                const data = await response.json();
                backendRoads = data.roads || [];
            } catch (error) {
                console.error('Failed to update roads:', error);
            }
        }

        // Sayfa yüklendiğinde başlat
        window.onload = function() {
            initCanvas();
            updateStats();
        };
    </script>
</body>
</html>
            """;
        }
    }
    
    /**
     * Main method to start the server
     */
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port number, using default: 8080");
            }
        }
        
        SimpleHTTPServer server = new SimpleHTTPServer(port);
        
        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n🛑 Shutting down server...");
            server.stop();
        }));
        
        server.start();
    }
} 