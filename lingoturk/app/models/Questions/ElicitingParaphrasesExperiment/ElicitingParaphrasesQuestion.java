package models.Questions.ElicitingParaphrasesExperiment;


import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Repository;
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

// _IMPORT_PLACEHOLDER_

@Entity
@Inheritance
@DiscriminatorValue("ElicitingParaphrasesExperiment.ElicitingParaphrasesQuestion")
public class ElicitingParaphrasesQuestion extends PartQuestion {

    /* BEGIN OF VARIABLES BLOCK */

	@Basic
	@Column(name="ElicitingParaphrases_text", columnDefinition = "TEXT")
	public java.lang.String text = "";

	@Basic
	@Column(name="ElicitingParaphrases_fileName", columnDefinition = "TEXT")
	public java.lang.String fileName = "";

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
		JsonNode textNode = questionNode.get("text");
		if (textNode != null){
			this.text = textNode.asText();
		}

		JsonNode fileNameNode = questionNode.get("fileName");
		if (fileNameNode != null){
			this.fileName = fileNameNode.asText();
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

        for (Iterator<JsonNode> resultIterator = resultNode.get("answers").iterator(); resultIterator.hasNext(); ) {
            PreparedStatement statement = Repository.getConnection().prepareStatement(
                    "INSERT INTO ElicitingParaphrasesResults(id,WorkerId,questionId,answer) VALUES(nextval('ElicitingParaphrasesResults_seq'),?,?,?)"
            );

            statement.setString(1, workerId);

            JsonNode result = resultIterator.next();
            int questionId = result.get("questionId").asInt();
            String answer = result.get("answer").asText();

            statement.setInt(2, questionId);
            statement.setString(3, answer);

            statement.execute();
            statement.close();
        }
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return null;
    }

}