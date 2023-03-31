package it.unibo.unibomber.game.controller.api;

import it.unibo.unibomber.game.controller.impl.Menu;
import it.unibo.unibomber.game.controller.impl.Option;
import it.unibo.unibomber.game.controller.impl.Play;
import it.unibo.unibomber.game.controller.impl.StateGame;
import it.unibo.unibomber.game.model.api.Game;

/**
 * World class.
 */
public interface World {

    /**
     * @return menu.
     */
    Menu getMenu();

    /**
     * @return play.
     */
    Play getPlay();

    /**
     * set Play.
     */
    void setPlay();

    /**
     * @return option.
     */
    Option getOption();

    /**
     * @return stateGame.
     */
    StateGame getEndGame();

    /**
     * @return game.
     */
    Game getGame();

    /**
     * Stop timer of game.
     */
    void stopTimer();
    /**
     * Set in pause timer.
     */
    void pauseTimer();
    /**
     * Start Timer.
     */
    void startTimer();
}
