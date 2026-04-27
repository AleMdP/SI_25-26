package si2026.alejandrodelpozoalu.p02;

import ontology.Types.ACTIONS;
import core.game.StateObservation;

public class Accion implements Nodo {
    private ACTIONS accion;

    public Accion(ACTIONS accion) {
        this.accion = accion;
    }

    @Override
    public ACTIONS decidir(StateObservation stateObs) {
        // Simplemente devuelve la acción asignada a este nodo
        return this.accion;
    }
}
