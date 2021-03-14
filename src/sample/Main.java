package sample;

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
import javafx.stage.Stage;
import javafx.util.converter.NumberStringConverter;

import java.io.IOException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;

public class Main extends Application {
    LauncherParams launcherParams;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        final Group root = new Group();
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(10);
        gridPane.setHgap(10);
        gridPane.setPadding(new Insets(10, 10, 10, 10));

        final Label title = new Label("GC Analyzer");
        final Label appNameLabel = new Label("App Name: ");
        final Label numberOfRunsLabel = new Label("Number Of Runs: ");
        final Label initHeapIncrementLabel = new Label("Xms Increment (in Mbytes): ");
        final Label maxHeapIncrementLabel = new Label("Xmx Increment (in Mbytes): ");

        final CheckBox serial = new CheckBox(GCType.SERIAL.name());
        final CheckBox parallel = new CheckBox(GCType.PARALLEL.name());
        final CheckBox g1 = new CheckBox(GCType.G1.name());
        final CheckBox zgc = new CheckBox(GCType.ZGC.name());
        final CheckBox shenandoah = new CheckBox(GCType.SHENANDOAH.name());

        final TextField appName = new TextField();
        appName.setMaxWidth(300);
        final TextField numberOfRuns = new TextField();
        numberOfRuns.setMaxWidth(60);
        final TextField initHeapIncrement = new TextField();
        initHeapIncrement.setMaxWidth(60);
        final TextField maxHeapIncrement = new TextField();
        maxHeapIncrement.setMaxWidth(60);
        final NumberStringFilteredConverter converter = new NumberStringFilteredConverter();

        final TextFormatter<Number> numberOfRunFormatter = new TextFormatter<>(converter, 3, converter.getFilter());
        final TextFormatter<Number> initHeapIncrementFormatter = new TextFormatter<>(converter, 64, converter.getFilter());
        final TextFormatter<Number> maxHeapIncrementFormatter = new TextFormatter<>(converter, 256, converter.getFilter());

        final Button runGcAnalysisButton = new Button("Run GC Analysis");
        final Button addButton = new Button("Set Parameters");
        addButton.setOnAction(e -> {
            if ((appName.getText() == null || appName.getText().isEmpty())
            || (numberOfRuns.getText() == null || numberOfRuns.getText().isEmpty())
            || (initHeapIncrement.getText() == null || initHeapIncrement.getText().isEmpty())
            || (maxHeapIncrement.getText() == null || maxHeapIncrement.getText().isEmpty())) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Input fields empty");
                alert.setContentText("Please fill all input fields");
                alert.showAndWait();
            } else if(!serial.isSelected() && !parallel.isSelected() && !g1.isSelected() && !zgc.isSelected()
            && !shenandoah.isSelected()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("No GC selected");
                alert.setContentText("Please select at least one Garbage Collector to measure its performance");
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
                launcherParams = new LauncherParams(appName.getText(), Integer.parseInt(numberOfRuns.getText()),
                        Integer.parseInt(initHeapIncrement.getText()), Integer.parseInt(maxHeapIncrement.getText()),
                        gcTypes);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Parameters set");
                alert.setContentText("All parameters set, GC analysis is ready to be started!");
                alert.showAndWait();
            }
        });

        numberOfRuns.setTextFormatter(numberOfRunFormatter);
        initHeapIncrement.setTextFormatter(initHeapIncrementFormatter);
        maxHeapIncrement.setTextFormatter(maxHeapIncrementFormatter);
        title.setAlignment(Pos.TOP_LEFT);
        title.setTextFill(Color.GREEN);
        title.setFont(Font.font("Times New Roman", FontWeight.BOLD, 30));
        gridPane.add(title, 0, 0);
        gridPane.add(appNameLabel, 0, 1);
        gridPane.add(numberOfRunsLabel, 0, 2);
        gridPane.add(initHeapIncrementLabel, 0, 3);
        gridPane.add(maxHeapIncrementLabel, 0, 4);
        gridPane.add(appName, 1, 1);
        gridPane.add(numberOfRuns, 1, 2);
        gridPane.add(initHeapIncrement, 1, 3);
        gridPane.add(maxHeapIncrement, 1, 4);
        gridPane.add(serial, 1, 5);
        gridPane.add(parallel, 2, 5);
        gridPane.add(g1, 3, 5);
        gridPane.add(zgc, 4, 5);
        gridPane.add(shenandoah, 5, 5);
        gridPane.add(addButton, 1, 6);
        gridPane.add(runGcAnalysisButton, 1, 7);
        root.getChildren().add(gridPane);
        primaryStage.setTitle("Java GC Performance Analyzer");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        executor.execute(() -> runGcAnalysisButton.setOnAction(e -> {
            if(launcherParams == null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Start failed");
                alert.setContentText("Parameters were not set correctly.");
                alert.showAndWait();
            }
            else {
                try {
                    GCPerfDriver.launch(launcherParams.getAppName(), launcherParams.getNumOfRuns(), launcherParams.getInitHeapIncrementSize(),
                            launcherParams.getMaxHeapIncrementSize(), launcherParams.getGcTypes());
                } catch (IOException | PythonExecutionException ioException) {
                    ioException.printStackTrace();
                }
            }
        }));
    }
}
class NumberStringFilteredConverter extends NumberStringConverter {
    public UnaryOperator<TextFormatter.Change> getFilter() {
        return change -> {
            String newText = change.getControlNewText();
            if (newText.isEmpty()) {
                return change;
            }
            ParsePosition parsePosition = new ParsePosition(0);
            Object object = getNumberFormat().parse( newText, parsePosition );
            if ( object == null || parsePosition.getIndex() < newText.length()) {
                return null;
            } else {
                return change;
            }
        };
    }
}
class LauncherParams {
    private String appName;
    private int numOfRuns;
    private int initHeapIncrementSize;
    private int maxHeapIncrementSize;
    private List<GCType> gcTypes;

    public LauncherParams(){}

    public LauncherParams(String appName, int numOfRuns, int initHeapIncrementSize, int maxHeapIncrementSize, List<GCType> gcTypes) {
        this.appName = appName;
        this.numOfRuns = numOfRuns;
        this.initHeapIncrementSize = initHeapIncrementSize;
        this.maxHeapIncrementSize = maxHeapIncrementSize;
        this.gcTypes = gcTypes;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public int getNumOfRuns() {
        return numOfRuns;
    }

    public void setNumOfRuns(int numOfRuns) {
        this.numOfRuns = numOfRuns;
    }

    public int getInitHeapIncrementSize() {
        return initHeapIncrementSize;
    }

    public void setInitHeapIncrementSize(int initHeapIncrementSize) {
        this.initHeapIncrementSize = initHeapIncrementSize;
    }

    public int getMaxHeapIncrementSize() {
        return maxHeapIncrementSize;
    }

    public void setMaxHeapIncrementSize(int maxHeapIncrementSize) {
        this.maxHeapIncrementSize = maxHeapIncrementSize;
    }

    public List<GCType> getGcTypes() {
        return gcTypes;
    }

    public void setGcTypes(List<GCType> gcTypes) {
        this.gcTypes = gcTypes;
    }
}
