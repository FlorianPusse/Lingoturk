package services;

import com.google.inject.ImplementedBy;
import play.mvc.Result;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

@ImplementedBy(DatabaseServiceImplementation.class)
public interface DatabaseService {

    void closeConnection();
    Connection getConnection();
    String[] getTableNames(Connection c) throws SQLException;
    boolean isDatabaseCreated(Connection c) throws SQLException;
    String[] getColumnNames(ResultSetMetaData metaData) throws SQLException;
    String[] parseQueryValues(String query);
    Result backupDatabase();
    Result backupDatabase(Connection c);
    void restoreDatabase(File backupFile) throws SQLException, IOException;
    int countExperiments() throws SQLException;
}
