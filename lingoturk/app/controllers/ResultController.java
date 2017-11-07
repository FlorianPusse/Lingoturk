package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import models.Questions.Question;
import models.Worker;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import services.DatabaseService;

import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/***
 * Controller handling handling requests related to the submission of new study results
 */
public class ResultController extends Controller {

    private final DatabaseService databaseService;

    @Inject
    public ResultController(final DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    /**
     * Stores feedback we got from a participant for a given experiment, represented by {@code expId}.
     * If an error occurs, an {@code internalServerError} is returned.
     *
     * @return Returns {@code ok} if no problem occurs. {@code internalServerError} otherwise
     */
    @BodyParser.Of(value = BodyParser.Json.class)
    public Result submitFeedback() {
        JsonNode json = request().body().asJson();
        String expId = json.get("expId").asText();
        String workerId = json.get("workerId").asText();
        String feedback = json.get("feedback").asText();

        PreparedStatement statement = null;
        try {
            statement = databaseService.getConnection().prepareStatement("INSERT INTO participantFeedback(id,expId,workerId,feedback) VALUES (nextVal('ParticipantFeedback_Seq'),?,?,?)");
            statement.setString(1, expId);
            statement.setString(2, workerId);
            statement.setString(3, feedback);

            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerError("Could not store feedback.");
        } finally {
            try {statement.close();} catch (SQLException e) {}
        }

        return ok();
    }

    /**
     * Stores results we got from a participant for a given experiment, represented by {@code expId}.
     * If an error occurs, an {@code internalServerError} is returned.
     *
     * @return Returns {@code ok} if no problem occurs. {@code internalServerError} otherwise
     */
    @BodyParser.Of(value = BodyParser.Json.class)
    public Result submitResults() {
        JsonNode json = request().body().asJson();
        String experimentType = json.get("experimentType").asText();

        try {
            if (json.get("statistics") != null && json.get("workerId") != null) {
                String origin = json.get("origin") != null ? json.get("origin").asText() : null;
                String statistics = json.get("statistics").toString();
                String workerId = json.get("workerId").asText();
                int expId = json.get("expId").asInt();

                Worker w = Worker.getWorkerById(workerId);
                w.addStatistics(expId, origin, statistics);
            }

            Question question = new Question();
            question.writeResults(json);
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerError("Could not store results.");
        }

        return ok();
    }

}
