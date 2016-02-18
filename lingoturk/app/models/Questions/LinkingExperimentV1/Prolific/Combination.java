package models.Questions.LinkingExperimentV1.Prolific;

import au.com.bytecode.opencsv.CSVWriter;
import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.SQLException;


@Entity
@Inheritance
@DiscriminatorValue("Combination")
public class Combination extends PartQuestion {

    @Basic
    int lhs;

    @Basic
    int rhs;

    public Combination(int[] c) {
        lhs = c[0];
        rhs = c[1];
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        throw new RuntimeException("Method \"setJSONData\" not implemented for class \"Combination\"");
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("id", id);
        objectBuilder.add("lhs", lhs);
        objectBuilder.add("rhs", rhs);
        return objectBuilder.build();
    }

    @Override
    public void writeResults(JsonNode resultNode) {
        // This should never be called
        throw new RuntimeException("Called \"writeResults\" on Combination class.");
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return null;
    }

    public int getLhs() {
        return lhs;
    }

    public int getRhs() {
        return rhs;
    }
}
