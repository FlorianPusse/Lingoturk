package models.Questions.RephrasingExperiment;

import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.ExampleQuestion;
import play.data.validation.Constraints;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.SQLException;
import java.util.Random;

import static play.mvc.Results.ok;

/**
 * DND-Example consisting of the two Sentences and the proposed answer
 */

@Entity
@Inheritance
@DiscriminatorValue("NewExpEQuestion")
public class RephrasingExampleQuestion extends RephrasingQuestion implements ExampleQuestion {

    private static Random random = new Random();

    @Column(name="restatement", columnDefinition = "TEXT")
    @Constraints.Required
    private String restatement;

    @Basic
    private boolean answer;

    @Basic
    private String sentence;

    @Basic
    private String question;

    @Basic
    private boolean questionFirst;

    private static Finder<Integer, RephrasingExampleQuestion> finder = new Finder<Integer, RephrasingExampleQuestion>(Integer.class, RephrasingExampleQuestion.class);

    public RephrasingExampleQuestion(String sentence, String question, String restatement, boolean answer, int experimentID) {
        this.sentence = sentence;
        this.question = question;
        this.restatement = restatement;
        this.answer = answer;
        this.experimentID = experimentID;
    }

    public static RephrasingExampleQuestion createExampleQuestion(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        String sentence = questionNode.get("sentence").asText();
        String question = questionNode.get("question").asText();
        String restatement = questionNode.get("restatement").asText();
        boolean answer = questionNode.get("answer").asBoolean();

        RephrasingExampleQuestion eq = new RephrasingExampleQuestion(sentence,question, restatement, answer, experiment.getId());
        if (random.nextInt(2) == 1){
            eq.questionFirst = true;
        }

        eq.save();
        ExampleQuestion.EQ.addUsedInExperiments(experiment,eq);

        return eq;
    }

    public static RephrasingExampleQuestion byId(int id) {
        return finder.byId(id);
    }

    public Result render() {
        return ok(views.html.renderExperiments.RephrasingExperiment.RephrasingExperimentExampleQuestion.render(this));
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder exampleQuestionBuilder = Json.createObjectBuilder();
        exampleQuestionBuilder.add("sentence", this.sentence);
        exampleQuestionBuilder.add("question", this.question);
        exampleQuestionBuilder.add("restatement", this.restatement);
        exampleQuestionBuilder.add("answer", this.answer);

        return exampleQuestionBuilder.build();
    }

    public String getRestatement() {
        return restatement;
    }

    public boolean getAnswer() {
        return answer;
    }

    public String getQuestion() {
        return question;
    }

    public String getSentence() {
        return sentence;
    }

}
