import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    public static final Double WINDOW_WIDTH = 800d, WINDOW_HEIGHT = 600d;

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("Main.fxml"));
        primaryStage.setTitle("Project 3: Photography Studio");
        primaryStage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    public static void main(String[] args) {
        Database.connect();
        launch(args);
    }
}
