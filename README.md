# 🏙️ Traffic Simulation 2.0 - Modern Hybrid Stack

A sophisticated traffic simulation system combining **Java's computational power** with **modern web technologies**.

## 🚀 **Architecture Overview**

```
┌─────────────────────────────────────────────────────┐
│                 🌐 Frontend                         │
│            React + TypeScript                       │
│         Real-time Canvas Visualization             │
└─────────────────┬───────────────────────────────────┘
                  │ GraphQL + WebSockets
┌─────────────────▼───────────────────────────────────┐
│               🔗 API Layer                          │
│            Node.js + Express                        │
│         Apollo GraphQL Server                       │
│         Socket.io WebSocket Server                  │
└─────────────────┬───────────────────────────────────┘
                  │ IPC Communication
┌─────────────────▼───────────────────────────────────┐
│            🏭 Simulation Engine                     │
│               Pure Java                             │
│         Multi-threaded Processing                   │
│         Advanced Traffic Algorithms                 │
└─────────────────────────────────────────────────────┘
```

## ✨ **Key Features**

### 🎯 **Modern Technology Stack**
- **Frontend**: React 18 + TypeScript + Vite
- **API Layer**: Node.js + GraphQL + WebSockets  
- **Simulation Engine**: Java 21 + Multithreading
- **Real-time Communication**: Apollo Client + Socket.io

### 🏙️ **Realistic City Simulation**
- **Zone-based Architecture**: Residential, Commercial, Industrial, Downtown, Parks
- **Road Hierarchy**: Highways, Main Roads, Secondary Roads, Local Streets
- **Smart Traffic Management**: Dynamic congestion calculation, intelligent pathfinding
- **Multiple Intersection Types**: Traffic lights, Stop signs, Roundabouts, Uncontrolled

### 🚗 **Advanced Vehicle System**
- **6 Vehicle Types**: Cars, Trucks, Motorcycles, Buses, Taxis, Cargo vehicles
- **Intelligent Behavior**: Zone-based spawning, purpose-driven travel, fuel management
- **Dynamic Routing**: Real-time path recalculation based on traffic conditions

### 📊 **Real-time Analytics**
- **Live Metrics**: Vehicle counts, average speeds, congestion levels
- **Interactive Controls**: Start/Stop/Reset simulation, vehicle count adjustment
- **Visual Indicators**: Congestion heat maps, traffic light states, vehicle status

## 🛠️ **Installation & Setup**

### **Prerequisites**
- **Node.js 16+** - [Download](https://nodejs.org/)
- **Java JDK 11+** - [Download](https://www.oracle.com/java/technologies/javase-downloads.html)

### **Quick Start**
```bash
# Clone and navigate to the project
git clone <repository-url>
cd TrafficSimulation

# Start everything with one command
./start.bat    # Windows
```

The script will:
1. ✅ Verify Java and Node.js installation
2. 📦 Compile Java simulation engine
3. 📥 Install all dependencies automatically
4. 🚀 Start all services in correct order
5. 🌐 Open browser to http://localhost:5173

## 🔧 **Manual Setup** (Advanced)

### **1. Java Simulation Engine**
```bash
# Compile Java code
javac -cp "src" src/core/*.java -d compiled

# Run simulation server (standalone)
java -cp compiled src.core.SimulationServer
```

### **2. Node.js API Layer**
```bash
cd api-server
npm install
npm start    # Starts on port 4000
```

### **3. React Frontend**
```bash
cd traffic-sim-frontend
npm install
npm run dev  # Starts on port 5173
```

## 🎮 **How to Use**

### **Web Interface**
1. Open **http://localhost:5173** in your browser
2. Use the **control panel** to manage simulation
3. Adjust **vehicle count** with the slider (5-50 vehicles)
4. Watch **real-time metrics** and **traffic visualization**

### **GraphQL Playground**
- Access **http://localhost:4000/graphql** for API exploration
- Query simulation state, vehicles, roads, zones
- Execute mutations to control simulation

### **Sample GraphQL Queries**
```graphql
# Get simulation state
query {
  simulationState {
    isRunning
    vehicles {
      id
      type
      speed
      x
      y
    }
    metrics {
      activeVehicles
      averageCongestion
    }
  }
}

# Start simulation
mutation {
  startSimulation {
    success
    message
  }
}

# Reset with custom vehicle count
mutation {
  resetSimulation(vehicleCount: 25) {
    success
    message
  }
}
```

## 📡 **API Endpoints**

### **GraphQL API** (http://localhost:4000/graphql)
- `Query.simulationState` - Get complete simulation state
- `Query.vehicles` - Get all vehicles
- `Query.roads` - Get road network
- `Query.zones` - Get city zones
- `Query.metrics` - Get performance metrics
- `Mutation.startSimulation` - Start traffic simulation
- `Mutation.stopSimulation` - Stop traffic simulation
- `Mutation.resetSimulation` - Reset with new parameters

### **WebSocket Events** (ws://localhost:4000/socket.io)
- `simulationState` - Real-time state updates (500ms)
- `metricsUpdate` - Live metrics (2s intervals)
- `vehicleAdded` - New vehicle notifications
- `simulationStarted/Stopped` - Control events

### **REST Endpoints**
- `GET /health` - Service health check
- `GET /api` - API information

## 🏗️ **Project Structure**

```
TrafficSimulation/
├── 🏭 src/core/                 # Java Simulation Engine
│   ├── SimulationEngine.java    # Core simulation logic
│   ├── CityMap.java            # Realistic city layout
│   ├── Vehicle.java            # Smart vehicle behavior
│   ├── Road.java               # Dynamic road system
│   ├── TrafficLight.java       # Traffic management
│   ├── Intersection.java       # Junction handling
│   └── SimulationServer.java   # IPC communication
│
├── 🌐 api-server/               # Node.js API Layer
│   ├── src/
│   │   ├── server.js           # Express + GraphQL + WebSocket
│   │   ├── graphql/            # GraphQL schema & resolvers
│   │   └── java-bridge.js      # Java communication bridge
│   └── package.json
│
├── ⚛️ traffic-sim-frontend/     # React Frontend
│   ├── src/
│   │   ├── App.tsx             # Main React application
│   │   └── App.css             # Modern styling
│   └── package.json
│
├── 📁 compiled/                 # Java bytecode
├── 🚀 start.bat                # One-click launcher
└── 📖 README.md                # This file
```

## 🔧 **Development**

### **Adding New Features**
1. **Java Engine**: Modify core classes in `src/core/`
2. **API Layer**: Update GraphQL schema in `api-server/src/graphql/`
3. **Frontend**: Add React components in `traffic-sim-frontend/src/`

### **Debugging**
- **Java logs**: Check console output from SimulationServer
- **API logs**: Node.js server logs in terminal
- **Frontend**: Browser developer tools

### **Performance Tuning**
- Adjust WebSocket update intervals in `server.js`
- Modify vehicle spawn rates in `SimulationEngine.java`
- Optimize Canvas rendering in React components

## 🚀 **Technology Benefits**

### **Java Simulation Engine**
- ✅ **High Performance**: Multi-threaded vehicle processing
- ✅ **Robust**: Established ecosystem for complex algorithms
- ✅ **Scalable**: Handle hundreds of simultaneous vehicles
- ✅ **Memory Efficient**: Optimized garbage collection

### **Node.js API Layer**
- ✅ **Real-time**: WebSocket connections for live updates
- ✅ **Modern**: GraphQL for flexible data queries
- ✅ **Fast Development**: Rich npm ecosystem
- ✅ **Type Safety**: TypeScript integration

### **React Frontend**
- ✅ **Interactive**: Smooth Canvas-based visualization
- ✅ **Responsive**: Modern UI/UX design
- ✅ **Component-based**: Reusable, maintainable code
- ✅ **Developer Experience**: Hot reload, debugging tools

## 🎯 **Use Cases**

- **Urban Planning**: Test traffic flow scenarios
- **Algorithm Research**: Experiment with routing algorithms  
- **Education**: Demonstrate traffic engineering concepts
- **Simulation Studies**: Performance benchmarking
- **Game Development**: Traffic system prototyping

## 🤝 **Contributing**

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

## 📄 **License**

This project is licensed under the MIT License - see the LICENSE file for details.

---

**🎉 Enjoy exploring the future of traffic simulation with our modern hybrid stack!** 🚗💨