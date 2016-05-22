import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class MainController {
    @FXML
    private TabPane tabPane;

    @FXML
    private Tab presetTab;

    @FXML
    private TextField textField;

    @FXML
    public void initialize() {
        try {
            List<Table> tables = Database.getTables();
            for(Table table: tables) {
                TablePane tablePane = new TablePane(table);
                Tab t = new Tab(table.getTableName(), tablePane);
                tabPane.getTabs().add(t);
            }
            tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

            FXMLLoader presetLoader = new FXMLLoader(getClass().getResource("PresetSelectTemplate.fxml"));
            presetLoader.setController(new PresetSelectController(tables));
            presetTab.setContent(presetLoader.<Node>load());
        } catch(SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}
