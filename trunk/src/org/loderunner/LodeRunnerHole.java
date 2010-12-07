package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import javax.microedition.lcdui.*;

/**
 * A hole digged into a Lode Runner game stage 
 */
class LodeRunnerHole {

    /** Lode Runner stage where this hole is digged */
    protected LodeRunnerStage stage;
    /** Position of this hole, in tiles */
    protected int xTile, yTile;
    /** Number of heartBeats before this hole will refill */
    protected int delayBusy;
    /** Delay in heartbeats before refill */
    public static final int DELAY_REFILL = 96;
    /** Delay in heartbeats before refilling becomes visible */
    public static final int DELAY_VISIBLE_REFILL = 2;

    LodeRunnerHole(LodeRunnerStage stage, int xTile, int yTile) {
        this.stage = stage;
        delayBusy = DELAY_REFILL;
        this.xTile = xTile;
        this.yTile = yTile;
    }

    /** Fill this hole */
    protected void fill() {
        stage.setTile(xTile, yTile, LodeRunnerStage.TILE_BRICK);
        stage.holes.removeElement(this);
    }

    /** Heartbeat for this character */
    public void heartBeat() {
        // Time to fill this hole?
        if (delayBusy == 0) {
            fill();
        } // Decrease (if any) the number of heartBeats before this hole is filled
        else if (delayBusy > 0) {
            delayBusy--;
        }
    }

    /** Render this hole */
    protected boolean paint(Graphics g) {
        int frameHole = 0;
        if (delayBusy < DELAY_VISIBLE_REFILL) {
            frameHole = 75;
        }
        if (delayBusy < 2 * DELAY_VISIBLE_REFILL) {
            frameHole = 74;
        }
        if (frameHole == 0) {
            return false;
        }
        return stage.sprites[stage.spriteSize].paint(g, frameHole, xTile * LodeRunnerStage.SPRITE_WIDTH[stage.spriteSize], yTile * LodeRunnerStage.SPRITE_HEIGHT[stage.spriteSize]);
    }
}
