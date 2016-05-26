import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nate on 4/19/16.
 */
public class ConstraintHandler {
    private static Pattern foreignRegex = Pattern.compile("foreign (.*?)\\.(.*?)");
    private static Pattern typeCastRegex = Pattern.compile("\\(([^\\(]*?)\\)::(.+?\\b(\\[\\])?)");
    private static Pattern inArrayRegex = Pattern.compile("= ANY \\(ARRAY(.*?)\\)");

    public static String convert(String constraint) {
        Matcher m;
        MatchResult mr;

        // Auto increments?
        if(constraint.equals("auto increment"))
            return constraint;

        // Is a foreign key
        m = foreignRegex.matcher(constraint);
        if(m.matches()) {
            return constraint;
        }

        // Replace ('now'::text)::date with now()
        constraint = constraint.replaceAll("\\('now'::text\\)::date", "now()");

        //Now the general case
        m = typeCastRegex.matcher(constraint);
        while(m.find()) {
            mr = m.toMatchResult();
            constraint = constraint.substring(0, mr.start()) + mr.group(1) + constraint.substring(mr.end());
            m.reset(constraint);
        }

        // Reformat 'in array'
        m = inArrayRegex.matcher(constraint);
        while(m.find()) {
            mr = m.toMatchResult();
            constraint = constraint.substring(0, mr.start()) + "in " + mr.group(1) + constraint.substring(mr.end());
            m.reset(constraint);
        }

        // If I cared about types, this would be nice, but I don't so...
        constraint = constraint.replaceAll("::[^,\\]]+", "");
        // Remove parenthesis
        constraint = constraint.substring(1, constraint.length()-1);
        return constraint;
    }

    public static boolean valueMeetsConstraint(Object value, String constraint, Class clazz) {
        if(constraint.equals("") || constraint.equals("auto increment"))
            return true;

        Matcher m;
        m = foreignRegex.matcher(constraint);
        if(m.matches()) {
            String tableName = m.group(1);
            String fieldName = m.group(2);

            try(Statement stm = Database.getStatement()) {
                ResultSet rs = stm.executeQuery("SELECT " + fieldName +
                        " FROM " + tableName +
                        " where " + fieldName +
                        " = " + value);
                return rs.next();
            } catch(SQLException e) {
                e.printStackTrace();
            }
        }

        constraint = constraint.substring(constraint.indexOf(' ')+1);
        String operator = constraint.substring(0, constraint.indexOf(' '));
        constraint = constraint.substring(constraint.indexOf(' ')+1);
        //TODO: Move comparable checks to Util file
        switch(operator.toUpperCase()) {
            case "=":
                if(constraint.equals("now()")) {
                    Date now = Date.valueOf(LocalDate.now());
                    return Date.class.cast(value).equals(now);
                }
                return value.equals(Util.castStr(constraint, clazz));
            case ">":
                if(constraint.equals("now()")) {
                    Date now = Date.valueOf(LocalDate.now());
                    return Date.class.cast(value).after(now);
                }
                return ((Comparable) value).compareTo(Util.castStr(constraint, clazz)) > 0;
            case "<":
                if(constraint.equals("now()")) {
                    Date now = Date.valueOf(LocalDate.now());
                    return Date.class.cast(value).before(now);
                }
                return ((Comparable) value).compareTo(Util.castStr(constraint, clazz)) < 0;
            case ">=":
                if(constraint.equals("now()")) {
                    Date now = Date.valueOf(LocalDate.now());
                    return Date.class.cast(value).after(now) || Date.class.cast(value).equals(now);
                }
                return ((Comparable) value).compareTo(Util.castStr(constraint, clazz)) >= 0;
            case "<=":
                if(constraint.equals("now()")) {
                    Date now = Date.valueOf(LocalDate.now());
                    return Date.class.cast(value).before(now) || Date.class.cast(value).equals(now);
                }
                return ((Comparable) value).compareTo(Util.castStr(constraint, clazz)) <= 0;
            case "!=":
                if(constraint.equals("now()")) {
                    Date now = Date.valueOf(LocalDate.now());
                    return !Date.class.cast(value).equals(now);
                }
                return ((Comparable) value).compareTo(Util.castStr(constraint, clazz)) != 0;
            case "IN":
                String[] pos = Util.splitArray(constraint);
                for(String po : pos) {
                    if(value.equals(Util.castStr(po, clazz)))
                        return true;
                }
                return false;
        }
        return false;
    }
}
