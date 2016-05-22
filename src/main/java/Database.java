import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Nate on 4/19/16.
 */
public class Database {
    private static Connection con = null;

    private Database() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static boolean connect() {
        return connect("reference.conf");
    }

    public static boolean connect(String confFile) {
        if(con == null) {
            Config conf;
            try {
                conf = ConfigFactory.load(confFile);
                String host = conf.getString("database.host");
                String port = conf.getString("database.port");
                String dbName = conf.getString("database.name");
                String url = "jdbc:postgresql://" + host + (port == null? "":":"+port) + "/" + dbName;
                String dbUser = conf.getString("user.name");
                String dbPassword = conf.getString("user.password");
                con = DriverManager.getConnection(url, dbUser, dbPassword);
                return true;
            } catch(SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public static Statement getStatement() throws SQLException {
        if(con == null)
            throw new SQLException("No database connection");
        return con.createStatement();
    }

    public static PreparedStatement getPreparedStatement(String query) throws SQLException {
        if(con == null)
            throw new SQLException("No database connection");
        return con.prepareStatement(query);
    }

    public static List<Table> getTables() throws SQLException {
        if(con == null)
            throw new SQLException("No database connection");
        ResultSet rs;
        ArrayList<String> tableNames = new ArrayList<>();
        ArrayList<Table> ret = new ArrayList<>();
        try(Statement stm = Database.getStatement();
            //http://stackoverflow.com/questions/1152260/postgres-sql-to-list-table-foreign-keys
            PreparedStatement foreignKeyPS = Database.getPreparedStatement("SELECT " +
                    "kcu.column_name, " +
                    "ccu.table_name AS foreign_table_name, " +
                    "ccu.column_name AS foreign_column_name " +
                    "FROM " +
                    "information_schema.table_constraints AS tc " +
                    "JOIN information_schema.key_column_usage AS kcu " +
                    "ON tc.constraint_name = kcu.constraint_name " +
                    "JOIN information_schema.constraint_column_usage AS ccu " +
                    "ON ccu.constraint_name = tc.constraint_name " +
                    "WHERE constraint_type = 'FOREIGN KEY' AND tc.table_name=?");
            PreparedStatement constraintPS = Database.getPreparedStatement("SELECT con.consrc " +
                    "FROM " +
                    "pg_constraint AS con JOIN pg_class AS clazz " +
                    "ON con.conrelid = clazz.oid " +
                    "WHERE con.contype='c' and clazz.relname=?")
        ) {
            rs = stm.executeQuery("select distinct table_name\n" +
                    "from INFORMATION_SCHEMA.COLUMNS where table_schema = 'public'");
            while(rs.next()) {
                tableNames.add(rs.getString(1));
            }

            Table t;
            ResultSetMetaData metaData;
            for(String tableName: tableNames) {
                t = new Table(tableName);
                rs = stm.executeQuery("SELECT * FROM " + tableName + " LIMIT 1");
                metaData = rs.getMetaData();
                int col = metaData.getColumnCount();
                for(int i = 1; i <= col; i++) {
                    if(metaData.isAutoIncrement(i)) {
                        t.addField(metaData.getColumnName(i),
                                Class.forName(metaData.getColumnClassName(i)),
                                metaData.getPrecision(i),
                                "auto increment");
                    } else {
                        t.addField(metaData.getColumnName(i),
                                Class.forName(metaData.getColumnClassName(i)),
                                metaData.getPrecision(i));
                    }
                }
                foreignKeyPS.setString(1, tableName);
                rs = foreignKeyPS.executeQuery();
                while(rs.next()) {
                    t.addConstraint(rs.getString(1), "foreign " + rs.getString(2) + "." + rs.getString(3));
                }

                constraintPS.setString(1, tableName);
                rs = constraintPS.executeQuery();
                while(rs.next()) {
                    String row = rs.getString(1);
                    t.addConstraint(row.substring(0, row.indexOf(' ')), row);
                }

                foreignKeyPS.clearParameters();
                constraintPS.clearParameters();
                ret.add(t);
            }
            rs.close();
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
