package services;

import models.Groups.AbstractGroup;
import models.LingoExpModel;
import play.db.Database;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.*;
import java.nio.file.Files;
import java.sql.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Implementation of the database service. Provides access to the database and database
 * related functions to the user
 */
public class DatabaseServiceImplementation extends Controller implements DatabaseService {

    /**
     * Possible types of variables we read.
     */
    private enum VALUE_TYPE {
        /**
         * The variable has text type
         */
        TEXT,

        /**
         * The variable has no text type (can be anything form integer to double or numeric
         */
        NOT_TEXT,

        /**
         * No type has been assigned yet
         */
        NONE
    }

    /**
     * The database that stores all of the information related to LingoTurk
     */
    private static Database db;

    /**
     * The connection to the database
     */
    private static Connection connection;

    /**
     * The currently used LingoTurk config
     */
    private LingoturkConfig lingoturkConfig;

    @Inject
    public DatabaseServiceImplementation(Database db, LingoturkConfig lingoturkConfig) {
        DatabaseServiceImplementation.db = db;
        connection = DatabaseServiceImplementation.db.getConnection();
        this.lingoturkConfig = lingoturkConfig;
    }

    /**
     * Closes the current connection, if any exists.
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                try {
                    connection.close();
                } catch (SQLException e1) {
                }
            }
        }
    }

    /**
     * Counts the number of experiments currently stored
     *
     * @return The count of experiments
     * @throws SQLException Propagated from JDBC
     */
    public int countExperiments() throws SQLException {
        Statement s = getConnection().createStatement();
        ResultSet resultSet = s.executeQuery("SELECT count(*) FROM LingoExpModels");
        int result = -1;
        if (resultSet.next()) {
            result = resultSet.getInt(1);
        }
        resultSet.close();
        s.close();
        return result;
    }

    /**
     * Retrieves the current open connection. If no connection is open,
     * a new one is opened.
     *
     * @return The connection
     */
    public Connection getConnection() {
        try {
            if (connection.isClosed()) {
                connection = db.getConnection();
            }
        } catch (SQLException e) {
            connection = db.getConnection();
        }
        return connection;
    }

    /**
     * Retrieves the current open conncetion form a static context. If no connection is open,
     * a new one is opened. This might be necessary, as model objects can't be injected with
     * the DatabaseService directly.
     *
     * @return The connection
     */
    public static Connection staticConnection() {
        try {
            if (connection.isClosed()) {
                connection = db.getConnection();
            }
        } catch (SQLException e) {
            connection = db.getConnection();
        }
        return connection;
    }

    /**
     * Retrieves an array of all tables existing in the database. Names will be all lower case
     *
     * @param c The connection to the database
     * @return The array of table names
     * @throws SQLException Propagated from JDBC
     */
    public String[] getTableNames(Connection c) throws SQLException {
        List<String> tableNames = new LinkedList<>();

        DatabaseMetaData metaData = c.getMetaData();
        ResultSet tableResult = metaData.getTables(null, null, null, new String[]{"TABLE"});

        while (tableResult.next()) {
            tableNames.add(tableResult.getString("TABLE_NAME").toLowerCase());
        }

        tableResult.close();

        /*
         * Ordering of tables is important:
         * LingoExpModels -> Workers_areBlockedFor_LingoExpModels
         * LingoExpModels -> LingoExpModels_contain_Parts
         * Groups -> LingoExpModels_contain_Parts
         * Groups -> Parts_contain_Questions
         * Questions -> Parts_contain_Questions
         * LingoExpModels -> LingoExpModelPublishedAs
         * (with "A -> B" meaning: A is referenced by B
         * --> Do LingoExpModels, Groups, Questions, and then the rest
         */

        List<String> specialTables = Arrays.asList(new String[]{"lingoexpmodels", "groups", "questions"});
        tableNames.removeAll(specialTables);
        tableNames.addAll(0, specialTables);

        return tableNames.toArray(new String[0]);
    }

    /**
     * Checks whether the database schema has been created yet.
     *
     * @param c The connection to the database
     * @return true iff the database schema is already existent
     * @throws SQLException Propagated from JDBC
     */
    public boolean isDatabaseCreated(Connection c) throws SQLException {
        Statement s = c.createStatement();
        ResultSet resultSet = s.executeQuery("SELECT EXISTS (\n" +
                "   SELECT 1\n" +
                "   FROM   information_schema.tables \n" +
                "   WHERE  table_schema = 'public'\n" +
                "   AND    table_name = 'lingoexpmodels'\n" +
                "   );");

        boolean exists = false;
        if (resultSet.next()) {
            exists = resultSet.getBoolean(1);
        }
        resultSet.clearWarnings();
        s.close();

        return exists;
    }

    /**
     * Looks up the column names for a table given its {@code ResultSetMetaData}
     *
     * @param metaData The meta data of the table
     * @return The array of column names
     * @throws SQLException Propagated from JDBC
     */
    public String[] getColumnNames(ResultSetMetaData metaData) throws SQLException {
        int nrColumns = metaData.getColumnCount();
        String[] columnNames = new String[nrColumns];

        for (int i = 1; i <= nrColumns; ++i) {
            columnNames[i - 1] = metaData.getColumnName(i);
        }

        return columnNames;
    }

    /**
     * Parses an array of query values from the textual representation
     *
     * @param query The textual representation of the query
     * @return The parsed values
     */
    public String[] parseQueryValues(String query) {
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

    /**
     * Create a backup of the database using the default connection
     *
     * @return The result containing the info whether the backup was successful or not
     */
    public Result backupDatabase() {
        Connection c = getConnection();
        return backupDatabase(c);
    }

    /**
     * Create a backup of the database using the connection c. The backup is represented in a plain
     * SQL format consisting only of INSERT statements for the data-part of the database.
     *
     * @param c The connection to use for the backup
     * @return The result containing the info whether the backup was successful or not
     */
    public Result backupDatabase(Connection c) {
        Statement tableDataStatement = null;
        ResultSet tableDataResult = null;

        try {
            if (!isDatabaseCreated(c)) {
                return ok("Database not yet created. Nothing to do here.");
            }

            tableDataStatement = c.createStatement();
            tableDataResult = tableDataStatement.executeQuery("SELECT count(*) FROM LingoExpModels");
            if (!tableDataResult.next() || tableDataResult.getInt(1) == 0) {
                tableDataResult.close();
                tableDataStatement.close();
                return ok("No entries yet. Nothing to back up");
            }
            tableDataResult.close();
            tableDataStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerError("Could not back up database. Check the logs.");
        } finally {
            if (tableDataResult != null) try {
                tableDataResult.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (tableDataStatement != null) try {
                tableDataStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            File f = new File(lingoturkConfig.getPathPrefix() + "backup/");
            if (!f.exists()) {
                if (!f.mkdir()) {
                    return internalServerError("Could not create directory 'backup/'");
                }
            }

            File backupFile = new File(lingoturkConfig.getPathPrefix() + "backup/backup.sql_tmp");
            BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile));

            // Retrieve all tables
            for (String tableName : getTableNames(c)) {

                // Ignore "play_evolutions"
                if (!tableName.toLowerCase().equals("play_evolutions")) {
                    // retrieve table data
                    tableDataStatement = c.createStatement();
                    try {
                        tableDataResult = tableDataStatement.executeQuery("SELECT * FROM " + tableName);
                    } catch (SQLException e) {
                        continue;
                    }

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
                                row[i - 1] = "E\'" + tableDataResult.getString(i).replace("'", "''").replace("\\", "\\\\").replaceAll("(\r\n|\n)", "\\\\n") + '\'';
                            }
                        }
                        writer.write("INSERT INTO " + tableName + " (" + String.join(", ", columnNames) + ") VALUES (" + String.join(", ", row) + ");\n");
                    }

                    tableDataResult.close();
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
                sequenceValueResult.close();
                sequenceStatement.close();
            }
            sequenceResult.close();
            s.close();
            writer.close();

            File backupFile_renamed = new File(lingoturkConfig.getPathPrefix() + "backup/backup.sql");

            Files.move(backupFile.toPath(), backupFile_renamed.toPath(), REPLACE_EXISTING);

            System.out.println("[info] play - Backup successfully created.");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        } finally {
            if (tableDataStatement != null) try {
                tableDataStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return ok();
    }

    /**
     * Restores the database from a given backup {@code backupFile}. If the file does not exist,
     * a FileNotFound exception is thrown.
     *
     * @param backupFile The file containing the backup
     * @throws SQLException Propagated from JDBC
     * @throws IOException  If the file does not exist, or could not be read.
     */
    public void restoreDatabase(File backupFile) throws SQLException, IOException {
        int endBeforeStatements = -1;
        int startAfterStatements = -1;
        int i = 0;
        String firstQuestionInsert = null;

        // we need to check the file once in order to find the positions of the different types of SQL statements
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(backupFile)));
        for (String line; (line = br.readLine()) != null; ) {
            if (line.startsWith("INSERT INTO questions (")) {
                if (endBeforeStatements == -1) {
                    endBeforeStatements = i;
                    firstQuestionInsert = line;
                }
            } else {
                if (endBeforeStatements != -1) {
                    startAfterStatements = i;
                    break;
                }
            }
            ++i;
        }

        if (endBeforeStatements == -1) {
            return;
        }

        // get column names in file
        String columns = firstQuestionInsert.substring(23);
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

        // Create new insert
        StringBuilder tmpInsert = new StringBuilder("INSERT INTO questions (");
        List<String> tmpColumnNames = new LinkedList<>();
        for (int columnCounter = 0; columnCounter < matchingColumnNames.length; ++columnCounter) {
            if (matchingColumnNames[columnCounter]) {
                tmpColumnNames.add(columnNames[columnCounter]);
            }
        }
        tmpInsert.append(String.join(", ", tmpColumnNames));
        tmpInsert.append(") VALUES (");

        // Execute part before question inserts; we need to reset the reader first
        br.close();
        br = new BufferedReader(new InputStreamReader(new FileInputStream(backupFile)));

        for (i = 0; i < endBeforeStatements; ++i) {
            String line = br.readLine();
            try {
                s.execute(line);
            } catch (SQLException sqle) {
                System.out.println("[info] play - Couldn't execute action: \"" + sqle.getMessage() + "\" If you deleted an experiment type, this is completely normal.");
            }
        }

        System.out.print("\t- Updating inserts: ");

        // Construct new insert query for question table that uses only the actually existing columns
        for (i = endBeforeStatements; i < startAfterStatements; ++i) {
            String line = br.readLine();

            String query = line.substring(line.indexOf(") VALUES (") + 10).replace(");", "");
            String[] queryValues = parseQueryValues(query);

            StringBuilder tmpString = new StringBuilder();

            for (int columnCounter = 0; columnCounter < matchingColumnNames.length; ++columnCounter) {
                if (matchingColumnNames[columnCounter]) {
                    tmpString.append(queryValues[columnCounter]).append(", ");
                }
            }
            tmpString = new StringBuilder(tmpString.subSequence(0, tmpString.length() - 2));
            tmpString.append(");");

            try {
                s.execute(tmpInsert.toString() + tmpString.toString());
            } catch (SQLException sqle) {
                System.out.println("[info] play - Couldn't execute action: \"" + sqle.getMessage() + "\"");
            }

            if ((i - endBeforeStatements) % 1000 == 0) {
                System.out.print('.');
            }
        }

        System.out.println("\n\t- Inserts successfully updated.");

        // Execute part after question inserts
        for (String line; (line = br.readLine()) != null; ) {
            try {
                s.execute(line);
            } catch (SQLException sqle) {
                System.out.println("[info] play - Couldn't execute action: \"" + sqle.getMessage() + "\" If you deleted an experiment type, this is completely normal.");
            }
        }

        System.out.println("\t- Loaded data into DB successfully.");

        // Delete experiments that became useless after deleting experiment types
        List<String> availableExperimentTypes = lingoturkConfig.getExperimentNames();
        for (LingoExpModel exp : LingoExpModel.getAllExperiments()) {
            if (!availableExperimentTypes.contains(exp.getExperimentType())) {
                System.out.println("\t- Delete experiment \"" + exp.getName() + "\" because type \"" + exp.getExperimentType() + "\" got deleted.");
                // We can't use exp.delete() here, because the experiment type doesn't exist anymore.
                // We have to delete all that stuff individually
                Statement deleteStatement = connection.createStatement();
                for (AbstractGroup part : exp.getParts()) {
                    deleteStatement.execute("DELETE FROM Groups WHERE PartId = " + part.getId());
                }
                deleteStatement.execute("DELETE FROM Questions WHERE dtype LIKE '" + exp.getExperimentType() + ".%'");
                deleteStatement.execute("DELETE FROM LingoExpModels WHERE LingoExpModelId = " + exp.getId());
                deleteStatement.close();
            }
        }

        System.out.println("\t- Deleted unnecessary entries successfully.");

        s.close();
    }


}
