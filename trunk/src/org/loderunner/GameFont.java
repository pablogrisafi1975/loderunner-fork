package org.loderunner;

/* Copyright � 2006 - Fabien GIGANTE */

import java.io.IOException;
import javax.microedition.lcdui.*;

/**
 * Game font using sprites as characters
 */
class GameFont extends GameSprite {

    /** Characters represented by each frame of the sprite (in order) */
    private String characterSet;

    /** Constructor from a resource name and a character set */
    public GameFont(String name, int frameWidth, int frameHeight, String characterSet) throws IOException {
        super(name, frameWidth, frameHeight, 0, 0);
        this.characterSet = characterSet;
        if (characterSet.length() != framesCount) {
            throw new IllegalArgumentException();
        }
    }

    /** Draw a character string, using this font */
    public void drawString(Graphics g, String str, int x, int y, int anchor) {
        int strLength = str.length();
        if (strLength == 0) {
            return;
        }
        // Adjust position according to anchor
        if ((anchor & Graphics.BOTTOM) != 0) {
            y -= frameHeight;
        } else if ((anchor & Graphics.VCENTER) != 0) {
            y -= frameHeight / 2;
        }
        if ((anchor & Graphics.RIGHT) != 0) {
            x -= (frameWidth + 1) * strLength - 1;
        } else if ((anchor & Graphics.HCENTER) != 0) {
            x -= ((frameWidth + 1) * strLength - 1) / 2;
        }
        // Draw each character
        for (int i = 0; i < strLength; i++) {
            int frameNumber = characterSet.indexOf(str.charAt(i));
            if (frameNumber >= 0) {
                paint(g, frameNumber, x, y);
            }
            x += frameWidth + 1;
        }
    }
}
