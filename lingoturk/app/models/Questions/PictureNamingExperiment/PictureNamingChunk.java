package models.Questions.PictureNamingExperiment;

import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.DatabaseController;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.JsonObject;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("PictureNamingExperiment.ChunkQuestion")
public class PictureNamingChunk extends PartQuestion {

    /* BEGIN OF VARIABLES BLOCK */

    @Basic
    @Column(columnDefinition = "TEXT", name = "PictureNaming_text")
    public String PictureNamingChunk_number;

    @OneToMany(cascade = CascadeType.ALL)
    public List<PictureNamingQuestion> pictures = new LinkedList<>();

    /* END OF VARIABLES BLOCK */

    public PictureNamingChunk(){}

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.PictureNamingExperiment.PictureNamingExperiment_render.render(this, null,worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        String number = questionNode.get("number").asText();

        List<PictureNamingQuestion> questions = new LinkedList<>();
        for (Iterator<JsonNode> pictureIterator = questionNode.get("pictures").iterator(); pictureIterator.hasNext(); ) {
            JsonNode pictureNode = pictureIterator.next();
            questions.add(new PictureNamingQuestion(pictureNode));
        }

        this.PictureNamingChunk_number = number;
        this.pictures = questions;
    }

    public static Result submitMailAddress(String mailAddress, String workerId) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                "INSERT INTO PictureNamingMailAddress(id,workerId,mailAddress) VALUES(nextval('PictureNamingResults_seq'),?,?)"
        );
        statement.setString(1, workerId);
        statement.setString(2, mailAddress);
        statement.execute();
        statement.close();
        return ok();
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        int partId = resultNode.get("partId").asInt();
        int chunkId = resultNode.get("chunkId").asInt();

        PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                "INSERT INTO PictureNamingResults(id,WorkerId,partId,chunkId,pictureId,answer) VALUES(nextval('PictureNamingResults_seq'), ?,?,?,?,?)"
        );

        statement.setString(1, workerId);
        statement.setInt(2, partId);
        statement.setInt(3, chunkId);

        for (Iterator<JsonNode> resultIterator = resultNode.get("answers").iterator(); resultIterator.hasNext(); ) {
            JsonNode result = resultIterator.next();
            int pictureId = result.get("pictureId").asInt();
            String answer = result.get("answer").asText();

            statement.setInt(4, pictureId);
            statement.setString(5, answer);

            statement.execute();
        }

        statement.close();
    }
}
