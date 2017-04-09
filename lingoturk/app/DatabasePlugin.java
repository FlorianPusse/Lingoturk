import controllers.DatabaseController;
import play.api.Application;
import play.api.Plugin;
import play.db.DB;

import java.sql.Connection;
import java.sql.SQLException;


public class DatabasePlugin implements Plugin {

    public DatabasePlugin(Application app) {}

    @Override
    public void onStart() {
        Connection connection = DB.getConnection();
        DatabaseController.backupDatabase(connection);
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStop() {}

    public boolean enabled(){
        return true;
    }
}
