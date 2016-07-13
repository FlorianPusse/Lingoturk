package models.Questions._TEMPLATE_Experiment;


import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import play.data.DynamicForm;
import play.mvc.Result;
import javax.json.JsonObject;
import javax.persistence.*;
import java.sql.SQLException;
import java.util.*;
import models.Repository;
import java.sql.PreparedStatement;

import static play.mvc.Results.ok;

// _IMPORT_PLACEHOLDER_

@Entity
@Inheritance
@DiscriminatorValue("_TEMPLATE_Experiment._TEMPLATE_Question")
public class _TEMPLATE_Question extends PartQuestion {

// _VARIABLES_PLACEHOLDER_

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String workerId = resultNode.get("workerId").asText();
        String assignmentId = resultNode.get("assignmentId").asText();
        String hitId = resultNode.get("hitId").asText();
        int partId = resultNode.get("partId").asInt();

        for(Iterator<JsonNode> questionNodeIterator = resultNode.get("results").iterator(); questionNodeIterator.hasNext(); ){
            JsonNode questionNode = questionNodeIterator.next();
            int questionId = questionNode.get("id").asInt();
            JsonNode result = questionNode.get("answer");

            if(result != null){
                PreparedStatement preparedStatement = Repository.getConnection().prepareStatement(
                        "INSERT INTO _TEMPLATE_Results(id,workerId,assignmentId,hitId,partId,questionId,answer) VALUES(nextval('_TEMPLATE_Results_seq'),?,?,?,?,?,?)"
                );
                preparedStatement.setString(1,workerId);
                preparedStatement.setString(2,assignmentId);
                preparedStatement.setString(3,hitId);
                preparedStatement.setInt(4,partId);
                preparedStatement.setInt(5,questionId);
                preparedStatement.setString(6,result.toString());

                preparedStatement.execute();
                preparedStatement.close();
            }
        }
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        return super.returnJSON();
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering._TEMPLATE_Experiment._TEMPLATE_Experiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

}