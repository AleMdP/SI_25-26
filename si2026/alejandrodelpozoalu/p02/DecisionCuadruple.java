package si2026.alejandrodelpozoalu.p02;

import core.game.StateObservation;
import ontology.Types.ACTIONS;

public class DecisionCuadruple implements Nodo {
    private SelectorDireccion selector;
    private Nodo arriba, abajo, izquierda, derecha;

    public DecisionCuadruple(SelectorDireccion selector, Nodo ar, Nodo ab, Nodo iz, Nodo de) {
        this.selector = selector;
        this.arriba = ar;
        this.abajo = ab;
        this.izquierda = iz;
        this.derecha = de;
    }

    @Override
    public ACTIONS decidir(StateObservation obs) {
        // El selector nos dirá hacia dónde queremos ir
        ACTIONS direccionDeseada = selector.elegir(obs);
        
        switch (direccionDeseada) {
            case ACTION_UP:    return arriba.decidir(obs);
            case ACTION_DOWN:  return abajo.decidir(obs);
            case ACTION_LEFT:  return izquierda.decidir(obs);
            case ACTION_RIGHT: return derecha.decidir(obs);
            default:           return ACTIONS.ACTION_NIL;
        }
    }

    // Interfaz funcional para que el selector sea una lambda
    public interface SelectorDireccion {
        ACTIONS elegir(StateObservation obs);
    }
}