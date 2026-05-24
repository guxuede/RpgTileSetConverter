package org.greg.image;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class DraggableNodeDemo extends Application {

    private double offsetX, offsetY; // 鼠标相对于节点左上角的偏移

    @Override
    public void start(Stage primaryStage) {
        Label draggableLabel = new Label("拖我试试");
        draggableLabel.setStyle("-fx-background-color: lightblue; -fx-padding: 10;");

        // 鼠标按下：记录初始位置和偏移
        draggableLabel.setOnMousePressed(e -> {
            offsetX = e.getX();
            offsetY = e.getY();
        });

        // 鼠标拖动：更新节点位置
        draggableLabel.setOnMouseDragged(e -> {
            draggableLabel.setLayoutX(e.getSceneX() - offsetX);
            draggableLabel.setLayoutY(e.getSceneY() - offsetY);
        });

        AnchorPane root = new AnchorPane(draggableLabel);
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("JavaFX 自由拖动示例");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}