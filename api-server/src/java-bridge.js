const { spawn } = require('child_process');
const path = require('path');
const fs = require('fs');

class JavaBridge {
    constructor() {
        this.javaProcess = null;
        this.connected = false;
        this.outputBuffer = '';
        this.lastState = null;
        this.commandQueue = [];
        this.processing = false;
    }
    
    async initialize() {
        console.log('🔧 Initializing Java Bridge...');
        
        try {
            // Compile Java code first
            await this.compileJavaCode();
            
            // Start Java simulation server
            await this.startJavaProcess();
            
            this.connected = true;
            console.log('✅ Java Bridge initialized successfully');
            
        } catch (error) {
            console.error('❌ Failed to initialize Java Bridge:', error);
            throw error;
        }
    }
    
    async compileJavaCode() {
        console.log('📦 Compiling Java simulation engine...');
        
        return new Promise((resolve, reject) => {
            const javaFiles = [
                '../../src/core/SimulationEngine.java',
                '../../src/core/CityMap.java',
                '../../src/core/Vehicle.java',
                '../../src/core/Road.java',
                '../../src/core/TrafficLight.java',
                '../../src/core/Intersection.java',
                '../../src/core/SimulationServer.java'
            ];
            
            // Create compiled directory if it doesn't exist
            const compiledDir = path.join(__dirname, '../../compiled');
            if (!fs.existsSync(compiledDir)) {
                fs.mkdirSync(compiledDir, { recursive: true });
            }
            
            const compileProcess = spawn('javac', [
                '-cp', path.join(__dirname, '../../src'),
                ...javaFiles.map(f => path.join(__dirname, f)),
                '-d', compiledDir
            ]);
            
            let errorOutput = '';
            
            compileProcess.stderr.on('data', (data) => {
                errorOutput += data.toString();
            });
            
            compileProcess.on('close', (code) => {
                if (code === 0) {
                    console.log('✅ Java code compiled successfully');
                    resolve();
                } else {
                    console.error('❌ Java compilation failed:', errorOutput);
                    reject(new Error(`Compilation failed with exit code ${code}: ${errorOutput}`));
                }
            });
        });
    }
    
    async startJavaProcess() {
        console.log('🚀 Starting Java simulation engine...');
        
        return new Promise((resolve, reject) => {
            const javaArgs = [
                '-cp', path.join(__dirname, '../../compiled'),
                'src.core.SimulationServer'
            ];
            
            console.log('🔧 Java args:', javaArgs);
            console.log('🔧 Working directory:', process.cwd());
            console.log('🔧 Compiled path:', path.join(__dirname, '../../compiled'));
            
            this.javaProcess = spawn('java', javaArgs, {
                stdio: ['pipe', 'pipe', 'pipe']
            });
            
            this.javaProcess.stdout.on('data', (data) => {
                const output = data.toString();
                console.log('📤 Java stdout:', output);
                this.outputBuffer += output;
                this.processOutput();
            });
            
            this.javaProcess.stderr.on('data', (data) => {
                const error = data.toString();
                console.error('📥 Java stderr:', error);
            });
            
            this.javaProcess.on('close', (code) => {
                console.log(`☠️ Java process exited with code ${code}`);
                this.connected = false;
            });
            
            this.javaProcess.on('error', (error) => {
                console.error('🚨 Java process error:', error);
                reject(error);
            });
            
            // Test if process started correctly
            setTimeout(() => {
                if (this.javaProcess && !this.javaProcess.killed) {
                    console.log('✅ Java process started successfully');
                    console.log('🔧 Process PID:', this.javaProcess.pid);
                    console.log('🔧 Process connected:', this.javaProcess.connected);
                    
                    // Test sending a simple command
                    try {
                        this.javaProcess.stdin.write('{"command":"getState"}\n');
                        console.log('🧪 Test command sent');
                    } catch (e) {
                        console.error('❌ Failed to send test command:', e);
                    }
                    
                    resolve();
                } else {
                    reject(new Error('Java process failed to start'));
                }
            }, 3000); // Increased timeout
        });
    }
    
    processOutput() {
        const lines = this.outputBuffer.split('\n');
        this.outputBuffer = lines.pop() || ''; // Keep incomplete line
        
        for (const line of lines) {
            if (line.trim()) {
                try {
                    const data = JSON.parse(line.trim());
                    console.log('✅ Parsed JSON from Java:', data.type);
                    
                    if (data.type === 'state') {
                        this.lastState = data.payload;
                        console.log('📊 State updated - vehicles:', data.payload.vehicles?.length || 0, 'roads:', data.payload.roads?.length || 0);
                    } else if (data.type === 'response') {
                        console.log('📨 Response from Java:', data.command, data.success);
                    } else if (data.type === 'event') {
                        console.log('📢 Event from Java:', data.event);
                    }
                } catch (e) {
                    // Not JSON or incomplete JSON, treat as log message
                    if (line.trim().startsWith('{') || line.trim().includes('"type"')) {
                        // This looks like JSON but failed to parse - might be incomplete
                        console.log('⚠️ Incomplete JSON detected, length:', line.length);
                        console.log('⚠️ JSON snippet:', line.substring(0, 100) + '...');
                        
                        // Try to collect more data for next iteration
                        this.outputBuffer = line + '\n' + this.outputBuffer;
                    } else {
                        console.log('Java Log:', line);
                    }
                }
            }
        }
    }
    
    async sendCommand(command, payload = {}) {
        return new Promise((resolve, reject) => {
            if (!this.connected || !this.javaProcess) {
                console.error('❌ Java bridge not connected');
                reject(new Error('Java bridge not connected'));
                return;
            }
            
            const commandObj = {
                command,
                payload,
                id: Date.now() + Math.random()
            };
            
            console.log('📨 Sending command to Java:', JSON.stringify(commandObj));
            
            try {
                this.javaProcess.stdin.write(JSON.stringify(commandObj) + '\n');
                console.log('✅ Command sent successfully');
                
                // For simplicity, resolve immediately
                // In production, you'd wait for response with matching ID
                setTimeout(() => resolve({ success: true }), 100);
                
            } catch (error) {
                console.error('❌ Error sending command:', error);
                reject(error);
            }
        });
    }
    
    // Public API methods
    async getSimulationState() {
        if (this.lastState) {
            console.log('📊 Returning real state from Java:', {
                running: this.lastState.running,
                vehicles: this.lastState.vehicles?.length || 0,
                roads: this.lastState.roads?.length || 0
            });
            return this.transformSimulationState(this.lastState);
        }
        
        console.log('⚠️ No state from Java, returning fallback data');
        // Fallback mock data
        return {
            isRunning: false,
            simulationTime: 0,
            vehicles: [],
            roads: [],
            zones: [],
            intersections: [],
            trafficLights: [],
            metrics: {
                activeVehicles: 0,
                totalVehicles: 0,
                averageSpeed: 0,
                totalRoads: 0,
                averageCongestion: 0,
                redLightsCount: 0,
                totalLights: 0,
                totalZones: 0,
                totalIntersections: 0,
                simulationTime: 0
            }
        };
    }
    
    async getVehicles() {
        const state = await this.getSimulationState();
        return state.vehicles;
    }
    
    async getRoads() {
        const state = await this.getSimulationState();
        return state.roads;
    }
    
    async getZones() {
        const state = await this.getSimulationState();
        return state.zones;
    }
    
    async getIntersections() {
        const state = await this.getSimulationState();
        return state.intersections;
    }
    
    async getTrafficLights() {
        const state = await this.getSimulationState();
        return state.trafficLights;
    }
    
    async getMetrics() {
        const state = await this.getSimulationState();
        return state.metrics;
    }
    
    async getVehicleMetrics() {
        // Mock data for now
        return {
            byType: [
                { type: 'CAR', count: 10 },
                { type: 'TRUCK', count: 3 },
                { type: 'MOTORCYCLE', count: 5 },
                { type: 'BUS', count: 2 }
            ],
            averageSpeedByType: [
                { type: 'CAR', averageSpeed: 35.5 },
                { type: 'TRUCK', averageSpeed: 28.2 },
                { type: 'MOTORCYCLE', averageSpeed: 42.1 },
                { type: 'BUS', averageSpeed: 25.8 }
            ],
            completedJourneys: 45,
            averageJourneyTime: 240.5
        };
    }
    
    async getRoadMetrics() {
        // Mock data for now
        return {
            mostCongestedRoads: [
                { roadId: 'HW_MAIN_EW', congestionLevel: 0.8, vehicleCount: 12 },
                { roadId: 'NORTH_1', congestionLevel: 0.6, vehicleCount: 8 }
            ],
            averageSpeedByRoadType: [
                { type: 'HIGHWAY', averageSpeed: 85.2 },
                { type: 'MAIN_ROAD', averageSpeed: 45.8 },
                { type: 'SECONDARY_ROAD', averageSpeed: 32.4 }
            ],
            totalTrafficVolume: 156
        };
    }
    
    async startSimulation() {
        return await this.sendCommand('start');
    }
    
    async stopSimulation() {
        return await this.sendCommand('stop');
    }
    
    async resetSimulation(vehicleCount = 15) {
        return await this.sendCommand('reset', { vehicleCount });
    }
    
    async addVehicle(startZone, endZone) {
        const response = await this.sendCommand('addVehicle', { startZone, endZone });
        
        // Mock vehicle response
        return {
            id: `V${Date.now()}`,
            type: 'CAR',
            x: 100 + Math.random() * 800,
            y: 100 + Math.random() * 600,
            speed: 30 + Math.random() * 20,
            isActive: true,
            onRoad: true,
            roadId: 'HW_MAIN_EW',
            progress: 0,
            startZone,
            endZone,
            route: ['HW_MAIN_EW', 'NORTH_1'],
            fuel: 85.5,
            travelPurpose: 'work'
        };
    }
    
    async removeVehicle(vehicleId) {
        return await this.sendCommand('removeVehicle', { vehicleId });
    }
    
    async setTrafficLight(lightId, state) {
        return await this.sendCommand('setTrafficLight', { lightId, state });
    }
    
    async enableEmergencyMode(intersectionId) {
        return await this.sendCommand('enableEmergencyMode', { intersectionId });
    }
    
    // Transform Java data to GraphQL format
    transformSimulationState(javaState) {
        return {
            isRunning: javaState.running || false,
            simulationTime: javaState.simulationTime || 0,
            vehicles: (javaState.vehicles || []).map(vehicle => this.transformVehicle(vehicle)),
            roads: (javaState.roads || []).map(road => this.transformRoad(road)),
            zones: (javaState.zones || []).map(zone => this.transformZone(zone)),
            intersections: (javaState.intersections || []).map(intersection => this.transformIntersection(intersection)),
            trafficLights: (javaState.lights || []).map(light => this.transformTrafficLight(light)),
            metrics: this.transformMetrics(javaState.metrics || {})
        };
    }
    
    transformVehicle(vehicle) {
        return {
            id: vehicle.id,
            type: this.mapVehicleType(vehicle.type),
            x: vehicle.x || 0,
            y: vehicle.y || 0,
            speed: vehicle.speed || 0,
            isActive: vehicle.active || false,
            onRoad: vehicle.onRoad || false,
            roadId: vehicle.roadId,
            progress: vehicle.progress || 0,
            startZone: vehicle.startZone,
            endZone: vehicle.endZone,
            route: vehicle.route || [],
            fuel: vehicle.fuel || 100,
            travelPurpose: vehicle.travelPurpose || 'unknown'
        };
    }
    
    transformRoad(road) {
        return {
            id: road.id,
            type: this.mapRoadType(road.type),
            x1: road.x1 || 0,
            y1: road.y1 || 0,
            x2: road.x2 || 0,
            y2: road.y2 || 0,
            capacity: road.capacity || 2,
            speedLimit: road.speedLimit || 50,
            congestionLevel: road.congestionLevel || 0,
            vehicleCount: road.vehicleCount || 0,
            hasTrafficLights: road.hasTrafficLights || false
        };
    }
    
    transformZone(zone) {
        return {
            id: zone.id,
            type: this.mapZoneType(zone.type),
            x: zone.x || 0,
            y: zone.y || 0,
            width: zone.width || 100,
            height: zone.height || 100,
            roadIds: zone.roadIds || []
        };
    }
    
    transformIntersection(intersection) {
        return {
            id: intersection.id,
            type: this.mapIntersectionType(intersection.type),
            x: intersection.x || 0,
            y: intersection.y || 0,
            connectedRoads: intersection.connectedRoads || [],
            waitingVehicles: intersection.waitingVehicles || 0
        };
    }
    
    transformTrafficLight(light) {
        return {
            id: light.id,
            roadId: light.roadId,
            state: this.mapTrafficLightState(light.state),
            timeRemaining: light.timeRemaining || 0,
            isEmergency: light.isEmergency || false,
            brightness: light.brightness || 1.0
        };
    }
    
    transformMetrics(metrics) {
        return {
            activeVehicles: parseInt(metrics.activeVehicles) || 0,
            totalVehicles: parseInt(metrics.totalVehicles) || 0,
            averageSpeed: parseFloat(metrics.averageSpeed) || 0,
            totalRoads: parseInt(metrics.totalRoads) || 0,
            averageCongestion: parseFloat(metrics.averageCongestion) || 0,
            redLightsCount: parseInt(metrics.redLightsCount) || 0,
            totalLights: parseInt(metrics.totalLights) || 0,
            totalZones: parseInt(metrics.totalZones) || 0,
            totalIntersections: parseInt(metrics.totalIntersections) || 0,
            simulationTime: parseFloat(metrics.simulationTime) || 0
        };
    }
    
    // Type mapping methods
    mapVehicleType(javaType) {
        const mapping = {
            'Şahsi Araç': 'CAR',
            'Kamyon': 'TRUCK',
            'Motosiklet': 'MOTORCYCLE',
            'Otobüs': 'BUS',
            'Taksi': 'TAXI',
            'Kargo Aracı': 'CARGO'
        };
        return mapping[javaType] || 'CAR';
    }
    
    mapRoadType(javaType) {
        const mapping = {
            'Otoyol': 'HIGHWAY',
            'Ana Yol': 'MAIN_ROAD',
            'Tali Yol': 'SECONDARY_ROAD',
            'Mahalle Yolu': 'LOCAL_STREET',
            'Konut Sokağı': 'RESIDENTIAL_STREET'
        };
        return mapping[javaType] || 'LOCAL_STREET';
    }
    
    mapZoneType(javaType) {
        const mapping = {
            'Konut Bölgesi': 'RESIDENTIAL',
            'Ticaret Bölgesi': 'COMMERCIAL',
            'Sanayi Bölgesi': 'INDUSTRIAL',
            'Şehir Merkezi': 'DOWNTOWN',
            'Park Alanı': 'PARK'
        };
        return mapping[javaType] || 'RESIDENTIAL';
    }
    
    mapIntersectionType(javaType) {
        const mapping = {
            'Işıklı Kavşak': 'TRAFFIC_LIGHT',
            'Stop Levhası': 'STOP_SIGN',
            'Dönel Kavşak': 'ROUNDABOUT',
            'Kontrolsüz Kavşak': 'UNCONTROLLED'
        };
        return mapping[javaType] || 'UNCONTROLLED';
    }
    
    mapTrafficLightState(javaState) {
        const mapping = {
            'RED': 'RED',
            'YELLOW': 'YELLOW',
            'GREEN': 'GREEN'
        };
        return mapping[javaState] || 'RED';
    }
    
    isConnected() {
        return this.connected && this.javaProcess && !this.javaProcess.killed;
    }
    
    async disconnect() {
        console.log('🔌 Disconnecting Java Bridge...');
        
        if (this.javaProcess) {
            this.javaProcess.kill();
            this.javaProcess = null;
        }
        
        this.connected = false;
        console.log('✅ Java Bridge disconnected');
    }
}

module.exports = JavaBridge; 