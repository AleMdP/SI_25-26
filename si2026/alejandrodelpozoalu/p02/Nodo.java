package si2026.alejandrodelpozoalu.p02;

import ontology.Types.ACTIONS;
import core.game.StateObservation;

public interface Nodo {
    // Devuelve la acción que el agente debe realizar
    ACTIONS decidir(StateObservation stateObs);
}