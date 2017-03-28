package models.Questions.PlausibilityExperiment;

import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import controllers.DatabaseController;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.JsonObject;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("PlausibilityExperiment.PlausibilityQuestion")
public class PlausibilityQuestion extends PartQuestion{

    /* BEGIN OF VARIABLES BLOCK */

	@Basic
	@Column(name="Plausibility_number", columnDefinition = "TEXT")
	public java.lang.String Plausibility_number = "";

	@Basic
	@Column(name="Plausibility_condition", columnDefinition = "TEXT")
	public java.lang.String Plausibility_condition = "";

	@Basic
	@Column(name="Plausibility_text", columnDefinition = "TEXT")
	public java.lang.String Plausibility_text = "";

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
		JsonNode numberNode = questionNode.get("number");
		if (numberNode != null){
			this.Plausibility_number = numberNode.asText();
		}

		JsonNode conditionNode = questionNode.get("condition");
		if (conditionNode != null){
			this.Plausibility_condition = conditionNode.asText();
		}

		JsonNode textNode = questionNode.get("text");
		if (textNode != null){
			this.Plausibility_text = textNode.asText();
		}

    }

	/* END OF VARIABLES BLOCK */



    public PlausibilityQuestion(){}

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.PlausibilityExperiment.PlausibilityExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
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
                        "INSERT INTO PlausibilityResults(id,workerId,assignmentId,hitId,partId,questionId,answer,origin) VALUES(nextval('PlausibilityResults_seq'),?,?,?,?,?,?,?)"
                );
                preparedStatement.setString(1,workerId);
                preparedStatement.setString(2,assignmentId);
                preparedStatement.setString(3,hitId);
                preparedStatement.setInt(4,partId);
                preparedStatement.setInt(5,questionId);
                preparedStatement.setInt(6,result.asInt());
                preparedStatement.setString(7,origin);

                preparedStatement.execute();
                preparedStatement.close();
            }
        }
    }
}
