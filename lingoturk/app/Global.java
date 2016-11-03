import controllers.DatabaseController;
import org.apache.commons.io.FileUtils;
import play.Application;
import play.GlobalSettings;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Global extends GlobalSettings {

    static boolean loadBackUp = false;

    @Override
    public void onStart(Application app) {
        System.out.println("[info] play - Application has started...");
        Connection c = DatabaseController.getConnection();

        if (loadBackUp) {
            try {
                Statement s = c.createStatement();
                ResultSet resultSet = s.executeQuery("SELECT count(*) FROM LingoExpModels");
                if (resultSet.next()) {
                    int experimentCount = resultSet.getInt(1);
                    if (experimentCount == 0) {
                        String queryData;
                        try{
                            queryData = FileUtils.readFileToString(new File("conf/backup.sql"));
                        }catch (FileNotFoundException fnfe){
                            // No backup yet. That isn't unusual
                            return;
                        }
                        System.out.println("[info] play - Load backup file.");

                        DatabaseController.restoreDatabase(queryData);

                        System.out.println("[info] play - Backup imported successfully.");
                    }
                }
            } catch (SQLException | IOException e) {
                e.printStackTrace();
            }
        }
        /*try {
            AsynchronousJob.loadQueue();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        ActorRef asynchronousJob = Akka.system().actorOf(Props.create(AsynchronousJob.class));
        Akka.system().scheduler().schedule(
                Duration.create(0, TimeUnit.MILLISECONDS), //Initial delay 0 milliseconds
                Duration.create(10, TimeUnit.SECONDS),     //Frequency 5 seconds
                asynchronousJob,
                "message",
                Akka.system().dispatcher(),
                null
        );*/
    }

    @Override
    public void onStop(Application app) {
        /*try {
            AsynchronousJob.saveQueue();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("[info] play - Application shutdown...");*/
    }
}
