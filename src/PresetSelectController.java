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
    private HashMap<String, List<Class>> preset;
    private HashMap<String, String> presetDescrip;

    @FXML
    private BorderPane root;

    @FXML
    private ChoiceBox<String> queryBox;

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
        this.preset = new HashMap<>();
        this.presetDescrip = new HashMap<>();
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
            String sql = presetDescrip.get(newValue);
            getPreset(sql, preset.get(sql));
        });
    }

    private void addPresets() {
        //Good
        addPreset("List All Upcoming Jobs",
                "SELECT jobID, date, eventType, locationName, primaryPhotographer, assistantPhotographer, clientRef\n" +
                        "FROM Job\n" +
                        "WHERE date > CURRENT_DATE");
        //Good
        addPreset("List All Jobs (past and present)",
                "SELECT *\n" +
                        "FROM Job\n" +
                        "ORDER BY date");
        //Good
        addPreset("List All Jobs for a certain day/timeframe",
                "SELECT *\n" +
                        "FROM Job\n" +
                        "WHERE ? <= date\n" +
                        "AND date <= ?", Date.class, Date.class);
        //Good
        addPreset("List all jobs which expire in a week or less that haven't been paid for",
                "SELECT *\n" +
                        "FROM Job\n" +
                        "WHERE balancePaid = 0.0\n" +
                        "AND date <= CURRENT_DATE + INTERVAL '7 days'");
        //Good
        addPreset("List All client by name within a certain city",
                "SELECT firstName, lastName\n" +
                        "FROM Client\n" +
                        "WHERE city = ?", String.class);
        //Good
        addPreset("List the full address of the client associated with a single job",
                "SELECT Client.addressLine1, Client.city, Client.state, Client.zipcode\n" +
                        "FROM Client, Job\n" +
                        "WHERE Job.clientRef = Client.clientID\n" +
                        "AND Job.jobID = ?", Integer.class);
        //Good
        addPreset("List All jobs and their clients",
                "SELECT Job.jobID, Job.date, Job.eventType, Job.locationName, Job.description," +
                        "Client.firstName, Client.lastName, Client.clientID\n" +
                "FROM Job, Client\n" +
                "WHERE Job.clientRef = Client.clientID");
        //Good
        addPreset("List All jobs for a client by name",
                "SELECT Job.jobID, Job.date, Job.eventType, Job.locationName, Job.description\n" +
                "FROM Job, Client\n" +
                "WHERE Job.clientRef = Client.clientID\n" +
                "AND Client.firstName = ?\n" +
                "AND Client.lastName = ?", String.class, String.class);
        //Good
        addPreset("List all OrderCarts for a client ID",
                "SELECT OrderCart.orderID, OrderCart.orderCost, OrderCart.totalPaid\n" +
                "FROM OrderCart, Job\n" +
                "WHERE OrderCart.orderJob = Job.jobID\n" +
                "AND Job.clientRef = ?", Integer.class);
        //Good
        addPreset("List the names and statuses of all photographers",
                "SELECT empID, lastName, firstName, status\n" +
                "FROM Photographer\n" +
                "ORDER BY lastName");
        //Good
        addPreset("List All names of full-time photographers",
                "SELECT empID, lastName, firstName\n" +
                "FROM Photographer\n" +
                "WHERE status = 'Full Time'\n" +
                "ORDER BY lastName");
        //Good
        addPreset("List All names of freelance photographers",
                "SELECT empID, lastName, firstName\n" +
                "FROM Photographer\n" +
                "WHERE status = 'Freelance'\n" +
                "ORDER BY lastName");
        //Good
        addPreset("List All Jobs for a photographer by empID",
                "SELECT Job.jobID, Job.date, Job.eventType, Job.locationName, Job.description\n" +
                "FROM Job, Photographer\n" +
                "WHERE (Job.primaryPhotographer = Photographer.empId\n" +
                "    OR Job.assistantPhotographer = Photographer.empID)\n" +
                "    AND Photographer.empID = ?", Integer.class);
        //Good
        addPreset("List All Available photographers in a time frame",
                "SELECT empID, firstName, lastName, status\n" +
                "FROM Photographer\n" +
                "EXCEPT\n" +
                "SELECT empID, firstName, lastName, status\n" +
                "FROM Photographer as P, \n" +
                "    (SELECT primaryPhotographer, assistantPhotographer\n" +
                "    FROM Job\n" +
                "    WHERE ? <= date\n" +
                "        AND date <= ?) as X\n" +
                "WHERE P.empID = X.primaryPhotographer\n" +
                "OR P.empID = X.assistantPhotographer", Date.class, Date.class);
        //Good
        addPreset("List All Available photographers for a date",
                "SELECT empID, firstName, lastName, status\n" +
                "FROM Photographer\n" +
                "EXCEPT\n" +
                "SELECT empID, firstName, lastName, status\n" +
                "FROM Photographer as P, \n" +
                "    (SELECT primaryPhotographer, assistantPhotographer\n" +
                "    FROM Job\n" +
                "    WHERE date = ?) as X\n" +
                "WHERE P.empID = X.primaryPhotographer\n" +
                "OR P.empID = X.assistantPhotographer", Date.class);
        //Good
        addPreset("List all photos whose expiration date is within the next week",
                "SELECT photoID\n" +
                "FROM Photo\n" +
                "WHERE expirationDate <= current_date + INTERVAL '7 days'");
        //Good
        addPreset("List Photos for a certain package X and # of copies of each",
                "SELECT Photo.photoID, PackageContains.copies\n" +
                "FROM PackageContains, Photo\n" +
                "WHERE PackageContains.photoNo=Photo.photoID AND PackageContains.packageNo=?", Integer.class);
        //Good
        addPreset("List all Photos from job X",
                "SELECT photoID, expirationDate\n" +
                "FROM Photo\n" +
                "WHERE jobRef=?", Integer.class);
        //Good
        addPreset("List all Photos in an Order X",
                "SELECT DISTINCT PackageContains.photoNo\n" +
                "FROM Package, PackageContains\n" +
                "WHERE PackageContains.packageNo=Package.packageID AND Package.orderNo=? ORDER BY PackageContains.photoNo", Integer.class);
        //Good
        addPreset("List All packages in an OrderCart X",
                "SELECT Package.*\n" +
                "FROM Package, OrderCart\n" +
                "WHERE OrderCart.orderID=? ORDER BY packageID", Integer.class);
        //Good
        addPreset("List all OrderCarts yet to be fulfilled",
                "SELECT *\n" +
                "FROM OrderCart\n" +
                "WHERE orderFulfilled is null\n" +
                "ORDER BY orderPlaced");
        //Good
        addPreset("List the number packages of type X that are part of any OrderCart",
                "SELECT count(type) as num_packages\n" +
                "FROM Package\n" +
                "WHERE type=?", Integer.class);
        //Good
        addPreset("Find fulfilled Orders that have remaining balance left to be paid",
                "SELECT *\n" +
                "FROM OrderCart\n" +
                "WHERE orderCost<>totalPaid AND orderfulfilled IS NOT null");
        //Good
        addPreset("List all OrderCarts for a given job X",
                "SELECT *\n" +
                "FROM OrderCart\n" +
                "WHERE orderJob=?", Integer.class);
        //Good
        addPreset("List all orders that in total cost more than $1,000",
                "SELECT orderNo, SUM(packageCost)\n" +
                "FROM Package\n" +
                "GROUP BY orderNo\n" +
                "HAVING SUM(packageCost) > 1000");
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
        try(PreparedStatement ps = Database.getPreparedStatement(presetDescrip.get(queryBox.getValue()))) {
            int index = 1;
            for(Node n: additionalParams.getChildren()) {
                Object obj = Util.getValue(n);
                if(n instanceof TextField) {
                    obj = Util.castStr((String)obj, preset.get(presetDescrip.get(queryBox.getValue())).get(index-1));
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

    public void addPreset(String descrip, String sql, Class... clazz) {
        presetDescrip.put(descrip, sql);
        preset.put(sql, Arrays.asList(clazz));
        queryBox.getItems().add(descrip);
    }

    public void getPreset(String sql, List<Class> clazz) {
        setUpTable(Util.parseSQL(sql, tables));
        additionalParams.getChildren().remove(0, additionalParams.getChildren().size());
        outputTable.getItems().remove(0, outputTable.getItems().size());
        for(Class cl: clazz) {
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
            String sql = presetDescrip.get(queryBox.getValue());
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
