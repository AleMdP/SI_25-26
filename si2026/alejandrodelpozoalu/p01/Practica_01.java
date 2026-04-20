package si2026.alejandrodelpozoalu.p01;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class Practica_01 extends AbstractPlayer {

    int bloque;
    MotorReglas motor;
    Cerebro cerebro;

    public Practica_01(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        bloque = stateObs.getBlockSize();
        cerebro = new Cerebro();
        
        List<Regla> listaReglas = new LinkedList<>();
        
        listaReglas.add(new Regla(c -> c.esquivaHaciaArriba, ACTIONS.ACTION_UP));
        listaReglas.add(new Regla(c -> c.esquivaHaciaAbajo, ACTIONS.ACTION_DOWN));
        listaReglas.add(new Regla(c -> c.esquivaHaciaIzquierda, ACTIONS.ACTION_LEFT));
        listaReglas.add(new Regla(c -> c.esquivaHaciaDerecha, ACTIONS.ACTION_RIGHT));
        
        
        listaReglas.add(new Regla(c -> (c.enemigoArriba || c.barrilArriba) && !c.mirandoArriba, ACTIONS.ACTION_UP));
        listaReglas.add(new Regla(c -> (c.enemigoArriba || c.barrilArriba) && c.mirandoArriba, ACTIONS.ACTION_USE));
        
        listaReglas.add(new Regla(c -> (c.enemigoAbajo || c.barrilAbajo) && !c.mirandoAbajo, ACTIONS.ACTION_DOWN));
        listaReglas.add(new Regla(c -> (c.enemigoAbajo || c.barrilAbajo) && c.mirandoAbajo, ACTIONS.ACTION_USE));
        
        listaReglas.add(new Regla(c -> (c.enemigoDerecha || c.barrilDerecha) && !c.mirandoDerecha, ACTIONS.ACTION_RIGHT));
        listaReglas.add(new Regla(c -> (c.enemigoDerecha || c.barrilDerecha) && c.mirandoDerecha, ACTIONS.ACTION_USE));
        
        listaReglas.add(new Regla(c -> (c.enemigoIzquierda || c.barrilIzquierda) && !c.mirandoIzquierda, ACTIONS.ACTION_LEFT));
        listaReglas.add(new Regla(c -> (c.enemigoIzquierda || c.barrilIzquierda) && c.mirandoIzquierda, ACTIONS.ACTION_USE));


        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoX > 0.1 && !cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_RIGHT), ACTIONS.ACTION_RIGHT));
        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoX > 0.1 && cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_RIGHT), ACTIONS.ACTION_LEFT));

        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoX < -0.1 && !cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_LEFT), ACTIONS.ACTION_LEFT));
        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoX < -0.1 && cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_LEFT), ACTIONS.ACTION_RIGHT));

        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoY > 0.1 && !cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_DOWN), ACTIONS.ACTION_DOWN));
        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoY > 0.1 && cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_DOWN), ACTIONS.ACTION_UP));

        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoY < -0.1 && !cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_UP), ACTIONS.ACTION_UP));
        listaReglas.add(new Regla(c -> c.distanciaAlObjetivoY < -0.1 && cerebro.esPosicionPeligrosa(stateObs, ACTIONS.ACTION_UP), ACTIONS.ACTION_DOWN));

        this.motor = new MotorReglas(listaReglas);
    }
    
	@Override
	public ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		if (motor == null || cerebro == null) return ACTIONS.ACTION_NIL;
		
		cerebro.comprobar(stateObs);
		Regla r = motor.disparo(cerebro);
		
		if (r == null) return ACTIONS.ACTION_NIL;
		
		return r.getAccion();
	}

    public class Cerebro {
        public boolean esquivaHaciaArriba, esquivaHaciaAbajo, esquivaHaciaIzquierda, esquivaHaciaDerecha;
        public boolean enemigoArriba, enemigoAbajo, enemigoDerecha, enemigoIzquierda;
        public boolean barrilArriba, barrilAbajo, barrilDerecha, barrilIzquierda;
        public boolean mirandoArriba, mirandoAbajo, mirandoDerecha, mirandoIzquierda;
        public double distanciaAlObjetivoX, distanciaAlObjetivoY;

        public void comprobar(StateObservation stateObs) {
            esquivaHaciaArriba = esquivaHaciaAbajo = esquivaHaciaIzquierda = esquivaHaciaDerecha = false;
            enemigoArriba = enemigoAbajo = enemigoDerecha = enemigoIzquierda = false;
            barrilArriba = barrilAbajo = barrilDerecha = barrilIzquierda = false;
            mirandoArriba = mirandoAbajo = mirandoDerecha = mirandoIzquierda = false;
            distanciaAlObjetivoX = distanciaAlObjetivoY = 0;
            
            ArrayList<Observation>[][] mapa = stateObs.getObservationGrid();
            int xAv = (int) (stateObs.getAvatarPosition().x / bloque);
            int yAv = (int) (stateObs.getAvatarPosition().y / bloque);
            int maxX = mapa.length;
            int maxY = mapa[0].length;

            // Orientación
            if (stateObs.getAvatarOrientation().y == -1) mirandoArriba = true;
            else if (stateObs.getAvatarOrientation().y == 1) mirandoAbajo = true;
            else if (stateObs.getAvatarOrientation().x == 1) mirandoDerecha = true;
            else if (stateObs.getAvatarOrientation().x == -1) mirandoIzquierda = true;

            procesarEntorno(stateObs, xAv, yAv);
            
            //Busca mejpr posicion
            double minDist = Double.MAX_VALUE;
            int posXEnemigo = xAv; 
            int posYEnemigo = yAv;

            for (int i = 0; i < maxX; i++) {
                for (int j = 0; j < maxY; j++) {
                    if (mapa[i][j].size() > 2) { 
                        double d = Math.sqrt(Math.pow(i - xAv, 2) + Math.pow(j - yAv, 2));
                        if (d < minDist && d > 0.1) {
                            minDist = d;
                            int ex = i; int ey = j;

                            //  IZQUIERDA
                            if (ex <= 1 && ey <= maxY/2) {
                            	if(ey >= maxY/2) {
                                    posXEnemigo = 4; 
                                    posYEnemigo = maxY / 2;
                            	}
                            	else {
                                    posXEnemigo = maxX / 3; 
                                    posYEnemigo = maxY - 5;
                            	}
                            } 
                            //  DERECHA
                            if (ex >= maxX - 2) {
                            	if(ey <= maxY/2) {
                                    posXEnemigo = maxX - 5; 
                                    posYEnemigo = maxY / 2;
                            	}
                            	else {
	                                posXEnemigo = (int) (maxX / 1.5); 
	                                posYEnemigo = 4;
                            	}
                            } 
                            //  ARRIBA
                            if (ey <= 1) { 
                            	if(ex > maxX / 1.5) {
	                                posXEnemigo = (int) (maxX / 1.5); 
	                                posYEnemigo = 4;
                            	}
                            	else if(ex >= maxX/3) {
                                    posXEnemigo = maxX / 3; 
                                    posYEnemigo = 4;
                            	}
                            	else if(ex < maxX/1.5){
                                    posXEnemigo = 4; 
                                    posYEnemigo = maxY / 2;
                            	}
                            }
                            //  ABAJO
                            if (ey >= maxY - 2) {
                            	if(ex < maxX / 3) {
                                    posXEnemigo = maxX / 3; 
                                    posYEnemigo = maxY - 5;
                            	}
                            	else if(ex <= maxX/1.5) {
                                    posXEnemigo = (int) (maxX / 1.5); 
                                    posYEnemigo = maxY - 5;
                            	}
                            	else if(ex > maxX / 1.5) {
                                    posXEnemigo = maxX - 5; 
                                    posYEnemigo = maxY / 2;
                            	}
                            }

                            
                            // Disparar
                            if (ex == (xAv + yAv/6)) if (ey < yAv ) enemigoArriba = true;
                            if (ex == (xAv - (maxY - yAv)/6)) if(ey > yAv ) enemigoAbajo = true;
                            if (ey == (yAv + xAv/16)) if (ex > xAv && ex < xAv+5) enemigoDerecha = true;
                            if (ey == (yAv - (maxX - xAv)/16)) if(ex < xAv && ex > xAv-5) enemigoIzquierda = true;
                        }
                    }
                }
            }
            distanciaAlObjetivoX = posXEnemigo - xAv;
            distanciaAlObjetivoY = posYEnemigo - yAv;
        }

        private void procesarEntorno(StateObservation stateObs, int xAv, int yAv) {
            ArrayList<Observation>[] mov = stateObs.getMovablePositions();
            ArrayList<Observation>[] imm = stateObs.getImmovablePositions();
            
            // Balas
            if (mov != null) {
                for (Observation obs : mov[0]) {
	                int ox = (int)(obs.position.x/bloque); 
	                int oy = (int)(obs.position.y/bloque);
	                    
	                if (oy == yAv) {
	                    if (ox == xAv + 1 || ox == xAv + 2) esquivaHaciaArriba = true;
	                    if (ox == xAv - 1 || ox == xAv - 2) esquivaHaciaAbajo = true;
	                }
	                if (ox == xAv) {
	                    if (oy == yAv + 1 || oy == yAv + 2) esquivaHaciaDerecha = true;
	                    if (oy == yAv - 1 || oy == yAv - 2) esquivaHaciaIzquierda = true;
	                }
	            }
            }
            
            if (imm != null && imm.length > 2) {
                // Barriles
                for (Observation obs : imm[2]) {
                    int ox = (int)(obs.position.x/bloque); 
                    int oy = (int)(obs.position.y/bloque);
                    
                    if (ox == xAv+1 && oy == yAv) barrilDerecha = true;
                    if (ox == xAv-1 && oy == yAv) barrilIzquierda = true;
                    if (ox == xAv && oy == yAv-1) barrilArriba = true;
                    if (ox == xAv && oy == yAv+1) barrilAbajo = true;
                }
            }
            
            if(esPosicionPeligrosa(stateObs, ACTIONS.ACTION_DOWN) && esquivaHaciaAbajo) {
            	esquivaHaciaAbajo = false;
            	esquivaHaciaArriba = true;
            }
        	if(esPosicionPeligrosa(stateObs, ACTIONS.ACTION_UP) && esquivaHaciaArriba) {
        		esquivaHaciaArriba = false;
        		esquivaHaciaAbajo = true;
        	}
        	
        	if(esPosicionPeligrosa(stateObs, ACTIONS.ACTION_RIGHT) && esquivaHaciaDerecha) {
        		esquivaHaciaDerecha = false;
        		esquivaHaciaIzquierda = true;
        	}
        	if(esPosicionPeligrosa(stateObs, ACTIONS.ACTION_LEFT) && esquivaHaciaIzquierda) {
        		esquivaHaciaIzquierda = false;
        		esquivaHaciaDerecha= true;
        	}
        }

        public boolean esPosicionPeligrosa(StateObservation stateObs, ACTIONS movi) {
            int xf = (int) (stateObs.getAvatarPosition().x / bloque);
            int yf = (int) (stateObs.getAvatarPosition().y / bloque);
            
            if (movi == ACTIONS.ACTION_UP) yf--;
            else if (movi == ACTIONS.ACTION_DOWN) yf++;
            else if (movi == ACTIONS.ACTION_LEFT) xf--;
            else if (movi == ACTIONS.ACTION_RIGHT) xf++;

            ArrayList<Observation>[][] mapa = stateObs.getObservationGrid();
            if (xf <= 1 || xf >= mapa.length-2 || yf <= 1 || yf >= mapa[0].length-2) return true;

            ArrayList<Observation>[] imm = stateObs.getImmovablePositions();
            
            //Camino de enemigos
            for(Observation obs : imm[3]) {
                if((int)(obs.position.x / bloque) == xf && (int)(obs.position.y / bloque) == yf) return true;
            }
            

            ArrayList<Observation>[] mov = stateObs.getMovablePositions();
            if(mov != null) {
                for(Observation obs : mov[0]) {
                	int ex = (int) (obs.position.x / bloque);
                    int ey = (int) (obs.position.y / bloque);
                    
                    if (ex == xf && ey == yf) return true;
                    
                    int distManhattan = Math.abs(ex - xf) + Math.abs(ey - yf);
                    if (distManhattan <= 1) return true;
                }
            }
            return false;
        }
    }

    public interface Condicion { 
    	boolean seCumple(Cerebro c); 
    }
    
    public class Regla {
        private Condicion c; 
        private ACTIONS a;
        
        public Regla(Condicion c, ACTIONS a) { 
        	this.c = c; this.a = a; 
        }
        public boolean evaluar(Cerebro c) { 
        	return this.c.seCumple(c); 
        }
        public ACTIONS getAccion() { 
        	return a; 
        }
    }
    
    public class MotorReglas {
        private List<Regla> r;
        public MotorReglas(List<Regla> r) { this.r = r; }
        public Regla disparo(Cerebro c) {
            for (Regla re : r) { 
            	if (re.evaluar(c)) return re; 
            }
            return null;
        }
    }
}