package models.Questions.PictureNamingExperiment;


import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import play.data.DynamicForm;
import play.mvc.Result;
import javax.json.JsonObject;
import javax.persistence.*;
import java.sql.SQLException;
import java.util.*;
import controllers.DatabaseController;
import java.sql.PreparedStatement;

import static play.mvc.Results.ok;

// _IMPORT_PLACEHOLDER_

@Entity
@Inheritance
@DiscriminatorValue("PictureNaming1Experiment.PictureNaming1Question")
public class PictureNamingQuestion extends PartQuestion {

/* BEGIN OF VARIABLES BLOCK */

    @Basic
    @Column(name="PictureNaming_picturePath", columnDefinition = "TEXT")
    public String PictureNaming_picturePath = "";

    @Basic
    @Column(name="PictureNaming_text", columnDefinition = "TEXT")
    public String PictureNaming_text = "";

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        JsonNode picturePathNode = questionNode.get("picturePath");
        if (picturePathNode != null){
            this.PictureNaming_picturePath = picturePathNode.asText();
        }

        JsonNode textNode = questionNode.get("text");
        if (textNode != null){
            this.PictureNaming_text = textNode.asText();
        }

    }

	/* END OF VARIABLES BLOCK */


    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        String assignmentId = resultNode.get("assignmentId").asText();
        String hitId = resultNode.get("hitId").asText();
        int partId = resultNode.get("partId") != null ? resultNode.get("partId").asInt() : -1;

        int expId = -1;
        JsonNode expIdNode = resultNode.get("expId");
        if(expIdNode != null){
            expId = expIdNode.asInt();
        }

        String origin = "NOT AVAILABLE";
        JsonNode originNode = resultNode.get("origin");
        if(originNode != null){
            origin = originNode.asText();
        }

        JsonNode statisticsNode = resultNode.get("statistics");
        if(statisticsNode != null){
            String statistics = statisticsNode.toString();
            Worker.addStatistics(workerId, expId, origin, statistics);
        }

        for(Iterator<JsonNode> questionNodeIterator = resultNode.get("results").iterator(); questionNodeIterator.hasNext(); ){
            JsonNode questionNode = questionNodeIterator.next();
            int questionId = questionNode.get("id").asInt();
            JsonNode result = questionNode.get("answer");

            if(result != null){
                PreparedStatement preparedStatement = DatabaseController.getConnection().prepareStatement(
                        "INSERT INTO PictureNamingResults(id,workerId,assignmentId,hitId,partId,questionId,answer,origin) VALUES(nextval('PictureNamingResults_seq'),?,?,?,?,?,?,?)"
                );
                preparedStatement.setString(1,workerId);
                preparedStatement.setString(2,assignmentId);
                preparedStatement.setString(3,hitId);
                preparedStatement.setInt(4,partId);
                preparedStatement.setInt(5,questionId);
                preparedStatement.setString(6,result.toString());
                preparedStatement.setString(7,origin);

                preparedStatement.execute();
                preparedStatement.close();
            }
        }
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.PictureNamingExperiment.PictureNamingExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

}