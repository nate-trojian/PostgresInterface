import com.typesafe.config.Config;
import com.typesafe.config.ConfigObject;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created by Nate on 4/20/16.
 */
public class PresetSelectController {
    private final Double COLUMN_WIDTH = 50d, CHARACTER_OFFSET = 10d;
    private List<Table> tables;
    private ArrayList<Preset> presets;
    private Preset curPreset;

    @FXML
    private BorderPane root;

    @FXML
    private ChoiceBox<Preset> queryBox;

    @FXML
    private HBox additionalParams;

    @FXML
    private Button submitButton;

    @FXML
    private ScrollPane tableScroll;

    @FXML
    private TableView<HashMap<String, Object>> outputTable;

    public PresetSelectController(List<Table> tables) {
        this.tables = tables;
        this.presets = new ArrayList<>();
        queryBox.setConverter(new Preset.PresetStringConverter(presets));
        curPreset = null;
    }

    @FXML
    public void initialize() {
        outputTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        outputTable.minWidthProperty().bind(root.widthProperty());
        outputTable.prefWidth(getWidth());
        tableScroll.setPrefViewportWidth(Main.WINDOW_WIDTH);
        submitButton.setOnAction(event -> query());
        addPresets();
        queryBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            curPreset = newValue;
            getPreset(curPreset);
        });
    }

    @SuppressWarnings("unchecked")
    private void addPresets() {
        //Database connect involves importing conf file which is done before we launch our app
        //So this is safe
        Config conf = ConfigUtil.getConf();
        List<ConfigObject> presets = (List<ConfigObject>)conf.getObjectList("presets");
        String desc, query;
        String[] classes;
        for(ConfigObject configObject: presets) {
            desc = configObject.get("description").unwrapped().toString();
            query = configObject.get("query").unwrapped().toString();
            if(configObject.containsKey("classes")) {
                classes = (String[])configObject.get("classes").unwrapped();
                addPreset(new Preset(desc, query, classes));
            } else {
                addPreset(new Preset(desc, query, new String[0]));
            }
        }
    }

    private void setUpTable(List<String> fields) {
        outputTable.getColumns().remove(0, outputTable.getColumns().size());
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
        try(PreparedStatement ps = Database.getPreparedStatement(curPreset.getQuery())) {
            int index = 1;
            ArrayList<Class> classes = curPreset.getClasses();
            for(Node n: additionalParams.getChildren()) {
                Object obj = Util.getValue(n);
                if(n instanceof TextField) {
                    obj = Util.castStr((String) obj, classes.get(index - 1));
                }
                ps.setObject(index, obj);
                index++;
            }
            ResultSet rs = ps.executeQuery();
            List<HashMap<String, Object>> results = formatResultSet(rs);
            outputTable.getItems().remove(0, outputTable.getItems().size());
            for(HashMap<String, Object> result: results) {
                outputTable.getItems().add(result);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }

    public void addPreset(Preset preset) {
        presets.add(preset);
    }

    public void getPreset(Preset preset) {
        String sql = preset.getQuery();
        ArrayList<Class> classes = preset.getClasses();
        setUpTable(Util.parseSQL(sql, tables));
        additionalParams.getChildren().remove(0, additionalParams.getChildren().size());
        outputTable.getItems().remove(0, outputTable.getItems().size());
        for(Class cl: classes) {
            if(cl == Date.class) {
                DatePicker datePicker = new DatePicker();
                additionalParams.getChildren().add(datePicker);
            } else if(cl == Integer.class) {
                TextField textField = new TextField();
                textField.setOnKeyTyped(event -> {
                    if(!event.getCharacter().matches("\\d"))
                        event.consume();
                });
                additionalParams.getChildren().add(textField);
            } else if(cl == String.class) {
                TextField textField = new TextField();
                additionalParams.getChildren().add(textField);
            } else if(cl == Boolean.class) {
                RadioButton radioButton = new RadioButton();
                additionalParams.getChildren().add(radioButton);
            }
        }
    }

    private List<HashMap<String, Object>> formatResultSet(ResultSet rs) {
        ArrayList<HashMap<String, Object>> ret = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int col = metaData.getColumnCount();
            String sql = curPreset.getQuery();
            List<String> fields = Util.parseSQL(sql, tables);
            while(rs.next()) {
                HashMap<String, Object> row = new HashMap<>();
                for(int i = 1; i <= col; i++) {
                    row.put(fields.get(i-1), rs.getObject(i));
                }
                ret.add(row);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
