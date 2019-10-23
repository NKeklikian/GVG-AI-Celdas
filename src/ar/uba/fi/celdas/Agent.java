package ar.uba.fi.celdas;

import java.util.ArrayList;
import java.util.List;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

/**
 * Created with IntelliJ IDEA.
 * User: ssamot
 * Date: 14/11/13
 * Time: 21:45
 * This is a Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Agent extends AbstractPlayer {
    /**
     * List of available actions for the agent
     */
    protected ArrayList<Types.ACTIONS> actions;

    protected Planner planner;
    private Types.ACTIONS pastAction;
    private StateObservation pastState;
    private Vector2d pastPosition;
    private Types.ACTIONS action;

    protected Theories theories;

    /**
     * Public constructor with state observation and time due.
     * @param so state observation of the current game.
     * @param elapsedTimer Timer for the controller creation.
     */
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        try {
            theories = TheoryPersistant.load();
        } catch (Exception e) {
            theories = new Theories();
        }

        Vector2d exit = so.getPortalsPositions()[0].get(0).position;
        exit.x = exit.x / so.getBlockSize();
        exit.y = exit.y / so.getBlockSize();

        planner = new Planner(theories, exit);
        pastAction = Types.ACTIONS.ACTION_LEFT;
        actions = so.getAvailableActions();
        pastState = so;
    }

    public Vector2d avatarPosition(StateObservation stateObs) {
        Vector2d avatarPosition = stateObs.getAvatarPosition();
        avatarPosition.x = avatarPosition.x / stateObs.getBlockSize();
        avatarPosition.y = avatarPosition.y / stateObs.getBlockSize();
        return avatarPosition;
    }

    public char[][] getState(StateObservation stateObs){
        /* Attempt to use 3x3 theories
        int avatarX = (int) avatarPosition(stateObs).x;
        int avatarY = (int) avatarPosition(stateObs).y;
        char[][] state = new char[3][3];
        for (int i = avatarY - 1; i < avatarY + 2; i++) {
            for (int j = avatarX - 1; j < avatarX + 2; j++) {
                state[i + 1 - avatarY][j + 1 - avatarX] = new Perception(stateObs).getLevel()[i][j];
            }
        }
        return state
        */
        return new Perception(stateObs).getLevel();
    }

    public String charArrayToStr(char[][] charrarray ){
        StringBuilder sb = new StringBuilder("");
        if(charrarray!=null){
            for(int i=0;i< charrarray.length; i++){
                for(int j=0;j<  charrarray[i].length; j++){
                    sb.append(charrarray[i][j]);
                }
                sb.append("\n");
            }
        }
        return sb.toString();
    }


    /**
     * Picks an action. This function is called every game step to request an
     * action from the player.
     * @param stateObs Observation of the current state.
     * @param elapsedTimer Timer when the action returned is due.
     * @return An action for the current state
     */
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        Perception perception = new Perception(stateObs);
        Perception pastPerception = new Perception(pastState);
        System.out.println(pastPerception.toString());
        char[][] currentState = getState(pastState);
        char[][] predictedState = getState(stateObs);

        Theory theory = new Theory();
        theory.setCurrentState(currentState);
        theory.setAction(pastAction);
        theory.setPredictedState(predictedState);
        //System.out.println(theory);
        //System.out.println(theories.getSortedListForCurrentState(theory));
        theories = planner.updateTheories(theories,theory,stateObs,!(avatarPosition(pastState).equals(avatarPosition(stateObs))),false);
        theory = new Theory();
        theory.setCurrentState(predictedState);
        List<Theory> theoryList = theories.getSortedListForCurrentState(theory);
        //System.out.println(theories.getSortedListForCurrentState(theory));
        //System.out.println(theoryList.size());
        //System.out.println(actions);
        //System.out.println(actions.size());
        //System.out.println(theoryList);
        if (theoryList.size() == actions.size()){
            action = planner.getTheory(theoryList).getAction();
        } else {
            //System.out.println("Explore");
            List<Types.ACTIONS> actionsCopy = new ArrayList(actions);
            for (Theory t: theoryList) {
                actionsCopy.remove(t.getAction());
            }
            //System.out.println(actionsCopy);
            action = planner.random(actionsCopy);
        }
        //System.out.println(action);

        pastAction = action;
        pastState = stateObs;

        if (planner.hasWinningPath()) {
            planner.createLabyrinth();
            return planner.nextAction(charArrayToStr(perception.getLevel()).hashCode());
        }

        return action;
    }

    public void result(StateObservation stateObs, ElapsedCpuTimer elapsedCpuTimer) {
        if (stateObs.isGameOver()) {
            Theory theory = new Theory();
            theory.setCurrentState(getState(pastState));
            theory.setAction(pastAction);
            if (stateObs.isAvatarAlive()) {
                theory.setPredictedState(getState(stateObs));
            } else {
                theory.setPredictedState(getState(pastState));
            }
            theories = planner.updateTheories(theories,theory,stateObs,true, stateObs.isAvatarAlive() );
            try {
                TheoryPersistant.save(theories);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}