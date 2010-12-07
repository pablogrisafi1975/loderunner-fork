package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import java.util.*;
import java.io.*;
import javax.microedition.lcdui.Display;
import javax.microedition.rms.*;

/**
 * Specialized FullCanvas used to implement a game, providing:
 * - an animation thread for periodic rendering (frame rate)
 * - a timer thread for game event sequencing (including keeping the lights on)
 * - game persistency management in record stores
 * 
 * This is NOT the MIDP 2.0 javax.microedition.lcdui.game.GameCanvas
 */
public abstract class GameCanvas extends javax.microedition.lcdui.game.GameCanvas {

    /** Parent MIDlet */
    protected GameMIDlet midlet;
    /** Rendering frequency used by the animation thread */
    protected final static int FRAMERATE_MILLISEC = 66;
    /** "In pause" status of the game */
    protected volatile boolean isPaused = false;
    /** All game events are scheduled and sequenced by a single Timer thread */
    protected Timer timer = null;
    private Timer timerKeepLightsOn = null;
    /** Rendering is done in a dedicated animation thread */
    private volatile Thread animationThread = null;
    /** Flag set to false if this game canvas was never rendered, to true otherwise */
    private volatile boolean hasBeenShown = false;
    /** No repaint is needed, display is up to date */
    protected static final int REPAINT_NONE = 0;
    /** Repaint requested by a timer event (generally minor changes) */
    protected static final int REPAINT_TIMER = 1;
    /** Repaint requested by a key event (generally more important changes) */
    protected static final int REPAINT_KEY = 2;
    /** Full repaint is needed, display must be rendered again entirely */
    protected static final int REPAINT_ALL = 3;
    /** Tells the animation threads what elements should be rendered again */
    protected volatile int needsRepaint;
    protected int KEY_SOFT2;
    protected int KEY_SOFT1;

    /**
     * Game events are scheduled and sequenced by a Timer thread of GameCanvas.
     * Game events should inherit from this GameTask abstract class.
     */
    protected abstract class GameTask extends TimerTask {
    }

    /**
     * Game event tasks that require a display rendering refresh should inherit from this class
     */
    protected class RepaintTask extends GameTask {

        /** Triggered by the Timer. Ask the animation thread for a repaint (due to timer). */
        public void run() {
            needsRepaint |= REPAINT_TIMER;
        }
    }

    /**
     * Specialized TimerTask used to keep the LCD lights on during the game.
     */
    protected class KeepLightsOnTask extends TimerTask {

        /** Typically scheduled every 1 sec. */
        public final static int PERIOD = 1000;
        private int cntFlashesOnPause = 0;
        private final static int MAX_FLASHES_ON_PAUSE = 16;

        /** Triggered by the timerKeepLightsOn. Set on the LCD lights. */
        public void run() {
            if (!isPaused) {
                Display.getDisplay(midlet).flashBacklight(100);
                cntFlashesOnPause = 0;
            } else {
                Display.getDisplay(midlet).flashBacklight(100);
                cntFlashesOnPause++;
                if (cntFlashesOnPause > MAX_FLASHES_ON_PAUSE) {
                    timerKeepLightsOn.cancel();
                    timerKeepLightsOn = null;
                }
            }
        }
    }

    /** Constructor is called by the owning game MIDlet */
    GameCanvas(GameMIDlet midlet) {
        super(false);
        detectSoftKeys();
        System.out.println("KEY_SOFT1:" + KEY_SOFT1 + " KEY_SOFT2:" + KEY_SOFT2);
        setFullScreenMode(true);
        this.midlet = midlet;
        needsRepaint = REPAINT_ALL;
    }

    /** Animation thread for rendering  */
    private class AnimationThread extends Thread {

        /** Main loop for animation thread. Manage rendering every frame rate, when necessary. */
        public void run() {
            try {
                // when the GameCanvas will set its animationThread member to null, this thread will die
                while (this == animationThread) {
                    long startTime = System.currentTimeMillis();
                    if (!isPaused) {
                        if (isShown()) {
                            hasBeenShown = true;
                        } else if (hasBeenShown) {
                            pause();
                        }
                    }
                    if (isShown() && needsRepaint != REPAINT_NONE) {
                        // a rendering is needed
                        needsRepaint = REPAINT_NONE;
                        repaint(0, 0, getWidth(), getHeight());
                        serviceRepaints();
                    }
                    long timeTaken = System.currentTimeMillis() - startTime;
                    // see you soon (at next frame rate)...
                    if (timeTaken < FRAMERATE_MILLISEC) {
                        synchronized (this) {
                            wait(FRAMERATE_MILLISEC - timeTaken);
                        }
                    } else {
                        yield();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    /** Start the animation thread and start/resume the game */
    public synchronized void start() {
        animationThread = new AnimationThread();
        animationThread.start();
        resume();
    }

    /** Pause/stop the game and kill the animation thread */
    public synchronized void stop() {
        pause();
        animationThread = null;
    }

    /** Pause or stop the game. Timer is destroyed. */
    public synchronized void pause() {
        needsRepaint = REPAINT_ALL;
        isPaused = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /** Resume or start the game. Timer is constructed, its tasks are scheduled. */
    public synchronized void resume() {
        needsRepaint = REPAINT_ALL;
        isPaused = false;
        timer = new Timer();
        if (timerKeepLightsOn == null) {
            timerKeepLightsOn = new Timer();
            timerKeepLightsOn.schedule(new KeepLightsOnTask(), 0, KeepLightsOnTask.PERIOD);
        }
    }

    /** Game must be paused when the screen is hidden */
    public void hideNotify() {
        pause();
    }

    /**
     * Get the game action associated with the given key code of the device.
     * Overloaded to support button keys.
     */
    public int getGameAction(int keyCode) {

        int actionCode = 0;
        try {
            actionCode = super.getGameAction(keyCode);
        } catch (Exception e) {
        }
        if (actionCode == 0) {
            if (keyCode == KEY_SOFT1 || keyCode == KEY_NUM1) {
                actionCode = GAME_A;
            } else if (keyCode == KEY_SOFT2 || keyCode == KEY_NUM3) {
                actionCode = GAME_B;
            } else if (keyCode == KEY_NUM2) {
                actionCode = UP;
            } else if (keyCode == KEY_NUM4) {
                actionCode = LEFT;
            } else if (keyCode == KEY_NUM5) {
                actionCode = FIRE;
            } else if (keyCode == KEY_NUM6) {
                actionCode = RIGHT;
            } else if (keyCode == KEY_NUM8) {
                actionCode = DOWN;
            }
        }
        return actionCode;
    }

    /** Called when a game action key is pressed */
    abstract protected void gameAction(int actionCode);

    /** Pressing a key trigers a game action or resumes a paused game */
    public void keyPressed(int keyCode) {
        //System.out.println("GameCanvas.keyPressed(" + keyCode + ")");

        int actionCode = getGameAction(keyCode);
        if (isPaused && isShown()) {
            resume();
        }
        if (actionCode != 0) {
            gameAction(actionCode);
        }
        needsRepaint |= REPAINT_KEY;
    }

    /** Implemented by derivated classes to serialize the persistent state of the game */
    protected abstract void serializeState(DataOutput output) throws IOException;

    /** Implemented by derivated classes to deserialize the persistent state of the game */
    protected abstract void deserializeState(DataInput input) throws IOException;

    /** Saves the game in a persistent record store */
    protected void saveToStore(String storeName) throws IOException, RecordStoreException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutput output = new DataOutputStream(stream);
        serializeState(output);
        RecordStore store = RecordStore.openRecordStore(storeName, true);
        byte[] record = stream.toByteArray();
        if (store.getNumRecords() == 0) {
            store.addRecord(record, 0, record.length);
        } else {
            store.setRecord(1, record, 0, record.length);
        }
        store.closeRecordStore();
    }

    /** Loads the game from a persistent record store */
    protected void loadFromStore(String storeName) throws IOException, RecordStoreException {
        RecordStore store = RecordStore.openRecordStore(storeName, false);
        if (store != null) {
            ByteArrayInputStream stream = new ByteArrayInputStream(store.getRecord(1));
            store.closeRecordStore();
            DataInput input = new DataInputStream(stream);
            deserializeState(input);
        }
    }

    private void detectSoftKeys() {
        // Set nokia specific keycodes
        if (trySoftKeys("com.nokia.mid.ui.DeviceControl", -6, -7, false)) {
            return;
        }

        // Set Siemens specific keycodes
        if (trySoftKeys("com.siemens.mp.game.Light", -1, -4, false)) {
            return;
        }
        // Set Motorola specific keycodes, old phones
        if (trySoftKeys("com.motorola.phonebook.PhoneBookRecord", -21, -22, true)) {
            return;
        }

        if (trySoftKeys("com.motorola.pim.ContactList", -21, -22, true)) {
            return;
        }

        // check for often used values
        // This fixes bug with some Sharp phones and others
        try { // Check for "SOFT" in name description
            if (getKeyName(21).toUpperCase().indexOf("SOFT") >= 0) {
                KEY_SOFT1 = 21; // check for the 1st softkey
                KEY_SOFT2 = 22; // check for 2nd softkey
            } // Check for "SOFT" in name description
            if (getKeyName(-6).toUpperCase().indexOf("SOFT") >= 0) {
                KEY_SOFT1 = -6; // check for the 1st softkey
                KEY_SOFT2 = -7; // check for 2nd softkey
            }
            return;
        } catch (Exception e) {
        }

        // run thru all the keys
        for (int i = -127; i < 127; i++) {
            String keyname = "";

            try { // Check for "SOFT" in name description
                keyname = getKeyName(i).toUpperCase();
            } catch (Exception e) {
                continue;
            }
            if (keyname.indexOf("SOFT") >= 0) {
                // check for the 1st softkey
                if (keyname.indexOf("1") >= 0) {
                    KEY_SOFT1 = i; // check for 2nd softkey
                }
                if (keyname.indexOf("2") >= 0) {
                    KEY_SOFT2 = i;
                }
            }
        }
        // Sony calls exception on some keys
        // including softkeys
        // bugfix is to set the values ourself

        if (KEY_SOFT1 == 0) {
            KEY_SOFT1 = -6;
            KEY_SOFT2 = -7;
        }

    }

    private boolean trySoftKeys(String classNameToCheck, int keySoft1, int keySoft2, boolean mayChangeSign) {
        try {
            Class.forName(classNameToCheck);
            if (mayChangeSign) {
                if (getKeyName(keySoft1).toUpperCase().indexOf("SOFT") >= 0) {
                    KEY_SOFT1 = keySoft1;
                    KEY_SOFT2 = keySoft2;
                } else {
                    KEY_SOFT1 = -keySoft1;
                    KEY_SOFT2 = -keySoft2;
                }
            } else {
                KEY_SOFT1 = keySoft1;
                KEY_SOFT2 = keySoft2;
            }
            return true;
        } catch (ClassNotFoundException e) {
        }
        return false;
    }
}
