package sample;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;

import java.io.IOException;

public class Controller {
    @FXML
    public ProgressBar progressBar;
    public Label elapsedTime;
    public Label remainingTime;

    public Label currentSpeed;
    public Label averageSped;
}
