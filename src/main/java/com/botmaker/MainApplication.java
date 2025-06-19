package com.botmaker;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import se.vidstige.jadb.JadbConnection;
import se.vidstige.jadb.JadbDevice;

import java.util.List;
import java.util.stream.Collectors;

public class MainApplication extends Application {

    @Override
    public void start(Stage stage) {
        // --- 1. Create the UI Components ---
        Label titleLabel = new Label("BotMaker Control Panel");
        titleLabel.setFont(new Font("System Bold", 24.0));

        Button javaCvButton = new Button("Test JavaCV (Open Webcam)");
        javaCvButton.setPrefWidth(250.0);

        Button jadbButton = new Button("Test JADB (List Devices)");
        jadbButton.setPrefWidth(250.0);

        Label statusLabel = new Label("Status: Idle");
        statusLabel.setWrapText(true);

        // --- 2. Create the Layout Container ---
        VBox rootLayout = new VBox();
        rootLayout.setAlignment(Pos.CENTER); // Center the content
        rootLayout.setSpacing(20.0); // Space between elements
        rootLayout.setPadding(new Insets(20, 20, 20, 20)); // Padding around the edges

        // --- 3. Add Components to the Layout ---
        rootLayout.getChildren().addAll(
                titleLabel,
                javaCvButton,
                jadbButton,
                statusLabel
        );

        // --- 4. Define the Actions (Event Handlers) ---
        // This is the equivalent of 'onAction="#onTestJavaCvClick"'
        javaCvButton.setOnAction(event -> {
            statusLabel.setText("Testing JavaCV... Opening webcam.");
            // Run on a background thread to not freeze the UI
            new Thread(() -> testJavaCv(statusLabel)).start();
        });

        jadbButton.setOnAction(event -> {
            statusLabel.setText("Testing JADB... Listing devices.");
            // Run on a background thread
            new Thread(() -> testJadb(statusLabel)).start();
        });


        // --- 5. Create and Show the Scene ---
        Scene scene = new Scene(rootLayout, 800, 600);
        stage.setTitle("BotMaker Control Panel");
        stage.setScene(scene);
        stage.show();
    }

    // --- Helper Methods for Logic ---
    // It's good practice to move the logic out of the lambda for clarity

    private void testJavaCv(Label statusLabel) {
        try (FrameGrabber grabber = new OpenCVFrameGrabber(0)) {
            grabber.start();
            CanvasFrame canvas = new CanvasFrame("Webcam Feed");
            canvas.setCanvasSize(grabber.getImageWidth(), grabber.getImageHeight());

            while (canvas.isVisible()) {
                Frame frame = grabber.grab();
                canvas.showImage(frame);
            }
            canvas.dispose();
            grabber.stop();
            Platform.runLater(() -> statusLabel.setText("Webcam closed."));
        } catch (FrameGrabber.Exception e) {
            Platform.runLater(() -> statusLabel.setText("Error: Could not start webcam."));
            e.printStackTrace();
        }
    }

    private void testJadb(Label statusLabel) {
        try {
            JadbConnection jadb = new JadbConnection();
            List<JadbDevice> devices = jadb.getDevices();

            String deviceList;
            if (devices.isEmpty()) {
                deviceList = "No ADB devices found.";
            } else {
                deviceList = "Found devices: " + devices.stream()
                        .map(JadbDevice::getSerial)
                        .collect(Collectors.joining(", "));
            }
            // Update the UI on the JavaFX Application Thread
            Platform.runLater(() -> statusLabel.setText(deviceList));

        } catch (Exception e) {
            Platform.runLater(() -> statusLabel.setText("Error connecting to ADB."));
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch();
    }
}