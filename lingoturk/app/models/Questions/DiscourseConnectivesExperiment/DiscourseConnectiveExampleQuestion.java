package models.Questions.DiscourseConnectivesExperiment;

import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.ExampleQuestion;
import models.Repository;
import models.Word;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import static play.mvc.Results.ok;
import static play.mvc.Results.redirect;

/**
 * DND-Example consisting of the two Sentences and the proposed answer
 */

@Entity
@Inheritance
@DiscriminatorValue("DNDExpEQuestion")
public class DiscourseConnectiveExampleQuestion extends DiscourseConnectivesQuestion implements ExampleQuestion {

    private static Finder<Integer, DiscourseConnectiveExampleQuestion> finder = new Finder<Integer, DiscourseConnectiveExampleQuestion>(Integer.class, DiscourseConnectiveExampleQuestion.class);

    public DiscourseConnectiveExampleQuestion(String sentence1, String sentence2, int experimentID) {
        this.sentence1 = sentence1;
        this.sentence2 = sentence2;
        this.experimentID = experimentID;
    }

    public static DiscourseConnectiveExampleQuestion createExampleQuestion(String sentence1, String sentence2, String proposedAnswers, String possibleConnectives, int experimentID) throws SQLException {
        List<Word> proposedAnswers_tmp = new LinkedList<>();
        for (String s : proposedAnswers.split(",")) {
            try {
                proposedAnswers_tmp.add(Word.createWord(s));
            } catch (SQLException e) {

            }
        }

        List<Word> possibleConnectives_tmp = new LinkedList<>();
        for (String s : possibleConnectives.split(",")) {
            try {
                possibleConnectives_tmp.add(Word.createWord(s));
            } catch (SQLException e) {

            }
        }

        DiscourseConnectiveExampleQuestion result = new DiscourseConnectiveExampleQuestion(sentence1, sentence2, experimentID);
        result.save();

        for (Word w : proposedAnswers_tmp) {
            w.addExampleQuestionUsedIn(result);
        }

        for (Word w : possibleConnectives_tmp) {
            w.addExampleQuestionPossibleIn(result);
        }

        return result;
    }

    public static ExampleQuestion createExampleQuestion(LingoExpModel experiment, JsonNode question) throws SQLException {
        String sentence1 = question.get("sentence1").asText();
        String sentence2 = question.get("sentence2").asText();
        String connectives = question.get("connectives").asText();
        String possibleConnectives = question.get("possibleConnectives").asText();

        DiscourseConnectiveExampleQuestion eq = DiscourseConnectiveExampleQuestion.createExampleQuestion(sentence1, sentence2, connectives, possibleConnectives, experiment.getId());
        ExampleQuestion.EQ.addUsedInExperiments(experiment,eq);

        return eq;
    }

    public List<Word> getProposedAnswers() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM ExampleQuestions_haveRightAnswer_Words WHERE QuestionID=" + this.getId());

        List<Word> result = new LinkedList<Word>();
        while (rs.next()) {
            result.add(Word.getWordbyId(rs.getInt("WordID")));
        }

        return result;
    }

    public List<Word> getPossibleConnectives() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM ExampleQuestions_havePossible_Words WHERE QuestionID=" + this.getId());

        List<Word> result = new LinkedList<Word>();
        while (rs.next()) {
            result.add(Word.getWordbyId(rs.getInt("WordID")));
        }

        return result;
    }

    public static DiscourseConnectiveExampleQuestion byId(int id) {
        return finder.byId(id);
    }

    @Override
    public Result render() {
        return ok();
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder exampleQuestionBuilder = Json.createObjectBuilder();
        exampleQuestionBuilder.add("sentence1", this.sentence1);
        exampleQuestionBuilder.add("sentence2", this.sentence2);

        List<String> words = new LinkedList<>();
        for (Word w : this.getProposedAnswers()) {
            words.add(w.getWord());
        }

        String connectives = org.apache.commons.lang.StringUtils.join(words, ",");
        exampleQuestionBuilder.add("connectives", connectives);

        words = new LinkedList<>();
        for (Word w : this.getPossibleConnectives()) {
            words.add(w.getWord());
        }
        connectives = org.apache.commons.lang.StringUtils.join(words, ",");
        exampleQuestionBuilder.add("possibleConnectives", connectives);

        return exampleQuestionBuilder.build();
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return redirect("https://www.youtube.com/watch?v=0FFiXDrAhFM");
    }

}
