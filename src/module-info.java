module GCPERF.GUI {
    requires java.sql;
    requires javafx.fxml;
    requires javafx.controls;
    requires gcperf.driver;
    requires matplotlib4j;
    opens hu.antalnagy.gcperf.gui;
}