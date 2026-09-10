package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;

public class Main extends Application {

    private static final int WINDOW_WIDTH  = 1260;
    private static final int WINDOW_HEIGHT = 800;

    // Stores the file path passed from Explorer (if any)
    private static String explorerFilePath = null;

    @Override
    public void start(Stage primaryStage) {

        // If launched from File Explorer with a file argument,
        // open the mini window instead of the full app
        if (explorerFilePath != null) {
            showMiniWindow(new File(explorerFilePath));
            return;
        }

        // Normal launch — open the full application
        MainPanel panel = new MainPanel();
        Scene scene = new Scene(panel.buildLayout(), WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("Huffman Coding");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    /**
     * Small popup window shown when the app is launched from File Explorer.
     * Just shows the file name and two buttons: Compress or Decompress.
     */
    private void showMiniWindow(File file) {
        Stage mini = new Stage();
        mini.setTitle("Huffman Coding");
        mini.setResizable(false);

        int dot = file.getName().lastIndexOf('.');
        String ext = dot == -1 ? "" : file.getName().substring(dot);
        boolean isHuff = ext.equalsIgnoreCase(".huff");

        Label fileLabel = new Label("File: " + file.getName());
        fileLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        Label statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12px;");
        statusLabel.setWrapText(true);

        TextArea logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(100);

        Button actionBtn = new Button(isHuff ? "Decompress" : "Compress");
        actionBtn.setPrefWidth(160);
        actionBtn.setStyle(isHuff
                ? "-fx-background-color: #7b7b7b; -fx-text-fill: white; -fx-font-weight: bold;"
                : "-fx-background-color: #4a90d9; -fx-text-fill: white; -fx-font-weight: bold;");

        Button closeBtn = new Button("Close");
        closeBtn.setPrefWidth(80);
        closeBtn.setOnAction(e -> mini.close());

        actionBtn.setOnAction(e -> {
            actionBtn.setDisable(true);
            statusLabel.setText("Processing...");
            new Thread(() -> {
                try {
                    HuffProcessor proc = new HuffProcessor();
                    String outputDir = file.getParent();

                    if (isHuff)
                        proc.decode(file, outputDir);
                    else
                        proc.encode(file, outputDir);

                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 12px;");
                        statusLabel.setText("Done! Saved to: " + outputDir);
                        logArea.appendText("Output: " + outputDir + "\n");
                        logArea.appendText("Original size:   " + formatSize(proc.getOriginalSize()) + "\n");
                        logArea.appendText("Processed size:  " + formatSize(proc.getEncodedSize()) + "\n");
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
                        statusLabel.setText("Error: " + ex.getMessage());
                    });
                }
            }).start();
        });

        VBox root = new VBox(12, fileLabel, actionBtn, statusLabel, logArea, closeBtn);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setStyle("-fx-background-color: white;");

        mini.setScene(new Scene(root, 380, 280));
        mini.show();
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    public static void main(String[] args) {
        // Only store the path if it's a non-empty argument
        if (args.length > 0 && !args[0].trim().isEmpty())
            explorerFilePath = args[0];
        launch(args);
    }
}