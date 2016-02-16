package models.Questions.RephrasingExperiment;

import au.com.bytecode.opencsv.CSVWriter;
import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Results.AssignmentResult;
import org.dom4j.DocumentException;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.SQLException;

import static play.mvc.Results.ok;
@Entity
@Inheritance
@DiscriminatorValue("NewExpQuestion")
public class RephrasingQuestion extends PartQuestion {

    @Column(name="sentence1", columnDefinition = "TEXT")
    String sentence1;

    @Column(name="question1", columnDefinition = "TEXT")
    String question1;

    @Column(name="questionFirst1")
    boolean questionFirst1;

    @Column(name="sentence2", columnDefinition = "TEXT")
    String sentence2;

    @Column(name="question2", columnDefinition = "TEXT")
    String question2;

    @Column(name="questionFirst2")
    boolean questionFirst2;

    String fillerSentence1 = "When Mary finally found the dress she had been looking for, she was very happy.";
    String fillerQuestion1 = "Did Mary find the dress she had been looking for?";

    String fillerSentence2 = "Although John is rather short, he likes riding horses that are very tall.";
    String fillerQuestion2 = "Is John very tall?";

    public RephrasingQuestion(String sentence1, String question1, boolean questionFirst1, String sentence2, String question2, boolean questionFirst2) {
        this.sentence1 = sentence1;
        this.question1 = question1;
        this.questionFirst2 = questionFirst2;
        this.sentence2 = sentence2;
        this.questionFirst1 = questionFirst1;
        this.question2 = question2;
    }

    public RephrasingQuestion() {}

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        String sentence1 = questionNode.get("sentence1").asText();
        String question1 = questionNode.get("question1").asText();
        boolean questionFirst1 = questionNode.get("questionFirst1").asBoolean();
        String sentence2 = questionNode.get("sentence2").asText();
        String question2 = questionNode.get("question2").asText();
        boolean questionFirst2 = questionNode.get("questionFirst2").asBoolean();

        this.sentence1 = sentence1;
        this.question1 = question1;
        this.questionFirst1 = questionFirst1;
        this.sentence2 = sentence2;
        this.question2 = question2;
        this.questionFirst2 = questionFirst2;
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder newQuestionBuilder = Json.createObjectBuilder();
        newQuestionBuilder.add("question1", question1);
        newQuestionBuilder.add("sentence1", sentence1);
        newQuestionBuilder.add("questionFirst1", questionFirst1);
        newQuestionBuilder.add("question2", question2);
        newQuestionBuilder.add("sentence2", sentence2);
        newQuestionBuilder.add("questionFirst2", questionFirst2);
        newQuestionBuilder.add("fillerSentence1",fillerSentence1);
        newQuestionBuilder.add("fillerQuestion1",fillerQuestion1);
        newQuestionBuilder.add("fillerSentence2",fillerSentence2);
        newQuestionBuilder.add("fillerQuestion2",fillerQuestion2);

        return newQuestionBuilder.build();
    }

    @Override
    public Result render(String assignmentId, String hitId, String workerId, String turkSubmitTo, String additionalExplanations) {
        return ok(views.html.renderExperiments.RephrasingExperiment.RephrasingExperiment.render(this, assignmentId, workerId, turkSubmitTo, additionalExplanations));
    }

    public String getFillerQuestion2() {
        return fillerQuestion2;
    }

    public String getFiller2() {
        return fillerSentence2;
    }

    public String getFillerQuestion1() {
        return fillerQuestion1;
    }

    public String getQuestion2() {
        return question2;
    }

    public String getQuestion1() {
        return question1;
    }

    public String getSentence1() {
        return sentence1;
    }

    public boolean isQuestionFirst1() {
        return questionFirst1;
    }

    public String getSentence2() {
        return sentence2;
    }

    public boolean isQuestionFirst2() {
        return questionFirst2;
    }

    public String getFiller1() {
        return fillerSentence1;
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public void writeResults(JsonNode resultNode) {

    }
}
