import numpy as np
from typing import Tuple, List, Dict, Any
from .lambert import LambertSolver
from api.jeod_engine import get_jeod_engine

class DifferentialCorrector:
    """
    Differential corrector (shooting method) for high-fidelity trajectory refinement.
    
    Refines a Lambert guess using JEOD's full force models.
    """
    
    def __init__(self, mu_central: float = 1.32712440018e20):
        self.mu = mu_central
        self.lambert = LambertSolver(mu_central)
        self._engine = None
    
    @property
    def engine(self):
        if self._engine is None:
            self._engine = get_jeod_engine()
        return self._engine

    def refine_transfer(
        self,
        r1: np.ndarray,
        r2: np.ndarray,
        tof_days: float,
        accuracy_threshold: float = 1000.0, # 1km miss distance
        max_iterations: int = 10
    ) -> Tuple[np.ndarray, np.ndarray, List[Dict[str, Any]]]:
        """
        Refine a transfer using iterative shooting.
        
        Args:
            r1: Initial position (m)
            r2: Target position (m)
            tof_days: Time of flight
            
        Returns:
            (v1_refined, v2_refined, trajectory)
        """
        tof_sec = tof_days * 86400.0
        
        # 1. Initial guess from Lambert
        v1_guess, v2_guess = self.lambert.solve(r1, r2, tof_sec)
        
        v_current = v1_guess.copy()
        
        for i in range(max_iterations):
            # 2. Propagate with JEOD
            initial_state = np.concatenate([r1, v_current])
            traj = self.engine.propagate(initial_state, tof_days, dt_sec=3600)
            
            final_pos = np.array([traj[-1]["position"]["x"], 
                                 traj[-1]["position"]["y"], 
                                 traj[-1]["position"]["z"]])
            
            # 3. Calculate miss distance
            error = final_pos - r2
            miss_dist = np.linalg.norm(error)
            
            if miss_dist < accuracy_threshold:
                v2_refined = np.array([traj[-1]["velocity"]["x"], 
                                      traj[-1]["velocity"]["y"], 
                                      traj[-1]["velocity"]["z"]])
                return v_current, v2_refined, traj
            
            # 4. Compute Jacobian via finite differencing
            dv = 1.0 # 1 m/s perturbation
            J = np.zeros((3, 3))
            
            for j in range(3):
                v_perturbed = v_current.copy()
                v_perturbed[j] += dv
                
                state_p = np.concatenate([r1, v_perturbed])
                traj_p = self.engine.propagate(state_p, tof_days, dt_sec=tof_sec) # Just endpoint
                
                final_p = np.array([traj_p[-1]["position"]["x"], 
                                   traj_p[-1]["position"]["y"], 
                                   traj_p[-1]["position"]["z"]])
                
                J[:, j] = (final_p - final_pos) / dv
            
            # 5. Update initial velocity
            # delta_v = inv(J) * error
            try:
                delta_v = np.linalg.solve(J, error)
                v_current = v_current - delta_v
            except np.linalg.LinAlgError:
                # Singularity?
                break
                
        # Return best effort
        v2_refined = np.array([traj[-1]["velocity"]["x"], 
                              traj[-1]["velocity"]["y"], 
                              traj[-1]["velocity"]["z"]])
        return v_current, v2_refined, traj
