package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import javax.microedition.lcdui.*;

/**
 * The Lode Runner hero player character evolving within a game stage.
 * - implements character's response to move requests (typically coming from keyboard)
 * - implements digging behaviors
 */
class LodeRunnerHero extends LodeRunnerCharacter {

    /** Move type constant for digging left */
    public static final int MOVE_DIG_LEFT = 7;
    /** Move type constant for digging right */
    public static final int MOVE_DIG_RIGHT = 8;
    /** Delay in heartbeats of floating messages */
    private static final int DELAY_MESSAGE = 12;
    /** Number of heartBeats before the floating message disapear */
    private int delayMessage;
    /** Current floating message text */
    private String currentMessage;
    private boolean wasShowingMessage;

    /** Initialize this hero in the stage */
    LodeRunnerHero(LodeRunnerStage stage) {
        super(stage);
    }

    /** Position this hero at a given tile index */
    public void moveToTile(int tileIndex) {
        super.moveToTile(tileIndex);
        delayMessage = 0;
        currentMessage = null;
    }

    /** Compute the sprite frame number for painting this hero  */
    public int getFrame() {
        // Special frames for digging
        if (delayBusy > 2) {
            if (currentMove == MOVE_DIG_RIGHT) {
                return 72;
            } else if (currentMove == MOVE_DIG_LEFT) {
                return 73;
            }
        }
        // Get frame from "magic" keys (see LodeRunnerCharacter.getFrame)
        int[] keyFrames = {23, 6, 0, 6};
        return getFrame(keyFrames);
    }

    /** Make a floating message appear above this character */
    protected void sayMessage(String message) {
        delayMessage = DELAY_MESSAGE;
        currentMessage = message;
        wasShowingMessage = true;
    }

    public boolean wasShowingMessage() {
        return wasShowingMessage;
    }

    /** Set the given move as current for this hero. Compute directions for that move. */
    protected void setCurrentMove(int move) {
        super.setCurrentMove(move);
        switch (move) {
            case MOVE_DIG_LEFT:
                lookLeft = true;
                xDelta = 0;
                yDelta = 0;
                delayBusy = 6; // Hero will be busy during 6 heartBeats
                break;
            case MOVE_DIG_RIGHT:
                lookLeft = false;
                xDelta = 0;
                yDelta = 0;
                delayBusy = 6; // Hero will be busy during 6 heartBeats
                break;
        }
    }

    /** Check if this hero should fall */
    protected boolean shouldFall() {
        // Don't fall if standing on a vilain's head
        return super.shouldFall() && !stage.isVilainAt(xTile, yTile + 1);
    }

    /** Request this hero to perform a given move */
    public void requestMove(int move) {
        // Set the requested move as proposed next move
        nextMove = move;
        // If the requested move is the reverse of the current move, don't wait to end of the current move
        if (move == getReverseMove()) {
            setCurrentMove(move);
        }
    }

    /** Mark this hero's digging as a hole into the stage tiles */
    protected boolean digHole(boolean completed) {
        if (currentMove == MOVE_DIG_LEFT || currentMove == MOVE_DIG_RIGHT) {
            // Mark the hole in the stage tiles
            int xFire = lookLeft ? xTile - 1 : xTile + 1;
            stage.setTile(xFire, yTile + 1, completed ? LodeRunnerStage.TILE_HOLE_EMPTY : LodeRunnerStage.TILE_HOLE_FULL);
            return true;
        } else {
            return false;
        }
    }

    /** Take the chest at this hero's tile position. If this is the last chest, enable the stage exit. */
    protected boolean takeChest() {
        boolean chestTaken = super.takeChest();
        if (chestTaken) {
            sayMessage(Integer.toString(nChests) + "/" + Integer.toString(stage.nChests));
            if (nChests == stage.nChests) {
                stage.enableExit();
            }
        }
        return chestTaken;
    }

    /** Execute this hero's next move */
    protected void makeNextMove() {
        // Check if this hero has competed the current stage
        if (stage.exitEnabled && yTile == 0) {
            stage.endCompleted = true;
        }
        // Make a standard move
        super.makeNextMove();
        // If digging, mark the attempt in the stage tiles and stop moving after digging
        if (digHole(false)) {
            nextMove = MOVE_NONE;
        }
    }

    /** Check if this hero can perform a given move */
    protected boolean isPossibleMove(int move) {
        if (move == MOVE_DIG_LEFT || move == MOVE_DIG_RIGHT) {
            // Can only dig into bricks
            // Can only dig below an empty tile (therefore excluding ladder, rope, chest, etc.)
            // Can't dig below a vilain
            int xFire = move == MOVE_DIG_LEFT ? xTile - 1 : xTile + 1;
            int nextType = stage.getTileAppearance(xFire, yTile);
            int nextBottomType = stage.getTileBehavior(xFire, yTile + 1);
            return (nextType == LodeRunnerStage.TILE_VOID
                    && nextBottomType == LodeRunnerStage.TILE_BRICK
                    && !stage.isVilainAt(xFire, yTile));
        } else {
            return super.isPossibleMove(move);
        }
    }

    /** Kill this hero */
    protected void kill() {
        stage.endHeroDied = true;
    }

    /**
     * Heartbeat for this hero.
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
     *     digHole()
     */
    public void heartBeat() {
        // If this hero is cautch by a vilain, he should die
        if (stage.isVilainAt(xTile, yTile)) {
            kill();
            return;
        }
        super.heartBeat();
        // If digging since 2 heartBeats (4 heartBeats left), mark the hole completion in the stage tiles
        if (delayBusy == 4) {
            digHole(true);
        }
        // Countdown message heartbeats
        if (delayMessage > 0) {
            delayMessage--;
        } else {
            if (wasShowingMessage) {
                wasShowingMessage = false;
            }
        }
    }

    /** Render the floating message above this hero */
    private void paintMessage(Graphics g) {
        stage.font.drawString(g, currentMessage, getCenterX(), getY() - (DELAY_MESSAGE - delayMessage) * LodeRunnerStage.SPRITE_HEIGHT[stage.spriteSize] / DELAY_MESSAGE / 2, Graphics.HCENTER | Graphics.BOTTOM);
    }

    /** Render this hero */
    public boolean paint(Graphics g) {
        // if this hero is digging...
        if (currentMove == MOVE_DIG_LEFT || currentMove == MOVE_DIG_RIGHT) {
            //... neighboring tiles should be painted accordingly
            int frameBlaster = 0, frameMelting = 0;
            switch (delayBusy) {
                case 6:
                    frameBlaster = lookLeft ? 80 : 78;
                    frameMelting = 84;
                    break;
                case 5:
                    frameBlaster = lookLeft ? 81 : 79;
                    frameMelting = 85;
                    break;
                case 4:
                    frameBlaster = 82;
                    frameMelting = 86;
                    break;
                case 3:
                    frameBlaster = 83;
                    frameMelting = 87;
                    break;
                case 2:
                    frameMelting = 88;
                    break;
                case 1:
                    frameMelting = 89;
                    break;
            }
            int xFire = xTile + (lookLeft ? -1 : +1);
            if (frameBlaster != 0) {
                stage.sprites[stage.spriteSize].paint(g, frameBlaster, xFire * LodeRunnerStage.SPRITE_WIDTH[stage.spriteSize], yTile * LodeRunnerStage.SPRITE_HEIGHT[stage.spriteSize]);
            }
            if (frameMelting != 0) {
                stage.sprites[stage.spriteSize].paint(g, frameMelting, xFire * LodeRunnerStage.SPRITE_WIDTH[stage.spriteSize], (yTile + 1) * LodeRunnerStage.SPRITE_HEIGHT[stage.spriteSize]);
            }
        }
        // paint hero itself
        boolean isVisible = super.paint(g);
        // Render a floating message, if any
        if (delayMessage > 0 && stage.spriteSize == LodeRunnerStage.SPRITE_NORMAL) {
            paintMessage(g);
        }
        return isVisible;
    }
}
