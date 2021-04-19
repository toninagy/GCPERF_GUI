package hu.antalnagy.gcperf.gui;

import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import hu.antalnagy.gcperf.Analysis;
import hu.antalnagy.gcperf.GCType;
import hu.antalnagy.gcperf.driver.GCPerfDriver;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {
    private static final LauncherParams launcherParams = new LauncherParams();
    private static final GCPerfDriver gcPerfDriver = new GCPerfDriver();
    private static boolean correctParams = false;
    private static File appContainer;
    private static final AtomicBoolean error = new AtomicBoolean(false);

    public static void main(String[] args) {
        launch(args);
    }

    private static boolean setParams(File file, int numberOfRuns, int initHeap, int maxHeap, int initHeapIncrement,
                                     int maxHeapIncrement, List<GCType> gcTypes, List<Analysis.Metrics> metrics) {
        try {
            launcherParams.setFile(file);
            launcherParams.setNumOfRuns(numberOfRuns);
            launcherParams.setInitHeapSize(initHeap);
            launcherParams.setMaxHeapSize(maxHeap);
            launcherParams.setInitHeapIncrementSize(initHeapIncrement);
            launcherParams.setMaxHeapIncrementSize(maxHeapIncrement);
            launcherParams.setGcTypes(gcTypes);
            launcherParams.setMetrics(metrics);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static void setNumField(TextField numField) {
        numField.setMaxWidth(90);
        numField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                numField.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
    }

    private static void setDefaultCheckboxes(CheckBox checkbox, TextField numField, final int value) {
        checkbox.setOnAction(e -> {
            if (checkbox.isSelected()) {
                numField.clear();
                numField.setText(String.valueOf(value));
                numField.setEditable(false);
                numField.setStyle("-fx-control-inner-background: #e4e7ed;");
            } else {
                numField.setEditable(true);
                numField.setStyle("-fx-control-inner-background: white;");
            }
        });
    }

    @Override
    public void start(Stage primaryStage) {
        decorateGUI(primaryStage);
    }

    private void decorateGUI(Stage primaryStage) {
        final Group root = new Group();
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(24);
        gridPane.setHgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        final Label title = new Label("GC Analyzer");
        final Label browseLabel = new Label("Class File or Jar File: ");
        final Label selectedFileLabel = new Label("No File Selected");
        final Label numberOfRunsLabel = new Label("Number of Runs: ");
        final Label initHeapLabel = new Label("Initial Start Heap Size (Xms) in MB: ");
        final Label initMaxHeapLabel = new Label("Initial Maximum Heap Size (Xmx) in MB: ");
        final Label initHeapIncrementLabel = new Label("Xms Increment in MB: ");
        final Label maxHeapIncrementLabel = new Label("Xmx Increment in MB: ");
        final Label gcsLabel = new Label("Garbage Collectors: ");
        final Label metricsLabel = new Label("Metrics: ");
        final Label progressMessage = new Label("Waiting for analysis start ...");

        final CheckBox defaultInitHeapSize = new CheckBox("default");
        final CheckBox defaultInitMaxHeapSize = new CheckBox("default");
        final CheckBox defaultInitHeapIncrementSize = new CheckBox("default");
        final CheckBox defaultMaxHeapIncrementSize = new CheckBox("default");
        final CheckBox serial = new CheckBox(GCType.SERIAL.name());
        final CheckBox parallel = new CheckBox(GCType.PARALLEL.name());
        final CheckBox g1 = new CheckBox(GCType.G1.name());
        final CheckBox zgc = new CheckBox(GCType.ZGC.name());
        final CheckBox shenandoah = new CheckBox(GCType.SHENANDOAH.name());

        final CheckBox bestGCRuntime = new CheckBox("Best GC Runtime");
        final CheckBox avgGCRuntime = new CheckBox("Average GC Runtime");
        final CheckBox throughput = new CheckBox("Throughput");
        final CheckBox latency = new CheckBox("Latency");
        final CheckBox minorPauses = new CheckBox("No. of Minor Pauses");
        final CheckBox fullPauses = new CheckBox("No. of Full Pauses");

        final CheckBox exportToCSV = new CheckBox("Export Results to CSV");

        final FileChooser fileChooser = new FileChooser();
        final Button browseButton = new Button("Browse...");
        browseButton.setMinWidth(150);
        browseButton.setOnAction(e -> {
            appContainer = fileChooser.showOpenDialog(primaryStage);
            selectedFileLabel.setText(appContainer.getName());
        });

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefSize(180, 25);
        progressBar.setStyle("-fx-accent: #268de7");

        final TextField numberOfRuns = new TextField();
        setNumField(numberOfRuns);
        final TextField initHeap = new TextField();
        setNumField(initHeap);
        final TextField maxHeap = new TextField();
        setNumField(maxHeap);
        final TextField initHeapIncrement = new TextField();
        setNumField(initHeapIncrement);
        final TextField maxHeapIncrement = new TextField();
        setNumField(maxHeapIncrement);

        setDefaultCheckboxes(defaultInitHeapSize, initHeap, 4);
        setDefaultCheckboxes(defaultInitMaxHeapSize, maxHeap, 64);
        setDefaultCheckboxes(defaultInitHeapIncrementSize, initHeapIncrement, 128);
        setDefaultCheckboxes(defaultMaxHeapIncrementSize, maxHeapIncrement, 256);

        final Button runGcAnalysisButton = new Button("Run GC Analysis");
        final Button addButton = new Button("Set/Refresh Parameters");
        addButton.setOnAction(e -> {
            if ((numberOfRuns.getText() == null || numberOfRuns.getText().isEmpty())
                    || (initHeap.getText() == null || initHeap.getText().isEmpty())
                    || (maxHeap.getText() == null || maxHeap.getText().isEmpty())
                    || (initHeapIncrement.getText() == null || initHeapIncrement.getText().isEmpty())
                    || (maxHeapIncrement.getText() == null || maxHeapIncrement.getText().isEmpty())) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Setting/Refreshing Parameters Failed");
                alert.setContentText("Some input fields are empty\n" +
                        "Please fill in all input fields");
                alert.showAndWait();
            } else if (!serial.isSelected() && !parallel.isSelected() && !g1.isSelected() && !zgc.isSelected()
                    && !shenandoah.isSelected()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No GC selected");
                alert.setContentText("Please select at least one Garbage Collector to measure its performance\n" +
                        "Parameters were not updated");
                alert.showAndWait();
            } else if (!bestGCRuntime.isSelected() && !avgGCRuntime.isSelected() && !throughput.isSelected()
                    && !latency.isSelected() && !minorPauses.isSelected() && !fullPauses.isSelected()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No metric selected");
                alert.setContentText("Please select at least one metric to measure the performance on\n" +
                        "Parameters were not updated");
                alert.showAndWait();
            } else {
                List<GCType> gcTypes = new ArrayList<>();
                if (serial.isSelected()) {
                    gcTypes.add(GCType.SERIAL);
                }
                if (parallel.isSelected()) {
                    gcTypes.add(GCType.PARALLEL);
                }
                if (g1.isSelected()) {
                    gcTypes.add(GCType.G1);
                }
                if (zgc.isSelected()) {
                    gcTypes.add(GCType.ZGC);
                }
                if (shenandoah.isSelected()) {
                    gcTypes.add(GCType.SHENANDOAH);
                }
                List<Analysis.Metrics> metrics = new ArrayList<>();
                if (bestGCRuntime.isSelected()) {
                    metrics.add(Analysis.Metrics.BestGCRuntime);
                }
                if (avgGCRuntime.isSelected()) {
                    metrics.add(Analysis.Metrics.AvgGCRuntime);
                }
                if (throughput.isSelected()) {
                    metrics.add(Analysis.Metrics.Throughput);
                }
                if (latency.isSelected()) {
                    metrics.add(Analysis.Metrics.Latency);
                }
                if (minorPauses.isSelected()) {
                    metrics.add(Analysis.Metrics.MinorPauses);
                }
                if (fullPauses.isSelected()) {
                    metrics.add(Analysis.Metrics.FullPauses);
                }
                correctParams = setParams(appContainer, Integer.parseInt(numberOfRuns.getText()), Integer.parseInt(initHeap.getText()),
                        Integer.parseInt(maxHeap.getText()), Integer.parseInt(initHeapIncrement.getText()),
                        Integer.parseInt(maxHeapIncrement.getText()), gcTypes, metrics);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                if (correctParams) {
                    alert.setTitle("Parameters Set");
                    alert.setContentText("All parameters set, GC analysis is ready to be started");
                    Platform.runLater(() -> {
                        resetProgressBar(progressBar);
                        progressMessage.setText("Waiting for analysis start ...");
                    });
                } else {
                    alert.setTitle("Invalid Parameters");
                    alert.setContentText(launcherParams.getIllegalArgumentException().getMessage());
                }
                alert.showAndWait();
            }
        });

        title.setAlignment(Pos.TOP_LEFT);
        title.setTextFill(Color.GREEN);
        title.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));
        gridPane.add(title, 0, 0);
        gridPane.add(browseLabel, 0, 1);
        gridPane.add(selectedFileLabel, 2, 1);
        gridPane.add(numberOfRunsLabel, 0, 2);
        gridPane.add(initHeapLabel, 0, 3);
        gridPane.add(initMaxHeapLabel, 0, 4);
        gridPane.add(initHeapIncrementLabel, 0, 5);
        gridPane.add(maxHeapIncrementLabel, 0, 6);
        gridPane.add(gcsLabel, 0, 7);
        gridPane.add(browseButton, 1, 1);
        gridPane.add(numberOfRuns, 1, 2);
        gridPane.add(initHeap, 1, 3);
        gridPane.add(maxHeap, 1, 4);
        gridPane.add(initHeapIncrement, 1, 5);
        gridPane.add(maxHeapIncrement, 1, 6);
        gridPane.add(defaultInitHeapSize, 2, 3);
        gridPane.add(defaultInitMaxHeapSize, 2, 4);
        gridPane.add(defaultInitHeapIncrementSize, 2, 5);
        gridPane.add(defaultMaxHeapIncrementSize, 2, 6);
        gridPane.add(serial, 1, 7);
        gridPane.add(parallel, 2, 7);
        gridPane.add(g1, 3, 7);
        gridPane.add(zgc, 1, 8);
        gridPane.add(shenandoah, 2, 8);
        gridPane.add(addButton, 1, 12);
        gridPane.add(runGcAnalysisButton, 1, 14);
        gridPane.add(metricsLabel, 0, 9);
        gridPane.add(bestGCRuntime, 1, 9);
        gridPane.add(avgGCRuntime, 1, 10);
        gridPane.add(throughput, 2, 9);
        gridPane.add(latency, 2, 10);
        gridPane.add(minorPauses, 3, 9);
        gridPane.add(fullPauses, 3, 10);
        gridPane.add(exportToCSV, 2, 14);

        gridPane.add(progressBar, 1, 13);
        gridPane.add(progressMessage, 2, 13);

        TabPane tabPane = new TabPane();
        tabPane.setTabMinWidth(485);
        Tab mainTab = new Tab("GC Performance Analyzer");
        mainTab.setClosable(false);
        mainTab.setContent(gridPane);
        tabPane.getTabs().add(mainTab);
        Tab statisticsTab = new Tab("Statistics");
        GridPane statisticsGrid = new GridPane(); //TODO
        statisticsTab.setClosable(false);
        statisticsTab.setContent(statisticsGrid);
        tabPane.getTabs().add(statisticsTab);
        root.getChildren().add(tabPane);
        primaryStage.setTitle("Java GC Performance Analyzer");
        primaryStage.setScene(new Scene(root, 1024, 768));
        primaryStage.show();
        Service<Void> analysis = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        if (!correctParams) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Invalid Parameters");
                            alert.setContentText("Please set all parameters correctly");
                            alert.showAndWait();
                        } else {
                            try {
                                gcPerfDriver.launch(launcherParams.getFile(), launcherParams.getNumOfRuns(), launcherParams.getInitHeapSize(),
                                        launcherParams.getMaxHeapSize(), launcherParams.getInitHeapIncrementSize(),
                                        launcherParams.getMaxHeapIncrementSize(), launcherParams.getGcTypes(),
                                        launcherParams.getMetrics().toArray(Analysis.Metrics[]::new), exportToCSV.isSelected());
                            } catch (IOException | PythonExecutionException | InterruptedException exception) {
                                error.set(true);
                                exception.printStackTrace();
                            }
                        }
                        return null;
                    }
                };
            }
        };
        Service<Void> progressUpdate = new Service<>() {
            @Override
            protected Task<Void> createTask() {
                return new Task<>() {
                    @Override
                    protected Void call() {
                        sleep(200);
                        AtomicBoolean running = new AtomicBoolean(true);
                        while (running.get()) {
                            sleep(50);
                            Platform.runLater(() -> {
                                updateProgressBar(progressBar, progressMessage, gcPerfDriver.getProgress().getProgressLevel(),
                                        gcPerfDriver.getProgress().getProgressMessage());
                                if (gcPerfDriver.getProgress().isDone()) {
                                    updateProgressBar(progressBar, progressMessage, true);
                                    running.set(false);
                                    runGcAnalysisButton.setDisable(false);
                                } else if (gcPerfDriver.getProgress().isFailed() || error.get()) {
                                    updateProgressBar(progressBar, progressMessage, false);
                                    running.set(false);
                                    runGcAnalysisButton.setDisable(false);
                                }
                            });
                        }
                        return null;
                    }
                };
            }
        };

        runGcAnalysisButton.setOnAction(e -> {
            runGcAnalysisButton.setDisable(true);
            error.set(false);
            resetProgressBar(progressBar);
            analysis.restart();
            progressUpdate.restart();
        });

        primaryStage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateProgressBar(final ProgressBar progressBar, final Label progressMessage, double progress,
                                   String message) {
        progressBar.setProgress(progress);
        progressMessage.setText(message);
    }

    private void updateProgressBar(final ProgressBar progressBar, final Label progressMessage, boolean success) {
        if(success) {
            progressBar.setStyle("-fx-accent: #40bf15");
            progressMessage.setText("Analysis finished successfully!");
        }
        else {
            progressBar.setStyle("-fx-accent: #ef0606");
            progressMessage.setText("Analysis failed! Inspect the log for more details.");
        }
    }

    private void resetProgressBar(final ProgressBar progressBar) {
        progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progressBar.setStyle("-fx-accent: #268de7");
    }
}

class LauncherParams {
    private File file;
    private int numOfRuns;
    private int initHeapSize;
    private int maxHeapSize;
    private int initHeapIncrementSize;
    private int maxHeapIncrementSize;
    private List<GCType> gcTypes;
    private List<Analysis.Metrics> metrics;
    private IllegalArgumentException illegalArgumentException = null;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        if (file == null || (!file.getName().endsWith(".class") && !file.getName().endsWith(".jar"))) {
            illegalArgumentException = new IllegalArgumentException("Please select a Java application class file or a jar file");
            throw illegalArgumentException;
        }
        this.file = file;
    }

    public int getNumOfRuns() {
        return numOfRuns;
    }

    public void setNumOfRuns(int numOfRuns) {
        if (numOfRuns < 1 || 100 < numOfRuns) {
            illegalArgumentException = new IllegalArgumentException("Number of runs must be between 1 and 10");
            throw illegalArgumentException;
        }
        this.numOfRuns = numOfRuns;
    }

    public int getInitHeapSize() {
        return initHeapSize;
    }

    public void setInitHeapSize(int initHeapSize) {
        if (initHeapSize < 1 || 1999 < initHeapSize) {
            illegalArgumentException = new IllegalArgumentException("Initial heap size must be between 1MB and 1999MB");
            throw illegalArgumentException;
        }
        this.initHeapSize = initHeapSize;
    }

    public int getMaxHeapSize() {
        return maxHeapSize;
    }

    public void setMaxHeapSize(int maxHeapSize) {
        if (maxHeapSize < 16 || 1999 < maxHeapSize) {
            illegalArgumentException = new IllegalArgumentException("Maximum heap size must be between 16MB and 1999MB");
            throw illegalArgumentException;
        }
        this.maxHeapSize = maxHeapSize;
    }

    public int getInitHeapIncrementSize() {
        return initHeapIncrementSize;
    }

    public void setInitHeapIncrementSize(int initHeapIncrementSize) {
        if (initHeapIncrementSize < 1 || 999 < initHeapIncrementSize) {
            illegalArgumentException = new IllegalArgumentException("Initial heap increment size must be between 1MB and 999MB");
            throw illegalArgumentException;
        }
        this.initHeapIncrementSize = initHeapIncrementSize;
    }

    public int getMaxHeapIncrementSize() {
        return maxHeapIncrementSize;
    }

    public void setMaxHeapIncrementSize(int maxHeapIncrementSize) {
        if (maxHeapIncrementSize < 1 || 999 < maxHeapIncrementSize) {
            illegalArgumentException = new IllegalArgumentException("Maximum heap increment size must be between 1MB and 999MB");
            throw illegalArgumentException;
        }
        this.maxHeapIncrementSize = maxHeapIncrementSize;
    }

    public List<GCType> getGcTypes() {
        return gcTypes;
    }

    public void setGcTypes(List<GCType> gcTypes) {
        this.gcTypes = gcTypes;
    }

    public List<Analysis.Metrics> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Analysis.Metrics> metrics) {
        this.metrics = metrics;
    }

    public IllegalArgumentException getIllegalArgumentException() {
        return illegalArgumentException;
    }
}
