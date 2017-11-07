package models.Groups;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import controllers.RenderController;
import models.LingoExpModel;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;
import play.twirl.api.Html;
import services.LingoturkConfigImplementation;
import views.html.ExperimentRendering.experiment_main;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.SQLException;

import static play.mvc.Results.ok;

/**
 * Represents a group that forces a participant to answer each question in this group
 */
@Entity
@Inheritance
@DiscriminatorValue("FullGroup")
public class FullGroup extends AbstractGroup {

    /* BEGIN OF VARIABLES BLOCK */

    /* END OF VARIABLES BLOCK */

    public FullGroup() {
    }

    /**
     * Publishes this group on Mechanical Turk. As each participant has to answer each question,
     * only one HIT is created.
     *
     * @param service        The service (either Sandbox, or Mechanical Turk itself)
     * @param publishedId    The unique Id that is the same for all groups of an experiment that are published at the same time
     * @param hitTypeId      The hitTypeId (assigned by Mechanical Turk)
     * @param lifetime       How long the study should be running
     * @param maxAssignments How many participants should participate in this group
     * @return The URL of the study that is assigned by Mechanical turk
     * @throws SQLException Propagated from JDBC
     */
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException {

        String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                + "<ExternalURL> " + LingoturkConfigImplementation.staticGetStaticIp() + "/renderAMT?id=" + getId() + "&amp;Type=part</ExternalURL>"
                + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";
        HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignments, null, null, null, null, null, null);
        String url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();

        System.out.println(url);

        insert(hit.getHITId(), publishedId);

        availability = maxAssignments;
        update();

        return url;
    }

    /**
     * Renders this group for participants coming from Mechanical Turk. Each participant has to answer
     * all questions.
     *
     * @param worker       The worker that is doing the experiment
     * @param assignmentId The assignmentId that was assigned by Mechanical Turk
     * @param hitId        The hitId that was assigned by Mechanical Turk
     * @param turkSubmitTo The url to submit the results to (i.e. either the Sandbox or Mechanical Turk itself)
     * @param exp          The experiment that this group belongs to
     * @param df           df The DynamicForm, possibly containing more parameters
     * @return Returns a random question that belongs to this group
     */
    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        Html html = RenderController.getExperimentContent(exp);
        return ok(experiment_main.render(exp.getExperimentType(), null, this, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK", html));
    }
}
