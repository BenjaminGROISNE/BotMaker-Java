package com.botmaker;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
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

        Button adbButton = new Button("List ADB Devices");
        adbButton.setPrefWidth(250.0);

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
                adbButton,
                statusLabel
        );

        // --- 4. Define the Actions (Event Handlers) ---
        // This is the equivalent of 'onAction="#onTestJavaCvClick"'
        javaCvButton.setOnAction(event -> {
            statusLabel.setText("Testing JavaCV... Opening webcam.");
            // Run on a background thread to not freeze the UI
            new Thread(() -> testJavaCv(statusLabel)).start();
        });

        adbButton.setOnAction(event -> {
            statusLabel.setText("Checking for ADB devices...");
            new Thread(() -> testAdbDevices(statusLabel)).start();
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

    private void testAdbDevices(Label statusLabel) {
        try {
            AndroidDebugBridge.initIfNeeded(false);
            AndroidDebugBridge bridge = AndroidDebugBridge.createBridge("adb", false);

            // Wait for initial device list (max 5s)
            int attempts = 0;
            while (!bridge.hasInitialDeviceList() && attempts++ < 10) {
                Thread.sleep(500);
            }

            IDevice[] devices = bridge.getDevices();

            String message;
            if (devices.length == 0) {
                message = "No ADB devices connected.";
            } else {
                message = "Connected ADB devices:\n" +
                        List.of(devices).stream()
                                .map(IDevice::getName)
                                .collect(Collectors.joining("\n"));
            }

            String finalMessage = message;
            Platform.runLater(() -> statusLabel.setText(finalMessage));
            AndroidDebugBridge.terminate();

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> statusLabel.setText("Error: " + e.getMessage()));
        }
    }

    public static void main(String[] args) {
        launch();
    }
}