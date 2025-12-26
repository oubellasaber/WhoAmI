package org.example.testapp;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class ProgressDialog extends Stage {

    public ProgressDialog() {
        initStyle(StageStyle.UTILITY);
        initModality(Modality.APPLICATION_MODAL);
        setResizable(false);

        ProgressBar progressBar = new ProgressBar();
        progressBar.setPrefWidth(300);
        progressBar.setProgress(-1); // Indeterminate progress

        Label label = new Label("Uploading to Firestore...");
        label.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");

        VBox vbox = new VBox(10, label, progressBar);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: white; -fx-border-color: #ccc;");

        Scene scene = new Scene(vbox);
        setScene(scene);
    }
}