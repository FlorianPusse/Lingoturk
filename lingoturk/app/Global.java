import akka.actor.ActorRef;
import akka.actor.Props;
import controllers.AsynchronousJob;
import play.*;
import play.libs.Akka;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;
import scala.concurrent.duration.Duration;

public class Global extends GlobalSettings{

    @Override
    public void onStart(Application app) {
        System.out.println("[info] play - Application has started...");
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
