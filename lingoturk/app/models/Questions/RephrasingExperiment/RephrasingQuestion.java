package models.Questions.RephrasingExperiment;

import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.SQLException;

import static play.mvc.Results.ok;
@Entity
@Inheritance
@DiscriminatorValue("RephrasingExperiment.RephrasingQuestion")
public class RephrasingQuestion extends PartQuestion {

    /* BEGIN OF VARIABLES BLOCK */

	@Basic
	@Column(name="Rephrasing_sentence1", columnDefinition = "TEXT")
	public java.lang.String sentence1 = "";

	@Basic
	@Column(name="Rephrasing_question1", columnDefinition = "TEXT")
	public java.lang.String question1 = "";

	@Basic
	@Column(name="Rephrasing_questionFirst1")
	public boolean questionFirst1 = false;

	@Basic
	@Column(name="Rephrasing_sentence2", columnDefinition = "TEXT")
	public java.lang.String sentence2 = "";

	@Basic
	@Column(name="Rephrasing_question2", columnDefinition = "TEXT")
	public java.lang.String question2 = "";

	@Basic
	@Column(name="Rephrasing_questionFirst2")
	public boolean questionFirst2 = false;

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
		JsonNode sentence1Node = questionNode.get("sentence1");
		if (sentence1Node != null){
			this.sentence1 = sentence1Node.asText();
		}

		JsonNode question1Node = questionNode.get("question1");
		if (question1Node != null){
			this.question1 = question1Node.asText();
		}

		JsonNode questionFirst1Node = questionNode.get("questionFirst1");
		if (questionFirst1Node != null){
			this.questionFirst1 = questionFirst1Node.asBoolean();
		}

		JsonNode sentence2Node = questionNode.get("sentence2");
		if (sentence2Node != null){
			this.sentence2 = sentence2Node.asText();
		}

		JsonNode question2Node = questionNode.get("question2");
		if (question2Node != null){
			this.question2 = question2Node.asText();
		}

		JsonNode questionFirst2Node = questionNode.get("questionFirst2");
		if (questionFirst2Node != null){
			this.questionFirst2 = questionFirst2Node.asBoolean();
		}

    }

	/* END OF VARIABLES BLOCK */


    String fillerSentence1 = "When Mary finally found the dress she had been looking for, she was very happy.";
    String fillerQuestion1 = "Did Mary find the dress she had been looking for?";

    String fillerSentence2 = "Although John is rather short, he likes riding horses that are very tall.";
    String fillerQuestion2 = "Is John very tall?";

    public RephrasingQuestion() {}

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.RephrasingExperiment.RephrasingExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

    public String getSentence1() {
        return sentence1;
    }

    public String getSentence2() {
        return sentence2;
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public void writeResults(JsonNode resultNode) {

    }
}
