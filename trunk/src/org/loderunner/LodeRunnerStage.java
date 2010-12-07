package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import java.util.*;
import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

/**
 * A Lode Runner stage or level is composed of
 * - a tiles array describing the stage landscape
 * - a Lode Runner hero and an array of Lode Runner vilains
 */
class LodeRunnerStage {

    /** Number of levels per game */
    public static final int GAME_LEVELS = 150;
    /** Maximum number of levels  (for both Lode Runner and Championship) */
    public static final int MAX_LEVELS = 2 * GAME_LEVELS;
    /** Stage width in tiles */
    public static final int STAGE_WIDTH = 28;
    /** Stage height in tiles */
    public static final int STAGE_HEIGHT = 16;
    /** Tile/sprite width in pixels */
    public static final int[] SPRITE_WIDTH = {12, 4};
    /** Tile/sprite height in pixels */
    public static final int[] SPRITE_HEIGHT = {11, 4};
    /** Core tile type constant for void/empty tile */
    public static final int TILE_VOID = 0;
    /** Core tile type constant for diggable brick tile */
    public static final int TILE_BRICK = 1;
    /** Core tile type constant for non diggable solid/concrete tile */
    public static final int TILE_CONCRETE = 2;
    /** Core tile type constant for permanent ladder tile */
    public static final int TILE_LADDER = 3;
    /** Core tile type constant for rope tile */
    public static final int TILE_ROPE = 4;
    /** Core tile type constant for trap brick tile */
    public static final int TILE_TRAP = 5;
    /** Core tile type constant for exit ladder tile*/
    public static final int TILE_EXIT = 6;
    /** Core tile type constant for chest tile */
    public static final int TILE_CHEST = 7;
    /** Core tile type constant for vilain initial position (only valid when loading from resource file) */
    public static final int TILE_MONK = 8;
    /** Core tile type constant for hero initial position (only valid when loading from resource file) */
    public static final int TILE_HERO = 9;
    /** Volatile tile type constant used for tiles out of stage boundaries */
    public static final int TILE_OUTSIDE = 10;
    /** Volatile tile type constant used for brick just being digged (still considered full) */
    public static final int TILE_HOLE_FULL = 11;
    /** Volatile tile type constant used for brick completely digged (considered empty, can trap vilains) */
    public static final int TILE_HOLE_EMPTY = 12;
    /** Tiles array describing the stage landscape. Values are tile type TILE_* constants. */
    private int[] tiles = new int[STAGE_WIDTH * STAGE_HEIGHT];
    /** Lode Runner sprites (for both tiles & characters) */
    public GameSprite[] sprites = new GameSprite[2];
    /** Lode Runner small sprite font */
    public GameFont font = null;
    /** Mapping table from tile type to sprite index */
    public static final int[] spriteMap = { /*Core*/14, 15, 12, 16, 17, 18, 19, 20, 21, 0, /*Volatile*/ 13, 75, 74};
    /** Current sprite size */
    public int spriteSize = SPRITE_NORMAL;
    /** Sprite size constant for normal stage rendering */
    public static final int SPRITE_NORMAL = 0;
    /** Sprite size constant for small stage overview rendering */
    public static final int SPRITE_SMALL = 1;
    /** Lode Runner game hero */
    public LodeRunnerHero hero = null;
    /** Lode Runner game vilains. Vector of LodeRunnerVilain elements. */
    public Vector vilains = null;
    /** Lode Runner holes in this stage. */
    public Vector holes = null;
    /** Current random number generator for the stage */
    public GameRandom random = new GameRandom();
    /** Total number of chests to be collected in this stage */
    public int nChests = 0;
    /** Flag set if the exit of this stage is enabled */
    public boolean exitEnabled = false;
    /** Flag set if the hero has died and this stage is over */
    public boolean endHeroDied = false;
    /** Flag set if the hero has succesfully completed this stage */
    public boolean endCompleted = false;
    /** Stage background pixel image */
    private Image backgroundImage = null;
    private Vector backgroundTilesToRepaint = null;
    /** Game canvas using this stage*/
    private LodeRunnerCanvas canvas = null;
    /** Stage loading state */
    public boolean isLoaded = false;
    /** Stage loading is done in a separated thread */
    private Thread loadingThread = null;

    /** Initiatialize an empty stage. Load the sprites resources. */
    LodeRunnerStage(LodeRunnerCanvas canvas) {
        this.canvas = canvas;
        try {
            // Load game resource images (font and sprites)
            font = new GameFont("/res/Font.png", 3, 5, "0123456789/");
            for (int i = 0; i < 2; i++) {
                sprites[i] = new GameSprite("/res/Tiles " + SPRITE_WIDTH[i] + "x" + SPRITE_HEIGHT[i] + ".png", SPRITE_WIDTH[i], SPRITE_HEIGHT[i], 0, 0);
            }
            // If enough memory, use a background image to speed up normal stage rendering
            backgroundImage = Image.createImage(STAGE_WIDTH * SPRITE_WIDTH[SPRITE_NORMAL], STAGE_HEIGHT * SPRITE_HEIGHT[SPRITE_NORMAL]);
            backgroundTilesToRepaint = new Vector();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Loading thread for asynchroneous stage building */
    private class LoadingThread extends Thread {

        /** Entry point of this asynchroneous loading thread */
        public void run() {
            try {
                // In the original Apple II version, the levels can be found at offset 0x3000-0xc600
                // (aligned on 0x100 bytes blocks), in "loderunner.dsk" and "ch_lode.dsk"
                // "LodeRunner.bin" contains the usefull extracts of those two files
                // (without the alignment to save more space in the jar archive)
                // Each tile is encoded on 4 bits
                DataInput input = new DataInputStream(MIDlet.class.getResourceAsStream("/res/LodeRunner.bin"));
                // Read level's buffer
                int level = canvas.level % MAX_LEVELS;
                byte[] buffer = new byte[STAGE_WIDTH * STAGE_HEIGHT / 2];
                input.skipBytes(buffer.length * level);
                input.readFully(buffer);
                // Decode tiles from buffer content
                for (int i = 0; i < STAGE_WIDTH * STAGE_HEIGHT; i++) {
                    // Exit if thread is canceled
                    if (isLoaded) {
                        break;
                    }
                    // Decode next tile
                    int tile = (i % 2 == 0) ? buffer[i / 2] & 0xf : (buffer[i / 2] >> 4) & 0xf;
                    switch (tile) {
                        // Create hero at his starting point
                        case TILE_HERO:
                            hero = new LodeRunnerHero(LodeRunnerStage.this);
                            hero.moveToTile(i);
                            tile = TILE_VOID;
                            break;
                        // Add vilains at their initial positions
                        case TILE_MONK:
                            LodeRunnerVilain vilain = new LodeRunnerVilain(LodeRunnerStage.this);
                            vilain.moveToTile(i);
                            vilains.addElement(vilain);
                            tile = TILE_VOID;
                            break;
                        // Count number of chests
                        case TILE_CHEST:
                            nChests++;
                            break;
                    }
                    tiles[i] = tile;
                    // Track tiles to repaint
                    if (backgroundTilesToRepaint != null) {
                        backgroundTilesToRepaint.addElement(new Integer(i));
                    }
                    // Periodically yield to other threads
                    if (i % STAGE_WIDTH == 0) {
                        yield();
                    }
                }
                // The show can go on...
                exitEnabled = (nChests == 0);
                isLoaded = true;
            } catch (Exception e) {
                e.printStackTrace();
            } //  IOException, InterruptedException
            loadingThread = null;
            canvas.needsRepaint = GameCanvas.REPAINT_ALL;
        }
    }

    /** Load a stage from a given level in the levels resource file */
    public void loadFromResource() {
        // Abort previous loading attempt
        while (loadingThread != null) {
            isLoaded = true;
        }
        // Reset members
        isLoaded = false;
        hero = null;
        vilains = new Vector();
        holes = new Vector();
        nChests = 0;
        exitEnabled = false;
        endHeroDied = false;
        endCompleted = false;
        if (backgroundTilesToRepaint != null) {
            backgroundTilesToRepaint.removeAllElements();
        }
        // Asynchroneously load the stage
        loadingThread = new LoadingThread();
        loadingThread.start();
    }

    /** Get tile array index from x and y position of the tile. */
    public static int getTileIndex(int xTile, int yTile) {
        // Tiles are stored vertically (as in original game resource file format)
        return yTile * STAGE_WIDTH + xTile;
    }

    /** Get the tile type at a given postion (raw value from the array)  */
    public int getTile(int xTile, int yTile) {
        if (xTile < 0 || xTile >= STAGE_WIDTH || yTile < 0 || yTile >= STAGE_HEIGHT) {
            return TILE_OUTSIDE;
        }
        return tiles[getTileIndex(xTile, yTile)];
    }

    /**
     * Get the tile behavior at a given position.
     * A tile behavior is computed based on the tile type as follows:
     * - TILE_CHEST and TILE_HOLE_EMPTY have a TILE_VOID behavior
     * - TILE_OUTSIDE and TILE_HOLE_FULL have a TILE_CONCRETE behavior
     * - TILE_EXIT has either a TILE_LADDER (stage completed) or TILE_VOID (otherwise) behavior
     */
    public int getTileBehavior(int xTile, int yTile) {
        int tile = getTile(xTile, yTile);
        if (tile == TILE_CHEST || tile == TILE_MONK || tile == TILE_HERO || tile == TILE_HOLE_EMPTY) {
            tile = TILE_VOID;
        } else if (tile == TILE_OUTSIDE || tile == TILE_HOLE_FULL) {
            tile = TILE_CONCRETE;
        } else if (tile == TILE_EXIT) {
            tile = exitEnabled ? TILE_LADDER : TILE_VOID;
        }
        return tile;
    }

    /**
     * Get the tile appearance at a given position.
     * A tile appearance is computed based on the tile type as follows:
     * - TILE_TRAP has a TILE_BRICK appearance
     * - TILE_OUTSIDE, TILE_HOLE_EMPTY and TILE_HOLE_FULL have a TILE_VOID appearance
     * - TILE_EXIT has either a TILE_LADDER (stage completed) or TILE_VOID (otherwise) appearance
     */
    public int getTileAppearance(int xTile, int yTile) {
        int tile = getTile(xTile, yTile);
        if (tile == TILE_TRAP) {
            tile = TILE_BRICK;
        } else if (tile == TILE_MONK || tile == TILE_HERO || tile == TILE_OUTSIDE || tile == TILE_HOLE_FULL || tile == TILE_HOLE_EMPTY) {
            tile = TILE_VOID;
        } else if (tile == TILE_EXIT) {
            tile = exitEnabled ? TILE_LADDER : TILE_VOID;
        }
        return tile;
    }

    /** Set the tile type at a given postion */
    public void setTile(int xTile, int yTile, int type) {
        if (xTile < 0 || xTile >= STAGE_WIDTH || yTile < 0 || yTile >= STAGE_HEIGHT) {
            return;
        }
        tiles[getTileIndex(xTile, yTile)] = type;
        // Background image (if any) is no more up to date
        if (backgroundTilesToRepaint != null) {
            backgroundTilesToRepaint.addElement(new Integer(getTileIndex(xTile, yTile)));
        }
        // Keep track of digged holes (for delayed refill)
        if (type == TILE_HOLE_EMPTY) {
            holes.addElement(new LodeRunnerHole(this, xTile, yTile));
        }
    }

    /** Check if the given tile is occupied by a vilain */
    private boolean isVilainAt(int xTile, int yTile, boolean includeRespawning) {
        for (Enumeration e = vilains.elements(); e.hasMoreElements();) {
            LodeRunnerVilain vilain = ((LodeRunnerVilain) e.nextElement());
            if (vilain.xTile == xTile && vilain.yTile == yTile && (includeRespawning || vilain.currentMove != LodeRunnerVilain.MOVE_RESPAWN)) {
                return true;
            }
        }
        return false;
    }

    /** Check if the given tile is occupied by a living vilain */
    public boolean isVilainAt(int xTile, int yTile) {
        return isVilainAt(xTile, yTile, false);
    }

    /** Randomly computes a tile index suitable for respawning a vilain */
    public int computeRandomRespawnPoint() {
        // Compute possible respawn points (not on top row)
        Vector possiblePoints = new Vector();
        for (int y = 1; y < STAGE_HEIGHT; y++) {
            for (int x = 0; x < STAGE_WIDTH; x++) {
                if (getTile(x, y) == TILE_VOID && !isVilainAt(x, y, true)) {
                    possiblePoints.addElement(new Integer(getTileIndex(x, y)));
                }
            }
            if (!possiblePoints.isEmpty()) {
                break;
            }
        }
        // Return a random possible position
        return ((Integer) possiblePoints.elementAt(random.nextInt(possiblePoints.size()))).intValue();
    }

    /** Enables the exit for this stage */
    public void enableExit() {
        if (!exitEnabled) {
            exitEnabled = true;
            if (backgroundTilesToRepaint != null) {
                for (int i = 0; i < STAGE_WIDTH * STAGE_HEIGHT; i++) {
                    if (tiles[i] == TILE_EXIT) {
                        backgroundTilesToRepaint.addElement(new Integer(i));
                    }
                }
            }
        }
    }

    /** Renders the stage's tiles */
    public void paintTiles(Graphics g) {
        boolean screenCleared = (spriteSize == SPRITE_SMALL || backgroundImage == null);
        for (int x = 0; x < STAGE_WIDTH; x++) {
            for (int y = 0; y < STAGE_HEIGHT; y++) {
                // Tiles are drawn according to their appearance
                int tile = getTileAppearance(x, y);
                // If screen has been cleared, empty tiles can be skipped
                if (screenCleared && tile != TILE_VOID) {
                    sprites[spriteSize].paint(g, spriteMap[tile], x * SPRITE_WIDTH[spriteSize], y * SPRITE_HEIGHT[spriteSize]);
                }
            }
        }
    }

    /** Renders only the tiles that need repainting */
    public void repaintBackgroundTiles() {
        if (spriteSize != SPRITE_NORMAL || backgroundImage == null) {
            return;
        }
        Graphics g = backgroundImage.getGraphics();
        // Loop on every tile that needs repainting
        for (Enumeration e = backgroundTilesToRepaint.elements(); e.hasMoreElements();) {
            int tileIndex = ((Integer) e.nextElement()).intValue();
            int xTile = tileIndex % LodeRunnerStage.STAGE_WIDTH;
            int yTile = tileIndex / LodeRunnerStage.STAGE_WIDTH;
            // Tiles are drawn according to their appearance
            int tileAppearance = getTileAppearance(xTile, yTile);
            sprites[spriteSize].paint(g, spriteMap[tileAppearance], xTile * SPRITE_WIDTH[spriteSize], yTile * SPRITE_HEIGHT[spriteSize]);
        }
        backgroundTilesToRepaint.removeAllElements();
    }

    /** Render the stage's sprites (hero and vilains) */
    public void paintSprites(Graphics g) {
        for (Enumeration e = holes.elements(); e.hasMoreElements();) {
            ((LodeRunnerHole) e.nextElement()).paint(g);
        }
        for (Enumeration e = vilains.elements(); e.hasMoreElements();) {
            ((LodeRunnerVilain) e.nextElement()).paint(g);
        }
        if (hero != null) {
            hero.paint(g);
        }
    }

    /** Translate to center the screen */
    public void centerScreen(Graphics g) {
        int w0 = g.getClipWidth(), h0 = g.getClipHeight();
        int tx = 0, ty = 0;
        if (isLoaded && hero != null) {
            // Compute screen translation, based on hero's position
            if (w0 >= STAGE_WIDTH * SPRITE_WIDTH[spriteSize]) {
                tx = (w0 - STAGE_WIDTH * SPRITE_WIDTH[spriteSize]) / 2;
            } else if (hero.getCenterX() < w0 / 2) {
                tx = 0;
            } else if (hero.getCenterX() > STAGE_WIDTH * SPRITE_WIDTH[spriteSize] - w0 / 2) {
                tx = w0 - STAGE_WIDTH * SPRITE_WIDTH[spriteSize];
            } else {
                tx = w0 / 2 - hero.getCenterX();
            }
            if (h0 >= STAGE_HEIGHT * SPRITE_HEIGHT[spriteSize]) {
                ty = (h0 - STAGE_HEIGHT * SPRITE_HEIGHT[spriteSize]) / 2;
            } else if (hero.getCenterY() < h0 / 2) {
                ty = 0;
            } else if (hero.getCenterY() > STAGE_HEIGHT * SPRITE_HEIGHT[spriteSize] - h0 / 2) {
                ty = h0 - STAGE_HEIGHT * SPRITE_HEIGHT[spriteSize];
            } else {
                ty = h0 / 2 - hero.getCenterY();
            }
        } else {
            // Center stage
            tx = (w0 - STAGE_WIDTH * SPRITE_WIDTH[spriteSize]) / 2;
            ty = (h0 - STAGE_HEIGHT * SPRITE_HEIGHT[spriteSize]) / 2;
        }
        g.translate(tx, ty);
    }

    boolean isMessageAtTop() {
        return hero != null && hero.getY() <= 16 && hero.wasShowingMessage();
    }

    /** Render the stage */
    public void paint(Graphics g) {
        // Prepare the screen (clear, center)
        g.setColor(0);
        if (spriteSize == SPRITE_NORMAL && (!isLoaded || backgroundImage == null)) {
            g.fillRect(0, 0, g.getClipWidth(), g.getClipHeight());
        }
        centerScreen(g);
        // deleting the message when hero is at the top
        if (isMessageAtTop()) {
            int blockSize = SPRITE_WIDTH[SPRITE_NORMAL] ;
            g.fillRect(hero.getCenterX() - blockSize, hero.getY() - blockSize, 2 * blockSize, blockSize);
        }

        if (spriteSize == SPRITE_SMALL) {
            g.fillRect(0, 0, STAGE_WIDTH * SPRITE_WIDTH[spriteSize], STAGE_HEIGHT * SPRITE_HEIGHT[spriteSize]);
        }
        if (isLoaded) {
            // Paint tiles, using background Image if available
            if (spriteSize == SPRITE_NORMAL && backgroundImage != null) {
                repaintBackgroundTiles();
                g.drawImage(backgroundImage, 0, 0, Graphics.TOP | Graphics.LEFT);
            } else {
                paintTiles(g);
            }
            // Paint sprites
            paintSprites(g);
        }
        // When drawing in small size, frame the stage by a white rectangle
        if (spriteSize == SPRITE_SMALL) {
            g.setColor(0x00ffffff);
            g.drawRect(0, 0, STAGE_WIDTH * SPRITE_WIDTH[spriteSize], STAGE_HEIGHT * SPRITE_HEIGHT[spriteSize]);
        }
        // Revert translation
        g.translate(-g.getTranslateX(), -g.getTranslateY());
    }
}
