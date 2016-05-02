import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Nate on 4/19/16.
 */
public class Table {
    private String tableName;
    private ArrayList<String> fields;
    private HashMap<String, Class> classes;
    private HashMap<String, Integer> precisions;
    private HashMap<String, String> constraints;
    private String paramString, insertString;

    public Table(String tName) {
        tableName = tName;
        fields = new ArrayList<>();
        classes = new HashMap<>();
        precisions = new HashMap<>();
        constraints = new HashMap<>();
        paramString = "()";
        insertString = "()";
    }

    public void addField(String fieldName, Class type) {
        fields.add(fieldName);
        classes.put(fieldName, type);
        addToParamString(fieldName);
    }

    public void addField(String fieldName, Class type, Integer precision) {
        if(precision != 0)
            precisions.put(fieldName, precision);
        fields.add(fieldName);
        classes.put(fieldName, type);
        addToParamString(fieldName);
    }

    public void addField(String fieldName, Class type, Integer precision, String constraint) {
        if(precision != 0)
            precisions.put(fieldName, precision);
        fields.add(fieldName);
        classes.put(fieldName, type);
        addConstraint(fieldName, constraint);
        if(!constraint.equals("auto increment")) {
            addToParamString(fieldName);
        }
    }

    private void addToParamString(String fieldName) {
        if(paramString.length() == 2) {
            paramString = "(" + fieldName + ")";
            insertString = "(?)";
        } else {
            paramString = paramString.substring(0, paramString.length()-1) + "," + fieldName + ")";
            insertString = insertString.substring(0, insertString.length()-1) + ",?)";
        }
    }

    public void addConstraint(String fieldName, String constraint) {
        if(fields.contains(fieldName))
            constraints.put(fieldName, ConstraintHandler.convert(constraint));
    }

    public String createCommand() {
        return "INSERT into " + tableName + " " + paramString + " VALUES " + insertString;
    }

    public String updateCommand() {
        String ret = "UPDATE " + tableName + " set ";
        for(int i = 1; i < fields.size()-1; i++) {
            ret += fields.get(i) + " = ?, ";
        }
        ret += fields.get(fields.size()-1) + " = ? ";
        ret += "where " + fields.get(0) + " = ?";

        return ret;
    }

    public String deleteCommand() {
        String ret = "DELETE from " + tableName + " where " + fields.get(0) + " = ?";
        return ret;
    }

    public List<String> getFields() {
        return fields;
    }

    public String getTableName() {
        return tableName;
    }

    public Class getFieldType(String field) {
        return classes.getOrDefault(field, Object.class);
    }

    public String getConstraint(String field) {
        return constraints.getOrDefault(field, "");
    }

    public List<HashMap<String, Object>> formatResultSet(ResultSet rs) {
        ArrayList<HashMap<String, Object>> ret = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int col = metaData.getColumnCount();
            while(rs.next()) {
                HashMap<String, Object> obj = new HashMap<>();
                for(int i = 1; i <= col; i++) {
                    String fieldName = metaData.getColumnName(i);
                    obj.put(fieldName, classes.get(fieldName).cast(rs.getObject(i)));
                }
                ret.add(obj);
            }
        } catch(SQLException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
