const { gql } = require('apollo-server-express');

// GraphQL Type Definitions
const typeDefs = gql`
  type Query {
    # Simulation state
    simulationState: SimulationState!
    
    # Individual components
    vehicles: [Vehicle!]!
    roads: [Road!]!
    zones: [Zone!]!
    intersections: [Intersection!]!
    trafficLights: [TrafficLight!]!
    
    # Metrics and analytics
    metrics: Metrics!
    vehicleMetrics: VehicleMetrics!
    roadMetrics: RoadMetrics!
    
    # Health check
    health: HealthStatus!
  }
  
  type Mutation {
    # Simulation control
    startSimulation: MutationResponse!
    stopSimulation: MutationResponse!
    resetSimulation(vehicleCount: Int = 15): MutationResponse!
    
    # Vehicle management
    addVehicle(startZone: String!, endZone: String!): Vehicle
    removeVehicle(vehicleId: String!): MutationResponse!
    
    # Traffic light control
    setTrafficLight(lightId: String!, state: TrafficLightState!): MutationResponse!
    enableEmergencyMode(intersectionId: String!): MutationResponse!
  }
  
  type Subscription {
    # Real-time updates
    simulationUpdated: SimulationState!
    vehicleUpdated: Vehicle!
    metricsUpdated: Metrics!
    trafficLightChanged: TrafficLight!
  }
  
  # Core Types
  type SimulationState {
    isRunning: Boolean!
    simulationTime: Float!
    vehicles: [Vehicle!]!
    roads: [Road!]!
    zones: [Zone!]!
    intersections: [Intersection!]!
    trafficLights: [TrafficLight!]!
    metrics: Metrics!
  }
  
  type Vehicle {
    id: String!
    type: VehicleType!
    x: Float!
    y: Float!
    speed: Float!
    isActive: Boolean!
    onRoad: Boolean!
    roadId: String
    progress: Float
    startZone: String
    endZone: String
    route: [String!]!
    fuel: Float
    travelPurpose: String
  }
  
  type Road {
    id: String!
    type: RoadType!
    x1: Float!
    y1: Float!
    x2: Float!
    y2: Float!
    capacity: Int!
    speedLimit: Int!
    congestionLevel: Float!
    vehicleCount: Int!
    hasTrafficLights: Boolean!
  }
  
  type Zone {
    id: String!
    type: ZoneType!
    x: Float!
    y: Float!
    width: Float!
    height: Float!
    roadIds: [String!]!
  }
  
  type Intersection {
    id: String!
    type: IntersectionType!
    x: Float!
    y: Float!
    connectedRoads: [String!]!
    waitingVehicles: Int!
  }
  
  type TrafficLight {
    id: String!
    roadId: String!
    state: TrafficLightState!
    timeRemaining: Float!
    isEmergency: Boolean!
    brightness: Float!
  }
  
  type Metrics {
    activeVehicles: Int!
    totalVehicles: Int!
    averageSpeed: Float!
    totalRoads: Int!
    averageCongestion: Float!
    redLightsCount: Int!
    totalLights: Int!
    totalZones: Int!
    totalIntersections: Int!
    simulationTime: Float!
  }
  
  type VehicleMetrics {
    byType: [VehicleTypeCount!]!
    averageSpeedByType: [VehicleTypeSpeed!]!
    completedJourneys: Int!
    averageJourneyTime: Float!
  }
  
  type RoadMetrics {
    mostCongestedRoads: [RoadCongestion!]!
    averageSpeedByRoadType: [RoadTypeSpeed!]!
    totalTrafficVolume: Int!
  }
  
  type HealthStatus {
    status: String!
    javaEngine: String!
    connectedClients: Int!
    uptime: Float!
    timestamp: String!
  }
  
  type MutationResponse {
    success: Boolean!
    message: String!
    data: String
  }
  
  # Helper Types
  type VehicleTypeCount {
    type: VehicleType!
    count: Int!
  }
  
  type VehicleTypeSpeed {
    type: VehicleType!
    averageSpeed: Float!
  }
  
  type RoadCongestion {
    roadId: String!
    congestionLevel: Float!
    vehicleCount: Int!
  }
  
  type RoadTypeSpeed {
    type: RoadType!
    averageSpeed: Float!
  }
  
  # Enums
  enum VehicleType {
    CAR
    TRUCK
    MOTORCYCLE
    BUS
    TAXI
    CARGO
  }
  
  enum RoadType {
    HIGHWAY
    MAIN_ROAD
    SECONDARY_ROAD
    LOCAL_STREET
    RESIDENTIAL_STREET
  }
  
  enum ZoneType {
    RESIDENTIAL
    COMMERCIAL
    INDUSTRIAL
    DOWNTOWN
    PARK
  }
  
  enum IntersectionType {
    TRAFFIC_LIGHT
    STOP_SIGN
    ROUNDABOUT
    UNCONTROLLED
  }
  
  enum TrafficLightState {
    RED
    YELLOW
    GREEN
  }
`;

// GraphQL Resolvers
const resolvers = (javaBridge, io) => ({
  Query: {
    simulationState: async () => {
      return await javaBridge.getSimulationState();
    },
    
    vehicles: async () => {
      return await javaBridge.getVehicles();
    },
    
    roads: async () => {
      return await javaBridge.getRoads();
    },
    
    zones: async () => {
      return await javaBridge.getZones();
    },
    
    intersections: async () => {
      return await javaBridge.getIntersections();
    },
    
    trafficLights: async () => {
      return await javaBridge.getTrafficLights();
    },
    
    metrics: async () => {
      return await javaBridge.getMetrics();
    },
    
    vehicleMetrics: async () => {
      return await javaBridge.getVehicleMetrics();
    },
    
    roadMetrics: async () => {
      return await javaBridge.getRoadMetrics();
    },
    
    health: async () => {
      return {
        status: 'OK',
        javaEngine: javaBridge.isConnected() ? 'Connected' : 'Disconnected',
        connectedClients: io.engine.clientsCount,
        uptime: process.uptime(),
        timestamp: new Date().toISOString()
      };
    }
  },
  
  Mutation: {
    startSimulation: async () => {
      try {
        await javaBridge.startSimulation();
        
        // Notify all connected clients
        io.emit('simulationStarted', { timestamp: new Date().toISOString() });
        
        return {
          success: true,
          message: 'Simulation started successfully'
        };
      } catch (error) {
        return {
          success: false,
          message: `Failed to start simulation: ${error.message}`
        };
      }
    },
    
    stopSimulation: async () => {
      try {
        await javaBridge.stopSimulation();
        
        // Notify all connected clients
        io.emit('simulationStopped', { timestamp: new Date().toISOString() });
        
        return {
          success: true,
          message: 'Simulation stopped successfully'
        };
      } catch (error) {
        return {
          success: false,
          message: `Failed to stop simulation: ${error.message}`
        };
      }
    },
    
    resetSimulation: async (_, { vehicleCount = 15 }) => {
      try {
        await javaBridge.resetSimulation(vehicleCount);
        
        // Notify all connected clients
        io.emit('simulationReset', { 
          vehicleCount, 
          timestamp: new Date().toISOString() 
        });
        
        return {
          success: true,
          message: `Simulation reset with ${vehicleCount} vehicles`
        };
      } catch (error) {
        return {
          success: false,
          message: `Failed to reset simulation: ${error.message}`
        };
      }
    },
    
    addVehicle: async (_, { startZone, endZone }) => {
      try {
        const vehicle = await javaBridge.addVehicle(startZone, endZone);
        
        // Notify all connected clients
        io.emit('vehicleAdded', vehicle);
        
        return vehicle;
      } catch (error) {
        throw new Error(`Failed to add vehicle: ${error.message}`);
      }
    },
    
    removeVehicle: async (_, { vehicleId }) => {
      try {
        await javaBridge.removeVehicle(vehicleId);
        
        // Notify all connected clients
        io.emit('vehicleRemoved', { vehicleId });
        
        return {
          success: true,
          message: `Vehicle ${vehicleId} removed successfully`
        };
      } catch (error) {
        return {
          success: false,
          message: `Failed to remove vehicle: ${error.message}`
        };
      }
    },
    
    setTrafficLight: async (_, { lightId, state }) => {
      try {
        await javaBridge.setTrafficLight(lightId, state);
        
        // Notify all connected clients
        io.emit('trafficLightChanged', { lightId, state });
        
        return {
          success: true,
          message: `Traffic light ${lightId} set to ${state}`
        };
      } catch (error) {
        return {
          success: false,
          message: `Failed to set traffic light: ${error.message}`
        };
      }
    },
    
    enableEmergencyMode: async (_, { intersectionId }) => {
      try {
        await javaBridge.enableEmergencyMode(intersectionId);
        
        // Notify all connected clients
        io.emit('emergencyModeEnabled', { intersectionId });
        
        return {
          success: true,
          message: `Emergency mode enabled for intersection ${intersectionId}`
        };
      } catch (error) {
        return {
          success: false,
          message: `Failed to enable emergency mode: ${error.message}`
        };
      }
    }
  }
});

module.exports = { typeDefs, resolvers }; 