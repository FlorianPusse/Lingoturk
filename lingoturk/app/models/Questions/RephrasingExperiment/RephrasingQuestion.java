package models.Questions.RephrasingExperiment;

import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.services.identitymanagement.model.ReportStateType;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Repository;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("RephrasingExperiment.RephrasingQuestion")
public class RephrasingQuestion extends PartQuestion {

    /* BEGIN OF VARIABLES BLOCK */

    @Basic
    @Column(name = "Rephrasing_sentence1", columnDefinition = "TEXT")
    public java.lang.String Rephrasing_sentence1 = "";

    @Basic
    @Column(name = "Rephrasing_question1", columnDefinition = "TEXT")
    public java.lang.String Rephrasing_question1 = "";

    @Basic
    @Column(name = "Rephrasing_questionFirst1")
    public boolean Rephrasing_questionFirst1 = false;

    @Basic
    @Column(name = "Rephrasing_sentence2", columnDefinition = "TEXT")
    public java.lang.String Rephrasing_sentence2 = "";

    @Basic
    @Column(name = "Rephrasing_question2", columnDefinition = "TEXT")
    public java.lang.String Rephrasing_question2 = "";

    @Basic
    @Column(name = "Rephrasing_questionFirst2")
    public boolean Rephrasing_questionFirst2 = false;

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        JsonNode sentence1Node = questionNode.get("sentence1");
        if (sentence1Node != null) {
            this.Rephrasing_sentence1 = sentence1Node.asText();
        }

        JsonNode question1Node = questionNode.get("question1");
        if (question1Node != null) {
            this.Rephrasing_question1 = question1Node.asText();
        }

        JsonNode questionFirst1Node = questionNode.get("questionFirst1");
        if (questionFirst1Node != null) {
            this.Rephrasing_questionFirst1 = questionFirst1Node.asBoolean();
        }

        JsonNode sentence2Node = questionNode.get("sentence2");
        if (sentence2Node != null) {
            this.Rephrasing_sentence2 = sentence2Node.asText();
        }

        JsonNode question2Node = questionNode.get("question2");
        if (question2Node != null) {
            this.Rephrasing_question2 = question2Node.asText();
        }

        JsonNode questionFirst2Node = questionNode.get("questionFirst2");
        if (questionFirst2Node != null) {
            this.Rephrasing_questionFirst2 = questionFirst2Node.asBoolean();
        }

    }

	/* END OF VARIABLES BLOCK */


    String fillerSentence1 = "When Mary finally found the dress she had been looking for, she was very happy.";
    String fillerQuestion1 = "Did Mary find the dress she had been looking for?";

    String fillerSentence2 = "Although John is rather short, he likes riding horses that are very tall.";
    String fillerQuestion2 = "Is John very tall?";

    public RephrasingQuestion() {
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.RephrasingExperiment.RephrasingExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        int questionId = resultNode.get("questionId").asInt();

        JsonNode answerNode = resultNode.get("answer");

        String answer1 = answerNode.get("answer1").asText();
        String answer2 = answerNode.get("answer2").asText();

        boolean choice1 = answerNode.get("choice1").asBoolean();
        boolean choice2 = answerNode.get("choice2").asBoolean();

        int readingTime1 = answerNode.get("readingTime1").asInt();
        int readingTime2 = answerNode.get("readingTime2").asInt();

        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO RephrasingResults(id,workerId,questionId,answer1,answer2,choice1,choice2,readingTime1,readingTime2) VALUES(nextval('RephrasingResults_seq'),?,?,?,?,?,?,?,?)"
        );

        statement.setString(1, workerId);
        statement.setInt(2, questionId);

        statement.setString(3, answer1);
        statement.setString(4, answer2);

        statement.setBoolean(5, choice1);
        statement.setBoolean(6, choice2);

        statement.setInt(7,readingTime1);
        statement.setInt(8,readingTime2);

        statement.execute();
        statement.close();

    }
}
