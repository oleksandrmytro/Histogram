package histogram;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;

public class Main extends Application {

    private ImageView imageView = new ImageView();
    private Rectangle rectangle;
    private double startX;
    private double startY;
    private Pane imagePane = new Pane();
    private LineChart<Number, Number> histogram;
    private Stage stage;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;

        BorderPane root = new BorderPane();

        HBox controlPanel = new HBox();
        controlPanel.setPrefHeight(50);
        controlPanel.setStyle("-fx-background-color: #ccc; -fx-alignment: center;");

        Button loadButton = new Button("Load Picture");
        loadButton.setOnAction(event -> loadPicture());

        TextField rectangleSize = new TextField();
        rectangleSize.setPromptText("Size of the rectangle will be displayed here...");
        controlPanel.getChildren().addAll(loadButton, rectangleSize);
        root.setBottom(controlPanel);

        imagePane.setStyle("-fx-background-color: #ffff77;");
        imagePane.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        imagePane.setPrefSize(Control.USE_COMPUTED_SIZE, Control.USE_COMPUTED_SIZE);
        root.setCenter(imagePane);

        NumberAxis xAxis = new NumberAxis(0, 255, 64);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        histogram = new LineChart<>(xAxis, yAxis);
        histogram.setMinWidth(300);
        histogram.setCreateSymbols(false);
        histogram.setLegendVisible(false);
        histogram.setBorder(new Border(new BorderStroke(Color.BLUE, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1))));
        root.setRight(histogram);

        Scene scene = new Scene(root, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Image Viewer");
        primaryStage.show();

        // On imagePane mouse click
        imagePane.setOnMousePressed(event -> {
            startX = event.getX();
            startY = event.getY();
            if (rectangle != null) {
                imagePane.getChildren().remove(rectangle);
                rectangle = null;
                histogram.getData().clear();
            }
            rectangle = new Rectangle(startX, startY, 0, 0);
            rectangle.setStroke(Color.RED);
            rectangle.setFill(Color.TRANSPARENT);
            imagePane.getChildren().add(rectangle);
        });

        // On imagePane mouse drag
        imagePane.setOnMouseDragged(event -> {
            if (rectangle != null) {
                rectangle.setWidth(Math.abs(event.getX() - startX));
                rectangle.setHeight(Math.abs(event.getY() - startY));
                rectangle.setX(Math.min(event.getX(), startX));
                rectangle.setY(Math.min(event.getY(), startY));
                rectangleSize.setText("Width: " + rectangle.getWidth() + ", Height: " + rectangle.getHeight());
            }
        });

        // On imagePane mouse release
        imagePane.setOnMouseReleased(event -> {
            calculateHistogram();
            histogram.layout();
        });

        primaryStage.widthProperty().addListener((observable, oldValue, newValue) -> {
            if (imageView.getImage() != null && (imageView.getImage().getWidth() > newValue.doubleValue() || imageView.getImage().getHeight() > primaryStage.getHeight())) {
                imageView.setImage(null);
            } else {
                centerImage();
            }
        });

        primaryStage.heightProperty().addListener((observable, oldValue, newValue) -> {
            if (imageView.getImage() != null && (imageView.getImage().getHeight() > newValue.doubleValue() || imageView.getImage().getWidth() > primaryStage.getWidth())) {
                imageView.setImage(null);
            } else {
                centerImage();
            }
        });
    }

    private void loadPicture() {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try {
                Image image = new Image(new FileInputStream(file));
                imageView.setImage(image);
                imagePane.getChildren().add(imageView);
                centerImage();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void calculateHistogram() {
        if (rectangle == null || imageView.getImage() == null) {
            return;
        }

        int[] redData = new int[256];
        int[] greenData = new int[256];
        int[] blueData = new int[256];

        PixelReader pixelReader = imageView.getImage().getPixelReader();
        int imageWidth = (int) imageView.getImage().getWidth();
        int imageHeight = (int) imageView.getImage().getHeight();

        for (int x = (int) rectangle.getX(); x < rectangle.getX() + rectangle.getWidth() && x < imageWidth; x++) {
            for (int y = (int) rectangle.getY(); y < rectangle.getY() + rectangle.getHeight() && y < imageHeight; y++) {
                Color color = pixelReader.getColor(x, y);
                redData[(int) (color.getRed() * 255)]++;
                greenData[(int) (color.getGreen() * 255)]++;
                blueData[(int) (color.getBlue() * 255)]++;
            }
        }

        XYChart.Series<Number, Number> redSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> greenSeries = new XYChart.Series<>();
        XYChart.Series<Number, Number> blueSeries = new XYChart.Series<>();

        for (int i = 0; i < 256; i++) {
            redSeries.getData().add(new XYChart.Data<>(i, redData[i]));
            greenSeries.getData().add(new XYChart.Data<>(i, greenData[i]));
            blueSeries.getData().add(new XYChart.Data<>(i, blueData[i]));
        }

        histogram.getData().clear();
        histogram.getData().addAll(redSeries, greenSeries, blueSeries);
    }

    private void centerImage() {
        Image img = imageView.getImage();
        if (img != null) {
            double w = 0;
            double h = 0;

            double ratioX = imagePane.getWidth() / img.getWidth();
            double ratioY = imagePane.getHeight() / img.getHeight();

            double reducCoeff = 0;
            if (ratioX >= ratioY) {
                reducCoeff = ratioY;
            } else {
                reducCoeff = ratioX;
            }

            w = img.getWidth() * reducCoeff;
            h = img.getHeight() * reducCoeff;

            imageView.setX((imagePane.getWidth() - w) / 2);
            imageView.setY((imagePane.getHeight() - h) / 2);

            imageView.setFitWidth(w);
            imageView.setFitHeight(h);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}