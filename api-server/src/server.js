const express = require('express');
const { ApolloServer } = require('apollo-server-express');
const { createServer } = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const { spawn } = require('child_process');
const path = require('path');

const { typeDefs, resolvers } = require('./graphql');
const JavaBridge = require('./java-bridge');

class TrafficSimulationServer {
    constructor() {
        this.app = express();
        this.httpServer = createServer(this.app);
        this.io = new Server(this.httpServer, {
            cors: {
                origin: ["http://localhost:3000", "http://localhost:5173"],
                methods: ["GET", "POST"]
            }
        });
        
        this.javaBridge = new JavaBridge();
        this.connectedClients = new Set();
        
        this.setupMiddleware();
        this.setupGraphQL();
        this.setupWebSocket();
        this.setupRoutes();
    }
    
    setupMiddleware() {
        this.app.use(cors({
            origin: ["http://localhost:3000", "http://localhost:5173"],
            credentials: true
        }));
        this.app.use(express.json());
        this.app.use(express.static('public'));
    }
    
    async setupGraphQL() {
        this.apolloServer = new ApolloServer({
            typeDefs,
            resolvers: resolvers(this.javaBridge, this.io),
            context: ({ req }) => ({
                javaBridge: this.javaBridge,
                io: this.io
            })
        });
        
        await this.apolloServer.start();
        this.apolloServer.applyMiddleware({ 
            app: this.app, 
            path: '/graphql',
            cors: false 
        });
    }
    
    setupWebSocket() {
        this.io.on('connection', (socket) => {
            console.log('🔗 Client connected:', socket.id);
            this.connectedClients.add(socket.id);
            
            // Send current state immediately
            this.sendSimulationState(socket);
            
            socket.on('disconnect', () => {
                console.log('🔌 Client disconnected:', socket.id);
                this.connectedClients.delete(socket.id);
            });
            
            // Client can request specific data
            socket.on('requestVehicles', () => {
                this.sendVehicleData(socket);
            });
            
            socket.on('requestMetrics', () => {
                this.sendMetrics(socket);
            });
        });
        
        // Start real-time updates
        this.startRealTimeUpdates();
    }
    
    setupRoutes() {
        // Health check
        this.app.get('/health', (req, res) => {
            res.json({ 
                status: 'OK', 
                timestamp: new Date().toISOString(),
                javaEngine: this.javaBridge.isConnected() ? 'Connected' : 'Disconnected',
                connectedClients: this.connectedClients.size
            });
        });
        
        // API Info
        this.app.get('/api', (req, res) => {
            res.json({
                name: 'Traffic Simulation API',
                version: '2.0.0',
                endpoints: {
                    graphql: '/graphql',
                    websocket: '/socket.io',
                    health: '/health'
                },
                features: [
                    'Real-time vehicle tracking',
                    'GraphQL queries and mutations',
                    'WebSocket live updates',
                    'Java simulation engine bridge'
                ]
            });
        });
    }
    
    startRealTimeUpdates() {
        // Update every 500ms for smooth real-time experience
        setInterval(() => {
            if (this.connectedClients.size > 0) {
                this.broadcastSimulationState();
            }
        }, 500);
        
        // Update metrics every 2 seconds
        setInterval(() => {
            if (this.connectedClients.size > 0) {
                this.broadcastMetrics();
            }
        }, 2000);
    }
    
    async sendSimulationState(socket) {
        try {
            const state = await this.javaBridge.getSimulationState();
            socket.emit('simulationState', state);
        } catch (error) {
            console.error('Error sending simulation state:', error);
            socket.emit('error', { message: 'Failed to get simulation state' });
        }
    }
    
    async sendVehicleData(socket) {
        try {
            const vehicles = await this.javaBridge.getVehicles();
            socket.emit('vehicleUpdate', vehicles);
        } catch (error) {
            console.error('Error sending vehicle data:', error);
        }
    }
    
    async sendMetrics(socket) {
        try {
            const metrics = await this.javaBridge.getMetrics();
            socket.emit('metricsUpdate', metrics);
        } catch (error) {
            console.error('Error sending metrics:', error);
        }
    }
    
    async broadcastSimulationState() {
        try {
            const state = await this.javaBridge.getSimulationState();
            this.io.emit('simulationState', state);
        } catch (error) {
            console.error('Error broadcasting simulation state:', error);
        }
    }
    
    async broadcastMetrics() {
        try {
            const metrics = await this.javaBridge.getMetrics();
            this.io.emit('metricsUpdate', metrics);
        } catch (error) {
            console.error('Error broadcasting metrics:', error);
        }
    }
    
    async start(port = 4000) {
        try {
            // Initialize Java bridge
            await this.javaBridge.initialize();
            
            this.httpServer.listen(port, () => {
                console.log('\n🚀 Traffic Simulation API Server');
                console.log('=====================================');
                console.log(`🌐 HTTP Server: http://localhost:${port}`);
                console.log(`🔗 GraphQL Playground: http://localhost:${port}/graphql`);
                console.log(`⚡ WebSocket: ws://localhost:${port}/socket.io`);
                console.log(`💊 Health Check: http://localhost:${port}/health`);
                console.log('=====================================');
                console.log('🔄 Real-time updates: Active');
                console.log('🟢 Java Simulation Engine: Connected');
                console.log('✅ Server ready for connections!\\n');
            });
            
        } catch (error) {
            console.error('❌ Failed to start server:', error);
            process.exit(1);
        }
    }
    
    async stop() {
        console.log('🛑 Stopping Traffic Simulation API Server...');
        
        if (this.apolloServer) {
            await this.apolloServer.stop();
        }
        
        if (this.javaBridge) {
            await this.javaBridge.disconnect();
        }
        
        this.httpServer.close(() => {
            console.log('✅ Server stopped gracefully');
            process.exit(0);
        });
    }
}

// Graceful shutdown
process.on('SIGTERM', async () => {
    if (global.server) {
        await global.server.stop();
    }
});

process.on('SIGINT', async () => {
    if (global.server) {
        await global.server.stop();
    }
});

// Start server
const PORT = process.env.PORT || 4000;
global.server = new TrafficSimulationServer();
global.server.start(PORT);

module.exports = TrafficSimulationServer; 