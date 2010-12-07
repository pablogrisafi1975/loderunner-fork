package org.loderunner;

/* Copyright � 2006 - 030 */

import java.io.IOException;
import javax.microedition.lcdui.*;

/**
 * Visual element that can be rendered with one of the frames stored in an Image.
 * 
 * This is NOT the MIDP 2.0 javax.microedition.lcdui.game.Sprite
 */
class GameSprite {

    /** Image containing all frames for this sprite */
    private Image frames;
    /** Dimensions of a single frame in pixels */
    public int frameHeight, frameWidth;
    /** Total number of frames for this sprite */
    public int framesCount;
    /** Number of frames for each dimension of the image */
    public int framesCountX, framesCountY;
    /** Origin within each frame in pixels */
    public int refX, refY;

    /** Constructor from a given Image */
    public GameSprite(Image image, int frameWidth, int frameHeight, int refX, int refY) {
        this.refX = refX;
        this.refY = refY;
        this.frameHeight = frameHeight;
        this.frameWidth = frameWidth;
        this.framesCountX = image.getWidth() / frameWidth;
        this.framesCountY = image.getHeight() / frameHeight;
        this.framesCount = framesCountX * framesCountY;
        frames = image;
    }

    /** Constructor from image resource name */
    public GameSprite(String name, int frameWidth, int frameHeight, int refX, int refY) throws IOException {
        this(Image.createImage(name), frameWidth, frameHeight, refX, refY);
    }

    /** Paint a given frame of this sprite at a given position in pixels */
    public final boolean paint(Graphics g, int frameNumber, int x, int y) {
        x -= refX;
        y -= refY;
        int cx = g.getClipX(), cy = g.getClipY();
        int cw = g.getClipWidth(), ch = g.getClipHeight();
        // Only paint if at least partly visible
        if (x + frameWidth < cx || y + frameHeight < cy || x > cx + cw || y > cy + ch) {
            return false;
        }
        // MIDP 2.0 implementation
        g.drawRegion(frames, (frameNumber % framesCountX) * frameWidth, (frameNumber / framesCountX) * frameHeight,
                frameWidth, frameHeight, 0, x, y, Graphics.TOP | Graphics.LEFT);
        // MIDP 1.0 implementation, for portability
    /*
        // Clip to keep only the requested frame
        g.clipRect(x, y, frameWidth, frameHeight);
        // Draw the frame
        x -= (frameNumber % framesCountX) * frameWidth;
        y -= (frameNumber / framesCountX) * frameHeight;
        g.drawImage(frames, x, y, Graphics.TOP | Graphics.LEFT);
        // Restore clip
        g.setClip(cx, cy, cw, ch);
         */
        return true;
    }

    /** Paint a given frame of this sprite at a given position in pixels, with anchor */
    public final boolean paint(Graphics g, int frameNumber, int x, int y, int anchor) {
        // Adjust position according to anchor
        if ((anchor & Graphics.BOTTOM) != 0) {
            y -= frameHeight;
        } else if ((anchor & Graphics.VCENTER) != 0) {
            y -= frameHeight / 2;
        }
        if ((anchor & Graphics.RIGHT) != 0) {
            x -= frameWidth;
        } else if ((anchor & Graphics.HCENTER) != 0) {
            x -= frameWidth / 2;
        }
        return paint(g, frameNumber, x, y);
    }
}
