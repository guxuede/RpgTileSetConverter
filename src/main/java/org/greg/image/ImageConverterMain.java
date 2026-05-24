package org.greg.image;

import javafx.application.Application;

/**
 * Entry point for the Image Converter application.
 * Launches the JavaFX UI for converting RPG Maker tile images to Godot format.
 */
public class ImageConverterMain {
    public static void main(String[] args) {
        Application.launch(ImageConverterUI.class, args);
    }
}

