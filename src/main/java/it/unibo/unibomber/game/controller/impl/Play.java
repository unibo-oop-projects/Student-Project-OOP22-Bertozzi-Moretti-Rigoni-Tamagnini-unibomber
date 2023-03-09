package it.unibo.unibomber.game.controller.impl;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import it.unibo.unibomber.game.controller.api.GameLoop;
import it.unibo.unibomber.game.ecs.api.Component;
import it.unibo.unibomber.game.ecs.api.Entity;
import it.unibo.unibomber.game.model.api.Field;
import it.unibo.unibomber.game.model.api.Game;
import it.unibo.unibomber.game.model.impl.EntityFactoryImpl;
import it.unibo.unibomber.game.model.impl.FieldImpl;
import it.unibo.unibomber.game.model.impl.GameImpl;
import it.unibo.unibomber.game.view.PlayView;
import it.unibo.unibomber.utilities.Pair;
import it.unibo.unibomber.utilities.Constants.UI.SpritesMap;

public class Play extends StateImpl implements KeyListener, GameLoop {
    // TODO
	//private BufferedImage sprite;
	private Deque<Integer> keyQueue;
	private Game game;
	private List<String> map = new ArrayList<String>();
	private PlayView view;
	private Field field;

	public Play(final WorldImpl world) {
		super(world);
		new SpritesMap();
		game = new GameImpl(world);
		view = new PlayView(this);
		field = new FieldImpl(game);
		initClasses();
		// TODO load map at settings not in constructor
		loadMap();
	}

	private void initClasses() {
		game.addEntity(new EntityFactoryImpl(game).makePlayable(new Pair<Float, Float>(0f, 1f)));
		keyQueue = new LinkedList<>();
	}

	private void loadMap() {
		BufferedReader bf;
		try {
			bf = new BufferedReader(new FileReader("./src/main/res/area1.map"));
			String line;
			try {
				line = bf.readLine();
				while (line != null) {
					map.add(line);
					line = bf.readLine();
				}
				bf.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		/* TODO
		 * for (int index = 0; index < 19; index++) {
		 * List<String> singleLine = Arrays.asList(map.get(index).split(" "));
		 * for (int j = 0; j < singleLine.size(); j++) {
		 * switch(Integer.parseInt(singleLine.get(j))){
		 * case 6:
		 * game.addEntity(new EntityFactoryImpl(game).makeIndestructibleWall(new
		 * Pair<Float,Float>((float)j, (float)index)));
		 * break;
		 * case 2:
		 * game.addEntity(new EntityFactoryImpl(game).makePowerUp(new
		 * Pair<Float,Float>((float)j, (float)index),PowerUpType.FIREUP));
		 * break;
		 * }
		 * }
		 * }
		 */
		field.updateField();
	}

	@Override
	public final void update() {
		for (int i = 0; i < game.getEntities().size(); i++) {
			for (Component c : game.getEntities().get(i).getComponents()) {
				c.update();
			}
		}
		field.updateField();
		view.update();
	}

	@Override
	public final void draw(final Graphics g) {
		view.draw(g);
	}

	@Override
	public final void keyReleased(final KeyEvent e) {
		if (keyQueue.contains(e.getKeyCode())) {
			keyQueue.remove(e.getKeyCode());
		}
	}

	@Override
	public final void keyTyped(final KeyEvent e) {
		if (!keyQueue.contains(e.getKeyCode())) {
			keyQueue.addLast(e.getKeyCode());
		}
	}

	@Override
	public final void keyPressed(final KeyEvent e) {
		if (!keyQueue.contains(e.getKeyCode())) {
			keyQueue.addFirst(e.getKeyCode());
		}
	}

	public final Deque<Integer> getKeys() {
		return keyQueue;
	}

	public final List<Entity> getEntities() {
		return game.getEntities();
	}
}
