package models.Questions.SentenceCompletionExperiment;

import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.DatabaseController;
import models.LingoExpModel;
import models.Questions.PartQuestion;
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
@DiscriminatorValue("SentenceCompletionExperiment.SentenceCompletionQuestion")
public class SentenceCompletionQuestion extends PartQuestion {

    /* BEGIN OF VARIABLES BLOCK */

	@Basic
	@Column(name="SentenceCompletion_story", columnDefinition = "TEXT")
	public java.lang.String SentenceCompletion_story = "";

	@Basic
	@Column(name="SentenceCompletion_list", columnDefinition = "TEXT")
	public java.lang.String SentenceCompletion_list = "";

	@Basic
	@Column(name="SentenceCompletion_itemNr", columnDefinition = "TEXT")
	public java.lang.String SentenceCompletion_itemNr = "";

	@Basic
	@Column(name="SentenceCompletion_itemLength", columnDefinition = "TEXT")
	public java.lang.String SentenceCompletion_itemLength = "";

	@Basic
	@Column(name="SentenceCompletion_itemType", columnDefinition = "TEXT")
	public java.lang.String SentenceCompletion_itemType = "";

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
		JsonNode storyNode = questionNode.get("story");
		if (storyNode != null){
			this.SentenceCompletion_story = storyNode.asText();
		}

		JsonNode listNode = questionNode.get("list");
		if (listNode != null){
			this.SentenceCompletion_list = listNode.asText();
		}

		JsonNode itemNrNode = questionNode.get("itemNr");
		if (itemNrNode != null){
			this.SentenceCompletion_itemNr = itemNrNode.asText();
		}

		JsonNode itemLengthNode = questionNode.get("itemLength");
		if (itemLengthNode != null){
			this.SentenceCompletion_itemLength = itemLengthNode.asText();
		}

		JsonNode itemTypeNode = questionNode.get("itemType");
		if (itemTypeNode != null){
			this.SentenceCompletion_itemType = itemTypeNode.asText();
		}

    }

	/* END OF VARIABLES BLOCK */



    public SentenceCompletionQuestion(){}

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        int partId = resultNode.get("partId").asInt();

        PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                "INSERT INTO SentenceCompletionResults(id,WorkerId,partId,questionId,answer) VALUES(nextval('SentenceCompletionResults_seq'),?,?,?,?)"
        );

        statement.setString(1, workerId);
        statement.setInt(2, partId);

        for (Iterator<JsonNode> resultIterator = resultNode.get("answers").iterator(); resultIterator.hasNext(); ) {
            JsonNode result = resultIterator.next();
            int questionId = result.get("questionId").asInt();
            String answer = result.get("answer").asText();

            statement.setInt(3, questionId);
            statement.setString(4, answer);

            statement.execute();
        }

        statement.close();
    }

    @Override
    public JsonObject returnJSON() throws SQLException{
        return super.returnJSON();
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.SentenceCompletionExperiment.SentenceCompletionExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }
}
