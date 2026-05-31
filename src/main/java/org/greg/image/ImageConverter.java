package org.greg.image;

import com.sun.javafx.geom.Vec2d;
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

    // Tile dimensions
    public static final int TILE_WIDTH = 48;
    public static final int HALF_TILE_WIDTH = TILE_WIDTH/2;
    public static final int TILE_HEIGHT = 48;
    public static final int TWO_TILE_HEIGHT = TILE_HEIGHT * 2;

    // Offset parameters for edge handling
    private int leftOffset = 0;
    private int rightOffset = 0;
    private int bottomOffset = 0;
    private int topOffset = 0;


    private boolean isClifMode = true;

    public ImageConverter() {
    }

    public ImageConverter(int leftOffset, int rightOffset, int topOffset, int bottomOffset) {
        this.leftOffset = leftOffset;
        this.rightOffset = rightOffset;
        this.topOffset = topOffset;
        this.bottomOffset = bottomOffset;
    }



    public void setClifMode(boolean clifMode) {
        isClifMode = clifMode;
    }

    public void setOffsets(int leftOffset, int rightOffset, int topOffset, int bottomOffset) {
        this.leftOffset = leftOffset;
        this.rightOffset = rightOffset;
        this.topOffset = topOffset;
        this.bottomOffset = bottomOffset;
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
        BufferedImage tempCanvas = new BufferedImage(5 * TILE_WIDTH, isClifMode? (3*TILE_HEIGHT): TILE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

         // Quad copy operations (from JavaScript lines 189-209)
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(0, 0), QuadSection.TOP_LEFT, new Vec2d(0, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(0, 0), QuadSection.TOP_RIGHT, new Vec2d(1, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(0, 0), QuadSection.BOTTOM_LEFT, new Vec2d(0, 2));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(0, 0), QuadSection.BOTTOM_RIGHT, new Vec2d(1, 2));

         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(1, 0), QuadSection.BOTTOM_LEFT, new Vec2d(0, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(1, 0), QuadSection.BOTTOM_RIGHT, new Vec2d(1, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(1, 0), QuadSection.TOP_LEFT, new Vec2d(0, 2));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(1, 0), QuadSection.TOP_RIGHT, new Vec2d(1, 2));

         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(2, 0), QuadSection.TOP_RIGHT, new Vec2d(0, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(2, 0), QuadSection.TOP_LEFT, new Vec2d(1, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(2, 0), QuadSection.BOTTOM_RIGHT, new Vec2d(0, 2));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(2, 0), QuadSection.BOTTOM_LEFT, new Vec2d(1, 2));

         copyTile(inputImg, rectangle2D, tempCanvas, new Vec2d(3, 0), new Vec2d(1, 0));

         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(4, 0), QuadSection.BOTTOM_RIGHT, new Vec2d(0, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(4, 0), QuadSection.BOTTOM_LEFT, new Vec2d(1, 1));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(4, 0), QuadSection.TOP_RIGHT, new Vec2d(0, 2));
         copyTileQuadRaw(inputImg, rectangle2D, tempCanvas, new Vec2d(4, 0), QuadSection.TOP_LEFT, new Vec2d(1, 2));


        if(isClifMode){
             copyTile(inputImg, rectangle2D, tempCanvas, new Vec2d(0, 1), new Vec2d(0, 3));
             copyTile(inputImg, rectangle2D, tempCanvas, new Vec2d(1, 1), new Vec2d(1, 3));

             copyTile(inputImg, rectangle2D, tempCanvas, new Vec2d(0, 2), new Vec2d(0, 4));
             copyTile(inputImg, rectangle2D, tempCanvas, new Vec2d(1, 2), new Vec2d(1, 4));
         }

        // Step 2: Apply mini tile substitution (convert to Godot format - 192x144)
        BufferedImage outputCanvas = new BufferedImage(12 * TILE_WIDTH, (isClifMode? 8 :4) * TILE_HEIGHT, BufferedImage.TYPE_INT_ARGB);

        MiniTileEntry[] miniTilesToGodotData = isClifMode? createMiniTilesToGodotDataWithClif(): createMiniTilesToGodotData();

        doConvetMiniTileToGodot(tempCanvas, outputCanvas, miniTilesToGodotData);
        doConvertClif(tempCanvas, outputCanvas);

        return outputCanvas;
    }

    private void doConvertClif(BufferedImage tempCanvas, BufferedImage outputCanvas) {
        if(isClifMode){//copy Clif tile
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(0, 3), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(0, 3), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.RIGHT);
             //-----------------------------

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(1, 3), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(1, 3), HalfSection.RIGHT, new Vec2d(0, 1), HalfSection.RIGHT);

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(2, 3), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.RIGHT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(2, 3), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.LEFT);

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(3, 3), HalfSection.LEFT, new Vec2d(1, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(3, 3), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.RIGHT);


             //-----------------------------
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(0, 6), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(0, 6), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.RIGHT);


             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(1, 6), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(1, 6), HalfSection.RIGHT, new Vec2d(0, 1), HalfSection.RIGHT);

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(2, 6), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.RIGHT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(2, 6), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.LEFT);

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(3, 6), HalfSection.LEFT, new Vec2d(1, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(3, 6), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.RIGHT);


             //-----------------------------
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(5, 4), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(5, 4), HalfSection.RIGHT, new Vec2d(0, 1), HalfSection.RIGHT);

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(6, 4), HalfSection.LEFT, new Vec2d(1, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(6, 4), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.RIGHT);



             //-----------------------------
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(8, 4), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(8, 4), HalfSection.RIGHT, new Vec2d(0, 1), HalfSection.RIGHT);

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(9, 4), HalfSection.LEFT, new Vec2d(0, 1), HalfSection.RIGHT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(9, 4), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.LEFT);

             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(11, 4), HalfSection.LEFT, new Vec2d(1, 1), HalfSection.LEFT);
             copyClifHalfTile(tempCanvas, outputCanvas, new Vec2d(11, 4), HalfSection.RIGHT, new Vec2d(1, 1), HalfSection.RIGHT);
         }
    }


    /**
     * Copy a single tile from source to target position
     */
    private void copyTile(BufferedImage srcImg, Rectangle2D rectangle2D,
                          BufferedImage dstImg, Vec2d toPos, Vec2d fromPos) {
        Graphics2D g2d = dstImg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(
                srcImg,
                (int)(toPos.x * TILE_WIDTH),
                (int)(toPos.y * TILE_HEIGHT),
                (int)(toPos.x * TILE_WIDTH + TILE_WIDTH),
                (int)(toPos.y * TILE_HEIGHT + TILE_HEIGHT),
                (int)(fromPos.x * TILE_WIDTH + rectangle2D.getMinX()),
                (int)(fromPos.y * TILE_HEIGHT + rectangle2D.getMinY()),
                (int)(fromPos.x * TILE_WIDTH + TILE_WIDTH + rectangle2D.getMinX()),
                (int)(fromPos.y * TILE_HEIGHT + TILE_HEIGHT + rectangle2D.getMinY()),
                null
        );
        g2d.dispose();
    }


    private void copyClifHalfTile(BufferedImage srcImg,
                                  BufferedImage dstImg,
                                  Vec2d toPos,
                                  HalfSection toSection,
                                  Vec2d fromPos,
                                  HalfSection fromSection
    ) {
        Graphics2D g2d = dstImg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle2D targetRect = new Rectangle2D(
                toPos.x * TILE_WIDTH + (toSection.isLeft ? 0 : HALF_TILE_WIDTH),
                toPos.y * TILE_HEIGHT,
                HALF_TILE_WIDTH,
                TWO_TILE_HEIGHT
        )  ;

        Rectangle2D srcRect = new Rectangle2D(
                fromPos.x * TILE_WIDTH + (fromSection.isLeft ? 0 : HALF_TILE_WIDTH),
                fromPos.y * TILE_HEIGHT,
                HALF_TILE_WIDTH,
                TWO_TILE_HEIGHT
        )  ;

        g2d.drawImage(
                srcImg,
                (int)targetRect.getMinX(),
                (int)targetRect.getMinY(),
                (int)targetRect.getMaxX(),
                (int)targetRect.getMaxY(),

                (int)srcRect.getMinX(),
                (int)srcRect.getMinY(),
                (int)srcRect.getMaxX(),
                (int)srcRect.getMaxY(),
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
                                    Vec2d target, TileSection section, Vec2d data) {
        int x = (int)target.x;
        int y = (int)target.y;
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

        int offset_x = offsetX[section.xIndex];
        int offset_y = offsetY[section.yIndex];
        int size_x = sizeX[section.xIndex];
        int size_y = sizeY[section.yIndex];

        Graphics2D g2d = dstImg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle2D targetRect = new Rectangle2D(
                x * TILE_WIDTH + offset_x,
                y * TILE_HEIGHT + offset_y,
                size_x,
                size_y
        );

        Rectangle2D srcRect = new Rectangle2D(
                (int)data.x * TILE_WIDTH + offset_x,
                (int)data.y * TILE_HEIGHT + offset_y,
                size_x,
                size_y
        );

        g2d.drawImage(
                srcImg,
                (int) targetRect.getMinX(),
                (int) targetRect.getMinY(),
                (int) targetRect.getMaxX(),
                (int) targetRect.getMaxY(),
                (int) srcRect.getMinX(),
                (int) srcRect.getMinY(),
                (int) srcRect.getMaxX(),
                (int) srcRect.getMaxY(),
                null
        );
        g2d.dispose();
    }

    /**
     * Copy a quad (quarter tile) section
     * Used for sampling 4 quadrants of a tile
     */
    private void copyTileQuadRaw(BufferedImage srcImg, Rectangle2D rectangle2D, BufferedImage dstImg,
                                 Vec2d target, QuadSection section, Vec2d data) {
        int x = (int)target.x;
        int y = (int)target.y;

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

        int offset_x = offsetX[section.xIndex];
        int offset_y = offsetY[section.yIndex];
        int size_x = sizeX[section.xIndex];
        int size_y = sizeY[section.yIndex];

        Graphics2D g2d = dstImg.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Rectangle2D targetRect = new Rectangle2D(
                x * TILE_WIDTH + offset_x,
                y * TILE_HEIGHT + offset_y,
                size_x,
                size_y
        );

        Rectangle2D srcRect = new Rectangle2D(
                (int)data.x * TILE_WIDTH + offset_x + (int) rectangle2D.getMinX(),
                (int)data.y * TILE_HEIGHT + offset_y + (int) rectangle2D.getMinY(),
                size_x,
                size_y
        );

        g2d.drawImage(
                srcImg,
                (int) targetRect.getMinX(),
                (int) targetRect.getMinY(),
                (int) targetRect.getMaxX(),
                (int) targetRect.getMaxY(),
                (int) srcRect.getMinX(),
                (int) srcRect.getMinY(),
                (int) srcRect.getMaxX(),
                (int) srcRect.getMaxY(),
                null
        );
        g2d.dispose();
    }

    /**
     * Copy a tile section using the section data lookup
     */
    private void copyTileSection(BufferedImage srcImg, BufferedImage dstImg,
                                 Vec2d target, TileSection section, Vec2d[] data) {
        Vec2d sectionData = data[section.yIndex * 3 + section.xIndex];
        if (sectionData.x < 0 || sectionData.y < 0) {
            return;
        }
        copyTileSectionRaw(srcImg, dstImg, target, section, sectionData);
    }

    /**
     * Apply mini tile substitution mapping
     */
    private void doConvetMiniTileToGodot(BufferedImage srcImg, BufferedImage dstImg, MiniTileEntry[] miniTilesToGodotData) {
        for (MiniTileEntry entry : miniTilesToGodotData) {
            for (TileSection section : TileSection.values()) {
                copyTileSection(srcImg, dstImg, entry.targetTile, section, entry.tileData);
            }
        }
    }


    //维护源数据 到 目标坐标的映射关系
     // Data initialization method
     private static MiniTileEntry[] createMiniTilesToGodotData() {
         MiniTileEntry[] data = new MiniTileEntry[48];
         int idx = 0;

         // Row 0 (y=0)
         data[idx++] = new MiniTileEntry(new Vec2d(0,0), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,0), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(4,0), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,0), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,0), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,0), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0)});

         // Row 1 (y=1)
         data[idx++] = new MiniTileEntry(new Vec2d(0,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(4,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,1), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,1), new Vec2d[]{new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,1), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});

         // Row 2 (y=2)
         data[idx++] = new MiniTileEntry(new Vec2d(0,2), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,2), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,2), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,2), new Vec2d[]{new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(4,2), new Vec2d[]{new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,2), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0)});

         // Row 3 (y=3)
         data[idx++] = new MiniTileEntry(new Vec2d(0,3), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,3), new Vec2d[]{new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,3), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,3), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(4,3), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,3), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,3), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,3), new Vec2d[]{new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0)});

         return data;
     }


     private static MiniTileEntry[] createMiniTilesToGodotDataWithClif() {
         MiniTileEntry[] data = new MiniTileEntry[48];
         int idx = 0;

         // Row 0 (y=0)
         data[idx++] = new MiniTileEntry(new Vec2d(0,0), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,0), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(4,0), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,0), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,0), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,0), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,0), new Vec2d[]{new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0)});

         // Row 1 (y=1)
         data[idx++] = new MiniTileEntry(new Vec2d(0,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(4,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,1), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,1), new Vec2d[]{new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,1), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,1), new Vec2d[]{new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1),new Vec2d(-1,-1)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,1), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});

         // Row 2 (y=2)
         data[idx++] = new MiniTileEntry(new Vec2d(0,2), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,2), new Vec2d[]{new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,2), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,2), new Vec2d[]{new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(4,2), new Vec2d[]{new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(1,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(3,0),new Vec2d(1,0),new Vec2d(1,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,2), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,2), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0)});

         // Row 3 (y=3) as it is clif mode, move it to 5(original is 3), 3 is for clif now
         data[idx++] = new MiniTileEntry(new Vec2d(0,5), new Vec2d[]{new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(0,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(1,5), new Vec2d[]{new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(2,5), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(3,5), new Vec2d[]{new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(0,0)});

         data[idx++] = new MiniTileEntry(new Vec2d(4,3), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(5,3), new Vec2d[]{new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(6,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(7,3), new Vec2d[]{new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(4,0),new Vec2d(4,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(8,3), new Vec2d[]{new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(0,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(9,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(2,0),new Vec2d(2,0),new Vec2d(2,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(10,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(3,0),new Vec2d(3,0),new Vec2d(3,0)});
         data[idx++] = new MiniTileEntry(new Vec2d(11,3), new Vec2d[]{new Vec2d(4,0),new Vec2d(4,0),new Vec2d(1,0),new Vec2d(4,0),new Vec2d(4,0),new Vec2d(0,0),new Vec2d(2,0),new Vec2d(0,0),new Vec2d(0,0)});

         return data;
     }












    /**
     * Helper class to store mini tile mapping entry
     */
    public static class MiniTileEntry {
        public Vec2d targetTile;
        public Vec2d targetOffSetTile;
        public Vec2d[] tileData;  // 1D array of 9 coordinate references (3x3 grid)

        public MiniTileEntry(Vec2d target, Vec2d[] data) {
            this.targetTile = target;
            this.tileData = data;
        }

        public MiniTileEntry(Vec2d target, Vec2d targetOffSetTile, Vec2d[] data) {
            this.targetTile = target;
            this.targetOffSetTile = targetOffSetTile;
            this.tileData = data;
        }
    }

    // Quadrant enum for 3x3 copies, a tile is split into 3x3
    private enum TileSection {
        TOP_LEFT(0,0), TOP_CENTER(1,0), TOP_RIGHT(2,0),
        MIDDLE_LEFT(0,1), MIDDLE_CENTER(1,1), MIDDLE_RIGHT(2,1),
        BOTTOM_LEFT(0,2), BOTTOM_CENTER(1,2), BOTTOM_RIGHT(2,2);

        final int xIndex;
        final int yIndex;
        TileSection(int xIndex, int yIndex) { this.xIndex = xIndex; this.yIndex = yIndex; }
    }

    // Quadrant enum for 2x2 copies,  a tile is split into 3x3
    private enum QuadSection {
        TOP_LEFT(0,0), TOP_RIGHT(1,0), BOTTOM_LEFT(0,1), BOTTOM_RIGHT(1,1);
        final int xIndex;
        final int yIndex;
        QuadSection(int xIndex, int yIndex) { this.xIndex = xIndex; this.yIndex = yIndex; }
    }

    // Half tile section enum for left/right sections
    private enum HalfSection {
        LEFT(0, true), RIGHT(1, false);
        final int index;
        final boolean isLeft;
        HalfSection(int index, boolean isLeft) { this.index = index; this.isLeft = isLeft; }
    }

}

