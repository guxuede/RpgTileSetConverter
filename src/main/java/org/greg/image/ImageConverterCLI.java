package org.greg.image;

import javafx.geometry.Rectangle2D;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Simple command-line demo for the Image Converter.
 * Usage: java -cp target/NetGameV1Ai-1.0-SNAPSHOT.jar org.example.image.ImageConverterCLI <input_image> <output_image>
 */
public class ImageConverterCLI {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("RPG Maker Image Converter - CLI Mode");
            System.out.println("Usage: ImageConverterCLI <input_image> <output_image> [leftOffset] [rightOffset] [topOffset] [bottomOffset]");
            System.out.println();
            System.out.println("Example:");
            System.out.println("  java -cp . ImageConverterCLI input.png output.png");
            System.out.println("  java -cp . ImageConverterCLI input.png output.png 2 3 1 2");
            return;
        }

        String inputPath = args[0];
        String outputPath = args[1];

        int leftOffset = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int rightOffset = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        int topOffset = args.length > 4 ? Integer.parseInt(args[4]) : 0;
        int bottomOffset = args.length > 5 ? Integer.parseInt(args[5]) : 0;

        try {
            // Load input image
            System.out.println("Loading image from: " + inputPath);
            BufferedImage inputImage = ImageIO.read(new File(inputPath));
            if (inputImage == null) {
                System.err.println("Error: Failed to load image");
                return;
            }

            System.out.println("Image loaded: " + inputImage.getWidth() + "x" + inputImage.getHeight());

            // Create converter
            ImageConverter converter = new ImageConverter(leftOffset, rightOffset, topOffset, bottomOffset);
            System.out.println("Converter configured with offsets: L=" + leftOffset + " R=" + rightOffset +
                             " T=" + topOffset + " B=" + bottomOffset);

            // Convert
            System.out.println("Converting...");
            BufferedImage output = converter.convertToGodotImage(inputImage, new Rectangle2D(0,0,0,0));

            if (output == null) {
                System.err.println("Error: Conversion returned null image");
                return;
            }

            // Save output
            System.out.println("Conversion successful! Output size: " + output.getWidth() + "x" + output.getHeight());
            System.out.println("Saving to: " + outputPath);

            String format = "png";
            if (outputPath.endsWith(".jpg") || outputPath.endsWith(".jpeg")) {
                format = "jpg";
            } else if (outputPath.endsWith(".gif")) {
                format = "gif";
            } else if (outputPath.endsWith(".bmp")) {
                format = "bmp";
            }

            ImageIO.write(output, format, new File(outputPath));
            System.out.println("✓ Successfully saved!");

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

