module com.botmaker { // You can name your module whatever you like, e.g., com.botmaker.botmakerapp

    // --- Standard JavaFX Dependencies ---
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // This one contains ObservableList and the scene graph
    requires javafx.base;     // Good to have explicitly

    // --- JavaCV / OpenCV Dependencies ---
    // The main JavaCV JAR is modular.
    requires org.bytedeco.javacv;
    // However, the underlying OpenCV JAR that javacv-platform pulls in is NOT modular.
    // We need a 'requires' directive for its automatic module.
    // The name is derived from the JAR name: opencv-4.9.0-1.5.10.jar -> opencv
    requires org.bytedeco.opencv;
    requires ddmlib;

    opens com.botmaker to javafx.fxml;

    exports com.botmaker;
}