import React from 'react';

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

interface VehicleDetailsPanelProps {
  vehicles: Vehicle[];
}

const VehicleDetailsPanel: React.FC<VehicleDetailsPanelProps> = ({ vehicles }) => {
  const activeVehicles = vehicles.filter(v => v.isActive);
  const idleVehicles = vehicles.filter(v => !v.isActive);

  const formatTime = (timeStr?: string) => {
    if (!timeStr) return 'N/A';
    try {
      const time = new Date(timeStr);
      return time.toLocaleTimeString();
    } catch {
      return timeStr;
    }
  };

  const getVehicleStatusColor = (vehicle: Vehicle) => {
    if (!vehicle.isActive) return '#666666';
    if (vehicle.speed > 10) return '#00FF00';
    if (vehicle.speed > 0) return '#FFFF00';
    return '#FF6600';
  };

  const getThreadStateIcon = (state?: string) => {
    switch (state) {
      case 'RUNNABLE': return '🟢';
      case 'BLOCKED': return '🔴';
      case 'WAITING': return '🟡';
      case 'TIMED_WAITING': return '🟠';
      default: return '⚪';
    }
  };

  return (
    <div className="vehicle-details-container">
      <div className="vehicle-stats">
        <div className="stat-item">
          <span className="stat-value">{activeVehicles.length}</span>
          <span className="stat-label">Active Vehicles</span>
        </div>
        <div className="stat-item">
          <span className="stat-value">{idleVehicles.length}</span>
          <span className="stat-label">Idle Vehicles</span>
        </div>
        <div className="stat-item">
          <span className="stat-value">
            {activeVehicles.length > 0 
              ? (activeVehicles.reduce((sum, v) => sum + v.speed, 0) / activeVehicles.length).toFixed(1)
              : '0.0'
            }
          </span>
          <span className="stat-label">Avg Speed (km/h)</span>
        </div>
      </div>

      <div className="vehicle-list">
        <h4>🚗 Active Vehicles ({activeVehicles.length})</h4>
        <div className="vehicle-grid">
          {activeVehicles.map(vehicle => (
            <div key={vehicle.id} className="vehicle-card active">
              <div className="vehicle-header">
                <div className="vehicle-id">
                  <span className="vehicle-emoji">{getVehicleEmoji(vehicle.type)}</span>
                  <span className="vehicle-name">{vehicle.id}</span>
                </div>
                <div 
                  className="vehicle-status"
                  style={{ backgroundColor: getVehicleStatusColor(vehicle) }}
                >
                  {vehicle.speed.toFixed(1)} km/h
                </div>
              </div>

              {vehicle.threadName && (
                <div className="thread-info">
                  <span className="thread-icon">{getThreadStateIcon(vehicle.threadState)}</span>
                  <span className="thread-name">{vehicle.threadName}</span>
                  {vehicle.threadState && (
                    <span className="thread-state">({vehicle.threadState})</span>
                  )}
                </div>
              )}

              <div className="vehicle-details">
                <div className="detail-row">
                  <span className="detail-label">Position:</span>
                  <span className="detail-value">({vehicle.x}, {vehicle.y})</span>
                </div>
                
                {vehicle.roadId && (
                  <div className="detail-row">
                    <span className="detail-label">Road:</span>
                    <span className="detail-value">{vehicle.roadId}</span>
                  </div>
                )}

                {vehicle.distanceTraveled !== undefined && (
                  <div className="detail-row">
                    <span className="detail-label">Distance:</span>
                    <span className="detail-value">{vehicle.distanceTraveled.toFixed(2)} km</span>
                  </div>
                )}

                {vehicle.roadsVisited !== undefined && (
                  <div className="detail-row">
                    <span className="detail-label">Roads Visited:</span>
                    <span className="detail-value">{vehicle.roadsVisited}</span>
                  </div>
                )}

                {vehicle.journeyStartTime && (
                  <div className="detail-row">
                    <span className="detail-label">Started:</span>
                    <span className="detail-value">{formatTime(vehicle.journeyStartTime)}</span>
                  </div>
                )}

                {vehicle.estimatedTime !== undefined && (
                  <div className="detail-row">
                    <span className="detail-label">ETA:</span>
                    <span className="detail-value">{vehicle.estimatedTime.toFixed(1)}s</span>
                  </div>
                )}

                {vehicle.routeDensity !== undefined && (
                  <div className="detail-row">
                    <span className="detail-label">Route Density:</span>
                    <span className="detail-value">{(vehicle.routeDensity * 100).toFixed(1)}%</span>
                  </div>
                )}
              </div>

              <div className="journey-progress">
                {vehicle.startZone && vehicle.endZone && (
                  <div className="route-info">
                    <span className="route-start">📍 {vehicle.startZone}</span>
                    <span className="route-arrow">→</span>
                    <span className="route-end">🎯 {vehicle.endZone}</span>
                  </div>
                )}
                
                {vehicle.progress !== undefined && (
                  <div className="progress-bar">
                    <div 
                      className="progress-fill"
                      style={{ width: `${vehicle.progress * 100}%` }}
                    ></div>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {idleVehicles.length > 0 && (
          <>
            <h4>💤 Idle Vehicles ({idleVehicles.length})</h4>
            <div className="idle-vehicles-summary">
              {idleVehicles.map(vehicle => (
                <div key={vehicle.id} className="idle-vehicle-item">
                  <span className="vehicle-emoji">{getVehicleEmoji(vehicle.type)}</span>
                  <span className="vehicle-id">{vehicle.id}</span>
                  {vehicle.distanceTraveled !== undefined && (
                    <span className="distance-completed">
                      {vehicle.distanceTraveled.toFixed(2)} km completed
                    </span>
                  )}
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
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

export default VehicleDetailsPanel; 