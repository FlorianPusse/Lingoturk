import akka.actor.ActorRef;
import akka.actor.Props;
import controllers.Application;
import controllers.AsynchronousJob;
import controllers.DatabaseController;
import models.LingoExpModel;
import org.apache.commons.io.FileUtils;
import play.GlobalSettings;
import play.api.db.DB;
import play.api.db.DBApi;
import play.api.db.DBApi$class;
import play.api.db.DBPlugin;
import play.api.db.evolutions.Evolutions;
import play.api.db.evolutions.InvalidDatabaseRevision;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import scala.concurrent.duration.Duration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class Global extends GlobalSettings {

    @Override
    public void onStart(play.Application app) {
        Application.properties = new Properties();
        try {
            Application.properties.load(new java.io.FileInputStream(new java.io.File(Application.propertiesLocation)));
            System.out.println("[info] play - Properties loaded");
        } catch (IOException e) {
            System.out.println("[info] play - Couldn't load properties: " + e.getMessage());
        }

        System.out.println("[info] play - Application has started...");

        if (Application.properties.getProperty("useBackup").equals("true")) {
            try {
                if (LingoExpModel.countExperiments() == 0) {
                    String queryData;
                    try {
                        queryData = FileUtils.readFileToString(new File("backup/backup.sql"));
                    } catch (FileNotFoundException fnfe) {
                        // No backup yet. That isn't unusual
                        return;
                    }
                    System.out.println("[info] play - Load backup file.");

                    DatabaseController.restoreDatabase(queryData);

                    System.out.println("[info] play - Backup imported successfully.");
                }
            }catch(SQLException | IOException e){
                e.printStackTrace();
            }
        }

        ActorRef asynchronousJob = Akka.system().actorOf(Props.create(AsynchronousJob.class));
        Akka.system().scheduler().schedule(
                Duration.create(0, TimeUnit.MILLISECONDS),
                Duration.create(1, TimeUnit.MINUTES),
                asynchronousJob,
                "message",
                Akka.system().dispatcher(),
                null
        );
    }


    @Override
    public void onStop(play.Application app) {
        System.out.println("[info] play - Application shutdown...");
        try {
            // If data is stored, back it up
            if (LingoExpModel.countExperiments() > 0) {
                DatabaseController.backupDatabase();
                System.out.println("[info] play - Application shutdown... Database backup created.");
            }
        }catch(SQLException e){
            e.printStackTrace();
            System.out.println("[error] play - Application shutdown... Could not create Database backup.");
        }
    }
}
