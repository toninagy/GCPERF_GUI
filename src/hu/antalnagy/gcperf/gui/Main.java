package hu.antalnagy.gcperf.gui;

import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import hu.antalnagy.gcperf.GCType;
import hu.antalnagy.gcperf.driver.GCPerfDriver;
import javafx.application.Application;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main extends Application {
    private static final LauncherParams launcherParams = new LauncherParams();
    private static final AtomicBoolean correctParams = new AtomicBoolean(false);
    private static File appContainer;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        final Group root = new Group();
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(20);
        gridPane.setHgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        final Label title = new Label("GC Analyzer");
        final Label browseLabel = new Label("Class File or Jar File: ");
        final Label numberOfRunsLabel = new Label("Number of Runs: ");
        final Label initHeapIncrementLabel = new Label("Xms Increment (in Mbytes): ");
        final Label maxHeapIncrementLabel = new Label("Xmx Increment (in Mbytes): ");
        final Label gcsLabel = new Label("Garbage Collectors: ");

        final CheckBox serial = new CheckBox(GCType.SERIAL.name());
        final CheckBox parallel = new CheckBox(GCType.PARALLEL.name());
        final CheckBox g1 = new CheckBox(GCType.G1.name());
        final CheckBox zgc = new CheckBox(GCType.ZGC.name());
        final CheckBox shenandoah = new CheckBox(GCType.SHENANDOAH.name());

        final FileChooser fileChooser = new FileChooser();
        final Button browseButton = new Button("Browse...");
        browseButton.setOnAction(e -> appContainer = fileChooser.showOpenDialog(primaryStage));

        final TextField numberOfRuns = new TextField();
        numberOfRuns.setMaxWidth(60);
        numberOfRuns.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                numberOfRuns.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        final TextField initHeapIncrement = new TextField();
        initHeapIncrement.setMaxWidth(60);
        initHeapIncrement.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                initHeapIncrement.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
        final TextField maxHeapIncrement = new TextField();
        maxHeapIncrement.setMaxWidth(60);
        maxHeapIncrement.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                maxHeapIncrement.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });

        final Button runGcAnalysisButton = new Button("Run GC Analysis");
        final Button addButton = new Button("Set/Refresh Parameters");
        addButton.setOnAction(e -> {
            if ((numberOfRuns.getText() == null || numberOfRuns.getText().isEmpty())
            || (initHeapIncrement.getText() == null || initHeapIncrement.getText().isEmpty())
            || (maxHeapIncrement.getText() == null || maxHeapIncrement.getText().isEmpty())) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Setting/Refreshing Parameters Failed");
                alert.setContentText("Some input fields are empty\n" +
                        "Please fill all input fields");
                alert.showAndWait();
            } else if(!serial.isSelected() && !parallel.isSelected() && !g1.isSelected() && !zgc.isSelected()
            && !shenandoah.isSelected()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No GC selected");
                alert.setContentText("Please select at least one Garbage Collector to measure its performance\n" +
                        "Parameters were not updated");
                alert.showAndWait();
            } else {
                List<GCType> gcTypes = new ArrayList<>();
                if(serial.isSelected()) {
                    gcTypes.add(GCType.SERIAL);
                }
                if(parallel.isSelected()) {
                    gcTypes.add(GCType.PARALLEL);
                }
                if(g1.isSelected()) {
                    gcTypes.add(GCType.G1);
                }
                if(zgc.isSelected()) {
                    gcTypes.add(GCType.ZGC);
                }
                if(shenandoah.isSelected()) {
                    gcTypes.add(GCType.SHENANDOAH);
                }
                correctParams.set(setParams(appContainer, Integer.parseInt(numberOfRuns.getText()), Integer.parseInt(initHeapIncrement.getText()),
                        Integer.parseInt(maxHeapIncrement.getText()), gcTypes));
                if(correctParams.get()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Parameters Set");
                    alert.setContentText("All parameters set, GC analysis is ready to be started");
                    alert.showAndWait();
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Invalid Parameters");
                    alert.setContentText(launcherParams.getIllegalArgumentException().getMessage());
                    alert.showAndWait();
                }
            }
        });

        title.setAlignment(Pos.TOP_LEFT);
        title.setTextFill(Color.GREEN);
        title.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));
        gridPane.add(title, 0, 0);
        gridPane.add(browseLabel, 0, 1);
        gridPane.add(numberOfRunsLabel, 0, 2);
        gridPane.add(initHeapIncrementLabel, 0, 3);
        gridPane.add(maxHeapIncrementLabel, 0, 4);
        gridPane.add(gcsLabel, 0, 5);
        gridPane.add(browseButton, 1, 1);
        gridPane.add(numberOfRuns, 1, 2);
        gridPane.add(initHeapIncrement, 1, 3);
        gridPane.add(maxHeapIncrement, 1, 4);
        gridPane.add(serial, 1, 5);
        gridPane.add(parallel, 1, 6);
        gridPane.add(g1, 1, 7);
        gridPane.add(zgc, 2, 5);
        gridPane.add(shenandoah, 2, 6);
        gridPane.add(addButton, 1, 8);
        gridPane.add(runGcAnalysisButton, 1, 9);
        root.getChildren().add(gridPane);
        primaryStage.setTitle("Java GC Performance Analyzer");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        executor.execute(() -> runGcAnalysisButton.setOnAction(e -> {
            if(!correctParams.get()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Invalid Parameters");
                alert.setContentText("Please set all parameters correctly");
                alert.showAndWait();
            }
            else {
                try {
                    GCPerfDriver.launch(launcherParams.getFile(), launcherParams.getNumOfRuns(),
                            launcherParams.getInitHeapIncrementSize(), launcherParams.getMaxHeapIncrementSize(),
                            launcherParams.getGcTypes());
                } catch (IOException | PythonExecutionException | InterruptedException ioException) {
                    ioException.printStackTrace();
                }
            }
        }));
    }

    private static boolean setParams(File file, int numberOfRuns, int initHeapIncrement, int maxHeapIncrement,
                                  List<GCType> gcTypes) {
        try {
            launcherParams.setFile(file);
            launcherParams.setNumOfRuns(numberOfRuns);
            launcherParams.setInitHeapIncrementSize(initHeapIncrement);
            launcherParams.setMaxHeapIncrementSize(maxHeapIncrement);
            launcherParams.setGcTypes(gcTypes);
            return true;
        } catch(IllegalArgumentException e) {
            return false;
        }
    }
}

class LauncherParams {
    private File file;
    private int numOfRuns;
    private int initHeapIncrementSize;
    private int maxHeapIncrementSize;
    private List<GCType> gcTypes;
    private IllegalArgumentException illegalArgumentException = null;

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        if(file == null || (!file.getName().endsWith(".class") && !file.getName().endsWith(".jar"))) {
            illegalArgumentException = new IllegalArgumentException("Please select a Java application class file or a jar file");
            throw illegalArgumentException;
        }
        this.file = file;
    }

    public int getNumOfRuns() {
        return numOfRuns;
    }

    public void setNumOfRuns(int numOfRuns) {
        if(numOfRuns < 1 || 100 < numOfRuns) {
            illegalArgumentException = new IllegalArgumentException("Number of runs must be between 1 and 10");
            throw illegalArgumentException;
        }
        this.numOfRuns = numOfRuns;
    }

    public int getInitHeapIncrementSize() {
        return initHeapIncrementSize;
    }

    public void setInitHeapIncrementSize(int initHeapIncrementSize) {
        if(initHeapIncrementSize < 1 || 999 < initHeapIncrementSize) {
            illegalArgumentException = new IllegalArgumentException("Initial heap increment size must be between 1MB and 999MB");
            throw illegalArgumentException;
        }
        this.initHeapIncrementSize = initHeapIncrementSize;
    }

    public int getMaxHeapIncrementSize() {
        return maxHeapIncrementSize;
    }

    public void setMaxHeapIncrementSize(int maxHeapIncrementSize) {
        if(maxHeapIncrementSize < 1 || 999 < maxHeapIncrementSize) {
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

    public IllegalArgumentException getIllegalArgumentException() {
        return illegalArgumentException;
    }
}
