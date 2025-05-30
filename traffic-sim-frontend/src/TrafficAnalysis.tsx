import React, { useState } from 'react';

interface TrafficAnalysisProps {
  connectionStatus?: string;
}

const TrafficAnalysisPanel: React.FC<TrafficAnalysisProps> = ({ connectionStatus = 'Disconnected' }) => {
  const [startRoad, setStartRoad] = useState('MAIN_EAST_1');
  const [endRoad, setEndRoad] = useState('MAIN_WEST_1');
  const [analysisResult, setAnalysisResult] = useState<string>('');

  const roadOptions = [
    'MAIN_EAST_1', 'MAIN_EAST_2', 'MAIN_WEST_1', 'MAIN_WEST_2',
    'MAIN_NORTH_1', 'MAIN_NORTH_2', 'MAIN_SOUTH_1', 'MAIN_SOUTH_2',
    'HIGHWAY_CURVE_1', 'HIGHWAY_CURVE_2', 'RES_NORTH_1', 'RES_NORTH_2',
    'COM_CENTER_1', 'COM_CENTER_2', 'IND_FACTORY_1', 'IND_FACTORY_2'
  ];

  const getOptimalTime = async () => {
    try {
      const response = await fetch('http://localhost:4000/api/analysis', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          command: 'getOptimalTime',
          startRoad,
          endRoad
        })
      });
      
      const data = await response.json();
      setAnalysisResult(data.message || 'Analysis completed');
    } catch (error) {
      setAnalysisResult('Error: Could not fetch analysis');
      console.error('Analysis error:', error);
    }
  };

  const getHourlyReport = async () => {
    try {
      const response = await fetch('http://localhost:4000/api/analysis', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ command: 'HOURLY_REPORT' })
      });
      
      const data = await response.json();
      setAnalysisResult(data.message || 'Hourly report generated');
    } catch (error) {
      setAnalysisResult('Error: Could not fetch hourly report');
      console.error('Hourly report error:', error);
    }
  };

  return (
    <div className="traffic-analysis-container">
      <div className="connection-status">
        <span className={`status-dot ${connectionStatus === 'Connected' ? 'connected' : 'disconnected'}`}></span>
        WebSocket: {connectionStatus}
      </div>

      <div className="route-selector">
        <h4>🗺️ Route Analysis</h4>
        
        <div className="route-inputs">
          <div className="input-group">
            <label>Start Road:</label>
            <select value={startRoad} onChange={(e) => setStartRoad(e.target.value)}>
              {roadOptions.map(road => (
                <option key={road} value={road}>{road}</option>
              ))}
            </select>
          </div>
          
          <div className="input-group">
            <label>End Road:</label>
            <select value={endRoad} onChange={(e) => setEndRoad(e.target.value)}>
              {roadOptions.map(road => (
                <option key={road} value={road}>{road}</option>
              ))}
            </select>
          </div>
        </div>

        <div className="analysis-buttons">
          <button onClick={getOptimalTime} className="analysis-btn">
            🕐 Get Optimal Departure Time
          </button>
          <button onClick={getHourlyReport} className="analysis-btn">
            📊 Get Hourly Traffic Report
          </button>
        </div>
      </div>

      {analysisResult && (
        <div className="analysis-result">
          <h4>📋 Analysis Result:</h4>
          <pre className="result-text">{analysisResult}</pre>
        </div>
      )}

      <div className="traffic-legend">
        <h4>🚦 Traffic Density Legend</h4>
        <div className="legend-items">
          <div className="legend-item">
            <span className="legend-color" style={{backgroundColor: '#00FF00'}}></span>
            🟢 Light Traffic (&lt;30%)
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{backgroundColor: '#FFFF00'}}></span>
            🟡 Moderate Traffic (30-60%)
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{backgroundColor: '#FFA500'}}></span>
            🟠 Heavy Traffic (60-80%)
          </div>
          <div className="legend-item">
            <span className="legend-color" style={{backgroundColor: '#FF0000'}}></span>
            🔴 Severe Traffic (&gt;80%)
          </div>
        </div>
      </div>

      <div className="time-info">
        <h4>⏰ Simulation Time</h4>
        <p>1 minute real time = 1 hour simulation time</p>
        <p>Simulation starts at 06:00 and cycles through 24 hours</p>
      </div>
    </div>
  );
};

export default TrafficAnalysisPanel; 