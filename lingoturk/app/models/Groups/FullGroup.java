package models.Groups;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Questions.StoryCompletionExperiment.StoryCompletionQuestion;
import models.Worker;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static play.mvc.Results.ok;


@Entity
@Inheritance
@DiscriminatorValue("FullGroup")
public class FullGroup extends AbstractGroup {

    public FullGroup(){}

    public FullGroup(String fileName) {
        this.fileName = fileName;
    }

    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException {

        String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                + "<ExternalURL> " + Application.getStaticIp() + "/render?id=" + getId() + "&amp;Type=part</ExternalURL>"
                + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";
        HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignments, null, null, null, null, null, null);
        String url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();

        insert(hit.getHITId(), publishedId);

        availability = maxAssignments;
        this.update();

        return url;
    }

    @Override
    public Result render(Worker worker, String assignmentId, String hitId, String workerId, String turkSubmitTo, LingoExpModel exp) throws SQLException {
        return ok(views.html.renderExperiments.StoryCompletionExperiment.storyCompletionExperiment.render(exp));
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode partNode) throws SQLException {
        super.setJSONData(experiment,partNode);

        String fileName = partNode.get("fileName").asText();
        this.fileName = fileName;
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder partBuilder = Json.createObjectBuilder();
        JsonArrayBuilder questionsBuilder = Json.createArrayBuilder();

        List<PartQuestion> questions = getQuestions();
        for (StoryCompletionQuestion q : StoryCompletionQuestion.fillers) {
            questions.add(q);
        }

        Collections.shuffle(questions);

        for (PartQuestion partQuestion : getQuestions()) {
            questionsBuilder.add(partQuestion.returnJSON());
        }

        partBuilder.add("questions", questionsBuilder.build());

        return partBuilder.build();
    }

}
