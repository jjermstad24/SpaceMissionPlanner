import React, { useEffect, useState, useRef } from 'react';
import axios from 'axios';

const API = 'http://localhost:8000';

interface BodyPos {
  name: string;
  x: number;
  y: number;
  vx?: number;
  vy?: number;
}

function App() {
  const [bodies, setBodies] = useState<BodyPos[]>([]);
  const [origin, setOrigin] = useState('Earth');
  const [destination, setDestination] = useState('Mars');
  const [tof, setTof] = useState(180);
  const [result, setResult] = useState<any>(null);
  const [loading, setLoading] = useState(false);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  const planets = ['Mercury', 'Venus', 'Earth', 'Mars', 'Jupiter', 'Saturn', 'Uranus', 'Neptune'];

  useEffect(() => {
    loadMap();
  }, []);

  useEffect(() => {
    if (result?.trajectory) drawTrajectory();
  }, [result]);

  async function loadMap() {
    try {
      const res = await axios.get(`${API}/map`);
      setBodies(res.data.bodies);
    } catch (e) {
      console.error(e);
    }
  }

  function drawTrajectory() {
    if (!result?.trajectory || !canvasRef.current) return;
    drawMap();
    
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    const traj = result.trajectory;
    const maxR = Math.max(...traj.map((t: any) => 
      Math.sqrt(t.position.x**2 + t.position.y**2)
    )) / 1e11 || 2;
    
    const scale = Math.min(canvas.width, canvas.height) / (maxR * 2.5) / 2;
    const cx = canvas.width / 2;
    const cy = canvas.height / 2;
    
    ctx.beginPath();
    ctx.strokeStyle = '#4ade80';
    ctx.lineWidth = 2;
    
    traj.forEach((pt: any, i: number) => {
      const x = cx + (pt.position.x / 1e11) * scale;
      const y = cy - (pt.position.y / 1e11) * scale;
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();
  }

  function drawMap() {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    const w = canvas.width;
    const h = canvas.height;
    const cx = w / 2;
    const cy = h / 2;
    const maxAu = 35;
    const scale = Math.min(w, h) / (maxAu * 2.2) / 2;
    
    ctx.fillColor = '#0f0f23';
    ctx.fillRect(0, 0, w, h);
    
    // Sun
    ctx.beginPath();
    ctx.arc(cx, cy, 8, 0, Math.PI * 2);
    ctx.fillStyle = '#fbbf24';
    ctx.fill();
    
    // Orbit rings
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 1;
    planets.forEach((_, i) => {
      const r = [0.4, 0.7, 1.0, 1.5, 5.2, 9.5, 19.2, 30.1][i] * scale;
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();
    });
    
    // Bodies
    bodies.forEach(body => {
      const x = cx + body.x * scale;
      const y = cy - body.y * scale;
      ctx.beginPath();
      ctx.arc(x, y, body.name === 'Earth' ? 5 : 3, 0, Math.PI * 2);
      ctx.fillStyle = body.name === 'Earth' ? '#3b82f6' : '#9ca3af';
      ctx.fill();
    });
  }

  async function calculate() {
    setLoading(true);
    try {
      const res = await axios.post(`${API}/transfer`, {
        origin,
        destination,
        departure_time: new Date().toISOString(),
        time_of_flight: tof
      });
      setResult(res.data);
    } catch (e) {
      console.error(e);
    }
    setLoading(false);
  }

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Space Mission Planner</h1>
      </div>
      
      <div style={styles.content}>
        <div style={styles.panel}>
          <canvas 
            ref={canvasRef} 
            width={500} 
            height={500} 
            style={styles.map}
          />
          
          <div style={styles.controls}>
            <div style={styles.row}>
              <select 
                value={origin} 
                onChange={e => setOrigin(e.target.value)}
                style={styles.select}
              >
                {planets.map(p => <option key={p} value={p}>{p}</option>)}
              </select>
              <span style={styles.arrow}>→</span>
              <select 
                value={destination} 
                onChange={e => setDestination(e.target.value)}
                style={styles.select}
              >
                {planets.map(p => <option key={p} value={p}>{p}</option>)}
              </select>
            </div>
            
            <div style={styles.row}>
              <label>TOF: {tof}d</label>
              <input 
                type="range" 
                min="30" 
                max="730" 
                value={tof} 
                onChange={e => setTof(Number(e.target.value))}
                style={styles.slider}
              />
            </div>
            
            <button 
              onClick={calculate} 
              disabled={loading}
              style={styles.button}
            >
              {loading ? 'Calculating...' : 'Calculate Transfer'}
            </button>
          </div>
          
          {result && (
            <div style={styles.result}>
              <div>{origin} → {destination}</div>
              <div>Departure: {result.departure_time?.slice(0, 10)}</div>
              <div>Arrival: {result.arrival_time?.slice(0, 10)}</div>
              <div>TOF: {result.time_of_flight} days</div>
              <div style={styles.dv}>
                ΔV dep: {result.delta_v_departure} km/s
              </div>
              <div style={styles.dv}>
                ΔV arr: {result.delta_v_arrival} km/s
              </div>
              <div style={styles.total}>
                Total: {result.total_delta_v} km/s
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  container: {
    minHeight: '100vh',
    background: '#0f0f23',
    color: '#fff',
    fontFamily: 'system-ui, sans-serif',
  },
  header: {
    padding: '1rem 2rem',
    borderBottom: '1px solid #333',
  },
  title: {
    margin: 0,
    fontSize: '1.5rem',
    fontWeight: 600,
  },
  content: {
    padding: '2rem',
    display: 'flex',
    justifyContent: 'center',
  },
  panel: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    gap: '1.5rem',
  },
  map: {
    borderRadius: '8px',
    border: '1px solid #333',
  },
  controls: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
    alignItems: 'center',
  },
  row: {
    display: 'flex',
    alignItems: 'center',
    gap: '1rem',
  },
  arrow: {
    fontSize: '1.5rem',
  },
  select: {
    padding: '0.5rem 1rem',
    fontSize: '1rem',
    borderRadius: '6px',
    border: '1px solid #444',
    background: '#1a1a2e',
    color: '#fff',
  },
  slider: {
    width: '200px',
  },
  button: {
    padding: '0.75rem 2rem',
    fontSize: '1rem',
    borderRadius: '6px',
    border: 'none',
    background: '#3b82f6',
    color: '#fff',
    cursor: 'pointer',
  },
  result: {
    padding: '1rem',
    borderRadius: '8px',
    background: '#1a1a2e',
    textAlign: 'center',
    minWidth: '200px',
  },
  dv: {
    color: '#9ca3af',
  },
  total: {
    fontWeight: 600,
    color: '#4ade80',
    fontSize: '1.1rem',
  },
};

export default App;