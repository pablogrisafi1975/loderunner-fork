package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import java.util.*;
import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.RecordStoreException;

/**
 * Specialized GameCanvas used to implement the Lode Runner game.
 */
class LodeRunnerCanvas extends GameCanvas {

    /** Game name use for store persistency */
    private static final String GAME_NAME = "LodeRunner";
    /** Game splash screen */
    private GameSprite splashScreen = null;
    /** Message to display when game is paused */
    private String pauseMessage = null;
    /** Maximum number of lifes given to the player when starting the game (constant)*/
    private static final int MAX_LIFES = 5;
    /** Lifes left to the player */
    private int lifes = MAX_LIFES;
    /** Level number of the current stage */
    public int level = 0;
    /** Current stage, when game is in progress */
    private LodeRunnerStage stage = null;
    private int newLevel;
    private byte[] levelStatuses = new byte[LodeRunnerStage.MAX_LEVELS];

    ;
    private static final byte STATUS_DONE = 1;
    private static final byte STATUS_NOT_DONE = 0;
    private static final int BG_COLOR_FOR_SOFTKEYS = 0x1E90FF;
    private int spaceBetweenLines;
    private int w0;
    private int h0;

    /**
     * Construct the Lode Runner canvas
     * - load resources
     * - read level from store
     * - load stage with current level
     */
    LodeRunnerCanvas(GameMIDlet midlet) {
        super(midlet);
        // Load resources
        try {
            splashScreen = new GameSprite("/res/LodeRunner.png", 112, 16, 0, 0);
        } catch (Exception e) {
        }
        // Read saved members from store
        try {
            loadFromStore(GAME_NAME);
        } catch (Exception e) {
        }
        // Load stage with current level
        try {
            stage = new LodeRunnerStage(this);
            stage.loadFromResource();
        } catch (Exception e) {
        }
        needsRepaint = REPAINT_ALL;
    }

    /** Implement game serialization (level, lifes) */
    public void serializeState(DataOutput output) throws IOException {
        output.writeInt(level);
        output.writeInt(lifes);
        output.write(levelStatuses);
    }

    /** Implement game deserialization (level, lifes) */
    public void deserializeState(DataInput input) throws IOException {
        level = input.readInt();
        lifes = input.readInt();
        input.readFully(levelStatuses);
        if (levelStatuses == null || levelStatuses.length != LodeRunnerStage.MAX_LEVELS) {
            levelStatuses = new byte[LodeRunnerStage.MAX_LEVELS];
        }

    }

    private void painTopMessage(Graphics g) {
        paintLeft(g, -1, "Level: " + format3(level + 1), 0);
        paintRight(g, -1, "0=Menu", 0);
    }

    /** Render the message or splash screen */
    private void paintMessage(Graphics g, int w, int h) {
        Font font = null;
        // There is a message to render
        if (pauseMessage != null) {
            font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD | Font.STYLE_ITALIC, Font.SIZE_LARGE);
            g.setFont(font);
            g.setColor(0x00ffffff);
            g.drawString(pauseMessage, w / 2, (h - font.getHeight()) / 2, Graphics.HCENTER | Graphics.TOP);
        } // No message but a splash screen
        else if (splashScreen != null) {
            if ((level / LodeRunnerStage.GAME_LEVELS) % 2 == 0) {
                // Lode Runner splash screen
                splashScreen.paint(g, 0, w / 2, h / 2, Graphics.HCENTER | Graphics.VCENTER);
            } else {
                // Championship splash screen
                splashScreen.paint(g, 0, w / 2, h / 2 - 6, Graphics.HCENTER | Graphics.VCENTER);
                splashScreen.paint(g, 1, w / 2, h / 2 + 6, Graphics.HCENTER | Graphics.VCENTER);
            }
        } // No message, no splash screen
        else {
            // Simulate a splash screen with text
            if ((level / LodeRunnerStage.GAME_LEVELS) % 2 == 0) {
                // Lode Runner splash screen
                font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
                g.setFont(font);
                g.setColor(0x00ffffff);
                g.drawString("Lode Runner", w / 2, (h - font.getHeight()) / 2, Graphics.HCENTER | Graphics.TOP);
            } else {
                // Championship splash screen
                font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD, Font.SIZE_LARGE);
                g.setFont(font);
                g.setColor(0x00ffffff);
                g.drawString("Lode Runner", w / 2, (h - font.getHeight()) / 2 - 6, Graphics.HCENTER | Graphics.TOP);
                font = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_ITALIC, Font.SIZE_LARGE);
                g.setFont(font);
                g.setColor(0x00ff0000);
                g.drawString("Championship", w / 2, (h - font.getHeight()) / 2 + 6, Graphics.HCENTER | Graphics.TOP);
            }
        }
    }
    boolean clearAfterPause = false;

    /** Render the game canvas */
    public void paint(Graphics g) {
        // Render the stage
        if (stage != null) {
            stage.spriteSize = LodeRunnerStage.SPRITE_NORMAL;
            stage.paint(g);
            if (isPaused || !stage.isLoaded) {
                clearAfterPause = true;

                // clean the stage
                w0 = g.getClipWidth();
                h0 = g.getClipHeight();
                g.setColor(0x000000);
                g.fillRect(0, 0, w0, h0);

                // Render the stage mini-map
                stage.spriteSize = LodeRunnerStage.SPRITE_SMALL;
                stage.paint(g);

                int cx = LodeRunnerStage.STAGE_WIDTH * LodeRunnerStage.SPRITE_WIDTH[stage.spriteSize];
                int cy = LodeRunnerStage.STAGE_HEIGHT * LodeRunnerStage.SPRITE_HEIGHT[stage.spriteSize];
                int x = (w0 - cx) / 2, y = (h0 - cy) / 2;
                if (x > 10) {
                    x = 10;
                }

                // Render the message or splash screen
                paintMessage(g, w0, y);
                // Display game information
                Font font = Font.getDefaultFont();
                g.setFont(font);
                g.setColor(0x00ffffff);
                spaceBetweenLines = LodeRunnerStage.SPRITE_HEIGHT[LodeRunnerStage.SPRITE_NORMAL];
                if (spaceBetweenLines < font.getHeight()) {
                    spaceBetweenLines = font.getHeight();
                }
                int startY = y + cy + 3;
                if (getHeight() - startY - 4 * spaceBetweenLines < 0) {
                    g.setColor(0x00888888);
                    for (int i = getHeight() - 4 * spaceBetweenLines; i < startY; i += 2) {
                        g.drawLine(0, i, getWidth(), i);
                    }
                    startY = getHeight() - 4 * spaceBetweenLines;
                    g.setColor(0x00ffffff);
                }
                paintLeft(g, LodeRunnerStage.TILE_LADDER, "Level " + format3(level + 1), startY);
                paintRight(g, LodeRunnerStage.TILE_HERO, "x" + Integer.toString(lifes), startY);

                // Display stage information
                if (stage.isLoaded) {
                    paintLeft(g, LodeRunnerStage.TILE_CHEST, Integer.toString(stage.hero == null ? 0 : stage.hero.nChests) + "/" + Integer.toString(stage.nChests), startY + spaceBetweenLines);
                    paintRight(g, LodeRunnerStage.TILE_MONK, "x" + Integer.toString(stage.vilains.size()), startY + spaceBetweenLines);
                    if (levelStatuses[level] == STATUS_DONE) {
                        paintCenter(g, "Done!", startY + spaceBetweenLines, w0);
                    }
                } else {
                    g.setColor(0x00ffff00);
                    g.drawString("Loading...", x + cx / 2, y + (cy - font.getHeight()) / 2, Graphics.TOP | Graphics.HCENTER);
                    g.setColor(0x001463af);
                    g.drawString("� 2006 - Fabien GIGANTE", w0 / 2, h0 - 2, Graphics.HCENTER | Graphics.BOTTOM);
                }
                paintLeft(g, -1, "Fire=Play", startY + spaceBetweenLines * 2);
                paintRight(g, -1, "#=Exit", startY + spaceBetweenLines * 2);
                if (getWidth() > 160) {
                    paintSoftLeft(g, -1, "Next Level");
                    paintSoftRight(g, -1, "Suicide");
                } else {
                    paintSoftLeft(g, -1, "Next");
                    paintSoftRight(g, -1, "Suic.");
                }


            } else {
                if (clearAfterPause) {
                    clearAfterPause = false;
                    g.setColor(0x000000);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(0x00ffffff);
                    painTopMessage(g);
                    paintSoftMenu(g);
                }
                if (stage != null && stage.isMessageAtTop()
                        && getHeight() >= 208 && getHeight() < 320) {
                    g.setColor(0x00ffffff);
                    painTopMessage(g);
                    paintSoftMenu(g);

                }
            }
        }

    }

    private void paintSoftMenu(Graphics g) {
        paintSoftLeft(g, -1, "<= Fire ");
        paintSoftRight(g, -1, "Fire =>");
    }

    /** Handle game actions */
    protected void gameAction(int actionCode) {

        if (!stage.isLoaded || stage.hero == null) {
            return;
        }
        switch (actionCode) {
            case UP:
                stage.hero.requestMove(LodeRunnerCharacter.MOVE_CLIMB_UP);
                break;
            case DOWN:
                stage.hero.requestMove(LodeRunnerCharacter.MOVE_CLIMB_DOWN);
                break;
            case LEFT:
                stage.hero.requestMove(LodeRunnerCharacter.MOVE_RUN_LEFT);
                break;
            case RIGHT:
                stage.hero.requestMove(LodeRunnerCharacter.MOVE_RUN_RIGHT);
                break;
            case GAME_A:
            case GAME_C:
                stage.hero.requestMove(LodeRunnerHero.MOVE_DIG_LEFT);
                break;
            case GAME_B:
            case GAME_D:
                stage.hero.requestMove(LodeRunnerHero.MOVE_DIG_RIGHT);
                break;
            case FIRE:
                stage.hero.requestMove(stage.hero.lookLeft ? LodeRunnerHero.MOVE_DIG_LEFT : LodeRunnerHero.MOVE_DIG_RIGHT);
                break;
        }
    }

    /**
     * Get the game action associated with the given key code of the device.
     * Overloaded to cancel button actions when resuming a paused game.
     */
    public int getGameAction(int keyCode) {
        int actionCode = super.getGameAction(keyCode);
        if (isPaused) {
            switch (actionCode) {
                case GAME_A:
                case GAME_C:
                case GAME_B:
                case GAME_D:
                    actionCode = 0;
                    break;
            }
        }
        return actionCode;
    }

    /** Handle special keys (that are not game actions) */
    public void keyPressed(int keyCode) {
        if (!isPaused && keyCode == KEY_NUM0) {
            pause();
        } else if (isPaused && getGameAction(keyCode) == FIRE && keyCode != KEY_NUM5) {
            levelStatuses[level] = STATUS_NOT_DONE;
            resume();
        } else if (isPaused && keyCode == KEY_SOFT1) {
            nextLevelNotDone();
        } else if (isPaused && keyCode == KEY_SOFT2) {
            stageOver(false);
        } else if (isPaused && keyCode == KEY_STAR) {
            clearDoneLevels();
        } else if (isPaused && keyCode == KEY_POUND) {
            endGame();
        } else if (isPaused) {
            int number = -1;

            switch (keyCode) {

                case KEY_NUM0:
                    number = 0;
                    break;
                case KEY_NUM1:
                    number = 1;
                    break;
                case KEY_NUM2:
                    number = 2;
                    break;
                case KEY_NUM3:
                    number = 3;
                    break;
                case KEY_NUM4:
                    number = 4;
                    break;
                case KEY_NUM5:
                    number = 5;
                    break;
                case KEY_NUM6:
                    number = 6;
                    break;
                case KEY_NUM7:
                    number = 7;
                    break;
                case KEY_NUM8:
                    number = 8;
                    break;
                case KEY_NUM9:
                    number = 9;
                    break;
            }
            if (number != -1) {
                needsRepaint = REPAINT_ALL;
                newLevel = ((newLevel * 10) % 1000 + number);
                if (newLevel == 0) {
                    newLevel = 1;
                }
                if (newLevel <= LodeRunnerStage.MAX_LEVELS) {
                    loadNewLevel();
                } else {
                    pauseMessage = "1 <= level <= " + LodeRunnerStage.MAX_LEVELS;
                }

            }

        } else {
            super.keyPressed(keyCode);
        }
    }

    public synchronized void pause() {
        super.pause();
        try {
            saveToStore(GAME_NAME);
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (RecordStoreException ex) {
            ex.printStackTrace();
        }
        this.newLevel = level % LodeRunnerStage.MAX_LEVELS + 1;
    }

    /** Called when the game is stopped. Save its current state in store. */
    public synchronized void stop() {
        super.stop();
        try {
            saveToStore(GAME_NAME);
        } catch (Exception e) {
        }
    }

    private void endGame() {

        midlet.destroyApp(true);
        midlet.notifyDestroyed();
    }

    private void loadNewLevel() {
        pauseMessage = null;
        this.level = newLevel - 1;
        try {
            this.stage.loadFromResource();
        } catch (Exception e) {
        }

    }

    private void paintSoftLeft(Graphics g, int tileIndex, String string) {
        g.setColor(BG_COLOR_FOR_SOFTKEYS);
        g.fillRect(0, h0 - spaceBetweenLines, w0 / 2 - 1, spaceBetweenLines);
        g.setColor(0xffffff);
        paintLeft(g, tileIndex, string, h0 - spaceBetweenLines);
    }

    private void paintSoftRight(Graphics g, int tileIndex, String string) {
        g.setColor(BG_COLOR_FOR_SOFTKEYS);
        g.fillRect(w0 / 2 + 1, h0 - spaceBetweenLines, w0 / 2 + 1, spaceBetweenLines);
        g.setColor(0xffffff);
        paintRight(g, tileIndex, string, h0 - spaceBetweenLines);
    }

    private void paintLeft(Graphics g, int tileIndex, String string, int y) {
        if (tileIndex >= 0) {
            int ySprite = y + (Font.getDefaultFont().getHeight() - LodeRunnerStage.SPRITE_HEIGHT[LodeRunnerStage.SPRITE_NORMAL]) / 2;
            stage.sprites[LodeRunnerStage.SPRITE_NORMAL].paint(g, LodeRunnerStage.spriteMap[tileIndex], 4, ySprite);
            g.drawString(string, 5 + LodeRunnerStage.SPRITE_WIDTH[LodeRunnerStage.SPRITE_NORMAL], y, Graphics.TOP | Graphics.LEFT);
        } else {
            g.drawString(string, 5, y, Graphics.TOP | Graphics.LEFT);
        }
    }

    private void paintRight(Graphics g, int tileIndex, String string, int y) {
        if (w0 == 0) {
            w0 = getWidth();
        }

        paintRight(g, tileIndex, string, y, w0);
    }

    private void paintRight(Graphics g, int tileIndex, String string, int y, int screenWidth) {
        if (tileIndex >= 0) {
            int ySprite = y + (Font.getDefaultFont().getHeight() - LodeRunnerStage.SPRITE_HEIGHT[LodeRunnerStage.SPRITE_NORMAL]) / 2;
            g.drawString(string, screenWidth - 4, y, Graphics.TOP | Graphics.RIGHT);
            int textWidth = Font.getDefaultFont().stringWidth(string);
            stage.sprites[LodeRunnerStage.SPRITE_NORMAL].paint(g, LodeRunnerStage.spriteMap[tileIndex], screenWidth - 4 - LodeRunnerStage.SPRITE_WIDTH[LodeRunnerStage.SPRITE_NORMAL] - textWidth, ySprite);
        } else {
            g.drawString(string, screenWidth - 4, y, Graphics.TOP | Graphics.RIGHT);
        }
    }

    private void paintCenter(Graphics g, String string, int y, int screenWidth) {

        int stringWidth = Font.getDefaultFont().stringWidth(string);
        int height = Font.getDefaultFont().getHeight();
        g.setColor(0x008800);
        g.fillRoundRect((screenWidth - stringWidth) / 2 - 2, y - 2, stringWidth + 4, height + 4, 8, 800);
        g.setColor(0xffffff);
        g.drawString(string, screenWidth / 2, y, Graphics.TOP | Graphics.HCENTER);
    }

    private String format3(int number) {
        if (number < 10) {
            return "00" + number;
        }
        if (number < 100) {
            return "0" + number;
        }
        return Integer.toString(number);
    }

    private void clearDoneLevels() {
        Alert alertClear = new Alert("Clear solved levels", "Are you sure you want to clear all solved levels?", null, AlertType.CONFIRMATION);
        final Command commandYes = new Command("Yes", Command.OK, 1);
        final Command commandNo = new Command("No", Command.CANCEL, 2);
        alertClear.addCommand(commandNo);
        alertClear.addCommand(commandYes);
        final Displayable currenDisplayable = Display.getDisplay(midlet).getCurrent();
        alertClear.setCommandListener(new CommandListener() {

            public void commandAction(Command c, Displayable d) {
                if (c == commandYes) {
                    for (int i = 0; i < levelStatuses.length; i++) {
                        levelStatuses[i] = STATUS_NOT_DONE;
                    }
                    try {
                        saveToStore(GAME_NAME);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } catch (RecordStoreException ex) {
                        ex.printStackTrace();
                    }
                }
                Display.getDisplay(midlet).setCurrent(currenDisplayable);
            }
        });

        Display.getDisplay(midlet).setCurrent(alertClear);
    }

    /**
     * Define the hero's heartBeat as a game event task.
     */
    protected class HeroHeartbeatTask extends RepaintTask {

        /** Scheduled every frame */
        public final static int PERIOD = FRAMERATE_MILLISEC;

        /** Heartbeat */
        public void run() {
            if (stage != null && stage.isLoaded && stage.hero != null) {
                stage.hero.heartBeat();
                if (stage.endCompleted) {
                    stageOver(true);
                } else if (stage.endHeroDied) {
                    stageOver(false);
                }
            }
            super.run();
        }
    }

    /**
     * Define the vilains' heartBeat as a game event task.
     */
    protected class VilainsHeartbeatTask extends RepaintTask {

        /** Scheduled every 2 frames */
        public final static int PERIOD = 2 * FRAMERATE_MILLISEC;

        /** Heartbeat */
        public void run() {
            // Loop on every vilain
            if (stage != null && stage.isLoaded) {
                for (Enumeration e = stage.vilains.elements(); e.hasMoreElements();) {
                    ((LodeRunnerVilain) e.nextElement()).heartBeat();
                }
            }
            super.run();
        }
    }

    /**
     * Define the stage's heartBeat as a game event task.
     */
    protected class StageHeartbeatTask extends RepaintTask {

        /** Scheduled every 2 frames */
        public final static int PERIOD = 2 * FRAMERATE_MILLISEC;

        /** Heartbeat */
        public void run() {
            // Loop on every hole
            if (stage != null && stage.isLoaded) {
                for (Enumeration e = stage.holes.elements(); e.hasMoreElements();) {
                    ((LodeRunnerHole) e.nextElement()).heartBeat();
                }
            }
            super.run();
        }
    }

    /** Start the Lode Runner game, in pause */
    public synchronized void start() {
        super.start();
        pause();
    }

    /**
     * Resume or start the game.
     * Lode Runner heartBeats game tasks are scheduled.
     */
    public synchronized void resume() {
        super.resume();
        pauseMessage = null;
        // Schedule the hero's heartBeat
        timer.schedule(new HeroHeartbeatTask(), 0, HeroHeartbeatTask.PERIOD);
        // Schedule the vilains' heartBeat
        timer.schedule(new VilainsHeartbeatTask(), 0, VilainsHeartbeatTask.PERIOD);
        // Schedule the stage's heartBeat
        timer.schedule(new StageHeartbeatTask(), 0, StageHeartbeatTask.PERIOD);
    }

    public void nextLevelNotDone() {
        pauseMessage = null;
        int nextLevelNotDone = -1;
        for (int i = level + 1; i < LodeRunnerStage.MAX_LEVELS; i++) {
            if (levelStatuses[i] == STATUS_NOT_DONE) {
                nextLevelNotDone = i;
                break;
            }
        }
        if (nextLevelNotDone == -1) {
            for (int i = 0; i <= level; i++) {
                if (levelStatuses[i] == STATUS_NOT_DONE) {
                    nextLevelNotDone = i;
                    break;
                }
            }
        }
        if(nextLevelNotDone == -1){
            pauseMessage = "All levels done!";
        }else{
            pauseMessage = null;
            newLevel = nextLevelNotDone + 1;
            loadNewLevel();
        }
    }

    /**
     * Stage is complete, or hero has died
     */
    public void stageOver(boolean hasCompleted) {
        if (!isPaused) {
            levelStatuses[level] = hasCompleted ? STATUS_DONE : STATUS_NOT_DONE;
            pause();
        }
        // Adjust lifes and level


        if (hasCompleted) {
            level++;
            if (level == LodeRunnerStage.MAX_LEVELS) {
                level = 0;
            }
            pauseMessage = (level % LodeRunnerStage.GAME_LEVELS == 0) ? null : "Congratulations !";
        } else {
            lifes--;
            if (lifes < 0) {
                lifes = MAX_LIFES;
                level = 0;
                pauseMessage = "Game Over";
            } else {
                pauseMessage = "Try again...";
            }
        }
        // Load appropriate stage
        try {
            stage.loadFromResource();
        } catch (Exception e) {
        }
        needsRepaint = REPAINT_ALL;
    }
}
