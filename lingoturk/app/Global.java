import models.Repository;
import org.apache.commons.io.FileUtils;
import play.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Global extends GlobalSettings{

    static boolean loadDemoExperiments = false;

    @Override
    public void onStart(Application app) {
        System.out.println("[info] play - Application has started...");
        Connection c = Repository.getConnection();

        if(loadDemoExperiments) {
            try {
                Statement s = c.createStatement();
                ResultSet resultSet = s.executeQuery("SELECT count(*) FROM LingoExpModels");
                if (resultSet.next()) {
                    int experimentCount = resultSet.getInt(1);
                    if (experimentCount == 0) {
                        // Populate with demo experiments
                        String query = FileUtils.readFileToString(new File("template/LingoturkDEMO.sql"));
                        s.execute(query);
                        s.close();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (IOException e) {
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
