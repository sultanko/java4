package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class Main extends Application {

    private static String destination;
    private static String source;
    private static final int TIME_CHECK_MILIS = 1000;

    private final FileWalker fileWalker = new FileWalker();
    private final Timer timer = new Timer();

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("sample.fxml"));
        Parent root = fxmlLoader.load();
        final Controller controller = fxmlLoader.getController();
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(new Scene(root, 450, 250));
        primaryStage.show();
        final long startTime = System.currentTimeMillis();
        final AtomicLong lastTime = new AtomicLong(System.currentTimeMillis());
        final AtomicLong readedBytes = new AtomicLong(0);

        try {
            fileWalker.copyDirs(source, destination);
        } catch (IOException e) {
            e.printStackTrace();
        }

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = (currentTime - startTime)/1000;
                long currentTimeElapsedSec = (currentTime - lastTime.get())/1000;
                double currentSpeed = (fileWalker.getCopiedBytest() - readedBytes.get())
                        /(1.0*currentTimeElapsedSec) / (1024.0*1024.0);
                double averageSpeed = (fileWalker.getCopiedBytest())
                        /(1.0*elapsedTime) / (1024.0*1024.0);
                long remainingTime =  Math.max(
                        (long) ((fileWalker.getTotalSize() - fileWalker.getCopiedBytest())/
                                (averageSpeed*1024*1024))
                        , 0);
                lastTime.set(currentTime);
                readedBytes.set(fileWalker.getCopiedBytest());
                Platform.runLater(() -> {
                    controller.progressBar.setProgress(fileWalker.getPercent());
                    controller.elapsedTime.setText(String.format("%d m:%d s", elapsedTime / 60, elapsedTime % 60));
                    controller.currentSpeed.setText(String.format("%.2f MiB/s", currentSpeed));
                    controller.averageSped.setText(String.format("%.2f MiB/s", averageSpeed));
                    controller.remainingTime.setText(String.format("%dm:%ds", remainingTime / 60, remainingTime % 60));
                });
                if (fileWalker.getCopiedBytest() >= fileWalker.getTotalSize()) {
                    cancel();
                }
            }
        }, TIME_CHECK_MILIS, TIME_CHECK_MILIS);
    }

    @Override
    public void stop() throws Exception {
        fileWalker.close();
        timer.cancel();
        super.stop();
    }

    public static void main(String[] args) {
        source = args[0];
        destination = args[1];
        launch(args);
    }
}
