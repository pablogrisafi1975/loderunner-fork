package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import javax.microedition.lcdui.*;

/**
 * A Lode Runner character evolving within a game stage 
 */
abstract class LodeRunnerCharacter {

    /** Lode Runner stage where this character evolves */
    protected LodeRunnerStage stage;
    /** Position of this character, in tiles */
    protected int xTile, yTile;
    /** Position of this character, relative to its tile (xAdjust is in 1/6 of a tile, yAdjust is in 1/5 of tile) */
    protected int xAdjust, yAdjust;
    /** Direction of this character's move */
    protected int xDelta, yDelta;
    /** Direction of this characted head (true if facing left) */
    protected boolean lookLeft;
    /** Number of heartBeats before this character can move again */
    protected int delayBusy;
    /** Current move of this character. Value is one of the MOVE_* move constants. */
    protected int currentMove;
    /** Next requested move for this character. Might not be executed if impossible. */
    protected int nextMove;
    /** Move type constant for standing still */
    public static final int MOVE_NONE = 0;
    /** Move type constant for running left */
    public static final int MOVE_RUN_LEFT = 1;
    /** Move type constant for running right */
    public static final int MOVE_RUN_RIGHT = 2;
    /** Move type constant for climbing up a ladder */
    public static final int MOVE_CLIMB_UP = 3;
    /** Move type constant for climbing down a ladder */
    public static final int MOVE_CLIMB_DOWN = 4;
    /** Move type constant for falling down */
    public static final int MOVE_FALL_DOWN = 5;
    /** Number of chests taken by this character */
    public int nChests;

    /** Initialize a new character in the stage */
    LodeRunnerCharacter(LodeRunnerStage stage) {
        this.stage = stage;
        moveToTile(0);
        nChests = 0;
    }

    /** Position this character at a given tile index */
    public void moveToTile(int tileIndex) {
        xTile = tileIndex % LodeRunnerStage.STAGE_WIDTH;
        yTile = tileIndex / LodeRunnerStage.STAGE_WIDTH;
        lookLeft = false;
        xAdjust = yAdjust = 0;
        xDelta = yDelta = delayBusy = 0;
        currentMove = MOVE_NONE;
        nextMove = MOVE_NONE;
    }

    /**
     * Compute the sprite frame number for painting this character.
     * keyFrames is a sprite index array of the form :
     * { FIRST_SPRITE_CLIMB, FIRST_SPRITE_LEFT, FIRST_SPRITE_RIGHT, BEFORE_SECOND_SPRITE_LEFT }
     */
    protected int getFrame(int[] keyFrames) {
        // starting at FIRST_SPRITE_CLIMB
        // - 5 fall climb frames
        // - 1 fall frame facing left, 1 fall frame facing right
        // - 6 rope frames facing right, 6 rope frames facing left
        int frame;
        if (yDelta != 0) {
            if (currentMove == MOVE_FALL_DOWN) {
                frame = lookLeft ? keyFrames[0] + 5 : keyFrames[0] + 6;
            } else {
                frame = keyFrames[0] + (yAdjust + 5) % 5;
            }
        } else {
            if (xDelta >= 0) {
                frame = (xAdjust + 6) % 6;
            } else {
                frame = (6 - xAdjust) % 6;
            }
            if (stage.getTileBehavior(xTile, yTile) == LodeRunnerStage.TILE_ROPE) {
                frame += keyFrames[0] + 7;
                if (lookLeft) {
                    frame += 6;
                }
            } else {
                // starting at FIRST_SPRITE_LEFT
                // - 1 (first) run frame facing left
                // contining with BEFORE_SECOND_SPRITE_LEFT+1
                // - 5 (next) run frame facing left
                // starting at FIRST_SPRITE_RIGHT
                // - 6 run frames facing right
                if (lookLeft && frame == 0) {
                    frame = keyFrames[1];
                } else if (lookLeft) {
                    frame += keyFrames[3];
                } else {
                    frame += keyFrames[2];
                }
            }
        }
        return frame;
    }

    /** Compute the sprite frame number for painting this character */
    abstract protected int getFrame();

    /** Get the X center position of this character in pixels */
    public int getCenterX() {
        return (6 * xTile + xAdjust + 3) * LodeRunnerStage.SPRITE_WIDTH[stage.spriteSize] / 6;
    }

    /** Get the Y center position of this character in pixels */
    public int getCenterY() {
        return (5 * yTile + yAdjust + 3) * LodeRunnerStage.SPRITE_HEIGHT[stage.spriteSize] / 5;
    }

    /** Get the X left position of this character in pixels */
    public int getX() {
        if (xDelta >= 0) {
            return (6 * xTile + xAdjust + (xAdjust + 6) % 2) * LodeRunnerStage.SPRITE_WIDTH[stage.spriteSize] / 6;
        } else {
            return (6 * xTile + xAdjust - (6 - xAdjust) % 2) * LodeRunnerStage.SPRITE_WIDTH[stage.spriteSize] / 6;
        }
    }

    /** Get the Y top position of this character in pixels */
    public int getY() {
        return (5 * yTile + yAdjust) * LodeRunnerStage.SPRITE_HEIGHT[stage.spriteSize] / 5;
    }

    /** Set the given move as current for this character. Compute directions for that move. */
    protected void setCurrentMove(int move) {
        switch (currentMove = move) {
            case MOVE_NONE:
                xDelta = 0;
                yDelta = 0;
                break;
            case MOVE_RUN_LEFT:
                lookLeft = true;
                xDelta = -1;
                yDelta = 0;
                break;
            case MOVE_RUN_RIGHT:
                lookLeft = false;
                xDelta = 1;
                yDelta = 0;
                break;
            case MOVE_CLIMB_UP:
                xDelta = 0;
                yDelta = -1;
                break;
            case MOVE_CLIMB_DOWN:
            case MOVE_FALL_DOWN:
                xDelta = 0;
                yDelta = 1;
                break;
        }
    }

    /** Check if this character should fall */
    protected boolean shouldFall() {
        // Don't fall if inside a brick (ouch!), on a ladder or hung to a rope
        int currentType = stage.getTileBehavior(xTile, yTile);
        if (currentType == LodeRunnerStage.TILE_BRICK || currentType == LodeRunnerStage.TILE_LADDER || currentType == LodeRunnerStage.TILE_ROPE) {
            return false;
        }
        // Don't fall if standing on brick, or concrete, or at the top of a ladder
        int bottomType = stage.getTileBehavior(xTile, yTile + 1);
        if (bottomType == LodeRunnerStage.TILE_BRICK || bottomType == LodeRunnerStage.TILE_CONCRETE || bottomType == LodeRunnerStage.TILE_LADDER) {
            return false;
        }
        return true;
    }

    /** Get the exact reverse of this character's current move, if reversible */
    protected int getReverseMove() {
        switch (currentMove) {
            case MOVE_RUN_LEFT:
                return MOVE_RUN_RIGHT;
            case MOVE_RUN_RIGHT:
                return MOVE_RUN_LEFT;
            case MOVE_CLIMB_UP:
                return MOVE_CLIMB_DOWN;
            case MOVE_CLIMB_DOWN:
                return MOVE_CLIMB_UP;
            default:
                return MOVE_NONE;
        }
    }

    /** Check if this character can perform a given move */
    protected boolean isPossibleMove(int move) {
        switch (move) {
            case MOVE_RUN_LEFT: {
                // Can't run into brick, trap or concrete
                int nextType = stage.getTileBehavior(xTile - 1, yTile);
                return (nextType != LodeRunnerStage.TILE_BRICK && nextType != LodeRunnerStage.TILE_TRAP && nextType != LodeRunnerStage.TILE_CONCRETE);
            }
            case MOVE_RUN_RIGHT: {
                // Can't run into brick, trap or concrete
                int nextType = stage.getTileBehavior(xTile + 1, yTile);
                return (nextType != LodeRunnerStage.TILE_BRICK && nextType != LodeRunnerStage.TILE_TRAP && nextType != LodeRunnerStage.TILE_CONCRETE);
            }
            case MOVE_CLIMB_UP: {
                // Need a ladder to climb up. Can't climb up into brick, trap or concrete
                int curType = stage.getTileBehavior(xTile, yTile);
                int nextType = stage.getTileBehavior(xTile, yTile - 1);
                return (curType == LodeRunnerStage.TILE_LADDER && nextType != LodeRunnerStage.TILE_BRICK && nextType != LodeRunnerStage.TILE_TRAP && nextType != LodeRunnerStage.TILE_CONCRETE);
            }
            case MOVE_CLIMB_DOWN: {
                // This move can also be used to force this character to fall (eg. from a rope)
                // Can't climb down (or fall down) into brick or concrete (but trap is OK).
                int nextType = stage.getTileBehavior(xTile, yTile + 1);
                return (nextType != LodeRunnerStage.TILE_BRICK && nextType != LodeRunnerStage.TILE_CONCRETE);
            }
            default:
                return false;
        }
    }

    /** Compute the next move for this character */
    protected void computeNextMove() {
        // Verify that requested move is possible
        if (!isPossibleMove(nextMove)) {
            nextMove = MOVE_NONE;
        } // Adjust move as necessary
        else if (nextMove == MOVE_CLIMB_DOWN) {
            // If this move was used to force this character to fall (eg. from a rope), make him really fall.
            int nextType = stage.getTileBehavior(xTile, yTile + 1);
            if (nextType != LodeRunnerStage.TILE_LADDER) {
                nextMove = MOVE_FALL_DOWN;
            }
        }
    }

    /** Take the chest at this character's tile position */
    protected boolean takeChest() {
        if (stage.getTile(xTile, yTile) == LodeRunnerStage.TILE_CHEST) {
            nChests++;
            stage.setTile(xTile, yTile, LodeRunnerStage.TILE_VOID);
            return true;
        }
        return false;
    }

    /** Execute this character's next move */
    protected void makeNextMove() {
        boolean shouldFall = shouldFall();
        // Attempt to take a chest if not just falling
        if (currentMove == MOVE_FALL_DOWN || !shouldFall) {
            takeChest();
        }
        // If this character should fall, ignore requested next move and make him really fall...
        if (shouldFall) {
            nextMove = MOVE_FALL_DOWN;
        } // ...else compute his move according to his requested next move
        else {
            computeNextMove();
        }
        // Perform the move
        setCurrentMove(nextMove);
        // Always stop moving after a fall
        if (nextMove == MOVE_FALL_DOWN) {
            nextMove = MOVE_NONE;
        }
    }

    /** Check if this character can change tile during a move. (That test is called during position adjustement.) */
    protected boolean canChangeTile(int xNewTile, int yNewTile) {
        return true;
    }

    /* Compute the next position of this character */
    protected boolean computeNewPosition() {
        // Compute new position
        int xNewTile = xTile, yNewTile = yTile;
        int xNewAdjust = xAdjust + xDelta, yNewAdjust = yAdjust + yDelta;
        if (xNewAdjust < -3) {
            xNewAdjust += 6;
            xNewTile--;
        } else if (xNewAdjust > 3) {
            xNewAdjust -= 6;
            xNewTile++;
        }
        if (yNewAdjust < -2) {
            yNewAdjust += 5;
            yNewTile--;
        } else if (yNewAdjust > 2) {
            yNewAdjust -= 5;
            yNewTile++;
        }
        // Validate new position before applying
        if ((xNewTile == xTile && yNewTile == yTile) || canChangeTile(xNewTile, yNewTile)) {
            xTile = xNewTile;
            yTile = yNewTile;
            xAdjust = xNewAdjust;
            yAdjust = yNewAdjust;
            return true;
        } else {
            return false;
        }
    }

    /** Kill this character */
    abstract protected void kill();

    /**
     * Heartbeat for this character.
     *
     * Calling diagram is the following:
     *   heartBeat()
     *     kill()
     *     makeNextMove()
     *       shouldFall()
     *       takeChest()
     *       computeNextMove()
     *         isPossibleMove()
     *       setCurrentMove()
     *     computeNewPosition()
     *       canChangeTile()
     */
    public void heartBeat() {
        // If this character is inside plain brick, he should die
        if (stage.getTileBehavior(xTile, yTile) == LodeRunnerStage.TILE_BRICK) {
            kill();
            return;
        }
        // Decrease (if any) the number of heartBeats before this character can move again
        if (delayBusy > 0) {
            delayBusy--;
            return;
        }
        // If the current move has ended, execute the next move
        if (xAdjust == 0 && yAdjust == 0) {
            makeNextMove();
        }
        // Compute new position
        computeNewPosition();
    }

    /** Render this character */
    public boolean paint(Graphics g) {
        // Paint appropriate sprite
        return stage.sprites[stage.spriteSize].paint(g, getFrame(), getX(), getY());
    }
}
