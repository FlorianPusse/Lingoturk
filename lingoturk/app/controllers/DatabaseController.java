package controllers;

import play.db.DB;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DatabaseController extends Controller {

    private enum VALUE_TYPE {TEXT, NOT_TEXT, NONE}

    private static Connection connection = DB.getConnection();

    public static Connection getConnection() {
        try {
            if (connection.isClosed()) {
                connection = DB.getConnection();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public static void saveErrorMessage(String message) {
        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO ErrorMessages(message) VALUES (?)");
            statement.setString(1, message);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static String[] getTableNames(Connection c) throws SQLException {
        List<String> tableNames = new LinkedList<>();

        DatabaseMetaData metaData = c.getMetaData();
        ResultSet tableResult = metaData.getTables(null, null, null, new String[]{"TABLE"});

        while (tableResult.next()) {
            tableNames.add(tableResult.getString("TABLE_NAME").toLowerCase());
        }

        tableResult.close();

        /**
         * Ordering of tables is important:
         * LingoExpModels -> Workers_areBlockedFor_LingoExpModels
         * LingoExpModels -> LingoExpModels_contain_Parts
         * Groups -> LingoExpModels_contain_Parts
         * Groups -> Parts_contain_Questions
         * Questions -> Parts_contain_Questions
         * LingoExpModels -> LingoExpModelPublishedAs
         * (with "A -> B" meaning: A is referenced by B
         * --> Do LingoExpModels, Groups, Questions, and then the rest
         **/

        List<String> specialTables = Arrays.asList(new String[]{"lingoexpmodels", "groups", "questions"});
        tableNames.removeAll(specialTables);
        tableNames.addAll(0, specialTables);

        return tableNames.toArray(new String[0]);
    }

    private static String[] getColumnNames(ResultSetMetaData metaData) throws SQLException {
        int nrColumns = metaData.getColumnCount();
        String[] columnNames = new String[nrColumns];

        for (int i = 1; i <= nrColumns; ++i) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }

        return columnNames;
    }

    private static String[] parseQueryValues(String query) {
        char[] queryAsArray = query.toCharArray();

        List<String> queryValues = new LinkedList<>();

        String currValue = "";
        VALUE_TYPE currType = VALUE_TYPE.NONE;
        for (int i = 0; i < queryAsArray.length; ++i) {
            char c = queryAsArray[i];

            if (currType != VALUE_TYPE.TEXT && Character.isWhitespace(c)) {
                continue;
            }

            if (currValue.isEmpty()) {
                if (c == '\'') {
                    currType = VALUE_TYPE.TEXT;
                } else if (c == 'E') {
                    if ((i + 1 < queryAsArray.length) && (queryAsArray[i + 1] == '\'')) {
                        currType = VALUE_TYPE.TEXT;
                        currValue += "E'";
                        ++i;
                        continue;
                    }
                } else {
                    currType = VALUE_TYPE.NOT_TEXT;
                }
                currValue += c;
            } else {
                if (c == ',') {
                    if (currType == VALUE_TYPE.NOT_TEXT) {
                        queryValues.add(currValue);
                        currValue = "";
                        currType = VALUE_TYPE.NONE;
                    } else if (currType == VALUE_TYPE.TEXT) {
                        currValue += c;
                    } else {
                        System.err.println("We shouldn't end up here actually.");
                    }
                } else if (c == '\'') {
                    if (currType == VALUE_TYPE.TEXT) {
                        if (i + 1 < queryAsArray.length) {
                            if (queryAsArray[i + 1] == '\'') {
                                currValue += "\'\'";
                                ++i;
                            } else {
                                queryValues.add(currValue + '\'');
                                currValue = "";
                                currType = VALUE_TYPE.NONE;
                                // expect a comma to come
                                if (queryAsArray[i + 1] == ',') {
                                    // skip the comma
                                    ++i;
                                } else {
                                    System.err.println("A comma is expected after the termination of a text value.");
                                }
                            }
                        } else {
                            // ' is last symbol -> just add the currValue + '\'' to the list of values and we are done
                            queryValues.add(currValue + '\'');
                            currValue = "";
                        }
                    } else {
                        System.err.println("We shouldn't end up here actually.");
                    }
                } else {
                    currValue += c;
                }
            }
        }

        if (!currValue.isEmpty()) {
            queryValues.add(currValue);
        }

        return queryValues.toArray(new String[0]);
    }

    public static void backupDatabase() {
        Connection c = DatabaseController.getConnection();

        try {
            File backupFile = new File("conf/backup.sql");
            BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile));

            // Retrieve all tables
            for (String tableName : getTableNames(c)) {

                // Ignore "play_evolutions"
                if (!tableName.toLowerCase().equals("play_evolutions")) {
                    // retrieve table data
                    Statement tableDataStatement = c.createStatement();
                    ResultSet tableDataResult = tableDataStatement.executeQuery("SELECT * FROM " + tableName);

                    // retrieve column names for this table
                    ResultSetMetaData tableMetaData = tableDataResult.getMetaData();
                    String[] columnNames = getColumnNames(tableMetaData);
                    int nrColumns = tableMetaData.getColumnCount();

                    while (tableDataResult.next()) {
                        String[] row = new String[nrColumns];
                        for (int i = 1; i <= nrColumns; ++i) {
                            int columnType = tableMetaData.getColumnType(i);

                            if ((columnType == Types.BOOLEAN || columnType == Types.BIT) && tableDataResult.getString(i) != null) {
                                if (tableDataResult.getString(i).equals("t")) {
                                    row[i - 1] = "true";
                                } else {
                                    row[i - 1] = "false";
                                }
                            } else if (columnType == Types.DOUBLE || columnType == Types.FLOAT
                                    || columnType == Types.INTEGER || columnType == Types.BIGINT || columnType == Types.NUMERIC
                                    || tableDataResult.getString(i) == null) {
                                row[i - 1] = tableDataResult.getString(i);
                            } else {
                                row[i - 1] = "E\'" + tableDataResult.getString(i).replace("'", "''").replace("\\","\\\\").replaceAll("(\r\n|\n)", "\\\\n") + '\'';
                            }
                        }
                        writer.write("INSERT INTO " + tableName + " (" + String.join(", ", columnNames) + ") VALUES (" + String.join(", ", row) + ");\n");
                    }

                    tableDataStatement.close();
                }
            }

            // Retrieve all sequences
            Statement s = c.createStatement();
            ResultSet sequenceResult = s.executeQuery("SELECT c.relname FROM pg_class c WHERE c.relkind = 'S';");
            while (sequenceResult.next()) {
                String sequenceName = sequenceResult.getString("relname");

                Statement sequenceStatement = c.createStatement();
                ResultSet sequenceValueResult = sequenceStatement.executeQuery("SELECT last_value FROM " + sequenceName);
                if (sequenceValueResult.next()) {
                    int sequenceValue = sequenceValueResult.getInt(1);
                    writer.write("SELECT pg_catalog.setval('" + sequenceName + "', " + sequenceValue + ", true);\n");
                }
                sequenceStatement.close();
            }
            s.close();
            writer.close();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void restoreDatabase(String data) throws SQLException {
        int endBeforeStatements = -1;
        int startAfterStatements = -1;

        String[] queries = data.split("\\r?\\n");

        int i = 0;
        for (String statement : queries) {
            if (statement.startsWith("INSERT INTO questions (")) {
                if (endBeforeStatements == -1) {
                    endBeforeStatements = i;
                }
            } else {
                if (endBeforeStatements != -1 && startAfterStatements == -1) {
                    startAfterStatements = i;
                }
            }
            ++i;
        }

        if(endBeforeStatements == -1){
            return;
        }

        // get column names in file
        String columns = queries[endBeforeStatements].substring(23);
        String[] columnNames = columns.substring(0, columns.indexOf(')')).split(", ");

        // get column names that currently exist
        Statement s = connection.createStatement();
        ResultSet questionResults = s.executeQuery("SELECT * FROM Questions LIMIT 1");
        ResultSetMetaData questionMetaData = questionResults.getMetaData();

        int nrColumns = questionMetaData.getColumnCount();
        LinkedList<String> actualColumns = new LinkedList<>();

        for (i = 1; i <= nrColumns; ++i) {
            actualColumns.add(questionMetaData.getColumnName(i));
        }

        boolean[] matchingColumnNames = new boolean[columnNames.length];
        for (i = 0; i < matchingColumnNames.length; ++i) {
            matchingColumnNames[i] = actualColumns.contains(columnNames[i]);
        }

        // Construct new insert query for question table that uses only the actually existing columns
        String constructedQuery = "";
        for (i = endBeforeStatements; i < startAfterStatements; ++i) {
            String query = queries[i].substring(queries[i].indexOf(") VALUES (") + 10).replace(");", "");
            String[] queryValues = parseQueryValues(query);

            if (queryValues.length != columnNames.length) {
                System.out.println(queries[i]);
            }

            StringBuilder tmpInsert = new StringBuilder("INSERT INTO questions (");
            for (int columnCounter = 0; columnCounter < matchingColumnNames.length; ++columnCounter) {
                if (matchingColumnNames[columnCounter]) {
                    tmpInsert.append(columnNames[columnCounter] + ", ");
                }
            }
            tmpInsert = new StringBuilder(tmpInsert.subSequence(0, tmpInsert.length() - 2));
            tmpInsert.append(") VALUES (");
            for (int columnCounter = 0; columnCounter < matchingColumnNames.length; ++columnCounter) {
                if (matchingColumnNames[columnCounter]) {
                    tmpInsert.append(queryValues[columnCounter] + ", ");
                }
            }
            tmpInsert = new StringBuilder(tmpInsert.subSequence(0, tmpInsert.length() - 2));
            tmpInsert.append(");\n");
            constructedQuery += tmpInsert.toString();
        }

        // Execute part before question insert  s
        for(String query : Arrays.copyOfRange(queries, 0, endBeforeStatements)){
            try{
                s.execute(query);
            }catch (SQLException sqle){
                System.out.println("[info] play - Couldn't execute action: \"" + sqle.getMessage() + "\" If you deleted an experiment type, this is completely normal.");
            }
        }

        // Execute question inserts
        s.execute(constructedQuery);

        // Execute part after question inserts
        for(String query : Arrays.copyOfRange(queries, startAfterStatements, queries.length)){
            try{
                s.execute(query);
            }catch (SQLException sqle){
                System.out.println("[info] play - Couldn't execute action: \"" + sqle.getMessage() + "\" If you deleted an experiment type, this is completely normal.");
            }
        }
    }

}
