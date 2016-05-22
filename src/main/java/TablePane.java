import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.io.IOException;

/**
 * Created by Nate on 4/19/16.
 */
public class TablePane extends Pane {
    private Table table;
    private Parent root;
    private Parent select;
    private SelectController selectController;
    private Parent form;
    private FormController formController;
    private ToggleGroup processGroup;

    @FXML
    private RadioButton createButton;
    @FXML
    private RadioButton retrieveButton;
    @FXML
    private RadioButton updateButton;
    @FXML
    private RadioButton deleteButton;


    @FXML
    private VBox processBox;

    public TablePane(Table table) {
        this.table = table;
        init();
    }

    public Table getTable() {
        return table;
    }

    @FXML
    public void initialize() {
        processGroup = new ToggleGroup();
        processGroup.getToggles().addAll(createButton, retrieveButton, updateButton, deleteButton);
        processGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            processBox.getChildren().remove(0, processBox.getChildren().size());
            switch(((RadioButton) newValue).getText()) {
                case "Create":
                    formController.setCreate(true);
                    processBox.getChildren().add(form);
                    break;
                case "Retrieve":
                    selectController.setRetrieve(true);
                    processBox.getChildren().add(select);
                    break;
                case "Update":
                    formController.setCreate(false);
                    processBox.getChildren().add(form);
                    break;
                case "Delete":
                    selectController.setRetrieve(false);
                    processBox.getChildren().add(select);
                    break;
            }
        });
    }

    private void init() {
        try {
            FXMLLoader rootLoader = new FXMLLoader(getClass().getResource("TablePaneTemplate.fxml"));
            FXMLLoader formLoader = new FXMLLoader(getClass().getResource("FormTemplate.fxml"));
            FXMLLoader selectLoader = new FXMLLoader(getClass().getResource("SelectTemplate.fxml"));

            rootLoader.setController(this);
            formController = new FormController(table);
            formLoader.setController(formController);
            selectController = new SelectController(table);
            selectLoader.setController(selectController);

            root = rootLoader.load();
            select = selectLoader.load();
            form = formLoader.load();

            formController.setCreate(true);
            processBox.getChildren().add(form);

            this.getChildren().add(root);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}
