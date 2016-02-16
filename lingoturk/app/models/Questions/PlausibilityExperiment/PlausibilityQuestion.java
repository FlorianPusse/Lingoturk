package models.Questions.PlausibilityExperiment;

import au.com.bytecode.opencsv.CSVWriter;
import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Repository;
import models.Results.AssignmentResult;
import org.dom4j.DocumentException;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

@Entity
@Inheritance
@DiscriminatorValue("PlausibilityQuestion")
public class PlausibilityQuestion extends PartQuestion{

    @Basic
    String number;

    @Basic
    String condition;

    @Column(columnDefinition = "TEXT")
    String text;

    public PlausibilityQuestion(String number, String condition, String text){
        this.number = number;
        this.condition = condition;
        this.text = text;
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        String number = questionNode.get("number").asText();
        String condition = questionNode.get("condition").asText();
        String text = questionNode.get("text").asText();

        this.number = number;
        this.condition = condition;
        this.text = text;
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder partBuilder = Json.createObjectBuilder();
        partBuilder.add("id",getId());
        partBuilder.add("number",getNumber());
        partBuilder.add("condition",getCondition());
        partBuilder.add("text",getText());
        return partBuilder.build();
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public Result render(String assignmentId, String hitId, String workerId, String turkSubmitTo, String additionalExplanations) {
        return null;
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        int partId = resultNode.get("partId").asInt();

        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO PlausibilityResult(WorkerId,partId,questionId,answer) VALUES(?,?,?,?)"
        );

        statement.setString(1, workerId);
        statement.setInt(2, partId);

        for (Iterator<JsonNode> resultIterator = resultNode.get("answers").iterator(); resultIterator.hasNext(); ) {
            JsonNode result = resultIterator.next();
            int questionId = result.get("questionId").asInt();
            int answer = result.get("answer").asInt();

            statement.setInt(3, questionId);
            statement.setInt(4, answer);

            statement.execute();
        }

        statement.close();
    }

    public String getNumber() {
        return number;
    }

    public String getCondition() {
        return condition;
    }

    public String getText() {
        return text;
    }
}
