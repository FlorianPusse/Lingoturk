package models.Groups;


import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import models.LingoExpModel;
import models.Questions.Question;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;
import services.DatabaseServiceImplementation;
import services.LingoturkConfigImplementation;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static play.mvc.Results.internalServerError;

/**
 * Represents a group that forces a participant to answer each question in this group
 */
@Entity
@Inheritance
@DiscriminatorValue("PoolGroup")
public class PoolGroup extends AbstractGroup {

    /* BEGIN OF VARIABLES BLOCK */

    /* END OF VARIABLES BLOCK */

    public PoolGroup() {
    }

    /**
     * Stores the information that a question in this group has been published.
     *
     * @param hitID       The Id that was assigned to the question in this group
     * @param publishedId The unique id that is assigned when the experiment that this group is in has been published
     * @param questionId  The question in this group that was published
     * @throws SQLException Propagated from JDBC
     */
    private void insert(String hitID, int publishedId, int questionId) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("INSERT INTO PartPublishedAs(PartID,mTurkID,publishedId,question1) VALUES(?,?,?,?)");
        statement.setInt(1, getId());
        statement.setString(2, hitID);
        statement.setInt(3, publishedId);
        statement.setInt(4, questionId);
        statement.execute();
    }

    /**
     * Publishes this group on Mechanical Turk. As each participant should be able to do as many
     * questions as s/he wants, an individual HIT is created for each question in this group.
     *
     * @param service                   The service (either Sandbox, or Mechanical Turk itself)
     * @param publishedId               The unique Id that is the same for all groups of an experiment that are published at the same time
     * @param hitTypeId                 The hitTypeId (assigned by Mechanical Turk)
     * @param lifetime                  How long the study should be running
     * @param maxAssignmentsPerQuestion How many participants should participate in each question of this group
     * @return The URL of the study that is assigned by Mechanical turk
     * @throws SQLException Propagated from JDBC
     */
    @Override
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignmentsPerQuestion) throws SQLException {
        String url = "";

        for (Question q : getQuestions()) {
            String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                    + "<ExternalURL> " + LingoturkConfigImplementation.staticGetStaticIp() + "/renderAMT?id=" + q.getId() + "&amp;Type=question</ExternalURL>"
                    + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";
            HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignmentsPerQuestion, null, null, null, null, null, null);
            url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();
            System.out.println(url);

            insert(hit.getHITId(), publishedId, q.getId());
        }

        return url;
    }

    /**
     * Renders this group for participants coming from Mechanical Turk. A random question
     * is returned, as participants aren't forced to participate in any specific question
     * for this type of Group.
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
        try {
            return getRandomQuestion(worker, assignmentId, hitId, turkSubmitTo, exp, df);
        } catch (SQLException e) {
            return internalServerError("Could not connect to DB.");
        }
    }


}
