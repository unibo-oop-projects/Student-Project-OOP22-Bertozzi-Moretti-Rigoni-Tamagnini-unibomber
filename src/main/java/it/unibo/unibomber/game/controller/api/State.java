package it.unibo.unibomber.game.controller.api;

import java.awt.event.MouseEvent;

import it.unibo.unibomber.game.model.impl.MenuButtonImpl;

public interface State {
    public boolean isMouseIn(MouseEvent e, MenuButtonImpl mb);
}
