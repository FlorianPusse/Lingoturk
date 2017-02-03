package models.Groups;


import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import controllers.DatabaseController;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.JsonObject;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static play.mvc.Results.internalServerError;

@Entity
@Inheritance
@DiscriminatorValue("PoolGroup")
public class PoolGroup extends AbstractGroup {

    /* BEGIN OF VARIABLES BLOCK */

    /* END OF VARIABLES BLOCK */

    public PoolGroup(){}

    @Override
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignmentsPerCombination) throws SQLException {
        String url = "";

        for(PartQuestion q : getQuestions()){
            String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                    + "<ExternalURL> " + Application.getStaticIp() + "/renderAMT?id=" + q.getId() + "&amp;Type=question</ExternalURL>"
                    + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";
            HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignmentsPerCombination, null, null, null, null, null, null);
            url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();
            System.out.println(url);

            insert(hit.getHITId(), publishedId, q.getId());
        }

        return url;
    }

    @Override
    public void publishOnProlific(int maxAssignments) {

    }

    public void insert(String hitID, int publishedId, int questionId) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("INSERT INTO PartPublishedAs(PartID,mTurkID,publishedId,question1) VALUES(?,?,?,?)");
        statement.setInt(1, getId());
        statement.setString(2, hitID);
        statement.setInt(3, publishedId);
        statement.setInt(4, questionId);
        statement.execute();
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode partNode) throws SQLException {
        super.setJSONData(experiment, partNode);
    }

    @Override
    public Result getRandomQuestion(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) throws SQLException {
        return super.getRandomQuestion(worker,assignmentId,hitId,turkSubmitTo,exp,df);
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        try {
            return getRandomQuestion(worker, assignmentId, hitId, turkSubmitTo, exp, df);
        } catch (SQLException e) {
            return internalServerError("Could not connect to DB.");
        }
    }


}
