import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nate on 4/20/16.
 */
public class SelectController {
    private final Double COLUMN_WIDTH = 50d, CHARACTER_OFFSET = 10d;
    private Table table;
    private boolean retrieve;

    @FXML
    private BorderPane root;

    @FXML
    private HBox submitBox;

    @FXML
    private TextField queryField;

    @FXML
    private Button submitButton;

    private Button deleteButton;

    @FXML
    private ScrollPane tableScroll;

    @FXML
    private TableView<HashMap<String, Object>> outputTable;

    public SelectController(Table table) {
        this.table = table;
    }

    public void setRetrieve(boolean retrieve) {
        this.retrieve = retrieve;
        if(retrieve) {
            submitBox.getChildren().remove(deleteButton);
        } else {
            submitBox.getChildren().add(deleteButton);
        }
    }

    @FXML
    public void initialize() {
        setUpTable();
        outputTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        outputTable.minWidthProperty().bind(root.widthProperty());
        outputTable.prefWidth(getWidth());
        tableScroll.setPrefViewportWidth(Main.WINDOW_WIDTH);
        submitButton.setOnAction(event -> query());
        deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            for(TablePosition tablePosition : outputTable.getSelectionModel().getSelectedCells()) {
                try (PreparedStatement ps = Database.getPreparedStatement(table.deleteCommand())) {
                    String idField = table.getFields().get(0);
                    Object id = outputTable.getItems().get(tablePosition.getRow()).get(idField);
                    ps.setObject(1, id);
                    ps.executeUpdate();
                    outputTable.getItems().remove(tablePosition.getRow());
                } catch(SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void setUpTable() {
        outputTable.getColumns().remove(0, outputTable.getColumns().size());
        List<String> fields = table.getFields();
        for(String field: fields) {
            TableColumn<HashMap<String, Object>, Object> column = new TableColumn<>(field);
            column.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().get(field)));
            column.setMinWidth(COLUMN_WIDTH);
            column.setPrefWidth(field.length()*CHARACTER_OFFSET);
            outputTable.getColumns().add(column);
        }
    }

    private Double getWidth() {
        Double total = 0d;
        for(TableColumn col: outputTable.getColumns()) {
            total += col.getPrefWidth();
        }
        return total;
    }

    public void query() {
        try(Statement stm = Database.getStatement()) {
            String query = queryField.getText();
            if(query.equals(""))
                return;
            ResultSet rs = stm.executeQuery(query);
            List<HashMap<String, Object>> results = table.formatResultSet(rs);
            outputTable.getItems().remove(0, outputTable.getItems().size());
            for(HashMap<String, Object> result: results) {
                outputTable.getItems().add(result);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
}
