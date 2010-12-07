package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;

/**
 * Game MIDLet composed of a game canvas.
 */
abstract public class GameMIDlet extends MIDlet {

    /** Game canvas */
    private GameCanvas canvas = null;

    /** Create the game canvas, to be overloaded */
    abstract public GameCanvas createCanvas();

    /** Start the game canvas */
    public void startApp() {
        Displayable current = Display.getDisplay(this).getCurrent();
        if (current == null) {
            canvas = createCanvas();
            canvas.start();
            Display.getDisplay(this).setCurrent(canvas);
        } else {
            Display.getDisplay(this).setCurrent(current);
            canvas.resume();
        }
    }

    /** Pause the game canvas */
    public void pauseApp() {
        if (canvas != null) {
            canvas.pause();
        }
    }

    /** Stop the game canvas */
    public void destroyApp(boolean unconditional) {
        if (canvas != null) {
            canvas.stop();
        }
    }
}
