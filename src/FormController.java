import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nate on 4/21/16.
 */
public class FormController {
    private Table table;
    private List<HBox> inputs;
    private boolean create;

    @FXML
    private GridPane root;

    private Button submitButton;

    public FormController(Table table) {
        this.table = table;
        this.inputs = new ArrayList<>();
        this.create = true;
    }

    public void setCreate(boolean create) {
        this.create = create;
        for(HBox box: inputs) {
            String field = ((Label)box.getChildren().get(0)).getText();
            boolean enabled = !create || !table.getConstraint(field).equals("auto increment");
            Node n = box.getChildren().get(1);
            if(n instanceof DatePicker)
                ((DatePicker) n).setEditable(enabled);
            else if(n instanceof TextField)
                ((TextField) n).setEditable(enabled);
            else if(n instanceof RadioButton)
                n.setDisable(!enabled);
        }
    }

    @FXML
    public void initialize() {
        List<String> fields = table.getFields();
        int row = 1;
        for(String field: fields) {
            HBox box = new HBox();
            box.setPadding(new Insets(0,0,0,2.0));
            box.setSpacing(2.0);
            Label fieldLabel = new Label(field);
            box.getChildren().add(fieldLabel);

            Class clazz = table.getFieldType(field);
            boolean enabled = !create || !table.getConstraint(field).equals("auto increment");
            if(clazz == Date.class) {
                DatePicker datePicker = new DatePicker();
                datePicker.setEditable(enabled);
                box.getChildren().add(datePicker);
            } else if(clazz == Integer.class || clazz == BigDecimal.class) {
                TextField textField = new TextField();
                textField.setOnKeyTyped(event -> {
                    if(!event.getCharacter().matches("\\d")) {
                        event.consume();
                    }
                });
                textField.setEditable(enabled);
                box.getChildren().add(textField);
            } else if(clazz == String.class) {
                TextField textField = new TextField();
                textField.setEditable(enabled);
                box.getChildren().add(textField);
            } else if(clazz == Boolean.class) {
                RadioButton radioButton = new RadioButton();
                radioButton.setDisable(!enabled);
                box.getChildren().add(radioButton);
            }
            this.inputs.add(box);
            root.add(box, 0, row++);
        }

        submitButton = new Button("Submit");
        submitButton.setOnAction(event -> {
            try(PreparedStatement ps = Database.getPreparedStatement(
                    (create?table.createCommand():table.updateCommand())
            )) {
                boolean submit = true;
                if(create) {
                    int index = 1;
                    for(HBox box: inputs) {
                        Label label = (Label)box.getChildren().get(0);
                        String fieldName = label.getText();
                        Node n = box.getChildren().get(1);
                        Object value = Util.getValue(n);
                        String constraint = table.getConstraint(fieldName);
                        Class clazz = table.getFieldType(fieldName);
                        if(n instanceof TextField && ((TextField)n).isEditable())
                            value = Util.castStr((String) value, clazz);
                        if(constraint.equals("auto increment"))
                            continue;
                        if(!ConstraintHandler.valueMeetsConstraint(value,
                                constraint,
                                clazz)) {
                            submit = false;
                            break;
                        }
                        ps.setObject(index, value);
                        index++;
                    }
                } else {
                    for(int i = 1; i < inputs.size(); i++) {
                        HBox box = inputs.get(i);
                        Label label = (Label)box.getChildren().get(0);
                        String fieldName = label.getText();
                        Node n = box.getChildren().get(1);
                        Object value = Util.getValue(n);
                        String constraint = table.getConstraint(fieldName);
                        Class clazz = table.getFieldType(fieldName);
                        if(n instanceof TextField && ((TextField)n).isEditable())
                            value = Util.castStr((String) value, clazz);
                        if(!ConstraintHandler.valueMeetsConstraint(value,
                                constraint,
                                clazz)) {
                            submit = false;
                            break;
                        }
                        ps.setObject(i, value);
                    }
                    if(submit) {
                        HBox box = inputs.get(0);
                        Label label = (Label)box.getChildren().get(0);
                        String fieldName = label.getText();
                        Node n = box.getChildren().get(1);
                        Object value = Util.getValue(n);
                        String constraint = table.getConstraint(fieldName);
                        Class clazz = table.getFieldType(fieldName);
                        if(n instanceof TextField && ((TextField)n).isEditable())
                            value = Util.castStr((String) value, clazz);
                        if(!ConstraintHandler.valueMeetsConstraint(value,
                                constraint,
                                clazz)) {
                            submit = false;
                        }
                        ps.setObject(inputs.size(), value);
                    }
                }
                if(submit)
                    ps.executeUpdate();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        });
        submitButton.setAlignment(Pos.BOTTOM_RIGHT);
        root.add(submitButton, 0, row);
        setCreate(true);
    }
}
