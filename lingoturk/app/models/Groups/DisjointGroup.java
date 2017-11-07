package models.Groups;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.LingoExpModel;
import models.Questions.Question;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;
import services.LingoturkConfigImplementation;

import javax.inject.Inject;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.Transient;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static play.libs.Json.stringify;
import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

/**
 * Represents a group where each participant is only allowed to answer 1 question
 */
@Entity
@Inheritance
@DiscriminatorValue("DisjointGroup")
public class DisjointGroup extends AbstractGroup {

    /* BEGIN OF VARIABLES BLOCK */

    /* END OF VARIABLES BLOCK */

    @Inject
    public DisjointGroup() {
    }

    /**
     * Caches which question has been assigned last for each group s.t.
     * the proceeding question can be assigned to the next participant.
     */
    @Transient
    static Map<Integer, Integer> fallBack = new HashMap<>();

    /**
     * Publishes this group on Mechanical Turk. As each participant is only allowed to answer
     * one question in this group, only one HIT is created
     *
     * @param service        The service (either Sandbox, or Mechanical Turk itself)
     * @param publishedId    The unique Id that is the same for all groups of an experiment that are published at the same time
     * @param hitTypeId      The hitTypeId (assigned by Mechanical Turk)
     * @param lifetime       How long the study should be running
     * @param maxAssignments How many participants should participate in this group
     * @return The URL of the study that is assigned by Mechanical turk
     * @throws SQLException Propagated from JDBC
     */
    @Override
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException {
        String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                + "<ExternalURL> " + LingoturkConfigImplementation.staticGetStaticIp() + "/renderAMT?id=" + getId() + "&amp;Type=part</ExternalURL>"
                + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";
        HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignments, null, null, null, null, null, null);
        String url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();

        insert(hit.getHITId(), publishedId);

        availability = maxAssignments;
        this.update();

        return url;
    }

    /**
     * Assigns a question in the group represented by {@code groupId} to the worker represented by
     * {@code workerId}. If this worker has already been assigned one
     * (NOTE: This function is called by the router which is why it is not static)
     *
     * @param groupId      The Id of the group
     * @param workerId     The Id of the worker
     * @param assignmentId The assignmentId, assigned by Mechanical Turk
     * @param hitId        The hitId, assigned by Mechanical Turk
     * @return Renders the question that is assigned to the worker
     */
    public Result returnQuestionAsJson(int groupId, String workerId, String assignmentId, String hitId) throws SQLException, IOException {
        DisjointGroup group = (DisjointGroup) DisjointGroup.byId(groupId);

        Worker worker = Worker.getWorkerById(workerId);
        if (worker == null) {
            worker = Worker.createWorker(workerId);
        }

        Worker.Participation participation = worker.getParticipatesInPart(group);
        Question question = null;

        if (assignmentId == null || (!assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") && !assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE_TEST"))) {
            assignmentId = (assignmentId == null) ? "NOT AVAILABLE" : assignmentId;

            if (participation == null) {
                // Worker hasn't participated in the HIT already
                System.out.println(worker.getId() + " takes part in part: " + group.getId());
                question = group.getNextQuestion();

                if (question == null) {
                    while (true) {
                        int fallBackCounter;
                        if (fallBack.containsKey(groupId)) {
                            fallBackCounter = fallBack.get(groupId);
                        } else {
                            fallBackCounter = 0;
                            fallBack.put(groupId, 0);
                        }

                        question = group.getQuestions().get(fallBackCounter);
                        fallBackCounter++;
                        if (fallBackCounter >= group.getQuestions().size()) {
                            fallBackCounter = 0;
                        }
                        fallBack.put(groupId, fallBackCounter);
                        if (!question.disabled) {
                            System.out.println("No Questions available. Fallback to question Nr. " + question.getId());
                            break;
                        }
                    }
                }


                worker.addParticipatesInPart(group, question, null, assignmentId, hitId);
            } else if (participation.getAssignmentID().equals(assignmentId)) {
                System.out.println(worker.getId() + " reloads part: " + group.getId());
                // Worker has already participated in a hit
                question = Question.byId(participation.getQuestionID());
            } else {
                // Worker has already participated but assignmentId has changed
                question = null;
            }
        }

        if (question == null) {
            // just a test -> return random question
            int nr = group.random.nextInt(group.getQuestions().size());
            question = group.getQuestions().get(nr);
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(question.returnJSON().toString());
        return ok(stringify(actualObj));
    }

    /**
     * Renders this group for participants coming from Mechanical Turk. As each participant is
     * only allowed to answer one question, we assign one question to the participant.
     * If a question has already be assigned, this one will be rendered again.
     *
     * @param worker       The worker that is doing the experiment
     * @param assignmentId The assignmentId that was assigned by Mechanical Turk
     * @param hitId        The hitId that was assigned by Mechanical Turk
     * @param turkSubmitTo The url to submit the results to (i.e. either the Sandbox or Mechanical Turk itself)
     * @param exp          The experiment that this group belongs to
     * @param df           df The DynamicForm, possibly containing more parameters
     * @return Returns a random question that belongs to this group
     */
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
                } else if (participation.getAssignmentID().equals(assignmentId)) {
                    System.out.println(worker.getId() + " reloads part: " + getId());
                    // Worker has already participated in a hit
                    Question question = Question.byId(participation.getQuestionID());
                    return question.renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
                } else {
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
}
