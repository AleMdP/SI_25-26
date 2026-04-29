package si2026.alejandrodelpozoalu.p02;

import ontology.Types.ACTIONS;
import core.game.StateObservation;
import java.util.function.Predicate;

public class Decision implements Nodo {
    private Predicate<StateObservation> condicion;
    private Nodo nodoSi, nodoNo;

    public Decision(Predicate<StateObservation> condicion, Nodo si, Nodo no) {
        this.condicion = condicion;
        this.nodoSi = si;
        this.nodoNo = no;
    }

    @Override
    public ACTIONS decidir(StateObservation stateObs) {
        // Si la condición se cumple, vamos por la rama 'Si', si no, por la 'No'
        if (condicion.test(stateObs)) {
            return nodoSi.decidir(stateObs);
        } else {
            return nodoNo.decidir(stateObs);
        }
    }
}
