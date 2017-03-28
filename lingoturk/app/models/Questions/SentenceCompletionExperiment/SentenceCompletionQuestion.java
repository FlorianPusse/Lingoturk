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
    @Column(name = "SentenceCompletion_story", columnDefinition = "TEXT")
    public java.lang.String SentenceCompletion_story = "";

    @Basic
    @Column(name = "SentenceCompletion_list", columnDefinition = "TEXT")
    public java.lang.String SentenceCompletion_list = "";

    @Basic
    @Column(name = "SentenceCompletion_itemNr", columnDefinition = "TEXT")
    public java.lang.String SentenceCompletion_itemNr = "";

    @Basic
    @Column(name = "SentenceCompletion_itemLength", columnDefinition = "TEXT")
    public java.lang.String SentenceCompletion_itemLength = "";

    @Basic
    @Column(name = "SentenceCompletion_itemType", columnDefinition = "TEXT")
    public java.lang.String SentenceCompletion_itemType = "";

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        JsonNode storyNode = questionNode.get("story");
        if (storyNode != null) {
            this.SentenceCompletion_story = storyNode.asText();
        }

        JsonNode listNode = questionNode.get("list");
        if (listNode != null) {
            this.SentenceCompletion_list = listNode.asText();
        }

        JsonNode itemNrNode = questionNode.get("itemNr");
        if (itemNrNode != null) {
            this.SentenceCompletion_itemNr = itemNrNode.asText();
        }

        JsonNode itemLengthNode = questionNode.get("itemLength");
        if (itemLengthNode != null) {
            this.SentenceCompletion_itemLength = itemLengthNode.asText();
        }

        JsonNode itemTypeNode = questionNode.get("itemType");
        if (itemTypeNode != null) {
            this.SentenceCompletion_itemType = itemTypeNode.asText();
        }

    }

	/* END OF VARIABLES BLOCK */


    public SentenceCompletionQuestion() {
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        String assignmentId = resultNode.get("assignmentId").asText();
        String hitId = resultNode.get("hitId").asText();
        int partId = resultNode.get("partId") != null ? resultNode.get("partId").asInt() : -1;

        int expId = -1;
        JsonNode expIdNode = resultNode.get("expId");
        if (expIdNode != null) {
            expId = expIdNode.asInt();
        }

        String origin = "NOT AVAILABLE";
        JsonNode originNode = resultNode.get("origin");
        if (originNode != null) {
            origin = originNode.asText();
        }

        JsonNode statisticsNode = resultNode.get("statistics");
        if (statisticsNode != null) {
            String statistics = statisticsNode.toString();
            Worker.addStatistics(workerId, expId, origin, statistics);
        }

        for (Iterator<JsonNode> questionNodeIterator = resultNode.get("results").iterator(); questionNodeIterator.hasNext(); ) {
            JsonNode questionNode = questionNodeIterator.next();
            int questionId = questionNode.get("id").asInt();
            JsonNode result = questionNode.get("answer");

            if (result != null) {
                PreparedStatement preparedStatement = DatabaseController.getConnection().prepareStatement(
                        "INSERT INTO SentenceCompletionResults(id,workerId,assignmentId,hitId,partId,questionId,answer,origin) VALUES(nextval('SentenceCompletionResults_seq'),?,?,?,?,?,?,?)"
                );
                preparedStatement.setString(1, workerId);
                preparedStatement.setString(2, assignmentId);
                preparedStatement.setString(3, hitId);
                preparedStatement.setInt(4, partId);
                preparedStatement.setInt(5, questionId);
                preparedStatement.setString(6, result.toString());
                preparedStatement.setString(7, origin);

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
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.SentenceCompletionExperiment.SentenceCompletionExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }
}
