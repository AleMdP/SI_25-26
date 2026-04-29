package si2026.alejandrodelpozoalu.p02;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;

public class Practica_02 extends AbstractPlayer {
    private Nodo raiz;

    public Practica_02(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        Nodo disparar = new Accion(ACTIONS.ACTION_USE);
        Nodo irArriba = new Accion(ACTIONS.ACTION_UP);
        Nodo irAbajo = new Accion(ACTIONS.ACTION_DOWN);
        Nodo irIzquierda = new Accion(ACTIONS.ACTION_LEFT);
        Nodo irDerecha = new Accion(ACTIONS.ACTION_RIGHT);
        Nodo quieto = new Accion(ACTIONS.ACTION_NIL);

        
        Decision bordeIzq = new Decision(
        		this::bordeIzq,
        		quieto,
        		irIzquierda);
        
        Decision bordeDer = new Decision(
        		this::bordeDer,
        		quieto,
        		irDerecha);

        DecisionCuadruple buscarBuceador = new DecisionCuadruple(
	            this::mejorDireccionBuceador,
	            new Decision(
	            		this::arribaSeguro, 
	            		irArriba, 
	            		new Decision(
	    	            		obs -> obs.getAvatarOrientation().y < 0, 
	    	            		disparar, 
	    	            		irArriba
	    	            	)
	            	),
	            new Decision(
	            		this::abajoSeguro, 
	            		irAbajo, 
	            		new Decision(
	    	            		obs -> obs.getAvatarOrientation().y > 0, 
	    	            		disparar, 
	    	            		irAbajo
	    	            	)
	            	),
	            new Decision(
	            		this::izquierdaSegura,
	            		bordeIzq,
	            		new Decision(
	    	            		obs -> obs.getAvatarOrientation().x < 0, 
	    	            		disparar, 
	    	            		irIzquierda
	    	            	)
	            	),
	            new Decision(
	            		this::derechaSegura,
	            		bordeDer,
	            		new Decision(
	    	            		obs -> obs.getAvatarOrientation().x > 0, 
	    	            		disparar, 
	    	            		irDerecha
	    	            	)
	            	)
	        );

        Decision gestionSuperficie = new Decision(
	            obs -> obs.getAvatarHealthPoints() < 5 || cargaLlena(obs),
	            new Decision(
	            		this::arribaSeguro, 
	            		irArriba, 
	            		new Decision(
	    	            		this::mirandoArriba,
	    	            		disparar, 
	    	            		irArriba
	    	            	)
	            	),
	            buscarBuceador
	        );

        Decision atacarAbajo = new Decision(this::mirandoAbajo, disparar, irAbajo);
        Decision checkAbajo = new Decision(obs -> !abajoSeguro(obs), atacarAbajo, buscarBuceador);
        
        Decision atacarArriba = new Decision(this::mirandoArriba, disparar, irArriba);
        Decision checkArriba = new Decision(obs -> !arribaSeguro(obs), atacarArriba, checkAbajo);
        
        Decision atacarDerecha = new Decision(this::mirandoDerecha, disparar, irDerecha);
        Decision checkDerecha = new Decision(obs -> !derechaSegura(obs), atacarDerecha, checkArriba);
        
        Decision atacarIzquierda = new Decision(this::mirandoIzquierda, disparar, irIzquierda);
        Decision arbolCombate = new Decision(obs -> !izquierdaSegura(obs), atacarIzquierda, checkDerecha);

        this.raiz = new Decision(
            this::hayPeligroCerca,
            arbolCombate,
            gestionSuperficie
        );
    }

    @Override
    public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        return raiz.decidir(stateObs);
    }

    
    private boolean bordeDer(StateObservation stateObs) {
        ArrayList<Observation>[][] mapa = stateObs.getObservationGrid();

        int bloque = stateObs.getBlockSize();
        int xAv = (int) (stateObs.getAvatarPosition().x / bloque);
        
        return (xAv+2 == mapa.length-1);
    }
    private boolean bordeIzq(StateObservation stateObs) {
        int bloque = stateObs.getBlockSize();
        int xAv = (int) (stateObs.getAvatarPosition().x / bloque);
        
        return (xAv-2 == 0);
    }
    
    private boolean mirandoIzquierda(StateObservation stateObs) { return stateObs.getAvatarOrientation().x < 0; }
    private boolean mirandoDerecha(StateObservation stateObs) { return stateObs.getAvatarOrientation().x > 0; }
    private boolean mirandoArriba(StateObservation stateObs) { return stateObs.getAvatarOrientation().y < 0; }
    private boolean mirandoAbajo(StateObservation stateObs) { return stateObs.getAvatarOrientation().y > 0; }
    
    public boolean hayPeligroCerca(StateObservation stateObs) {
        return !izquierdaSegura(stateObs) || !derechaSegura(stateObs) || 
               !arribaSeguro(stateObs) || !abajoSeguro(stateObs);
    }
    
    private boolean arribaSeguro(StateObservation stateObs) {
        int bloque = stateObs.getBlockSize();
        int xAv = (int) (stateObs.getAvatarPosition().x / bloque);
        int yAv = (int) (stateObs.getAvatarPosition().y / bloque);

        ArrayList<Observation>[] npcs = stateObs.getNPCPositions();
        if(npcs != null) {
        	for(int i = 0; i < npcs.length; i++) {
            	for(Observation obs : npcs[i]) {
            		if(obs.itype == 15) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if((yAv - yObs) <= 1 && (yAv - yObs) > 0) {
                        	if(Math.abs(xAv - xObs) <= 2) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        ArrayList<Observation>[] mov = stateObs.getMovablePositions();
        if(mov != null) {
        	for(int i = 0; i < mov.length; i++) {
            	for(Observation obs : mov[i]) {
            		if(obs.itype == 14) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if((yAv - yObs) <= 1 && (yAv - yObs) > 0) {
                        	if(Math.abs(xAv - xObs) <= 2) {
                        		return false;
                        	}
                        }
            		}
            		if(obs.itype == 16) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if((yAv - yObs) <= 1 && (yAv - yObs) > 0) {
                        	if(Math.abs(xAv - xObs) <= 4) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        return true;
    }
    
    private boolean abajoSeguro(StateObservation stateObs) {
        int bloque = stateObs.getBlockSize();
        int xAv = (int) (stateObs.getAvatarPosition().x / bloque);
        int yAv = (int) (stateObs.getAvatarPosition().y / bloque);

        ArrayList<Observation>[] npcs = stateObs.getNPCPositions();
        if(npcs != null) {
        	for(int i = 0; i < npcs.length; i++) {
            	for(Observation obs : npcs[i]) {
            		if(obs.itype == 15) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if((yObs - yAv) < 2 && (yObs - yAv) > 0) {
                        	if(Math.abs(xAv - xObs) <= 2) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        ArrayList<Observation>[] mov = stateObs.getMovablePositions();
        if(mov != null) {
        	for(int i = 0; i < mov.length; i++) {
            	for(Observation obs : mov[i]) {
            		if(obs.itype == 14) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if((yObs - yAv) <= 1 && (yObs - yAv) > 0) {
                        	if(Math.abs(xAv - xObs) <= 2) {
                        		return false;
                        	}
                        }
            		}
            		if(obs.itype == 16) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if((yObs - yAv) <= 1 && (yObs - yAv) > 0) {
                        	if(Math.abs(xAv - xObs) <= 4) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        return true;
    }
    
    private boolean derechaSegura(StateObservation stateObs) {
        int bloque = stateObs.getBlockSize();
        int xAv = (int) (stateObs.getAvatarPosition().x / bloque);
        int yAv = (int) (stateObs.getAvatarPosition().y / bloque);

        ArrayList<Observation>[] npcs = stateObs.getNPCPositions();
        if(npcs != null) {
        	for(int i = 0; i < npcs.length; i++) {
            	for(Observation obs : npcs[i]) {
            		if(obs.itype == 15) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if(yAv == yObs) {
                        	if((xObs - xAv) < 2 && (xObs - xAv) > 0) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        ArrayList<Observation>[] mov = stateObs.getMovablePositions();
        if(mov != null) {
        	for(int i = 0; i < mov.length; i++) {
            	for(Observation obs : mov[i]) {
            		if(obs.itype == 14) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if(yAv == yObs) {
                        	if((xObs - xAv) < 2 && (xObs - xAv) > 0) {
                        		return false;
                        	}
                        }
            		}
            		if(obs.itype == 16) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if(yAv == yObs) {
                        	if((xObs - xAv) < 4 && (xObs - xAv) > 0) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        return true;
    }
    
    private boolean izquierdaSegura(StateObservation stateObs) {
        int bloque = stateObs.getBlockSize();
        int xAv = (int) (stateObs.getAvatarPosition().x / bloque);
        int yAv = (int) (stateObs.getAvatarPosition().y / bloque);

        ArrayList<Observation>[] npcs = stateObs.getNPCPositions();
        if(npcs != null) {
        	for(int i = 0; i < npcs.length; i++) {
            	for(Observation obs : npcs[i]) {
            		if(obs.itype == 15) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);
                        
                        if(yAv == yObs) {
                        	if((xAv - xObs) < 2 && (xAv - xObs) > 0) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        ArrayList<Observation>[] mov = stateObs.getMovablePositions();
        if(mov != null) {
        	for(int i = 0; i < mov.length; i++) {
            	for(Observation obs : mov[i]) {
            		if(obs.itype == 14) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);

                        if(yAv == yObs) {
                        	if((xAv - xObs) < 2 && (xAv - xObs) > 0) {
                        		return false;
                        	}
                        }
            		}
            		if(obs.itype == 16) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);

                        if(yAv == yObs) {
                        	if((xAv - xObs) < 4 && (xAv - xObs) > 0) {
                        		return false;
                        	}
                        }
            		}
            	}
        	}
        }
        return true;
    }
    
    private boolean cargaLlena(StateObservation stateObs) {
        Integer buceadores = stateObs.getAvatarResources().get(18);
        return buceadores != null && buceadores >= 4;
    }
    
    private ACTIONS mejorDireccionBuceador(StateObservation stateObs) {
        int bloque = stateObs.getBlockSize();
        int xAv = (int)(stateObs.getAvatarPosition().x / bloque);
        int yAv = (int)(stateObs.getAvatarPosition().y / bloque);

        Observation buceador = buscarBuceadorMasCercano(stateObs);
        if (buceador == null) return ACTIONS.ACTION_NIL;

        int xB = (int)(buceador.position.x / bloque);
        int yB = (int)(buceador.position.y / bloque);

        if (xAv < xB && derechaSegura(stateObs))
            return ACTIONS.ACTION_RIGHT;

        if (xAv > xB && izquierdaSegura(stateObs))
            return ACTIONS.ACTION_LEFT;

        if (yAv < yB && abajoSeguro(stateObs))
            return ACTIONS.ACTION_DOWN;

        if (yAv > yB && arribaSeguro(stateObs))
            return ACTIONS.ACTION_UP;

        return ACTIONS.ACTION_NIL;
    }

    private Observation buscarBuceadorMasCercano(StateObservation stateObs) {
        int bloque = stateObs.getBlockSize();
        int[] posAvatar = {
            (int) (stateObs.getAvatarPosition().x / bloque),
            (int) (stateObs.getAvatarPosition().y / bloque)
        };
        
        double minDistancia = Double.MAX_VALUE;
        Observation masCercano = null;

        ArrayList<Observation>[] npcs = stateObs.getNPCPositions();

        if(npcs != null) {
        	for(int i = 0; i < npcs.length; i++) {
            	for(Observation obs : npcs[i]) {
            		if(obs.itype == 17) {
                        int xObs = (int) (obs.position.x / bloque);
                        int yObs = (int) (obs.position.y / bloque);

                        double dist = Math.abs(posAvatar[0] - xObs) + Math.abs(posAvatar[1] - yObs);
                        
                        if(dist < minDistancia){
                            minDistancia = dist;
                            masCercano = obs;
                        }
            		}
            	}
        	}
        }
        
//        if(masCercano != null)
//        System.out.println(masCercano.position);
        
        return masCercano;
    }
}

