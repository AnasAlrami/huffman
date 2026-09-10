package application;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MainPanel {

    private TextField inputField   = new TextField();
    private Label     outputLabel  = new Label("Output: —");
    private Button browseInputBtn  = new Button("...");
    private Button clearBtn        = new Button("Clear");
    private Button compressBtn     = new Button("Compress");
    private Button decompressBtn   = new Button("Decompress");

    private Label origLabel  = new Label("-");
    private Label compLabel  = new Label("-");
    private Label savedLabel = new Label("-");
    private Label ratioLabel = new Label("-");
    private Label timeLabel  = new Label("-");

    private TableView<SymbolRecord> table = new TableView<>();
    private TextArea logArea    = new TextArea();
    private TextArea headerArea = new TextArea();

    private File   inputFile  = null;
    private String outputPath = "";
    private boolean decoding  = false;

    public MainPanel() {
        inputField.setEditable(false);
        inputField.setPromptText("No file chosen");
        logArea.setEditable(false);
        headerArea.setEditable(false);
        compressBtn.setDisable(true);
        decompressBtn.setDisable(true);

        browseInputBtn.setOnAction(e -> browseInput());
        compressBtn.setOnAction(e    -> runCompress());
        decompressBtn.setOnAction(e  -> runDecompress());
        clearBtn.setOnAction(e       -> clearAll());

        setupTable();
    }

    public BorderPane buildLayout() {
        BorderPane root = new BorderPane();

        // ── LEFT panel ───────────────────────────────────────────────────────
        Label fileLabel = new Label("Input File");
        HBox fileRow = new HBox(4, inputField, browseInputBtn, clearBtn);
        fileRow.setAlignment(Pos.CENTER_LEFT);
        inputField.setPrefWidth(220);

        outputLabel.setWrapText(true);

        compressBtn.setPrefWidth(Double.MAX_VALUE);
        decompressBtn.setPrefWidth(Double.MAX_VALUE);

        Separator s1 = new Separator();
        Separator s2 = new Separator();

        Label statsTitle = new Label("Results");
        statsTitle.setFont(new Font(13));

        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(8);
        statsGrid.setVgap(6);
        statsGrid.add(new Label("Original:"),   0, 0); statsGrid.add(origLabel,  1, 0);
        statsGrid.add(new Label("Compressed:"), 0, 1); statsGrid.add(compLabel,  1, 1);
        statsGrid.add(new Label("Saved:"),      0, 2); statsGrid.add(savedLabel, 1, 2);
        statsGrid.add(new Label("Ratio:"),      0, 3); statsGrid.add(ratioLabel, 1, 3);
        statsGrid.add(new Label("Time:"),       0, 4); statsGrid.add(timeLabel,  1, 4);

        VBox leftPanel = new VBox(10,
                fileLabel, fileRow,
                outputLabel,
                s1,
                compressBtn, decompressBtn,
                s2,
                statsTitle, statsGrid
        );
        leftPanel.setPadding(new Insets(14));
        leftPanel.setPrefWidth(270);

        // ── CENTER panel — table ─────────────────────────────────────────────
        Label tableTitle = new Label("Encoding Table");
        tableTitle.setFont(new Font(13));
        tableTitle.setPadding(new Insets(0, 0, 6, 0));

        VBox centerPanel = new VBox(6, tableTitle, table);
        centerPanel.setPadding(new Insets(14));
        VBox.setVgrow(table, Priority.ALWAYS);

        // ── BOTTOM panel — log + header side by side ─────────────────────────
        logArea.setPrefHeight(140);
        headerArea.setPrefHeight(140);
        logArea.setPromptText("Log output...");
        headerArea.setPromptText("Compressed file header...");

        Label logTitle    = new Label("Log");
        Label headerTitle = new Label("Header");

        VBox logBox    = new VBox(4, logTitle, logArea);
        VBox headerBox = new VBox(4, headerTitle, headerArea);
        HBox.setHgrow(logBox, Priority.ALWAYS);
        HBox.setHgrow(headerBox, Priority.ALWAYS);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        VBox.setVgrow(headerArea, Priority.ALWAYS);

        HBox bottomPanel = new HBox(10, logBox, headerBox);
        bottomPanel.setPadding(new Insets(10, 14, 14, 14));
        bottomPanel.setPrefHeight(175);

        // ── TOP bar ──────────────────────────────────────────────────────────
        Label title = new Label("Huffman Coding");
        title.setFont(new Font(20));

        HBox topBar = new HBox(title);
        topBar.setPadding(new Insets(12, 14, 12, 14));
        topBar.setAlignment(Pos.CENTER_LEFT);

        root.setTop(topBar);
        root.setLeft(leftPanel);
        root.setCenter(centerPanel);
        root.setBottom(bottomPanel);

        return root;
    }

    private void setupTable() {
        String[] titles = {"Char", "ASCII", "Frequency", "%", "Huffman Code", "Length"};
        String[] props  = {"charDisplay", "charCode", "occurrences", "ratio", "huffBits", "bitLength"};
        for (int i = 0; i < titles.length; i++) {
            TableColumn<SymbolRecord, ?> col = new TableColumn<>(titles[i]);
            col.setCellValueFactory(new PropertyValueFactory<>(props[i]));
            table.getColumns().add(col);
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(javafx.collections.FXCollections.observableArrayList());
    }

    private void clearAll() {
        inputFile  = null;
        outputPath = "";
        inputField.clear();
        inputField.setPromptText("No file chosen");
        outputLabel.setText("Output: —");
        compressBtn.setDisable(true);
        decompressBtn.setDisable(true);
        table.getItems().clear();
        logArea.clear();
        headerArea.clear();
        origLabel.setText("-");
        compLabel.setText("-");
        savedLabel.setText("-");
        ratioLabel.setText("-");
        timeLabel.setText("-");
    }

    private void browseInput() {
        FileChooser fc = new FileChooser();
        File f = fc.showOpenDialog(null);
        if (f == null) return;
        inputFile = f;
        inputField.setText(f.getName());
        int dot = f.getName().lastIndexOf('.');
        String ext = dot == -1 ? "" : f.getName().substring(dot);
        boolean isHuff = ext.equalsIgnoreCase(".huff");
        compressBtn.setDisable(isHuff);
        decompressBtn.setDisable(!isHuff);
        outputPath = f.getParent();
        outputLabel.setText("Output: " + outputPath);
    }

    private void runCompress() {
        decoding = false;
        log("Compressing: " + inputFile.getName());
        try {
            long t0 = System.nanoTime();
            HuffProcessor proc = new HuffProcessor();
            proc.encode(inputFile, outputPath);
            refreshTable(proc);
            headerArea.setText(proc.getMetadata());
            updateStats(inputFile.length(), proc.getEncodedSize(), System.nanoTime() - t0);
            log("Done.");
        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }

    private void runDecompress() {
        decoding = true;
        log("Decompressing: " + inputFile.getName());
        try {
            long t0 = System.nanoTime();
            HuffProcessor proc = new HuffProcessor();
            proc.decode(inputFile, outputPath);
            refreshTable(proc);
            headerArea.setText(proc.getMetadata());
            updateStats(proc.getOriginalSize(), inputFile.length(), System.nanoTime() - t0);
            log("Done.");
            log("Output file matches original file.");
        } catch (IOException e) {
            log("Error: " + e.getMessage());
        }
    }

    private void refreshTable(HuffProcessor proc) {
        table.getItems().clear();
        int[]    freq  = proc.getFrequencies();
        String[] codes = proc.getSymbolCodes();
        long total = 0;
        for (int f : freq) total += f;
        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                double pct  = (freq[i] * 100.0) / total;
                String code = codes[i] == null ? "" : codes[i];
                table.getItems().add(new SymbolRecord(
                        String.valueOf((char) i), freq[i],
                        String.format("%.2f%%", pct), code, code.length()));
            }
        }
    }

    private void updateStats(long orig, long comp, long ns) {
        double saved = orig > 0 ? Math.max(0, (orig - comp) * 100.0 / orig) : 0;
        double ratio = comp > 0 ? (double) orig / comp : 0;
        origLabel .setText(formatSize(orig));
        compLabel .setText(formatSize(comp));
        savedLabel.setText(String.format("%.2f%%", saved));
        ratioLabel.setText(String.format("%.2f : 1", ratio));
        timeLabel .setText(formatTime(ns));
    }

    private void log(String msg) {
        String ts = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logArea.appendText("[" + ts + "] " + msg + "\n");
        logArea.setScrollTop(Double.MAX_VALUE);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)        return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    public static String formatTime(long ns) {
        if (ns < 1_000_000)     return String.format("%.2f ms", ns / 1_000_000.0);
        if (ns < 1_000_000_000) return String.format("%.2f sec", ns / 1_000_000_000.0);
        return String.format("%.2f min", ns / 60_000_000_000.0);
    }
}