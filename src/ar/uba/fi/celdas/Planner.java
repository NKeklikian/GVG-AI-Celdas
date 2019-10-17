package ar.uba.fi.celdas;

import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.*;

public class Planner {

    private Theories theories;
    private Random randomGenerator;
    private Comparator<Theory> comparator;
    private Vector2d exit;

    Planner(Theories theories, Vector2d exit) {
        this.theories = theories;
        randomGenerator = new Random();
        comparator = (left, right) -> {
            if (left.getUtility() < right.getUtility()) {
                return 1;
            } else if (left.getUtility() > right.getUtility()) {
                return -1;
            }
            return 0;
        };
        this.exit = exit;
    }

    public Theory getTheory(List<Theory> stateTheories) {
        Theory theory = new Theory();
        stateTheories.sort(comparator);
        return stateTheories.get(0);
    }

    public Vector2d avatarPosition(StateObservation stateObs) {
        Vector2d avatarPosition = stateObs.getAvatarPosition();
        avatarPosition.x = avatarPosition.x / stateObs.getBlockSize();
        avatarPosition.y = avatarPosition.y / stateObs.getBlockSize();
        return avatarPosition;
    }

    private float utility(StateObservation state) {
        double distance = avatarPosition(state).dist(exit);
        return 100 / (float)(1 + distance * distance);
    }

    public Theories updateTheories(Theories theories, Theory pastTheory, StateObservation state) {
        Map<Integer, List<Theory>> theoryMap = theories.getTheories();
        if(theories.existsTheory(pastTheory)) {
            List<Theory> theoryList = theoryMap.get(pastTheory.hashCodeOnlyCurrentState());
            for (final ListIterator<Theory> it = theoryList.listIterator(); it.hasNext(); ) {
                Theory theory = it.next();
                if (theory.equals(pastTheory)) {
                    theory.setUsedCount(theory.getUsedCount() + 1);
                    if (state.isAvatarAlive()) {
                        theory.setSuccessCount(theory.getSuccessCount() + 1);
                    }
                    it.set(theory);
                }
            }
            theoryMap.put(pastTheory.hashCodeOnlyCurrentState(), theoryList);
            theories.setTheories(theoryMap);
        } else {
            if (state.isAvatarAlive()) {
                pastTheory.setUsedCount(1);
                if (!pastTheory.getAction().equals(Types.ACTIONS.ACTION_NIL)) {
                    pastTheory.setUtility(utility(state));
                    pastTheory.setSuccessCount(1);
                } else {
                    pastTheory.setSuccessCount(0);
                    pastTheory.setUtility(0);
                }
            } else {
                pastTheory.setUtility(0);
                pastTheory.setUsedCount(1);
                pastTheory.setSuccessCount(0);
            }
            try {
                theories.add(pastTheory);
            } catch (Exception e) {}
        }
        return theories;
    }

    public boolean explore() {
        return randomGenerator.nextInt(2) == 1;
    }

    public Types.ACTIONS random(List<Types.ACTIONS> actions) {
        int index = randomGenerator.nextInt(actions.size());
        Types.ACTIONS actionToTake = actions.get(index);
        return actionToTake;
    }
}
