package models.Questions.DiscourseConnectivesExperiment;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import models.LingoExpModel;
import models.Repository;
import models.Word;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("DiscourseConnectivesExperiment.CDQuestion")
public class CheaterDetectionQuestion extends DiscourseConnectivesQuestion {


    public CheaterDetectionQuestion(String sentence1, String sentence2, int experimentID, String sentenceType) {
        this.sentence1 = sentence1;
        this.sentence2 = sentence2;
        this.experimentID = experimentID;
        this.sentenceType = sentenceType;
    }

    public static CheaterDetectionQuestion createCheaterDetectionQuestion(String sentence1, String sentence2, List<Word> proposedConnectives, List<Word> mustNotHaveConnectives, int experimentID, String sentenceType) throws SQLException {
        CheaterDetectionQuestion result = new CheaterDetectionQuestion(sentence1, sentence2, experimentID, sentenceType);
        result.save();

        for (Word w : proposedConnectives) {
            w.addContainedAsProposedConnective(result);
        }

        for (Word w : mustNotHaveConnectives) {
            w.addContainedAsMustNotHaveConnective(result);
        }

        return result;
    }

    public static List<CheaterDetectionQuestion> createCheaterDetectionQuestions(LingoExpModel experiment, JsonNode node) throws SQLException {
        List<CheaterDetectionQuestion> cheaterDetectionQuestions_tmp = new LinkedList<CheaterDetectionQuestion>();
        for (Iterator<JsonNode> cheaterDetectionQuestions = node.iterator(); cheaterDetectionQuestions.hasNext(); ) {
            JsonNode question = cheaterDetectionQuestions.next();

            String sentence1 = question.get("sentence1").asText();
            String sentence2 = question.get("sentence2").asText();
            String sentenceType = question.get("sentenceType").asText();
            List<Word> proposedConnectives = null;
            List<Word> mustNotHaveConnectives = null;
            try {
                proposedConnectives = Word.createWords(question.get("proposedConnectives"));
                mustNotHaveConnectives = Word.createWords(question.get("mustNotHaveConnectives"));
            } catch (SQLException e) {
                e.printStackTrace();
            }

            CheaterDetectionQuestion cdq = CheaterDetectionQuestion.createCheaterDetectionQuestion(sentence1, sentence2, proposedConnectives, mustNotHaveConnectives, experiment.getId(), sentenceType);
            cdq.addUsedInExperiments(experiment);
            cheaterDetectionQuestions_tmp.add(cdq);
        }
        return cheaterDetectionQuestions_tmp;
    }


    public void addUsedInExperiments(LingoExpModel exp) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO LingoExpModels_contain_CheaterDetectionQuestions(LingoExpModelID,QuestionID) " +
                        "SELECT " + exp.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM LingoExpModels_contain_CheaterDetectionQuestions WHERE LingoExpModelID=" + exp.getId() + " AND QuestionID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public List<Word> getProposedConnectives() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM CheaterDetectionQuestions_mustHave_Words WHERE QuestionID=" + this.getId());

        List<Word> result = new LinkedList<Word>();
        while (rs.next()) {
            result.add(Word.getWordbyId(rs.getInt("WordID")));
        }

        return result;
    }

    public List<Word> getMustNotHaveConnectives() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM CheaterDetectionQuestions_mustNotHave_Words WHERE QuestionID=" + this.getId());

        List<Word> result = new LinkedList<Word>();
        while (rs.next()) {
            result.add(Word.getWordbyId(rs.getInt("WordID")));
        }

        return result;
    }

    public List<String> getProposedConnectives_asString() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM CheaterDetectionQuestions_mustHave_Words WHERE QuestionID=" + this.getId());

        List<String> result = new LinkedList<String>();
        while (rs.next()) {
            result.add(Word.getWordbyId(rs.getInt("WordID")).getWord());
        }

        return result;
    }

    public List<String> getMustNotHaveConnectives_asString() throws SQLException {
        Statement statement = Repository.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT * FROM CheaterDetectionQuestions_mustNotHave_Words WHERE QuestionID=" + this.getId());

        List<String> result = new LinkedList<String>();
        while (rs.next()) {
            result.add(Word.getWordbyId(rs.getInt("WordID")).getWord());
        }

        return result;
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.DiscourseConnectivesExperiment.DiscourseConnectivesExperiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

    public String publish(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException {

        String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                + "<ExternalURL> " + Application.getStaticIp() + "/render?id=" + getId() + "&amp;Type=question</ExternalURL>"
                + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";
        HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignments, null, null, null, null, null, null);
        String url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();

        insert(hit.getHITId(), publishedId);

        return url;
    }


    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder cheaterDetectionQuestionBuilder = Json.createObjectBuilder();
        cheaterDetectionQuestionBuilder.add("sentence1", this.sentence1);
        cheaterDetectionQuestionBuilder.add("sentence2", this.sentence2);
        cheaterDetectionQuestionBuilder.add("type", "CD_Q");
        cheaterDetectionQuestionBuilder.add("sentenceType", this.getSentenceType());

        JsonArrayBuilder proposedConnectivesBuilder = Json.createArrayBuilder();
        for (Word w : getProposedConnectives()) {
            proposedConnectivesBuilder.add(w.getWord());
        }
        cheaterDetectionQuestionBuilder.add("proposedConnectives", proposedConnectivesBuilder.build());

        JsonArrayBuilder mustNotHaveConnectvesBuilder = Json.createArrayBuilder();
        for (Word w : getMustNotHaveConnectives()) {
            mustNotHaveConnectvesBuilder.add(w.getWord());
        }
        cheaterDetectionQuestionBuilder.add("mustNotHaveConnectives", mustNotHaveConnectvesBuilder.build());

        return cheaterDetectionQuestionBuilder.build();
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
