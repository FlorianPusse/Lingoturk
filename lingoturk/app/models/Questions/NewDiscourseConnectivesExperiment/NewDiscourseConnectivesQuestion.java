package models.Questions.NewDiscourseConnectivesExperiment;

import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Repository;
import models.Results.AssignmentResult;
import models.Questions.PartQuestion;
import models.Worker;
import org.dom4j.DocumentException;
import org.h2.command.Prepared;
import play.data.DynamicForm;
import play.libs.Json;
import play.mvc.Result;

import javax.json.JsonObject;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import static play.mvc.Results.ok;


@Entity
@Inheritance
@DiscriminatorValue("NewDiscourseConnectivesExperiment.NewDiscourseConnectivesQuestion")
public class NewDiscourseConnectivesQuestion extends PartQuestion {

    /* BEGIN OF VARIABLES BLOCK */

	@Basic
	@Column(name="NewDiscourseConnectives_sentence1", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_sentence1 = "";

	@Basic
	@Column(name="NewDiscourseConnectives_sentence2", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_sentence2 = "";

	@Basic
	@Column(name="NewDiscourseConnectives_context1", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_context1 = "";

	@Basic
	@Column(name="NewDiscourseConnectives_context2", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_context2 = "";

	@Basic
	@Column(name="NewDiscourseConnectives_condition", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_condition = "";

	@Basic
	@Column(name="NewDiscourseConnectives_WSJID", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_WSJID = "";

	@Basic
	@Column(name="NewDiscourseConnectives_relation", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_relation = "";

	@Basic
	@Column(name="NewDiscourseConnectives_pdtbConn", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_pdtbConn = "";

	@Basic
	@Column(name="NewDiscourseConnectives_pdtbSense", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_pdtbSense = "";

	@Basic
	@Column(name="NewDiscourseConnectives_rstSense", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_rstSense = "";

	@Basic
	@Column(name="NewDiscourseConnectives_nr", columnDefinition = "TEXT")
	public java.lang.String NewDiscourseConnectives_nr = "";

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
		JsonNode sentence1Node = questionNode.get("sentence1");
		if (sentence1Node != null){
			this.NewDiscourseConnectives_sentence1 = sentence1Node.asText();
		}

		JsonNode sentence2Node = questionNode.get("sentence2");
		if (sentence2Node != null){
			this.NewDiscourseConnectives_sentence2 = sentence2Node.asText();
		}

		JsonNode context1Node = questionNode.get("context1");
		if (context1Node != null){
			this.NewDiscourseConnectives_context1 = context1Node.asText();
		}

		JsonNode context2Node = questionNode.get("context2");
		if (context2Node != null){
			this.NewDiscourseConnectives_context2 = context2Node.asText();
		}

		JsonNode conditionNode = questionNode.get("condition");
		if (conditionNode != null){
			this.NewDiscourseConnectives_condition = conditionNode.asText();
		}

		JsonNode WSJIDNode = questionNode.get("WSJID");
		if (WSJIDNode != null){
			this.NewDiscourseConnectives_WSJID = WSJIDNode.asText();
		}

		JsonNode relationNode = questionNode.get("relation");
		if (relationNode != null){
			this.NewDiscourseConnectives_relation = relationNode.asText();
		}

		JsonNode pdtbConnNode = questionNode.get("pdtbConn");
		if (pdtbConnNode != null){
			this.NewDiscourseConnectives_pdtbConn = pdtbConnNode.asText();
		}

		JsonNode pdtbSenseNode = questionNode.get("pdtbSense");
		if (pdtbSenseNode != null){
			this.NewDiscourseConnectives_pdtbSense = pdtbSenseNode.asText();
		}

		JsonNode rstSenseNode = questionNode.get("rstSense");
		if (rstSenseNode != null){
			this.NewDiscourseConnectives_rstSense = rstSenseNode.asText();
		}

		JsonNode nrNode = questionNode.get("nr");
		if (nrNode != null){
			this.NewDiscourseConnectives_nr = nrNode.asText();
		}

    }

	/* END OF VARIABLES BLOCK */



    public NewDiscourseConnectivesQuestion() {
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.NewDiscourseConnectivesExperiment.NewDiscourseConnectivesExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        int partId = resultNode.get("partId").asInt();
        for(Iterator<JsonNode> answerIterater = resultNode.get("answers").iterator(); answerIterater.hasNext();){
            JsonNode answer = answerIterater.next();
            int questionId = answer.get("questionId").asInt();
            String connective1 = answer.get("connective1").asText();
            String connective2 = answer.get("connective2").asText();
            String manualAnswer1 =  answer.get("manualAnswer1").asText();
            String manualAnswer2 = answer.get("manualAnswer2").asText();

            PreparedStatement statement = Repository.getConnection().prepareStatement("INSERT INTO NewDiscourseConnectivesResults(id,workerId,partId,questionId,connective1,connective2,manualAnswer1,manualAnswer2) VALUES (nextval('newdiscourseconnectivesresults_seq'), ?,?,?,?,?,?,?)");
            statement.setString(1,workerId);
            statement.setInt(2, partId);
            statement.setInt(3, questionId);
            statement.setString(4,connective1);
            statement.setString(5,connective2);
            statement.setString(6,manualAnswer1);
            statement.setString(7,manualAnswer2);

            statement.execute();

            System.out.println("executed");

            statement.close();
        }
    }

    public int getExperimentID() {
        return experimentID;
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }
}
