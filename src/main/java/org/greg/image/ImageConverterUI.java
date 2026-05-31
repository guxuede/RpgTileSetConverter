package org.greg.image;

import com.sun.javafx.geom.Vec2d;
import javafx.animation.FadeTransition;
import javafx.animation.StrokeTransition;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * JavaFX UI for the Image Converter tool.
 * Allows users to:
 * 1. Load an image file (RPG Maker format)
 * 2. Adjust tile offsets (left, right, top, bottom)
 * 3. Preview the conversion result
 * 4. Save the converted image
 */
public class ImageConverterUI extends Application {


    private BufferedImage originalImage;
    private ImageView sourceImageView;
    private Rectangle sourceImageCursor;
    private Label statusLabel;

    private Spinner<Integer> leftOffsetSpinner;
    private Spinner<Integer> rightOffsetSpinner;
    private Spinner<Integer> topOffsetSpinner;
    private Spinner<Integer> bottomOffsetSpinner;

    private CheckBox hasClifCheckBox;
    private Spinner<Integer> numberTileSetWantedSpinner;
    private Spinner<Integer> tileSetXNumberSpinner;
    private Spinner<Integer> tileSetYNumberSpinner;



    AnchorPane outputImageViews;


    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("RPG Maker Image Converter");



        BorderPane root = new BorderPane();
//        root.setPadding(new Insets(10));

        // Left panel: Output image
        {
            VBox outputPanel = createOutputPanel();
            outputPanel.setAlignment(Pos.TOP_RIGHT);
            outputPanel.setFillWidth(true);
            root.setCenter(outputPanel);
        }


        // Right panel: Controls
        // Left panel: Source image
        {
            VBox panel = new VBox(10);
            panel.setAlignment(Pos.TOP_RIGHT);
            root.setRight(panel);

            VBox sourcePanel = createSourcePanel();
            VBox controlPanel = createControlPanel();

            panel.getChildren().addAll(sourcePanel, controlPanel);

        }

        {
            // Bottom panel: Status
            HBox bottomPanel = createStatusPanel();
            bottomPanel.setAlignment(Pos.BOTTOM_LEFT);
            root.setBottom(bottomPanel);
        }

        ScrollPane scrollPane = new ScrollPane(root);
// 滚动条策略：内容超了才显示
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);


        Scene scene = new Scene(scrollPane);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private double offsetX, offsetY;

    private VBox createSourcePanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");

        Label title = new Label("Source Image (RPG Maker Format)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        sourceImageView = new ImageView();
//        sourceImageView.setFitWidth(350);
//        sourceImageView.setFitHeight(350);
//        sourceImageView.setPreserveRatio(true);
        sourceImageView.setStyle("-fx-border-color: #999999;");
        sourceImageView.setOnMouseClicked(e -> {
            if(e.getButton().equals(MouseButton.PRIMARY)){
                onSourceImageViewClick(e);
            }
        });

        sourceImageCursor = new Rectangle();
        sourceImageCursor.setLayoutX(0);
        sourceImageCursor.setLayoutY(0);
        sourceImageCursor.setWidth(48*2);
        sourceImageCursor.setHeight(48*3);
        sourceImageCursor.setFill(Color.TRANSPARENT);
        sourceImageCursor.setStroke(Color.CORAL);
        sourceImageCursor.setStrokeWidth(3);
        sourceImageCursor.getStrokeDashArray().addAll(8.0,4.0);
        sourceImageCursor.toFront();
        sourceImageCursor.setManaged(false);
        sourceImageCursor.setVisible(false);
        FadeTransition flash = new FadeTransition(Duration.millis(500), sourceImageCursor);
        flash.setFromValue(1.0);
        flash.setToValue(0.2);
        flash.setCycleCount(-1); // 无限循环
        flash.setAutoReverse(true);
        flash.play();
        StrokeTransition colorAnim = new StrokeTransition(
                Duration.seconds(1),
                sourceImageCursor,
                Color.CORAL,Color.CYAN
        );
        colorAnim.setCycleCount(-1);
        colorAnim.setAutoReverse(true);
        colorAnim.play();

        StackPane sourceImagePane = new StackPane(sourceImageView, sourceImageCursor);
        sourceImagePane.setAlignment(Pos.TOP_LEFT);

        Button loadButton = new Button("Load Image");
        loadButton.setStyle("-fx-font-size: 12; -fx-padding: 8;");
        loadButton.setPrefWidth(200);
        loadButton.setOnAction(e -> loadImage());

        panel.getChildren().addAll(title, sourceImagePane, loadButton);
        return panel;
    }


    private VBox createOutputPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");

        Label title = new Label("Converted Image (Godot Format)");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        outputImageViews = new AnchorPane();


        panel.getChildren().addAll(title, outputImageViews);
        return panel;
    }

    private VBox createControlPanel() {
        VBox panel = new VBox(15);
        panel.setPadding(new Insets(5));
        panel.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");
        panel.setPrefWidth(400);

        Label title = new Label("Configuration");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        // Left Offset
        HBox leftOffsetBox = createOffsetControl("Left Offset", leftOffsetSpinner = new Spinner<>(0, 16, 0));

        // Right Offset
        HBox rightOffsetBox = createOffsetControl("Right Offset", rightOffsetSpinner = new Spinner<>(0, 16, 0));

        // Top Offset
        HBox topOffsetBox = createOffsetControl("Top Offset", topOffsetSpinner = new Spinner<>(0, 16, 0));

        // Bottom Offset
        HBox bottomOffsetBox = createOffsetControl("Bottom Offset", bottomOffsetSpinner = new Spinner<>(0, 16, 0));


        hasClifCheckBox = new CheckBox("Has Clif");
        HBox hasClifBox = createOffsetControl("",hasClifCheckBox);
        hasClifCheckBox.selectedProperty().addListener(e -> onSourceSelectionChange());

        HBox numberTileSet = createOffsetControl("Number of tileset Wanted", numberTileSetWantedSpinner = new Spinner<>(1, 20, 1));
        numberTileSetWantedSpinner.valueProperty().addListener(e -> onSourceSelectionChange());

        HBox tileSetXNumber = createOffsetControl("Tileset X Number Offset", tileSetXNumberSpinner = new Spinner<>(0, 16, 0));
        tileSetXNumberSpinner.valueProperty().addListener(e -> onSourceSelectionChange());

        HBox tileSetYNumber = createOffsetControl("Tileset Y Number Offset", tileSetYNumberSpinner = new Spinner<>(0, 16, 0));
        tileSetYNumberSpinner.valueProperty().addListener(e -> onSourceSelectionChange());


        Button resetButton = new Button("Reset Offsets");
        resetButton.setStyle("-fx-font-size: 12; -fx-padding: 8;");
        resetButton.setPrefWidth(200);
        resetButton.setOnAction(e -> resetOffsets());

        Button cleanConvertButton = new Button("Clean and Convert/Add");
        cleanConvertButton.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-font-weight: bold;");
        cleanConvertButton.setPrefWidth(200);
        cleanConvertButton.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #4CAF50;");
        cleanConvertButton.setOnAction(e -> applyConversion(true));

        Button convertAddButton = new Button("Convert/Add");
        convertAddButton.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-font-weight: bold;");
        convertAddButton.setPrefWidth(200);
        convertAddButton.setStyle("-fx-font-size: 12; -fx-padding: 10; -fx-font-weight: bold; -fx-text-fill: white; -fx-background-color: #4CAF50;");
        convertAddButton.setOnAction(e -> applyConversion(false));

        Button saveButton = new Button("Save Result");
        saveButton.setStyle("-fx-font-size: 12; -fx-padding: 8;");
        saveButton.setPrefWidth(200);
        saveButton.setOnAction(e -> saveImage());

        panel.getChildren().addAll(
            title,
            leftOffsetBox,
            rightOffsetBox,
            topOffsetBox,
            bottomOffsetBox,
            new Label("--------------"),
            new Label("TileSet Size:96x144(single tile:48)"),
                hasClifBox,
                numberTileSet,
                tileSetXNumber,
                tileSetYNumber,
            cleanConvertButton, convertAddButton, saveButton
        );

        return panel;
    }

    private HBox createOffsetControl(String label, Control spinner) {

        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(label);
        lbl.setPrefWidth(300);

        spinner.setPrefWidth(80);
        spinner.setStyle("-fx-font-size: 11;");

        box.getChildren().addAll(spinner,lbl);
        return box;
    }

    private HBox createStatusPanel() {
        HBox panel = new HBox(10);
        panel.setPadding(new Insets(10));
        panel.setStyle("-fx-border-color: #cccccc; -fx-border-radius: 5;");

        statusLabel = new Label("Ready. Load an image to begin.");
        statusLabel.setStyle("-fx-font-size: 12;");

        panel.getChildren().add(statusLabel);
        return panel;
    }

    private void loadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif", "*.bmp"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                originalImage = ImageIO.read(selectedFile);
                sourceImageView.setImage(bufferedImageToFXImage(originalImage));
                statusLabel.setText("Image loaded: " + selectedFile.getName() + " (" + originalImage.getWidth() + "x" + originalImage.getHeight() + ")");
                sourceImageCursor.setVisible(true);
            } catch (Exception e) {
                statusLabel.setText("Error loading image: " + e.getMessage());
            }
        }
    }

    private void saveImage() {
        if (outputImageViews.getChildren().isEmpty()) {
            statusLabel.setText("No converted image to save. Please load and convert an image first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Image", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Image", "*.jpg"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showSaveDialog(null);
        if (selectedFile != null) {
            try {
                List<ImageView> imageViews = new ArrayList<>();
                for(int i = 0; i < outputImageViews.getChildren().size(); i++) {
                    ImageView imageView = (ImageView) outputImageViews.getChildren().get(i);
                    imageViews.add(imageView);
                }
                imageViews.sort(Comparator.comparingDouble(Node::getLayoutY).thenComparingDouble(Node::getLayoutX));
                mergeToPng(imageViews, selectedFile.getAbsolutePath());
                statusLabel.setText("Image saved: " + selectedFile.getName());
            } catch (Exception e) {
                e.printStackTrace();
                statusLabel.setText("Error saving image: " + e.getMessage());
            }
        }
    }

    private void applyConversion(boolean clean) {
        Integer numberTileSetWanted = numberTileSetWantedSpinner.getValue();

        List<Rectangle2D> rectangles = new ArrayList<>();
        for(int i = 0; i < numberTileSetWanted; i++) {
            rectangles.add(new Rectangle2D(sourceImageCursor.getLayoutX(), sourceImageCursor.getLayoutY(), sourceImageCursor.getWidth(), sourceImageCursor.getHeight()));
        }

        if(clean){
            outputImageViews.getChildren().clear();
        }
        Vec2d last = findLastOne();
        for(int i = 0; i < rectangles.size(); i++) {
            applyConversions(rectangles.get(i), last, i);
        }
    }

    private void onSourceImageViewClick(MouseEvent e) {
        System.out.println("Clicked on source image at: " + e.getX() + ", " + e.getY());
        tileSetXNumberSpinner.getValueFactory().setValue((int)(e.getX() / sourceImageCursor.getWidth()));
        tileSetYNumberSpinner.getValueFactory().setValue((int)(e.getY() / sourceImageCursor.getHeight()));
    }

    private void onSourceSelectionChange(){
        Integer tileSetXNumberSpinnerValue = tileSetXNumberSpinner.getValue();
        Integer tileSetYNumberSpinnerValue = tileSetYNumberSpinner.getValue();
        Integer numberTileSetWanted = numberTileSetWantedSpinner.getValue();
        boolean hasClif = hasClifCheckBox.isSelected();
        sourceImageCursor.setHeight(hasClif? 48*5 : 48*3);
        sourceImageCursor.setLayoutX(48*2 * tileSetXNumberSpinnerValue);
        sourceImageCursor.setLayoutY(sourceImageCursor.getHeight() * tileSetYNumberSpinnerValue);
    }


    private void applyConversions(Rectangle2D rectangle2D, Vec2d last, int batchIndex) {
        if (originalImage == null) {
            statusLabel.setText("Please load an image first.");
            return;
        }

        try {
            ImageConverter converter = new ImageConverter();
            converter.setClifMode(hasClifCheckBox.isSelected());
            // Update converter offsets
            converter.setOffsets(
                leftOffsetSpinner.getValue(),
                rightOffsetSpinner.getValue(),
                topOffsetSpinner.getValue(),
                bottomOffsetSpinner.getValue()
            );

            // Apply conversion
            //source 96x144
            //converted 576x192
            BufferedImage convertedImage = converter.convertToGodotImage(originalImage, rectangle2D);

            if (convertedImage != null) {
                ImageView outputImageView = new ImageView();
//                outputImageView.setPreserveRatio(true);

                outputImageView.setStyle("-fx-border-color: #999999;");
                outputImageView.setImage(bufferedImageToFXImage(convertedImage));
                outputImageView.setLayoutX(batchIndex*convertedImage.getWidth());
                outputImageView.setLayoutY(last == null? 0 : (last.y + convertedImage.getHeight() + 10));
                outputImageView.setOnMouseClicked(e -> {
                    if(e.getButton().equals(MouseButton.SECONDARY)){
                        System.out.println("Clicked on converted image at, delete it: " + e.getX() + ", " + e.getY());
                        outputImageViews.getChildren().remove(outputImageView);
                    }
                });
                {
                    Vec2d offsets = new Vec2d(0D,0D);
                    outputImageView.setOnMousePressed(e -> {
                        offsets.set(e.getX() - outputImageView.getLayoutX(), e.getY() - outputImageView.getLayoutY());
                    });

                    // 鼠标拖动：更新节点位置
                    outputImageView.setOnMouseDragged(e -> {
                        outputImageView.setLayoutX(e.getX() - offsets.x);
                        outputImageView.setLayoutY(e.getY() - offsets.y);
                        System.out.println(outputImageView.getLayoutX() +","+ outputImageView.getLayoutY());
                    });
                }
                outputImageViews.getChildren().add(outputImageView);
                statusLabel.setText("Conversion successful! Output size: " + convertedImage.getWidth() + "x" + convertedImage.getHeight());
            } else {
                statusLabel.setText("Conversion failed: returned null image.");
            }
        } catch (Exception e) {
            statusLabel.setText("Conversion error: " + e.getClass().getName() + e.getMessage());
            e.printStackTrace();
        }
    }

    private void resetOffsets() {
        leftOffsetSpinner.getValueFactory().setValue(0);
        rightOffsetSpinner.getValueFactory().setValue(0);
        topOffsetSpinner.getValueFactory().setValue(0);
        bottomOffsetSpinner.getValueFactory().setValue(0);
        statusLabel.setText("Offsets reset to 0.");
    }

    /**
     * Convert BufferedImage to JavaFX Image
     */
    private javafx.scene.image.Image bufferedImageToFXImage(BufferedImage bufferedImage) {
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }



    /**
     * 自动紧凑拼接 + 自动换行
     * 规则：横向排列，遇到图片高度超过当前行高度就自动换行，继续横向排列
     */
    public static void mergeToPng(List<ImageView> imageViews, String outputPath) throws Exception {
        if (imageViews == null || imageViews.isEmpty()) {
            throw new IllegalArgumentException("图片列表不能为空");
        }

        // === 1. 先计算：最终画布的总宽度、总高度 ===
        int maxWidth = 0;         // 最终图片总宽度（最宽那一行）
        int maxHeight = 0;        // 最终图片总高度
        int currentWidth = 0;

        // 第一次遍历：计算画布大小
        for (ImageView iv : imageViews) {
            Image img = iv.getImage();
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();


            if(maxHeight ==0){
                maxHeight = h;
            }


            if(iv.getLayoutY() > maxHeight){
                maxHeight = maxHeight + h;//new line
                maxWidth = Math.max(maxWidth, currentWidth);
                currentWidth = 0;
            }else{
                maxHeight = Math.max(maxHeight, h);
                currentWidth = currentWidth + w;
            }
        }


        // === 2. 创建最终画布 ===
        WritableImage result = new WritableImage(Math.max(maxWidth, currentWidth), maxHeight);
        var writer = result.getPixelWriter();

        // === 3. 第二次遍历：开始绘制（自动紧凑 + 自动换行）===
         maxHeight = 0;

        int drawX = 0;        // 当前绘制 X 坐标
        int drawY = 0;        // 当前绘制 Y 坐标


        for (ImageView iv : imageViews) {
            Image img = iv.getImage();
            int w = (int) img.getWidth();
            int h = (int) img.getHeight();

            if(maxHeight ==0){
                maxHeight = h;
            }


            if(iv.getLayoutY() > maxHeight){
                drawY = drawY + h;//new line
                drawX = 0;
                writer.setPixels(drawX, drawY, w, h, img.getPixelReader(), 0, 0);
                drawX =  drawX + w;
                maxHeight = maxHeight + h;
            }else{
                writer.setPixels(drawX, drawY, w, h, img.getPixelReader(), 0, 0);
                drawX =  drawX + w;
                maxHeight = Math.max(maxHeight, h);
            }

        }

        // === 4. 保存 PNG（透明完美保留）===
        ImageIO.write(SwingFXUtils.fromFXImage(result, null), "png", new File(outputPath));
    }


    public Vec2d findLastOne(){
        List<ImageView> imageViews = new ArrayList<>();
        for(int i = 0; i < outputImageViews.getChildren().size(); i++) {
            ImageView imageView = (ImageView) outputImageViews.getChildren().get(i);
            imageViews.add(imageView);
        }
        return imageViews.stream().sorted(Comparator.comparingDouble(Node::getLayoutY).thenComparingDouble(Node::getLayoutX).reversed()).map(e->new Vec2d(e.getLayoutX(), e.getLayoutY())).findFirst().orElse(null);
    }

public static void main(String[] args) {
        launch(args);
    }
}

