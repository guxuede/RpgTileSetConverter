package org.greg.image;

import javafx.geometry.Rectangle2D;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Java implementation of the JavaScript image converter.
 * Converts RPG Maker tile format to Godot format using image manipulation.
 *
 * Core algorithm:
 * 1. RPG Maker format (192x64) -> Mini tiles (80x16) via quad sampling
 * 2. Mini tiles -> Godot format (192x144) via substitution mapping
 * 3. Godot format -> RPG Maker output (32x48)
 */
public class ImageConverter {

    /**
     * Helper class to store mini tile mapping entry
     */
    public static class MiniTileEntry {
        public int[] targetTile;  // [x, y]
        public int[][] tileData;  // 3x3 grid of [x, y] references

        public MiniTileEntry(int[] target, int[][] data) {
            this.targetTile = target;
            this.tileData = data;
        }
    }

    // Tile dimensions
    public static final int TILE_WIDTH = 48;
    public static final int TILE_HEIGHT = 48;

    // Offset parameters for edge handling
    private int leftOffset = 0;
    private int rightOffset = 0;
    private int bottomOffset = 0;
    private int topOffset = 0;

    // Internal mini tile data configuration: array of [targetTile, tilesData]
    private static final MiniTileEntry[] MINITILES_DATA = createMinitilesData();

    public ImageConverter() {
    }

    public ImageConverter(int leftOffset, int rightOffset, int topOffset, int bottomOffset) {
        this.leftOffset = leftOffset;
        this.rightOffset = rightOffset;
        this.topOffset = topOffset;
        this.bottomOffset = bottomOffset;
    }

    /**
     * Copy a single tile from source to target position
     */
    private void copyTile(BufferedImage srcImg, Rectangle2D rectangle2D,
                          BufferedImage dstImg, int[] toPos, int[] fromPos) {
        Graphics2D g2d = dstImg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2d.clearRect(toPos[0] * TILE_WIDTH, toPos[1] * TILE_HEIGHT,  TILE_WIDTH, TILE_HEIGHT);
        g2d.drawImage(
                srcImg,
                toPos[0] * TILE_WIDTH,
                toPos[1] * TILE_HEIGHT,
                toPos[0] * TILE_WIDTH + TILE_WIDTH,
                toPos[1] * TILE_HEIGHT + TILE_HEIGHT,
                fromPos[0] * TILE_WIDTH + (int)rectangle2D.getMinX(),
                fromPos[1] * TILE_HEIGHT + (int)rectangle2D.getMinY(),
                fromPos[0] * TILE_WIDTH + TILE_WIDTH + (int)rectangle2D.getMinX(),
                fromPos[1] * TILE_HEIGHT + TILE_HEIGHT  + (int)rectangle2D.getMinY(),
                null
        );
        g2d.dispose();
    }

    /**
     * Copy a section (subsection) of a tile
     * section[0] = x (0=left, 1=middle, 2=right)
     * section[1] = y (0=top, 1=middle, 2=bottom)
     */
    private void copyTileSectionRaw(BufferedImage srcImg, BufferedImage dstImg,
                                   int[] target, int[] section, int[] data) {
        int x = target[0];
        int y = target[1];
        int offx1 = TILE_WIDTH / 2 + leftOffset;
        int offx2 = offx1 - leftOffset + rightOffset;
        int offy1 = TILE_HEIGHT / 2 + topOffset;
        int offy2 = offy1 - topOffset + bottomOffset;
        int midwx = rightOffset - leftOffset;
        int midwy = bottomOffset - topOffset;
        int endwx = TILE_WIDTH - offx2;
        int endwy = TILE_HEIGHT - offy2;

        int[] offsetX = {0, offx1, offx2};
        int[] offsetY = {0, offy1, offy2};
        int[] sizeX = {offx1, midwx, endwx};
        int[] sizeY = {offy1, midwy, endwy};

        int offset_x = offsetX[section[0]];
        int offset_y = offsetY[section[1]];
        int size_x = sizeX[section[0]];
        int size_y = sizeY[section[1]];

        Graphics2D g2d = dstImg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2d.clearRect(x * TILE_WIDTH + offset_x, y * TILE_HEIGHT + offset_y,size_x, size_y );
        g2d.drawImage(
                srcImg,
                x * TILE_WIDTH + offset_x,
                y * TILE_HEIGHT + offset_y,
                x * TILE_WIDTH + offset_x + size_x,
                y * TILE_HEIGHT + offset_y + size_y,
                data[0] * TILE_WIDTH + offset_x,
                data[1] * TILE_HEIGHT + offset_y,
                data[0] * TILE_WIDTH + offset_x + size_x,
                data[1] * TILE_HEIGHT + offset_y + size_y,
                null
        );
        g2d.dispose();
    }

    /**
     * Copy a quad (quarter tile) section
     * Used for sampling 4 quadrants of a tile
     */
    private void copyTileQuadRaw(BufferedImage srcImg, Rectangle2D rectangle2D, BufferedImage dstImg,
                                int[] target, int[] section, int[] data) {
        int x = target[0];
        int y = target[1];

        int offx1 = TILE_WIDTH / 2 + leftOffset;
        int offx2 = offx1 - leftOffset + rightOffset;
        int offy1 = TILE_HEIGHT / 2 + topOffset;
        int offy2 = offy1 - topOffset + bottomOffset;
        int offx = (offx1 / 2 + offx2 / 2);
        int offy = (offy1 / 2 + offy2 / 2);

        int[] offsetX = {0, offx};
        int[] offsetY = {0, offy};
        int[] sizeX = {offx, TILE_WIDTH - offx};
        int[] sizeY = {offy, TILE_HEIGHT - offy};

        int offset_x = offsetX[section[0]];
        int offset_y = offsetY[section[1]];
        int size_x = sizeX[section[0]];
        int size_y = sizeY[section[1]];

        Graphics2D g2d = dstImg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//        g2d.clearRect(x * TILE_WIDTH + offset_x, y * TILE_HEIGHT + offset_y, size_x, size_y);
        g2d.drawImage(
                srcImg,
                x * TILE_WIDTH + offset_x,
                y * TILE_HEIGHT + offset_y,
                x * TILE_WIDTH + offset_x + size_x,
                y * TILE_HEIGHT + offset_y + size_y,
                data[0] * TILE_WIDTH + offset_x + (int)rectangle2D.getMinX(),
                data[1] * TILE_HEIGHT + offset_y + (int)rectangle2D.getMinY(),
                data[0] * TILE_WIDTH + offset_x + size_x + (int)rectangle2D.getMinX(),
                data[1] * TILE_HEIGHT + offset_y + size_y  + (int)rectangle2D.getMinY(),
                null
        );
        g2d.dispose();
    }

    /**
     * Copy a tile section using the section data lookup
     */
    private void copyTileSection(BufferedImage srcImg, BufferedImage dstImg,
                                int[] target, int[] section, int[][] data) {
        int[] sectionData = data[section[1] * 3 + section[0]];
        if (sectionData[0] < 0 || sectionData[1] < 0) {
            return;
        }
        copyTileSectionRaw(srcImg, dstImg, target, section, sectionData);
    }

    /**
     * Apply mini tile substitution mapping
     */
    private void applySubtileData(BufferedImage srcImg, BufferedImage dstImg) {
        for (MiniTileEntry entry : MINITILES_DATA) {
            for (int y = 0; y < 3; y++) {
                for (int x = 0; x < 3; x++) {
                    int[] section = {x, y};
                    copyTileSection(srcImg, dstImg, entry.targetTile, section, entry.tileData);
                }
            }
        }
    }

    /**
     * Main conversion: RPG Maker format -> Godot format
     * This is the Java equivalent of the JavaScript updatePreviews() method
     */
    public BufferedImage convertToGodotImage(BufferedImage inputImg, Rectangle2D rectangle2D) {
        if (inputImg == null) {
            return null;
        }

        // Step 1: Convert RPG Maker to mini tiles (80x16)
        BufferedImage tempCanvas = new BufferedImage(5 * TILE_WIDTH, TILE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        // Quad copy operations (from JavaScript lines 189-209)
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{0, 0}, new int[]{0, 0}, new int[]{0, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{0, 0}, new int[]{1, 0}, new int[]{1, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{0, 0}, new int[]{0, 1}, new int[]{0, 2});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{0, 0}, new int[]{1, 1}, new int[]{1, 2});

        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{1, 0}, new int[]{0, 1}, new int[]{0, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{1, 0}, new int[]{1, 1}, new int[]{1, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{1, 0}, new int[]{0, 0}, new int[]{0, 2});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{1, 0}, new int[]{1, 0}, new int[]{1, 2});

        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{2, 0}, new int[]{1, 0}, new int[]{0, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{2, 0}, new int[]{0, 0}, new int[]{1, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{2, 0}, new int[]{1, 1}, new int[]{0, 2});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{2, 0}, new int[]{0, 1}, new int[]{1, 2});

        copyTile(inputImg, rectangle2D, tempCanvas, new int[]{3, 0}, new int[]{1, 0});

        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{4, 0}, new int[]{1, 1}, new int[]{0, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{4, 0}, new int[]{0, 1}, new int[]{1, 1});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{4, 0}, new int[]{1, 0}, new int[]{0, 2});
        copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new int[]{4, 0}, new int[]{0, 0}, new int[]{1, 2});

        // Step 2: Apply mini tile substitution (convert to Godot format - 192x144)
        BufferedImage outputCanvas = new BufferedImage(12 * TILE_WIDTH, 4 * TILE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        applySubtileData(tempCanvas, outputCanvas);

        // Step 3: Convert Godot back to RPG Maker format (32x48)
//        BufferedImage rpgMakerCanvas = new BufferedImage(2 * TILE_WIDTH, 3 * TILE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
//        copyTile(outputCanvas, rpgMakerCanvas, new int[]{0, 0}, new int[]{0, 3});
//        copyTile(outputCanvas, rpgMakerCanvas, new int[]{1, 0}, new int[]{2, 1});
//        copyTile(outputCanvas, rpgMakerCanvas, new int[]{0, 1}, new int[]{8, 0});
//        copyTile(outputCanvas, rpgMakerCanvas, new int[]{1, 1}, new int[]{11, 0});
//        copyTile(outputCanvas, rpgMakerCanvas, new int[]{0, 2}, new int[]{8, 3});
//        copyTile(outputCanvas, rpgMakerCanvas, new int[]{1, 2}, new int[]{11, 3});

        return outputCanvas;
    }

    // Data initialization method
    private static MiniTileEntry[] createMinitilesData() {
        MiniTileEntry[] data = new MiniTileEntry[48];
        int idx = 0;
        
        // Row 0 (y=0)
        data[idx++] = new MiniTileEntry(new int[]{0,0}, new int[][]{{0,0},{0,0},{0,0},{1,0},{1,0},{1,0},{1,0},{1,0},{1,0}});
        data[idx++] = new MiniTileEntry(new int[]{1,0}, new int[][]{{0,0},{0,0},{2,0},{0,0},{0,0},{2,0},{1,0},{1,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{2,0}, new int[][]{{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{3,0},{3,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{3,0}, new int[][]{{2,0},{0,0},{0,0},{2,0},{0,0},{0,0},{3,0},{1,0},{1,0}});
        data[idx++] = new MiniTileEntry(new int[]{4,0}, new int[][]{{4,0},{4,0},{3,0},{4,0},{4,0},{3,0},{3,0},{3,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{5,0}, new int[][]{{2,0},{2,0},{2,0},{2,0},{4,0},{4,0},{3,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{6,0}, new int[][]{{2,0},{2,0},{2,0},{4,0},{4,0},{2,0},{4,0},{4,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{7,0}, new int[][]{{3,0},{4,0},{4,0},{3,0},{4,0},{4,0},{3,0},{3,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{8,0}, new int[][]{{0,0},{0,0},{2,0},{0,0},{4,0},{4,0},{1,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{9,0}, new int[][]{{3,0},{3,0},{3,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{10,0}, new int[][]{{2,0},{2,0},{2,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{11,0}, new int[][]{{2,0},{0,0},{0,0},{4,0},{4,0},{0,0},{4,0},{4,0},{1,0}});

        // Row 1 (y=1)
        data[idx++] = new MiniTileEntry(new int[]{0,1}, new int[][]{{1,0},{1,0},{1,0},{1,0},{1,0},{1,0},{1,0},{1,0},{1,0}});
        data[idx++] = new MiniTileEntry(new int[]{1,1}, new int[][]{{1,0},{1,0},{3,0},{1,0},{1,0},{3,0},{1,0},{1,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{2,1}, new int[][]{{3,0},{3,0},{3,0},{3,0},{3,0},{3,0},{3,0},{3,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{3,1}, new int[][]{{3,0},{1,0},{1,0},{3,0},{1,0},{1,0},{3,0},{1,0},{1,0}});
        data[idx++] = new MiniTileEntry(new int[]{4,1}, new int[][]{{1,0},{1,0},{3,0},{1,0},{4,0},{4,0},{1,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{5,1}, new int[][]{{3,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{6,1}, new int[][]{{4,0},{4,0},{3,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{7,1}, new int[][]{{3,0},{1,0},{1,0},{4,0},{4,0},{1,0},{4,0},{4,0},{1,0}});
        data[idx++] = new MiniTileEntry(new int[]{8,1}, new int[][]{{1,0},{4,0},{4,0},{1,0},{4,0},{4,0},{1,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{9,1}, new int[][]{{3,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{10,1}, new int[][]{{-1,-1},{-1,-1},{-1,-1},{-1,-1},{-1,-1},{-1,-1},{-1,-1},{-1,-1},{-1,-1}});
        data[idx++] = new MiniTileEntry(new int[]{11,1}, new int[][]{{4,0},{4,0},{3,0},{4,0},{4,0},{3,0},{4,0},{4,0},{3,0}});

        // Row 2 (y=2)
        data[idx++] = new MiniTileEntry(new int[]{0,2}, new int[][]{{1,0},{1,0},{1,0},{1,0},{1,0},{1,0},{0,0},{0,0},{0,0}});
        data[idx++] = new MiniTileEntry(new int[]{1,2}, new int[][]{{1,0},{1,0},{3,0},{0,0},{0,0},{2,0},{0,0},{0,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{2,2}, new int[][]{{3,0},{3,0},{3,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{3,2}, new int[][]{{3,0},{1,0},{1,0},{2,0},{0,0},{0,0},{2,0},{0,0},{0,0}});
        data[idx++] = new MiniTileEntry(new int[]{4,2}, new int[][]{{1,0},{4,0},{4,0},{1,0},{4,0},{4,0},{1,0},{1,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{5,2}, new int[][]{{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{3,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{6,2}, new int[][]{{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{7,2}, new int[][]{{4,0},{4,0},{1,0},{4,0},{4,0},{1,0},{3,0},{1,0},{1,0}});
        data[idx++] = new MiniTileEntry(new int[]{8,2}, new int[][]{{3,0},{4,0},{4,0},{3,0},{4,0},{4,0},{3,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{9,2}, new int[][]{{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{10,2}, new int[][]{{4,0},{4,0},{3,0},{4,0},{4,0},{4,0},{3,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{11,2}, new int[][]{{4,0},{4,0},{1,0},{4,0},{4,0},{1,0},{4,0},{4,0},{1,0}});

        // Row 3 (y=3)
        data[idx++] = new MiniTileEntry(new int[]{0,3}, new int[][]{{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0},{0,0}});
        data[idx++] = new MiniTileEntry(new int[]{1,3}, new int[][]{{0,0},{2,0},{2,0},{0,0},{2,0},{2,0},{0,0},{2,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{2,3}, new int[][]{{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{3,3}, new int[][]{{2,0},{2,0},{0,0},{2,0},{2,0},{0,0},{2,0},{2,0},{0,0}});
        data[idx++] = new MiniTileEntry(new int[]{4,3}, new int[][]{{3,0},{3,0},{3,0},{4,0},{4,0},{3,0},{4,0},{4,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{5,3}, new int[][]{{3,0},{4,0},{4,0},{2,0},{4,0},{4,0},{2,0},{2,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{6,3}, new int[][]{{4,0},{4,0},{3,0},{4,0},{4,0},{2,0},{2,0},{2,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{7,3}, new int[][]{{3,0},{3,0},{3,0},{3,0},{4,0},{4,0},{3,0},{4,0},{4,0}});
        data[idx++] = new MiniTileEntry(new int[]{8,3}, new int[][]{{1,0},{4,0},{4,0},{0,0},{4,0},{4,0},{0,0},{0,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{9,3}, new int[][]{{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{2,0},{2,0},{2,0}});
        data[idx++] = new MiniTileEntry(new int[]{10,3}, new int[][]{{4,0},{4,0},{4,0},{4,0},{4,0},{4,0},{3,0},{3,0},{3,0}});
        data[idx++] = new MiniTileEntry(new int[]{11,3}, new int[][]{{4,0},{4,0},{1,0},{4,0},{4,0},{0,0},{2,0},{0,0},{0,0}});
        
        return data;
    }


    public void setOffsets(int leftOffset, int rightOffset, int topOffset, int bottomOffset) {
        this.leftOffset = leftOffset;
        this.rightOffset = rightOffset;
        this.topOffset = topOffset;
        this.bottomOffset = bottomOffset;
    }
}

