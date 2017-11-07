package models.Questions;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import controllers.RenderController;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import models.LingoExpModel;
import models.Worker;
import play.data.DynamicForm;
import play.data.validation.Constraints;
import play.mvc.Result;
import play.twirl.api.Html;
import services.DatabaseServiceImplementation;
import views.html.ExperimentRendering.experiment_main;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorColumn(length = 100)
@Table(name = "Questions")
@DiscriminatorValue("DEFAULTTYPE")
@MappedSuperclass
public class Question extends Model {

    /**
     * The unique id of this question
     */
    @Id
    @Column(name = "Questionid")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "questions_seq")
    public int id;

    /**
     * The id of the experiment this question belongs to
     */
    @JsonIgnore
    @Basic
    @Constraints.Required
    @Column(name = "LingoExpModelId")
    public int experimentID;

    /**
     * The number of times this questions still should be answered
     */
    @JsonIgnore
    @Column(name = "Availability", columnDefinition = "integer default 1")
    public int availability;

    /**
     * Whether the question is currently disabled
     */
    @JsonIgnore
    @Column(name = "disabled", columnDefinition = "boolean default false")
    public boolean disabled;

    /**
     * The sublist this question belongs to
     */
    @Basic
    @Column(name = "subList", columnDefinition = "varchar(255) default ''")
    public String subList;

    /**
     * The type of the experiment this question belongs to
     */
    @Basic
    @Column(name = "experimentType", columnDefinition = "varchar(100)")
    public String experimentType;

    /**
     * The actual data of the question (encoded as JSON in the database)
     */
    @DbJson
    public
    Map<String, Object> data;

    @JsonIgnore
    private static Finder<Integer, Question> finder = new Finder<>(Question.class);

    public static Question byId(int id) {
        return finder.byId(id);
    }

    /**
     * Returns this question, if its availability is > 0. Null otherwise
     *
     * @return The question if its availability is > 0. Null otherwise
     * @throws SQLException Propagated from JDBC
     */
    public synchronized Question getIfAvailable() throws SQLException {
        if (disabled) {
            return null;
        }

        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM Questions WHERE QuestionID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        if (rs.next()) {
            this.availability = rs.getInt("Availability");
        }

        if (availability > 0) {
            this.availability--;
            statement = DatabaseServiceImplementation.staticConnection().prepareStatement("UPDATE Questions SET Availability = ? WHERE QuestionID = ?");
            statement.setInt(1, this.availability);
            statement.setInt(2, this.getId());
            statement.execute();
            return this;
        }

        return null;
    }

    /**
     * Converts and returns this question as JSON. Returns null if an internal error occurs
     *
     * @return The JSON representation of this question, or null, if an internal error occurs
     */
    public JsonObject returnJSON() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new Question.JsonRestriction());
        mapper.setPropertyNamingStrategy(new Question.JsonNaming());
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        try {
            JsonObject jsonRepresentation = Json.createReader(new StringReader(mapper.writeValueAsString(this))).readObject();
            JsonObjectBuilder builder = Json.createObjectBuilder();
            jsonRepresentation.forEach((key, value) -> {
                if (!key.equals("data")) {
                    builder.add(key, value);
                }
            });
            jsonRepresentation.getJsonObject("data").forEach(builder::add);

            return builder.build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Stores the results (encoded in {@code resultNode}) after this question has been answered by a user
     *
     * @param resultNode The JSON node containing the results
     * @throws SQLException             Propagated from JDBC
     * @throws IllegalArgumentException If the experiment does not exist.
     */
    public void writeResults(JsonNode resultNode) throws SQLException, IllegalArgumentException {
        String workerId = resultNode.get("workerId").asText();
        String assignmentId = resultNode.get("assignmentId").asText();
        String hitId = resultNode.get("hitId").asText();
        int partId = resultNode.get("partId") != null ? resultNode.get("partId").asInt() : -1;

        int expId = -1;
        JsonNode expIdNode = resultNode.get("expId");
        if (expIdNode != null) {
            expId = expIdNode.asInt();
        }
        LingoExpModel exp = LingoExpModel.byId(expId);
        if (exp == null) {
            throw new IllegalArgumentException("Unknown experimentId: " + expId);
        }

        String origin = "NOT AVAILABLE";
        JsonNode originNode = resultNode.get("origin");
        if (originNode != null) {
            origin = originNode.asText();
        }

        JsonNode statisticsNode = resultNode.get("statistics");
        if (statisticsNode != null) {
            String statistics = statisticsNode.toString();
            Worker worker = Worker.getWorkerById(workerId);
            worker.addStatistics(expId, origin, statistics);
        }

        for (Iterator<JsonNode> questionNodeIterator = resultNode.get("results").iterator(); questionNodeIterator.hasNext(); ) {
            JsonNode questionNode = questionNodeIterator.next();
            int questionId = questionNode.get("id").asInt();
            JsonNode result = questionNode.get("answer");

            if (result != null) {
                PreparedStatement preparedStatement = DatabaseServiceImplementation.staticConnection().prepareStatement(
                        "INSERT INTO Results(id,workerId,assignmentId,hitId,partId,questionId,answer,origin, experimentType) VALUES(nextval('results_seq'),?,?,?,?,?,to_json(?),?,?)"
                );
                preparedStatement.setString(1, workerId);
                preparedStatement.setString(2, assignmentId);
                preparedStatement.setString(3, hitId);
                preparedStatement.setInt(4, partId);
                preparedStatement.setInt(5, questionId);
                preparedStatement.setString(6, result.toString());
                preparedStatement.setString(7, origin);
                preparedStatement.setString(8, exp.getExperimentType());

                preparedStatement.execute();
                preparedStatement.close();
            }
        }
    }

    /**
     * Renders this question for participants coming from Mechanical Turk
     *
     * @param worker       The id of the worker
     * @param assignmentId The assignmentId assigned by Mechanical Turk
     * @param hitId        The HITid assigned by Mechanical Turk
     * @param turkSubmitTo Whether the question was answered on the Mechanical Turk Sandbox or Mechanical Turk itself
     * @param exp          The experiment this question belongs to
     * @param df           The DynamicForm containing additional parameters
     * @return The renedered experiment
     */
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        Html html = RenderController.getExperimentContent(exp);
        return ok(experiment_main.render(exp.getExperimentType(), this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK", html));
    }

    /**
     * Implementation of our restriction to ignore fields declared by Ebean when converting an object to JSON
     */
    private static class JsonRestriction extends JacksonAnnotationIntrospector {
        @Override
        public boolean hasIgnoreMarker(final AnnotatedMember m) {
            return m.getDeclaringClass() == io.ebean.Model.class || m.getName().contains("_ebean_") || super.hasIgnoreMarker(m);
        }
    }

    /**
     * Implements our own naming convention for our own Question classes.
     */
    private class JsonNaming extends PropertyNamingStrategy {
        @Override
        public String nameForField(MapperConfig config, AnnotatedField field, String defaultName) {
            if (Question.class.isAssignableFrom(field.getDeclaringClass())) {
                String className = field.getDeclaringClass().getSimpleName();
                String experimentName = className.substring(0, className.length() - "Question".length());
                return defaultName.replaceFirst(experimentName + "_", "");
            }
            return defaultName;
        }
    }

    /**
     * Loads the experimentId and the JSON encoded data into the question.
     *
     * @param experiment   The experiment this question belongs to
     * @param questionNode The JSON encoded data
     */
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) {
        experimentID = experiment.getId();

        ObjectMapper mapper = new ObjectMapper();
        data = mapper.convertValue(questionNode, Map.class);
    }

    /**
     * Adds this question to the list of published questions
     *
     * @param hitID       The hitId that was assigned to this question by Mechanical Turk
     * @param publishedId The id that is used for all questions published at the same time
     * @throws SQLException Propagated from JDBC
     */
    public void insert(String hitID, int publishedId) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("INSERT INTO QuestionPublishedAs(QuestionID,mTurkID,publishedId) VALUES(?,?,?)");
        statement.setInt(1, getId());
        statement.setString(2, hitID);
        statement.setInt(3, publishedId);
        statement.execute();
    }

    public int getExperimentID() {
        return experimentID;
    }

    public int getId() {
        return id;
    }
}
