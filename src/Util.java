import javafx.scene.Node;
import javafx.scene.control.DatePicker;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nate on 4/20/16.
 */
public class Util {
    private static Pattern selectRegex = Pattern.compile("SELECT (?:DISTINCT )?(.*?)\\n", Pattern.MULTILINE);
    private static Pattern fromRegex = Pattern.compile("FROM (.*?)\\n", Pattern.MULTILINE);

    public static String[] splitArray(String str) {
        str = str.substring(1, str.length()-1).trim();
        return str.split(",");
    }

    public static Object getValue(Node node) {
        if(node instanceof TextField) {
            return ((TextField) node).getText();
        } else if(node instanceof DatePicker) {
            LocalDate date = ((DatePicker) node).getValue();
            if(date == null) return null;
            return Date.valueOf(date);
        } else if(node instanceof RadioButton) {
            return ((RadioButton) node).selectedProperty().getValue();
        }
        return node.getUserData();
    }

    public static List<String> parseSQL(String sql, List<Table> tables) {
        List<String> fields = new ArrayList<>();
        Matcher m = selectRegex.matcher(sql);
        if(m.find()) {
            String line = m.group(1);
            if(line.contains("*")) {
                if(line.equals("*")) {
                    m = fromRegex.matcher(sql);
                    if(m.find()) {
                        String tableName = m.group(1);
                        for(Table t : tables) {
                            if(t.getTableName().equalsIgnoreCase(tableName)) {
                                fields = t.getFields();
                            }
                        }
                    }
                } else {
                    List<String> temp = Arrays.asList(Util.splitArray("[" + line + "]"));
                    for(int i = 0; i < temp.size(); i++) {
                        String field = temp.get(i);
                        String[] split = field.split("\\.");
                        if(split.length > 1) {
                            String tableName = split[0];
                            for(Table t : tables) {
                                if(t.getTableName().equalsIgnoreCase(tableName)) {
                                    for(String s: t.getFields())
                                        fields.add(s);
                                }
                            }
                        }
                    }
                }
            } else {
                fields = Arrays.asList(Util.splitArray("[" + line + "]"));
            }
        }
        return fields;
    }

    public static Object castStr(String str, Class clazz) {
        if(clazz == String.class) {
            return str;
        } else if(clazz == Integer.class) {
            return Integer.parseInt(str);
        } else if(clazz == Double.class) {
            return Double.parseDouble(str);
        } else if(clazz == BigDecimal.class) {
            return new BigDecimal(str);
        } else if(clazz == Boolean.class) {
            if(str.equals("1"))
                return true;
            else if(str.equals("0"))
                return false;
            return Boolean.parseBoolean(str);
        } else if(clazz == Date.class) {
            return Date.valueOf(str);
        }
        return clazz.cast(str);
    }
}
