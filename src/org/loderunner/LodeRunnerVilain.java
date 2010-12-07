package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import javax.microedition.lcdui.*;

/**
 * The Lode Runner vilain character evolving within a game stage.
 * - can be trapped in digged holes
 * - implements respawning behavior
 * - implements take/drop chest behavior
 * - implements moving AI
 */
class LodeRunnerVilain extends LodeRunnerCharacter {

    /** Move type constant for climbing outside a hole */
    public static final int MOVE_CLIMB_HOLE = 6;
    /** Move type constant for respawning */
    public static final int MOVE_RESPAWN = 7;
    /** Delay in heartbeats before retrying a fizzled move */
    public static final int DELAY_RETRY_LATER = 1;
    /** Delay in heartbeats of staying trapped inside a hole */
    public static final int DELAY_TRAPPED_HOLE = 32;
    /** Delay in heartbeats before respawn */
    public static final int DELAY_RESPAWN = 8;
    /**
     * Is this vilain trapped in a digged hole?
     * - if currentMove equals MOVE_FALL_DOWN, he is falling into the hole
     * - if currentMove equals MOVE_NONE, he is already trapped inside the hole
     * - if currentMove equals MOVE_CLIMB_HOLE, he is climbing outside the hole
     */
    private boolean isTrapped;

    /** Initialize this vilain in the stage */
    LodeRunnerVilain(LodeRunnerStage stage) {
        super(stage);
    }

    /** Position this vilain at a given tile index */
    public void moveToTile(int tile) {
        super.moveToTile(tile);
        lookLeft = true;
        isTrapped = false;
    }

    /** Compute the sprite frame number for painting this vilain */
    public int getFrame() {
        int[] keyFrames = {42, 21, 61, 66};
        // When respawning
        if (currentMove == MOVE_RESPAWN) {
            if (delayBusy > DELAY_RESPAWN / 2) {
                return 76;
            } else {
                return 77;
            }
        }
        // When trapped into a digged hole...
        if (isTrapped && currentMove == MOVE_NONE) {
            //... use falling sprites at the beggining ...
            if (delayBusy > DELAY_RESPAWN) {
                return lookLeft ? keyFrames[0] + 5 : keyFrames[0] + 6;
            } //... and running sprites just before climbing out the hole
            else {
                int frame = delayBusy % 2;
                if (lookLeft && frame == 0) {
                    frame = keyFrames[1];
                } else if (lookLeft) {
                    frame += keyFrames[3];
                } else {
                    frame += keyFrames[2];
                }
                return frame;
            }
        }
        // Get frame from "magic" keys (see LodeRunnerCharacter.getFrame)
        return getFrame(keyFrames);
    }

    /** Take the chest at this vilain's tile position. A vilain can hold only one chest. */
    protected boolean takeChest() {
        if (nChests == 0) {
            return super.takeChest();
        } else {
            return false;
        }
    }

    /**
     * Drop this vilain's chest, if any, at current tile position, if empty.
     * - always, if falling into a hole
     * - at random, if otherwise standing on brick, concrete or on top of a ladder
     */
    protected void dropChest() {
        boolean canDrop = nChests > 0 && stage.getTile(xTile, yTile) == LodeRunnerStage.TILE_VOID;
        if (canDrop && !isTrapped) {
            canDrop = currentMove != MOVE_FALL_DOWN && stage.random.nextBoolean(6);
            if (canDrop) {
                int bottomType = stage.getTileBehavior(xTile, yTile + 1);
                canDrop = bottomType == LodeRunnerStage.TILE_BRICK || bottomType == LodeRunnerStage.TILE_CONCRETE || bottomType == LodeRunnerStage.TILE_LADDER;
            }
        }
        if (canDrop) {
            nChests--;
            stage.setTile(xTile, yTile, LodeRunnerStage.TILE_CHEST);
        }
    }

    /**
     * Check if this vilain should fall.
     * - a vilain trapped into a digged hole doesn't fall further down
     * - a vilain on top of another trapped vilain doesn't fall further down
     */
    protected boolean shouldFall() {
        return super.shouldFall() && !isTrapped
                && !(stage.isVilainAt(xTile, yTile + 1) && stage.getTile(xTile, yTile + 1) == LodeRunnerStage.TILE_HOLE_EMPTY);
    }

    /** Check if this vilain can change tile during a move. (That test is called during position adjustement.) */
    protected boolean canChangeTile(int xNewTile, int yNewTile) {
        // Don't run into another vilain
        return super.canChangeTile(xNewTile, yNewTile) && !stage.isVilainAt(xNewTile, yNewTile);
    }

    /* Compute the next position of this character */
    protected boolean computeNewPosition() {
        // Additional AI condition: don't stay stuck...
        boolean hasMoved = super.computeNewPosition();
        if (!hasMoved) {
            // ...but rather try going backwards on occasion
            int reverseMove = getReverseMove();
            if (reverseMove != MOVE_NONE && stage.random.nextBoolean(3)) {
                setCurrentMove(reverseMove);
            }
        }
        return hasMoved;
    }

    /** Set the given move as current for this vilain. Compute directions for that move. */
    protected void setCurrentMove(int move) {
        super.setCurrentMove(move);
        if (move == MOVE_CLIMB_HOLE) {
            xDelta = 0;
            yDelta = -1;
        }
    }

    /** Check if this vilain can perform a given move */
    protected boolean isPossibleMove(int move) {
        boolean isPossible;
        if (move == MOVE_CLIMB_HOLE) {
            // Can't escape from a digged hole into brick, trap or  or concrete
            int nextType = stage.getTileBehavior(xTile, yTile - 1);
            isPossible = (nextType != LodeRunnerStage.TILE_BRICK && nextType != LodeRunnerStage.TILE_TRAP && nextType != LodeRunnerStage.TILE_CONCRETE);
        } else {
            isPossible = super.isPossibleMove(move);
        }
        if (isPossible) {
            // Additional AI conditions:
            // - don't run into another vilain
            // - don't jump into digged holes (avoid suicidal tendancies)
            if (move == MOVE_RUN_LEFT) {
                isPossible = !stage.isVilainAt(xTile - 1, yTile);
            } else if (move == MOVE_RUN_RIGHT) {
                isPossible = !stage.isVilainAt(xTile + 1, yTile);
            } else if (move == MOVE_CLIMB_UP || move == MOVE_CLIMB_HOLE) {
                isPossible = !stage.isVilainAt(xTile, yTile - 1);
            } else if (move == MOVE_CLIMB_DOWN) {
                isPossible = stage.getTile(xTile, yTile + 1) != LodeRunnerStage.TILE_HOLE_EMPTY && !stage.isVilainAt(xTile, yTile + 1);
            }
        }
        return isPossible;
    }

    /** Try the given move as the next move for this vilain. Returns true on success, false otherwise. */
    private boolean tryNextMove(int move) {
        nextMove = move;
        super.computeNextMove();
        return nextMove != MOVE_NONE;
    }

    /** Find the best move for this vilain to reach a vertical access to climb up or down towards the hero */
    private boolean findAccess(int move) {
        int xLeft = 0, xRight = 0;
        // Compute the vertical access locations
        // (Note: other vilains being considered impassable, further vilains will try to use a different access towards player)
        LodeRunnerVilain ghost = new LodeRunnerVilain(stage);
        ghost.moveToTile(LodeRunnerStage.getTileIndex(xTile, yTile));
        while (ghost.isPossibleMove(MOVE_RUN_LEFT)) {
            ghost.xTile--;
            if (ghost.isPossibleMove(move)) {
                xLeft = xTile - ghost.xTile;
                break;
            }
        }
        ghost.moveToTile(LodeRunnerStage.getTileIndex(xTile, yTile));
        while (ghost.isPossibleMove(MOVE_RUN_RIGHT)) {
            ghost.xTile++;
            if (ghost.isPossibleMove(move)) {
                xRight = ghost.xTile - xTile;
                break;
            }
        }
        ghost = null;
        // No reachable access, don't take any move yet
        if (xLeft == 0 && xRight == 0) {
            return tryNextMove(MOVE_NONE);
        }
        // Only one access, take it
        if (xRight == 0) {
            return tryNextMove(MOVE_RUN_LEFT);
        }
        if (xLeft == 0) {
            return tryNextMove(MOVE_RUN_RIGHT);
        }
        // Find shortest distance to hero
        int xHero = stage.hero.xTile;
        xLeft += Math.abs(xTile - xLeft - xHero);
        xRight += Math.abs(xTile + xRight - xHero);
        if (xLeft < xRight) {
            return tryNextMove(MOVE_RUN_LEFT);
        }
        if (xLeft > xRight) {
            return tryNextMove(MOVE_RUN_RIGHT);
        }
        // Routes have same length, don't take any move yet
        return tryNextMove(MOVE_NONE);
    }

    /** Compute the next AI move for this vilain */
    protected void computeNextMove() {
        int initialMove = nextMove;
        if (initialMove != MOVE_CLIMB_HOLE && stage.hero != null) {
            int yHero = stage.hero.yTile;
            // If this vilain is far enough, try to anticipate the hero's move
            if (stage.hero.xTile != xTile) {
                if (stage.hero.yDelta > 0) {
                    yHero += 2;
                } else if (stage.hero.yDelta < 0) {
                    yHero -= 2;
                }
            }
            // Try to move up or down towards the hero
            if (yHero < yTile && tryNextMove(MOVE_CLIMB_UP)) {
                return;
            }
            if (yHero > yTile && tryNextMove(MOVE_CLIMB_DOWN)) {
                return;
            }
            // Try to reach a ladder up or a way down towards the hero
            if (yHero < yTile && findAccess(MOVE_CLIMB_UP)) {
                return;
            }
            if (yHero > yTile && findAccess(MOVE_CLIMB_DOWN)) {
                return;
            }
            // Run towards the hero if not moving or at same height (without this condition, level 8 is impassable)
            if (yHero == yTile || initialMove == MOVE_NONE) {
                // Try to move left or right towards the hero
                if (stage.hero.xTile < xTile && tryNextMove(MOVE_RUN_LEFT)) {
                    return;
                }
                if (stage.hero.xTile > xTile && tryNextMove(MOVE_RUN_RIGHT)) {
                    return;
                }
                // Try to move in the same direction as the hero
                if (tryNextMove(stage.hero.lookLeft ? MOVE_RUN_LEFT : MOVE_RUN_RIGHT)) {
                    return;
                }
            }
        }
        // Continue initial move
        if (tryNextMove(initialMove)) {
            return;
        }
        // Try a random move (prevent vilains from being stuck)
        switch (stage.random.nextInt(4)) {
            case 0:
                tryNextMove(MOVE_CLIMB_UP);
                break;
            case 1:
                tryNextMove(MOVE_CLIMB_DOWN);
                break;
            case 2:
                tryNextMove(MOVE_RUN_LEFT);
                break;
            case 3:
                tryNextMove(MOVE_RUN_RIGHT);
                break;
        }
    }

    /** Execute this vilain's next move */
    protected void makeNextMove() {
        // Determine nextMove when:
        // a/ this vilain is trapped
        if (isTrapped) {
            // - he has fallen into a trap
            if (currentMove == MOVE_FALL_DOWN) {
                delayBusy = DELAY_TRAPPED_HOLE;
                setCurrentMove(MOVE_NONE);
                nextMove = MOVE_CLIMB_HOLE;
            } // - he can escape the trap
            else if (nextMove == MOVE_CLIMB_HOLE) {
                if (isPossibleMove(MOVE_CLIMB_HOLE)) {
                    setCurrentMove(MOVE_CLIMB_HOLE);
                    nextMove = MOVE_NONE;
                } else {
                    delayBusy = DELAY_RETRY_LATER;
                    setCurrentMove(MOVE_NONE);
                    nextMove = MOVE_CLIMB_HOLE;
                }
            } // - he has escaped the trap (make a normal AI move)
            else if (currentMove == MOVE_CLIMB_HOLE) {
                nextMove = MOVE_NONE;
                super.makeNextMove();
                isTrapped = false;
            }
        } // b/ this vilain should respawn but cannot
        else if (currentMove == MOVE_RESPAWN && stage.isVilainAt(xTile, yTile)) {
            delayBusy = DELAY_RETRY_LATER;
        } // c/ otherwise, make a normal AI move
        else {
            super.makeNextMove();
        }

        // When, after this, this vilain isn't in a trap
        if (!isTrapped) {
            // Check if he should fall in a trap
            if (currentMove == MOVE_FALL_DOWN && stage.getTile(xTile, yTile + 1) == LodeRunnerStage.TILE_HOLE_EMPTY) {
                isTrapped = true;
                nextMove = MOVE_NONE;
            }
            // Drop this vilain's chest, if appropriate
            dropChest();
        }
    }

    /** Kill this vilain */
    protected void kill() {
        moveToTile(stage.computeRandomRespawnPoint());
        currentMove = MOVE_RESPAWN;
        delayBusy = DELAY_RESPAWN;
    }

    /**
     * Heartbeat for this vilain.
     *
     * Calling diagram is the following:
     *   heartBeat()
     *     kill()
     *     makeNextMove()
     *       setCurrentMove()
     *       super.makeNextMove()
     *         shouldFall()
     *         takeChest()
     *         computeNextMove()
     *           tryNextMove()
     *             super.computeNextMove()
     *               isPossibleMove()
     *           findAccess()
     *             isPossibleMove()
     *             tryNextMove()
     *         setCurrentMove()
     *       dropChest()
     *     computeNewPosition()
     *       canChangeTile()
     *       setCurrentMove()
     */
    public void heartBeat() {
        // Special animation before escaping a hole
        if (isTrapped && currentMove == MOVE_NONE && delayBusy <= DELAY_RESPAWN) {
            lookLeft = delayBusy / 2 % 2 == 0;
        }
        super.heartBeat();
    }

    /** Draw a small red and white diamond */
    private void drawDiamond(Graphics g, int x, int y, int size) {
        g.setColor(0x00ff0000);
        if (size > 0) {
            g.drawLine(x - 1, y, x, y - 1);
            g.drawLine(x, y - 1, x + 1, y);
            g.drawLine(x + 1, y, x, y + 1);
            g.drawLine(x, y + 1, x - 1, y);
            if (size > 1) {
                g.drawLine(x - 2, y, x, y - 2);
                g.drawLine(x, y - 2, x + 2, y);
                g.drawLine(x + 2, y, x, y + 2);
                g.drawLine(x, y + 2, x - 2, y);
            }
            g.setColor(0x00ffffff);
        }
        g.fillRect(x, y, 1, 1);
    }

    /** Render this vilain */
    public boolean paint(Graphics g) {
        boolean isVisible = super.paint(g);
        if (!isVisible) {
            // Project this vilain's position on the clip borders
            int cw = g.getClipWidth() - 1, ch = g.getClipHeight() - 1;
            int cx = 2 * g.getClipX() + cw, cy = 2 * g.getClipY() + ch;
            int x = 2 * getCenterX() - cx, y = 2 * getCenterY() - cy;
            int z = Integer.MAX_VALUE;
            if (Math.abs(x) > cw) {
                z = Math.min(z, Math.abs(x) * ch);
            }
            if (Math.abs(y) > ch) {
                z = Math.min(z, Math.abs(y) * cw);
            }
            x = (cx + cw * ch * x / z) / 2;
            y = (cy + cw * ch * y / z) / 2;
            // Signal this vilain's direction with a small diamond
            int size = 0;
            if (100 * z < 200 * cw * ch) {
                size++;
            }
            if (100 * z < 130 * cw * ch) {
                size++;
            }
            drawDiamond(g, x, y, size);
        }
        return isVisible;
    }
}
