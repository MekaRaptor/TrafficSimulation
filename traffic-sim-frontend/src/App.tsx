import React, { useState, useEffect } from 'react';
import { ApolloClient, InMemoryCache, ApolloProvider, gql, useQuery, useMutation } from '@apollo/client';
import { io, Socket } from 'socket.io-client';
import TrafficAnalysisPanel from './TrafficAnalysis';
import VehicleDetailsPanel from './VehicleDetails';
import './App.css';

// Apollo Client Setup
const client = new ApolloClient({
  uri: 'http://localhost:4000/graphql',
  cache: new InMemoryCache(),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-and-network',
      errorPolicy: 'all',
    },
    query: {
      fetchPolicy: 'network-only',
      errorPolicy: 'all',
    },
  },
  connectToDevTools: true,
});

// GraphQL Queries
const GET_SIMULATION_STATE = gql`
  query GetSimulationState {
    simulationState {
      isRunning
      simulationTime
      vehicles {
        id
        threadName
        threadState
        type
        x
        y
        speed
        isActive
        onRoad
        roadId
        progress
        startZone
        endZone
        routeDensity
        estimatedTime
        distanceTraveled
        journeyStartTime
        roadsVisited
      }
      roads {
        id
        type
        x1
        y1
        x2
        y2
        capacity
        speedLimit
        congestionLevel
        vehicleCount
        hasTrafficLights
        baseDensity
        currentDensity
        densityLevel
        densityColor
        efficiency
      }
      trafficLights {
        id
        roadId
        state
        timeRemaining
        isEmergency
        brightness
        displayName
        active
        x
        y
        visible
        stateColor
      }
      zones {
        id
        type
        x
        y
        width
        height
      }
      metrics {
        activeVehicles
        totalVehicles
        averageSpeed
        totalRoads
        averageCongestion
        simulationTime
      }
    }
  }
`;

const GET_HEALTH = gql`
  query GetHealth {
    health {
      status
      javaEngine
      connectedClients
      uptime
      timestamp
    }
  }
`;

// GraphQL Mutations
const START_SIMULATION = gql`
  mutation StartSimulation {
    startSimulation {
      success
      message
    }
  }
`;

const STOP_SIMULATION = gql`
  mutation StopSimulation {
    stopSimulation {
      success
      message
    }
  }
`;

const RESET_SIMULATION = gql`
  mutation ResetSimulation($vehicleCount: Int) {
    resetSimulation(vehicleCount: $vehicleCount) {
      success
      message
    }
  }
`;

// Types
interface Vehicle {
  id: string;
  threadName?: string;
  threadState?: string;
  type: string;
  x: number;
  y: number;
  speed: number;
  isActive: boolean;
  onRoad: boolean;
  roadId?: string;
  progress?: number;
  startZone?: string;
  endZone?: string;
  routeDensity?: number;
  estimatedTime?: number;
  distanceTraveled?: number;
  journeyStartTime?: string;
  roadsVisited?: number;
}

interface Road {
  id: string;
  type: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  capacity?: number;
  speedLimit?: number;
  congestionLevel: number;
  vehicleCount: number;
  hasTrafficLights?: boolean;
  baseDensity?: number;
  currentDensity?: number;
  densityLevel?: string;
  densityColor?: string;
  efficiency?: number;
}

interface TrafficLight {
  id: string;
  roadId: string;
  state: string;
  timeRemaining: number;
  isEmergency: boolean;
  brightness: number;
  displayName: string;
  active: boolean;
  x: number;
  y: number;
  visible: boolean;
  stateColor: string;
}

interface Zone {
  id: string;
  type: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

interface Metrics {
  activeVehicles: number;
  totalVehicles: number;
  averageSpeed: number;
  totalRoads: number;
  averageCongestion: number;
  simulationTime: number;
}

interface SimulationState {
  isRunning: boolean;
  simulationTime: number;
  vehicles: Vehicle[];
  roads: Road[];
  trafficLights: TrafficLight[];
  zones: Zone[];
  metrics: Metrics;
}

interface HealthData {
  status: string;
  javaEngine: string;
  connectedClients: number;
  uptime: number;
  timestamp: string;
}

// Main Components
const TrafficSimulationApp: React.FC = () => {
  const [socket, setSocket] = useState<Socket | null>(null);
  const [realTimeData, setRealTimeData] = useState<SimulationState | null>(null);
  const [selectedVehicleCount, setSelectedVehicleCount] = useState(15);
  const [connectionAttempts, setConnectionAttempts] = useState(0);
  const [isRetrying, setIsRetrying] = useState(false);

  // GraphQL hooks
  const { data: simulationData, loading, error, refetch } = useQuery(GET_SIMULATION_STATE, {
    pollInterval: 2000, // Poll every 2 seconds as fallback
    notifyOnNetworkStatusChange: true,
    skip: connectionAttempts > 3, // Skip after 3 failed attempts
    onError: (error) => {
      console.error('GraphQL Error:', error);
      console.error('Network Error:', error.networkError);
      console.error('GraphQL Errors:', error.graphQLErrors);
      setConnectionAttempts(prev => prev + 1);
    },
    onCompleted: () => {
      setConnectionAttempts(0); // Reset on successful connection
    }
  });
  
  const { data: healthData, error: healthError } = useQuery(GET_HEALTH, {
    pollInterval: 5000,
    skip: connectionAttempts > 3,
    onError: (error) => {
      console.error('Health Query Error:', error);
    }
  });

  const [startSimulation] = useMutation(START_SIMULATION);
  const [stopSimulation] = useMutation(STOP_SIMULATION);
  const [resetSimulation] = useMutation(RESET_SIMULATION);

  // WebSocket connection
  useEffect(() => {
    const newSocket = io('http://localhost:4000');
    setSocket(newSocket);

    newSocket.on('connect', () => {
      console.log('🔗 Connected to WebSocket server');
    });

    newSocket.on('simulationState', (data: SimulationState) => {
      console.log('📡 Real-time data received:', data);
      setRealTimeData(data);
    });

    newSocket.on('disconnect', () => {
      console.log('🔌 Disconnected from WebSocket server');
    });

    newSocket.on('error', (error) => {
      console.error('❌ WebSocket error:', error);
    });

    return () => {
      newSocket.close();
    };
  }, []);

  // Use socket for debugging connection status
  const connectionStatus = socket?.connected ? 'Connected' : 'Disconnected';

  // Use real-time data if available, otherwise fallback to GraphQL polling
  const currentData = realTimeData || simulationData?.simulationState;

  const handleStart = async () => {
    try {
      const result = await startSimulation();
      console.log('✅ Simulation started:', result.data.startSimulation.message);
      refetch();
    } catch (err) {
      console.error('❌ Failed to start simulation:', err);
    }
  };

  const handleStop = async () => {
    try {
      const result = await stopSimulation();
      console.log('🛑 Simulation stopped:', result.data.stopSimulation.message);
      refetch();
    } catch (err) {
      console.error('❌ Failed to stop simulation:', err);
    }
  };

  const handleReset = async () => {
    try {
      const result = await resetSimulation({ 
        variables: { vehicleCount: selectedVehicleCount } 
      });
      console.log('🔄 Simulation reset:', result.data.resetSimulation.message);
      refetch();
    } catch (err) {
      console.error('❌ Failed to reset simulation:', err);
    }
  };

  const handleRetry = async () => {
    setIsRetrying(true);
    setConnectionAttempts(0);
    
    try {
      await refetch();
      console.log('🔄 Manual retry successful');
    } catch (error) {
      console.error('🔄 Manual retry failed:', error);
    } finally {
      setIsRetrying(false);
    }
  };

  if (loading || isRetrying) return <LoadingScreen retrying={isRetrying} />;
  if (error) return <ErrorScreen error={error} healthError={healthError} onRetry={handleRetry} />;

  return (
    <div className="traffic-simulation-app">
      <Header healthData={healthData?.health} />
      
      <div className="app-layout">
        <Sidebar 
          metrics={currentData?.metrics}
          isRunning={currentData?.isRunning}
          onStart={handleStart}
          onStop={handleStop}
          onReset={handleReset}
          vehicleCount={selectedVehicleCount}
          onVehicleCountChange={setSelectedVehicleCount}
          connectionStatus={connectionStatus}
        />
        
        <MainCanvas 
          vehicles={currentData?.vehicles || []}
          roads={currentData?.roads || []}
          zones={currentData?.zones || []}
          trafficLights={currentData?.trafficLights || []}
          isRunning={currentData?.isRunning || false}
        />

        <div className="right-panel">
          <VehicleDetailsPanel vehicles={currentData?.vehicles || []} />
        </div>
      </div>
    </div>
  );
};

// Header Component
const Header: React.FC<{ healthData?: HealthData }> = ({ healthData }) => (
  <header className="app-header">
    <div className="header-content">
      <h1>🏙️ Traffic Simulation 2.0</h1>
      <div className="header-status">
        <div className="status-item">
          <span className={`status-dot ${healthData?.javaEngine === 'Connected' ? 'connected' : 'disconnected'}`}></span>
          Java Engine: {healthData?.javaEngine || 'Unknown'}
        </div>
        <div className="status-item">
          <span className="status-dot connected"></span>
          Clients: {healthData?.connectedClients || 0}
        </div>
        <div className="status-item">
          Uptime: {healthData?.uptime ? Math.floor(healthData.uptime / 60) : 0}m
        </div>
      </div>
    </div>
  </header>
);

// Sidebar Component
const Sidebar: React.FC<{
  metrics?: Metrics;
  isRunning?: boolean;
  onStart: () => void;
  onStop: () => void;
  onReset: () => void;
  vehicleCount: number;
  onVehicleCountChange: (count: number) => void;
  connectionStatus: string;
}> = ({ metrics, isRunning, onStart, onStop, onReset, vehicleCount, onVehicleCountChange, connectionStatus }) => (
  <aside className="sidebar">
    <div className="control-panel">
      <h3>🎮 Simulation Control</h3>
      
      <div className="button-group">
        <button 
          className={`control-btn start-btn ${isRunning ? 'disabled' : ''}`}
          onClick={onStart}
          disabled={isRunning}
        >
          ▶️ Start Traffic
        </button>
        
        <button 
          className={`control-btn stop-btn ${!isRunning ? 'disabled' : ''}`}
          onClick={onStop}
          disabled={!isRunning}
        >
          ⏹️ Stop Traffic
        </button>
        
        <div className="reset-section">
          <label htmlFor="vehicleCount">Vehicle Count:</label>
          <input
            id="vehicleCount"
            type="range"
            min="5"
            max="50"
            value={vehicleCount}
            onChange={(e) => onVehicleCountChange(parseInt(e.target.value))}
          />
          <span>{vehicleCount}</span>
          
          <button 
            className="control-btn reset-btn"
            onClick={onReset}
          >
            🔄 Reset ({vehicleCount} vehicles)
          </button>
        </div>
      </div>
    </div>

    <div className="metrics-panel">
      <h3>📊 Live Metrics</h3>
      {metrics && (
        <div className="metrics-grid">
          <MetricCard title="Active Vehicles" value={metrics.activeVehicles} icon="🚗" />
          <MetricCard title="Total Roads" value={metrics.totalRoads} icon="🛣️" />
          <MetricCard title="Avg Speed" value={`${metrics.averageSpeed.toFixed(1)} km/h`} icon="⚡" />
          <MetricCard title="Congestion" value={`${metrics.averageCongestion.toFixed(1)}%`} icon="⚠️" />
          <MetricCard title="Simulation Time" value={`${(metrics.simulationTime / 1000).toFixed(1)}s`} icon="⏱️" />
        </div>
      )}
    </div>
    
    <div className="traffic-analysis-panel">
      <h3>📊 Traffic Analysis</h3>
      <TrafficAnalysisPanel connectionStatus={connectionStatus} />
    </div>
  </aside>
);

// Metric Card Component
const MetricCard: React.FC<{ title: string; value: string | number; icon: string }> = ({ title, value, icon }) => (
  <div className="metric-card">
    <div className="metric-icon">{icon}</div>
    <div className="metric-content">
      <div className="metric-value">{value}</div>
      <div className="metric-title">{title}</div>
    </div>
  </div>
);

// Main Canvas Component
const MainCanvas: React.FC<{
  vehicles: Vehicle[];
  roads: Road[];
  zones: Zone[];
  trafficLights?: TrafficLight[];
  isRunning: boolean;
}> = ({ vehicles, roads, zones, trafficLights = [], isRunning }) => {
  const canvasRef = React.useRef<HTMLCanvasElement>(null);

  React.useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Debug info
    console.log(`🎯 Canvas size: ${canvas.width}x${canvas.height}`);
    console.log(`🚗 Total vehicles: ${vehicles.length}, Active: ${vehicles.filter(v => v.isActive).length}`);

    // Draw canvas border for debugging
    ctx.strokeStyle = 'red';
    ctx.lineWidth = 2;
    ctx.strokeRect(0, 0, canvas.width, canvas.height);

    // Draw zones
    zones.forEach(zone => {
      ctx.fillStyle = getZoneColor(zone.type);
      ctx.globalAlpha = 0.3;
      ctx.fillRect(zone.x, zone.y, zone.width, zone.height);
      
      ctx.globalAlpha = 0.8;
      ctx.strokeStyle = getZoneColor(zone.type);
      ctx.lineWidth = 2;
      ctx.strokeRect(zone.x, zone.y, zone.width, zone.height);
      ctx.globalAlpha = 1.0;

      // Zone label
      ctx.fillStyle = 'white';
      ctx.font = 'bold 12px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(
        getZoneLabel(zone.type), 
        zone.x + zone.width / 2, 
        zone.y + zone.height / 2
      );
    });

    // Draw roads with density colors
    roads.forEach(road => {
      const width = getRoadWidth(road.type);
      const color = road.densityColor || getRoadColor(road.type, road.congestionLevel);
      
      ctx.strokeStyle = color;
      ctx.lineWidth = width;
      ctx.lineCap = 'round';
      
      ctx.beginPath();
      ctx.moveTo(road.x1, road.y1);
      ctx.lineTo(road.x2, road.y2);
      ctx.stroke();

      // Road markings for major roads
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

      // Display road density info
      if (road.densityLevel) {
        const midX = (road.x1 + road.x2) / 2;
        const midY = (road.y1 + road.y2) / 2;
        
        ctx.fillStyle = 'rgba(0,0,0,0.7)';
        ctx.fillRect(midX - 25, midY - 8, 50, 16);
        
        ctx.fillStyle = 'white';
        ctx.font = 'bold 10px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(road.densityLevel, midX, midY + 3);
      }
    });

    // Draw traffic lights
    trafficLights.forEach(light => {
      if (!light.visible) return;

      const radius = 8;
      
      // Light background
      ctx.fillStyle = 'black';
      ctx.beginPath();
      ctx.arc(light.x, light.y, radius + 2, 0, 2 * Math.PI);
      ctx.fill();

      // Light state
      ctx.fillStyle = light.stateColor;
      ctx.beginPath();
      ctx.arc(light.x, light.y, radius, 0, 2 * Math.PI);
      ctx.fill();

      // Light border
      ctx.strokeStyle = 'white';
      ctx.lineWidth = 2;
      ctx.stroke();

      // Light display name
      ctx.fillStyle = 'white';
      ctx.font = 'bold 8px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(light.state, light.x, light.y - radius - 5);
    });

    // Draw vehicles with enhanced info
    vehicles.forEach(vehicle => {
      if (!vehicle.isActive) return;

      const size = getVehicleSize(vehicle.type);
      const emoji = getVehicleEmoji(vehicle.type);
      const color = getVehicleColor(vehicle.type);

      // Debug: Log vehicle positions
      console.log(`🚗 Rendering vehicle ${vehicle.id} at (${vehicle.x}, ${vehicle.y})`);

      // Vehicle shadow
      ctx.fillStyle = 'rgba(0,0,0,0.3)';
      ctx.fillRect(vehicle.x + 2, vehicle.y + 2, size, size);

      // Vehicle body (larger and more visible)
      ctx.fillStyle = color;
      ctx.fillRect(vehicle.x - size/2, vehicle.y - size/2, size, size);
      
      // Vehicle border for better visibility
      ctx.strokeStyle = 'white';
      ctx.lineWidth = 2;
      ctx.strokeRect(vehicle.x - size/2, vehicle.y - size/2, size, size);

      // Vehicle emoji (larger)
      ctx.font = `${Math.max(size, 16)}px Arial`;
      ctx.textAlign = 'center';
      ctx.fillStyle = 'white';
      ctx.strokeStyle = 'black';
      ctx.lineWidth = 1;
      ctx.strokeText(emoji, vehicle.x, vehicle.y + size/3);
      ctx.fillText(emoji, vehicle.x, vehicle.y + size/3);

      // Vehicle ID for debugging
      ctx.fillStyle = 'yellow';
      ctx.font = 'bold 12px Arial';
      ctx.fillText(vehicle.id, vehicle.x, vehicle.y - size/2 - 5);

      // Enhanced vehicle info
      if (vehicle.threadName) {
        ctx.fillStyle = 'rgba(0,0,0,0.8)';
        ctx.fillRect(vehicle.x - 40, vehicle.y - size/2 - 35, 80, 15);
        
        ctx.fillStyle = 'white';
        ctx.font = 'bold 8px Arial';
        ctx.fillText(vehicle.threadName, vehicle.x, vehicle.y - size/2 - 23);
      }

      // Speed and status indicator
      if (vehicle.speed > 0 && vehicle.onRoad) {
        ctx.fillStyle = 'rgba(0,255,0,0.9)';
        ctx.font = 'bold 10px Arial';
        ctx.fillText(`${Math.round(vehicle.speed)} km/h`, vehicle.x, vehicle.y - size/2 - 40);
      }

      // Distance traveled
      if (vehicle.distanceTraveled) {
        ctx.fillStyle = 'rgba(0,150,255,0.9)';
        ctx.font = 'bold 8px Arial';
        ctx.fillText(`${vehicle.distanceTraveled.toFixed(1)} km`, vehicle.x, vehicle.y + size/2 + 15);
      }

      // Position coordinates (debug)
      ctx.fillStyle = 'rgba(255,255,255,0.8)';
      ctx.font = 'bold 8px Arial';
      ctx.fillText(`(${vehicle.x.toFixed(0)}, ${vehicle.y.toFixed(0)})`, vehicle.x, vehicle.y + size/2 + 25);
    });

  }, [vehicles, roads, zones, trafficLights]);

  return (
    <main className="main-canvas">
      <div className="canvas-header">
        <h2>🗺️ City Traffic Visualization</h2>
        <div className="simulation-status">
          <span className={`status-indicator ${isRunning ? 'running' : 'stopped'}`}></span>
          {isRunning ? 'Simulation Running' : 'Simulation Stopped'}
        </div>
      </div>
      <canvas 
        ref={canvasRef}
        width={1000}
        height={800}
        className="city-canvas"
      />
    </main>
  );
};

// Loading & Error Components
const LoadingScreen: React.FC<{ retrying: boolean }> = ({ retrying }) => (
  <div className="loading-screen">
    <div className="loading-spinner"></div>
    <h2>Loading Traffic Simulation...</h2>
    <p>Connecting to Java engine and initializing city...</p>
    {retrying && <p>Retrying connection...</p>}
  </div>
);

const ErrorScreen: React.FC<{ error: Error; healthError?: Error; onRetry: () => void }> = ({ error, healthError, onRetry }) => (
  <div className="error-screen">
    <h2>❌ Connection Error</h2>
    <p>Failed to connect to the simulation server.</p>
    
    <div className="error-details">
      <details>
        <summary>Main Error Details</summary>
        <pre>{error.message}</pre>
      </details>
      
      {healthError && (
        <details>
          <summary>Health Check Error</summary>
          <pre>{healthError.message}</pre>
        </details>
      )}
      
      <details>
        <summary>Debug Information</summary>
        <pre>{`
Backend URL: http://localhost:4000/graphql
Frontend URL: ${window.location.origin}
Network Status: ${navigator.onLine ? 'Online' : 'Offline'}
User Agent: ${navigator.userAgent}
        `}</pre>
      </details>
    </div>
    
    <div className="connection-help">
      <h3>🔧 Troubleshooting Steps:</h3>
      <ol>
        <li>Make sure backend is running: <code>./start.bat</code></li>
        <li>Check if Java engine is connected</li>
        <li>Verify port 4000 is accessible</li>
        <li>Try opening: <a href="http://localhost:4000/health" target="_blank">http://localhost:4000/health</a></li>
      </ol>
    </div>
    
    <button onClick={onRetry}>🔄 Retry</button>
  </div>
);

// Helper Functions
const getZoneColor = (type: string): string => {
  const colors = {
    RESIDENTIAL: '#2E8B57',
    COMMERCIAL: '#4169E1', 
    INDUSTRIAL: '#FF8C00',
    DOWNTOWN: '#8A2BE2',
    PARK: '#98FB98'
  };
  return colors[type as keyof typeof colors] || '#808080';
};

const getZoneLabel = (type: string): string => {
  const labels = {
    RESIDENTIAL: 'Residential',
    COMMERCIAL: 'Commercial',
    INDUSTRIAL: 'Industrial', 
    DOWNTOWN: 'Downtown',
    PARK: 'Park'
  };
  return labels[type as keyof typeof labels] || type;
};

const getRoadWidth = (type: string): number => {
  const widths = {
    HIGHWAY: 8,
    MAIN_ROAD: 5,
    SECONDARY_ROAD: 4,
    LOCAL_STREET: 3,
    RESIDENTIAL_STREET: 2
  };
  return widths[type as keyof typeof widths] || 3;
};

const getRoadColor = (type: string, congestion: number): string => {
  if (congestion > 0.7) return '#FF4444';
  if (congestion > 0.4) return '#FFAA44';
  
  const colors = {
    HIGHWAY: '#FFD700',
    MAIN_ROAD: '#C0C0C0',
    SECONDARY_ROAD: '#808080',
    LOCAL_STREET: '#696969',
    RESIDENTIAL_STREET: '#555555'
  };
  return colors[type as keyof typeof colors] || '#808080';
};

const getVehicleSize = (type: string): number => {
  const sizes = {
    CAR: 12,
    TRUCK: 18,
    MOTORCYCLE: 8,
    BUS: 20,
    TAXI: 12,
    CARGO: 15
  };
  return sizes[type as keyof typeof sizes] || 12;
};

const getVehicleEmoji = (type: string): string => {
  const emojis = {
    CAR: '🚗',
    TRUCK: '🚛',
    MOTORCYCLE: '🏍️',
    BUS: '🚌',
    TAXI: '🚕',
    CARGO: '🚐'
  };
  return emojis[type as keyof typeof emojis] || '🚗';
};

const getVehicleColor = (type: string): string => {
  const colors = {
    CAR: '#3498db',
    TRUCK: '#e74c3c',
    MOTORCYCLE: '#f39c12',
    BUS: '#2ecc71',
    TAXI: '#f1c40f',
    CARGO: '#9b59b6'
  };
  return colors[type as keyof typeof colors] || '#3498db';
};

// Main App with Apollo Provider
const App: React.FC = () => (
  <ApolloProvider client={client}>
    <TrafficSimulationApp />
  </ApolloProvider>
);

export default App;
