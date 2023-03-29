package it.unibo.unibomber.game.ecs.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import it.unibo.unibomber.game.ecs.api.Entity;
import it.unibo.unibomber.game.ecs.api.Type;
import it.unibo.unibomber.utilities.Constants;
import it.unibo.unibomber.utilities.Direction;
import it.unibo.unibomber.utilities.Pair;
import it.unibo.unibomber.utilities.Utilities;

/**
 * This component manage the AI of Bots, meaning movement,
 * placing bombs and interacting with the world.
 */
public final class AIComponent extends AbstractComponent {

     private List<Direction> followingPath;
     private Pair<Float, Float> oldPosition;
     private boolean isGettingCloser;

     /**
      * isGettingCloser is used to get the bot to more closely get to the next cell.
      */
     public AIComponent(Pair<Float, Float> position) {
          isGettingCloser = false;
          oldPosition = position;
          followingPath = new ArrayList<>(List.of(Direction.CENTER));
     }

     @Override
     public void update() {
          final Entity entity = this.getEntity();
          Type[][] typesMatrix = getMatrixTypes();

          setValidPath(typesMatrix);
          move(this.followingPath.get(0));
          placeBombIfAdvantageous(typesMatrix);
          updatePath(oldPosition, entity.getPosition());
          oldPosition = entity.getPosition();
          System.out.println(isSafe(typesMatrix));
     }

     private void placeBombIfAdvantageous(Type[][] typesMatrix) {
          final Type[][] typesWithBomber = getMatrixWithBombers(typesMatrix);
          List<Type> typesToDestroy = List.of(Type.DESTRUCTIBLE_WALL, Type.BOMBER);
          if (wouldBeSafe(typesMatrix, this.getEntity().getPosition())
           && wouldExplodeNextTo(typesToDestroy, typesWithBomber, this.getEntity().getPosition())) {
               placeBomb(typesMatrix);
               followingPath = new ArrayList<>(List.of(Direction.CENTER));
          }
     }

     /**
      * @param typesMatrix matrix of game types.
      *                    fills the path if empty.
      */
     private void setValidPath(final Type[][] typesMatrix) {
          if (this.followingPath.isEmpty()) {
               this.followingPath = getNextPath(typesMatrix);
               Collections.reverse(followingPath);
               this.followingPath = this.followingPath.stream()
                         .limit(1)
                         .collect(Collectors.toList());
          }
          if (this.followingPath.isEmpty()) {
               this.followingPath = new ArrayList<>(List.of(Direction.CENTER));

          }
     }

     /**
      * @param typesMatrix matrix of game types
      * @return a List of directions toward the most advantageous position
      */
     private List<Direction> getNextPath(final Type[][] typesMatrix) {
          if (!isSafe(typesMatrix)) {
               return getDirectionsTowards(Type.EXPLOSION, false, typesMatrix);
          } else {
               var goTowards = getDirectionsTowards(Type.POWERUP, true, typesMatrix);
               if (goTowards.contains(Direction.CENTER)) {
                    final Type[][] typesWithBomber = getMatrixWithBombers(typesMatrix);
                    goTowards = getDirectionsTowards(Type.BOMBER, true, typesWithBomber);
                    if (typeLeftExist(Type.DESTRUCTIBLE_WALL)) {
                         List<Direction> path = getDirectionsTowards(Type.DESTRUCTIBLE_WALL, true, typesMatrix);
                         path.remove(0);
                         return path;
                    } else {
                         goTowards = getDirectionsTowards(Type.BOMBER, true, typesMatrix);
                    }
               }
               return goTowards;
          }
     }

     private boolean wouldBeSafe(Type[][] typesMatrix, Pair<Float, Float> position){
          if(typesMatrix[Math.round(position.getX())][Math.round(position.getY())] == Type.EXPLOSION){
               return false;
          }
          Type[][] newMatrix = new Type[typesMatrix.length][typesMatrix[0].length];
          for (int x = 0; x < typesMatrix.length; x++)
               for (int y = 0; y < typesMatrix[0].length; y++)
                    newMatrix[x][y] = typesMatrix[x][y];
          for (final Direction d : Direction.valuesNoCenter()) {
               int strength = this.getEntity().getComponent(PowerUpListComponent.class).get().getBombFire();
               addExplosionToMatrix(newMatrix, new Pair<>(Math.round(position.getX()), Math.round(position.getY())),
                         strength, d, 0);
          }
          //TODO
          var a = getDirectionsTowards(Type.EXPLOSION, false, newMatrix);
          return a.get(0) == Direction.CENTER ? false : true;
     }

     private void placeBomb(Type[][] typesMatrix) {
          final PowerUpListComponent powerups = this.getEntity()
                    .getComponent(PowerUpListComponent.class)
                    .get();
          if (powerups.getBombNumber() - powerups.getBombPlaced() > 0
                    && nextTo(Type.AIR, typesMatrix, this.getEntity().getPosition())) {
               final BombPlaceComponent placeBomb = this.getEntity()
                         .getComponent(BombPlaceComponent.class).get();
                placeBomb.placeBomb();
          }
     }

     /**
      * @param searchedType the type I want to know info about
      * @param typesMatrix  matrix of game types
      * @param position     the position of the AI entity
      * @return whether the entity is next to the searched type
      */
     private boolean nextTo(final Type searchedType, final Type[][] typesMatrix, final Pair<Float, Float> position) {
          for (final Direction d : Direction.valuesNoCenter()) {
               final int nextX = Math.round(position.getX() + d.getX());
               final int nextY = Math.round(position.getY() + d.getY());
               if (Utilities.isBetween(nextX, 0, Constants.UI.Screen.getTilesWidth())
                         && Utilities.isBetween(nextY, 0, Constants.UI.Screen.getTilesHeight())
                         && typesMatrix[nextX][nextY] == searchedType) {
                    return true;
               }
          }
          return false;
     }

     private boolean wouldExplodeNextTo(final List<Type> searchedTypes, final Type[][] typesMatrix,
               final Pair<Float, Float> position) {
          int strength = this.getEntity().getComponent(PowerUpListComponent.class).get().getBombFire();
          List<Type> solidTypes = List.of(Type.INDESTRUCTIBLE_WALL, Type.BOMB, Type.DESTRUCTIBLE_WALL, Type.POWERUP);
          for (final Direction d : Direction.valuesNoCenter()) {
               for (int i = 1; i <= strength; i++) {
                    Pair<Integer, Integer> newPosition = new Pair<>(Math.round(position.getX()) + d.getX() * i,
                              Math.round(position.getY()) + d.getY() * i);
                    if (Utilities.isBetween(newPosition.getX(), 0, Constants.UI.Screen.getTilesWidth())
                              && Utilities.isBetween(newPosition.getY(), 0, Constants.UI.Screen.getTilesHeight())) {
                         if (searchedTypes.contains(typesMatrix[newPosition.getX()][newPosition.getY()])) {
                              return true;
                         }
                         if (solidTypes.contains(typesMatrix[newPosition.getX()][newPosition.getY()])) {
                              break;
                         }
                    }
               }
          }
          return false;
     }

     /**
      * @param types       the list of types to go towards/away from
      * @param goTowards   whether to go towards or away from types
      * @param typesMatrix matrix of game types
      * @return the closest (safe) path towards types
      */
     private List<Direction> getDirectionsTowards(final Type type, final boolean goTowards,
               final Type[][] typesMatrix) {
          final List<Type> toAvoid = new ArrayList<>(List.of(Type.RISING_WALL, Type.BOMB, Type.DESTRUCTIBLE_WALL,
                    Type.INDESTRUCTIBLE_WALL, Type.EXPLOSION));
          toAvoid.remove(type);
          int[][] checkedPositions = new int[typesMatrix.length][typesMatrix[0].length];
          final Deque<Pair<Integer, Integer>> unsafePositions = new LinkedList<>();
          final Pair<Float, Float> startingPosition = this.getEntity().getPosition();
          checkedPositions[Math.round(startingPosition.getX())][Math.round(startingPosition.getY())] = 1;
          unsafePositions.add(new Pair<Integer, Integer>(
                    Math.round(startingPosition.getX()),
                    Math.round(startingPosition.getY())));

          while (unsafePositions.size() > 0) {
               final Pair<Integer, Integer> current = unsafePositions.poll();
               final Type cellType = typesMatrix[current.getX()][current.getY()];
               if (toAvoid.contains(cellType)) {
                    continue;
               }
               if (type.equals(cellType) ^ goTowards) {
                    checkSides(unsafePositions, checkedPositions, typesMatrix, current, toAvoid);
               } else {
                    return extractPath(current, checkedPositions);
               }
          }
          // if this code is reached no safe position can be found, rip
          return new ArrayList<>(List.of(Direction.CENTER));
     }

     /**
      * @param checkPositions   the queue to add positions to check
      * @param checkedPositions the matrix which tracks already checked positions
      * @param typesMatrix      matrix of game types
      * @param current          the current position being checked
      * @param toAvoid          the types where an entity cannot go towards
      */
     private void checkSides(final Queue<Pair<Integer, Integer>> checkPositions, final int[][] checkedPositions,
               final Type[][] typesMatrix, final Pair<Integer, Integer> current, final List<Type> toAvoid) {
          for (final Direction d : Direction.values()) {
               if (d != Direction.CENTER) {
                    final int lastValue = checkedPositions[current.getX()][current.getY()];
                    final Pair<Integer, Integer> nextCell = new Pair<>(current.getX() + d.getX(),
                              current.getY() + d.getY());
                    if (Utilities.isBetween(nextCell.getX(), 0, Constants.UI.Screen.getTilesWidth())
                              && Utilities.isBetween(nextCell.getY(), 0, Constants.UI.Screen.getTilesHeight())
                              && checkedPositions[nextCell.getX()][nextCell.getY()] == 0
                              && !toAvoid.contains(typesMatrix[nextCell.getX()][nextCell.getY()])) {
                         checkPositions.add(nextCell);
                         checkedPositions[nextCell.getX()][nextCell.getY()] = lastValue + 1;
                    }
               }
          }
     }

     /**
      * @param finalPosition    the final position reached
      * @param checkedPositions the matrix of ordered checked positions
      * @return a list of directions toward the reached type
      */
     private List<Direction> extractPath(final Pair<Integer, Integer> finalPosition, final int[][] checkedPositions) {
          Pair<Integer, Integer> current = finalPosition;
          int currentValue = checkedPositions[current.getX()][current.getY()];
          final List<Direction> path = new ArrayList<>();
          while (currentValue != 1) {
               for (final Direction d : Direction.valuesNoCenter()) {
                    final Pair<Integer, Integer> nextCell = new Pair<>(current.getX() + d.getX(),
                              current.getY() + d.getY());
                    if (Utilities.isBetween(nextCell.getX(), 0, Constants.UI.Screen.getTilesWidth())
                              && Utilities.isBetween(nextCell.getY(), 0, Constants.UI.Screen.getTilesHeight())
                              && checkedPositions[nextCell.getX()][nextCell.getY()] == currentValue - 1) {
                         path.add(d);
                         currentValue--;
                         current = nextCell;
                         break;
                    }
               }
          }
          for (int i = 0; i < path.size(); i++) {
               path.set(i, path.get(i).reverse());
          }
          if (path.isEmpty())
               path.add(Direction.CENTER);
          return path;
     }

     /**
      * @param typesMatrix matrix of game types
      * @return whether a cell is safe
      */
     private boolean isSafe(final Type[][] typesMatrix) {
          final Pair<Float, Float> position = this.getEntity().getPosition();
          final Type type = typesMatrix[Math.round(position.getX())][Math.round(position.getY())];
          return type != Type.EXPLOSION && type != Type.BOMB;
     }

     /**
      * @param type the type searched for
      * @return whether or not a type exists in the current game
      */
     private boolean typeLeftExist(final Type type) {
          return this.getEntity().getGame().getEntities().stream()
                    .filter(e -> e.getType().equals(type))
                    .count() > 0;
     }

     /**
      * @param moveTo the direction to move towards
      */
     private void move(final Direction moveTo) {
          final MovementComponent movementComponent = this.getEntity().getComponent(MovementComponent.class).get();
          movementComponent.moveBy(new Pair<Float, Float>(
                    moveTo.getX() * Constants.Input.POSITIVE_MOVE,
                    moveTo.getY() * Constants.Input.POSITIVE_MOVE));
     }

     /**
      * @param oldPosition the position the entity had last frame
      * @param newPosition the position the entity has now
      *                    this method checks whether or not the next integer has
      *                    been reached, if so it follows the next
      *                    direction
      */
     private void updatePath(final Pair<Float, Float> oldPosition, final Pair<Float, Float> newPosition) {
          if (isGettingCloser || Math.round(oldPosition.getX()) != Math.round(newPosition.getX())
                    || Math.round(oldPosition.getY()) != Math.round(newPosition.getY())
                    || this.followingPath.get(0) == Direction.CENTER) {
               if (!canMoveFurther(newPosition)) {
                    this.followingPath.remove(0);
                    isGettingCloser = false;
               }
          }
     }

     /**
      * @param newPosition the position of the bot
      * @return whether or not the bot can move further to better allign with the
      *         cell
      *         without jepardizing it's safety
      */
     private boolean canMoveFurther(final Pair<Float, Float> newPosition) {
          isGettingCloser = true;
          float currentDifferenceX = Math.abs(newPosition.getX()) - Math.abs(Math.round(newPosition.getX()));
          float currentDifferenceY = Math.abs(newPosition.getY()) - Math.abs(Math.round(newPosition.getY()));
          Pair<Float, Float> tryPosition = new Pair<>(newPosition.getX() + followingPath.get(0).getX()
                    * this.getEntity().getSpeed() * Constants.Movement.MULTIPLIER_GLOBAL_SPEED,
                    newPosition.getY() + followingPath.get(0).getY() * this.getEntity().getSpeed()
                              * Constants.Movement.MULTIPLIER_GLOBAL_SPEED);
          float nextDifferenceX = Math.abs(tryPosition.getX()) - Math.abs(Math.round(newPosition.getX()));
          float nextDifferenceY = Math.abs(tryPosition.getY()) - Math.abs(Math.round(newPosition.getY()));

          boolean isCloser = Math.abs(currentDifferenceX) > Math.abs(nextDifferenceX)
                    || Math.abs(currentDifferenceY) > Math.abs(nextDifferenceY);
          boolean isOver = Math.round(tryPosition.getX()) != Math.round(newPosition.getX())
                    || Math.round(tryPosition.getY()) != Math.round(newPosition.getY());

          return (isCloser && !isOver);
     }

     public final Type[][] getMatrixTypes() {
          final Pair<Integer, Integer> gameDimensions = this.getEntity().getGame().getDimensions();
          final Type[][] typesMatrix = new Type[gameDimensions.getX()][gameDimensions.getY()];
          initializeTypeMatrix(typesMatrix);
          addEntitiesToMatrix(typesMatrix);
          handleBombExplosion(typesMatrix);

          return typesMatrix;
     }

     private Type[][] getMatrixWithBombers(Type[][] typesMatrix) {
          Type[][] typesWithBomber = new Type[typesMatrix.length][typesMatrix[0].length];
          for (int x = 0; x < typesMatrix.length; x++)
               for (int y = 0; y < typesMatrix[0].length; y++)
                    typesWithBomber[x][y] = typesMatrix[x][y];

          List<Entity> entities = this.getEntity().getGame().getEntities();
          entities.stream()
                    .filter(e -> e.getType().equals(Type.BOMBER))
                    .map(Entity::getPosition)
                    .filter(e -> !e.equals(this.getEntity().getPosition()))
                    .map(e -> Utilities.getRoundedPair(e))
                    .forEach(e -> {
                         typesWithBomber[e.getX()][e.getY()] = Type.BOMBER;
                    });
          return typesWithBomber;
     }

     private void initializeTypeMatrix(final Type[][] typesMatrix) {
          for (int x = 0; x < typesMatrix.length; x++) {
               for (int y = 0; y < typesMatrix[0].length; y++) {
                    typesMatrix[x][y] = Type.AIR;
               }
          }
     }

     private void handleBombExplosion(final Type[][] typesMatrix) {
          final var field = this.getEntity().getGame().getGameField().getField();
          field.keySet().stream()
                    .filter(e -> field.get(e).getX().equals(Type.BOMB))
                    .forEach(e -> {
                         final PowerUpListComponent powerupList = field.get(e).getY()
                                   .getComponent(PowerUpListComponent.class)
                                   .get();

                         for (final Direction d : Direction.valuesNoCenter()) {
                              addExplosionToMatrix(typesMatrix, e, powerupList.getBombFire(), d, 1);
                         }
                         if (Math.round(this.getEntity().getPosition().getX()) == e.getX()
                                   && Math.round(this.getEntity().getPosition().getY()) == e.getY()) {
                              typesMatrix[e.getX()][e.getY()] = Type.EXPLOSION;
                         }
                    });

     }

     private void addExplosionToMatrix(final Type[][] typesMatrix, final Pair<Integer, Integer> where,
               final int strength, final Direction d, final int step) {
          if (step <= strength) {
               final Pair<Integer, Integer> newDirection = new Pair<>(where.getX() + d.getX() * step,
                         where.getY() + d.getY() * step);
               List<Type> volatileTypes = List.of(Type.AIR, Type.EXPLOSION, Type.POWERUP);
               if (Utilities.isBetween(newDirection.getX(), 0, Constants.UI.Screen.getTilesWidth())
                         && Utilities.isBetween(newDirection.getY(), 0, Constants.UI.Screen.getTilesHeight())
                         && volatileTypes.contains(typesMatrix[newDirection.getX()][newDirection.getY()])) {
                    typesMatrix[newDirection.getX()][newDirection.getY()] = Type.EXPLOSION;
                    addExplosionToMatrix(typesMatrix, where, strength, d, step + 1);
               }
          }
     }

     private void addEntitiesToMatrix(final Type[][] typesMatrix) {
          final var field = this.getEntity().getGame().getGameField().getField();
          for (final Pair<Integer, Integer> pos : field.keySet()) {
               typesMatrix[pos.getX()][pos.getY()] = field.get(pos).getX();
          }
     }
}
