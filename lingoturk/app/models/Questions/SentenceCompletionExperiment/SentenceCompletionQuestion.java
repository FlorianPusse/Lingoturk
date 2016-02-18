package models.Questions.SentenceCompletionExperiment;

import au.com.bytecode.opencsv.CSVWriter;
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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("SentenceCompletion")
public class SentenceCompletionQuestion extends PartQuestion {

    @Column(columnDefinition = "TEXT")
    String story;

    @Basic
    String list;

    @Basic
    String itemNr;

    @Basic
    String itemLength;

    @Basic
    String itemType;

    public SentenceCompletionQuestion(String itemNr, String list, String itemLength, String itemType, String story){
        this.itemNr = itemNr;
        this.list = list;
        this.itemLength = itemLength;
        this.itemType = itemType;
        this.story = story;
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        String itemNr = questionNode.get("itemNr").asText();
        String list = questionNode.get("list").asText();
        String itemLength = questionNode.get("itemLength").asText();
        String itemType = questionNode.get("itemType").asText();
        String story = questionNode.get("story").asText();

        this.itemNr = itemNr;
        this.list = list;
        this.itemLength = itemLength;
        this.itemType = itemType;
        this.story = story;
    }

    @Override
    public JsonObject returnJSON(){
        JsonObjectBuilder question = Json.createObjectBuilder();
        question.add("list",list);
        question.add("story",story);
        question.add("id", id);

        return question.build();
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        int partId = resultNode.get("partId").asInt();

        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO StoryCompletionResultV2(WorkerId,partId,questionId,answer) VALUES(?,?,?,?)"
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
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return null;
    }

    public String getStory() {
        return story;
    }
}
