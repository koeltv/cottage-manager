module com.koeltv.cottagemanager {
    requires javafx.controls;
    requires javafx.fxml;
    requires kotlin.stdlib;
    requires java.sql;
    requires exposed.core;
    requires exposed.java.time;
    requires exposed.dao;

    requires ch.qos.logback.classic;
    requires kotlin.reflect;
    requires com.github.librepdf.openpdf;
    requires java.desktop;

    opens com.koeltv.cottagemanager to javafx.fxml, javafx.base;
    opens com.koeltv.cottagemanager.data to javafx.fxml, javafx.base;
    exports com.koeltv.cottagemanager;
}