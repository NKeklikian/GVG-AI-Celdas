package ar.uba.fi.celdas;

import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.*;

public class Planner {

    private Theories theories;
    private Random randomGenerator;
    //private Comparator<Theory> comparator; //  I might try to use this later
    private Vector2d exit;

    Planner(Theories theories, Vector2d exit) {
        this.theories = theories;
        randomGenerator = new Random();
        /* I might try to use this later
        comparator = (left, right) -> {
            if (left.getUtility() < right.getUtility()) {
                return 1;
            } else if (left.getUtility() > right.getUtility()) {
                return -1;
            }
            return 0;
        };*/
        this.exit = exit;
    }

    public Theory getTheory(List<Theory> stateTheories) {
        //stateTheories.sort(comparator);
        //System.out.println(stateTheories);
        int i = randomGenerator.nextInt((stateTheories.size()));
        while (stateTheories.get(i).getUtility()  == 0) { // Useless theory
            i = randomGenerator.nextInt((stateTheories.size()));
        }
        //System.out.println(stateTheories.get(i));
        System.out.printf("Predicted utility: %s\n",stateTheories.get(i).getUtility());
        return stateTheories.get(i);
    }

    public Vector2d avatarPosition(StateObservation stateObs) {
        Vector2d avatarPosition = stateObs.getAvatarPosition();
        avatarPosition.x = avatarPosition.x / stateObs.getBlockSize();
        avatarPosition.y = avatarPosition.y / stateObs.getBlockSize();
        return avatarPosition;
    }

    private float utility(StateObservation state) {
        double distance = avatarPosition(state).dist(exit);
        return 1000 / (float)(1 + distance);
    }

    public Theories updateTheories(Theories theories, Theory pastTheory, StateObservation state, boolean moved) {
        Map<Integer, List<Theory>> theoryMap = theories.getTheories();
        //System.out.printf("moved:%s\n",moved);
        if(theories.existsTheory(pastTheory)) {
            //System.out.println("Existing theory");
            List<Theory> theoryList = theories.getSortedListForCurrentState(pastTheory);
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
            //System.out.println("Not existing theory");
            if (state.isAvatarAlive()) {
                pastTheory.setUsedCount(1);
                if (moved) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return theories;
    }

    public boolean explore() {
        return randomGenerator.nextInt(20) > 10;
    }

    public Types.ACTIONS random(List<Types.ACTIONS> actions) {
        int index = randomGenerator.nextInt(actions.size());
        Types.ACTIONS actionToTake = actions.get(index);
        return actionToTake;
    }
}
