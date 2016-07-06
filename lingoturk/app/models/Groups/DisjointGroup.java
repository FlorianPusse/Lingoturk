package models.Groups;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.Application;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Questions.Question;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

import static play.libs.Json.stringify;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("DisjointGroup")
public class DisjointGroup extends AbstractGroup {

    /* BEGIN OF VARIABLES BLOCK */

    /* END OF VARIABLES BLOCK */

    public DisjointGroup(){}

    private Random random = new Random();

    @Override
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
    public void setJSONData(LingoExpModel experiment, JsonNode partNode) throws SQLException {
        super.setJSONData(experiment, partNode);
    }

    public Result returnQuestionAsJson(Worker worker, String assignmentId, String hitId) throws SQLException, IOException {
        Worker.Participation participation = worker.getParticipatesInPart(this);
        Question question = null;

        if (!assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") && !assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE_TEST")) {
            if (participation == null) {
                // Worker hasn't participated in the HIT already
                question = getNextQuestion();
                worker.addParticipatesInPart(this, question, null, assignmentId, hitId);
            }else if (participation.getAssignmentID().equals(assignmentId)) {
                System.out.println(worker.getId() + " reloads part: " + getId());
                // Worker has already participated in a hit
                question = Question.byId(participation.getQuestionID());
            }else{
                // Worker has already participated but assignmentId has changed
                question = null;
            }
        }

        // just a test -> return random question
        int nr = random.nextInt(getQuestions().size());
        question =  getQuestions().get(nr);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(question.returnJSON().toString());
        return ok(stringify(actualObj));
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        String workerId = worker.getId();

        try {
            Worker.Participation participation = worker.getParticipatesInPart(this);

            if (!assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") && !assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE_TEST")) {
                if (participation == null) {
                    // Worker hasn't participated in the HIT already
                    Question question = getNextQuestion();
                    worker.addParticipatesInPart(this, question, null, assignmentId, hitId);
                    return question.renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
                }else if (participation.getAssignmentID().equals(assignmentId)) {
                    System.out.println(worker.getId() + " reloads part: " + getId());
                    // Worker has already participated in a hit
                    Question question = Question.byId(participation.getQuestionID());
                    return question.renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
                }else{
                    // Worker has already participated but assignmentId has changed
                    return internalServerError("AssignmentId has changed for [workerId: " + workerId + ", assignmentId: " + assignmentId + "]");
                }
            }

            // just a test -> return random question
            return this.getRandomQuestion(worker, assignmentId, hitId, turkSubmitTo, exp, df);
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerError("Can't communicate with DB.");
        }
    }

    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("id", id);
        objectBuilder.add("number", listNumber);
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for (PartQuestion question : getQuestions()) {
            arrayBuilder.add(question.returnJSON());
        }
        objectBuilder.add("questions", arrayBuilder.build());
        return objectBuilder.build();
    }
}
