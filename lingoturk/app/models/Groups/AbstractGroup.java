package models.Groups;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import io.ebean.Finder;
import io.ebean.Model;
import models.LingoExpModel;
import models.Questions.Question;
import models.Questions.QuestionFactory;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;
import services.DatabaseServiceImplementation;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Represents an experiment's list. Most of the list's logic is dependent on its type,
 * which is why it is kept in the subclasses that extent it
 */
@Entity
@Inheritance
@DiscriminatorColumn(length = 50)
@Table(name = "Groups")
@DiscriminatorValue("DistinctGroup")
public abstract class AbstractGroup extends Model {

    /* BEGIN OF VARIABLES BLOCK */

    /**
     * The id of this group
     */
    @Id
    @Column(name = "PartId")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "groups_seq")
    public int id;

    /**
     * How often this group is still available.
     * Ideally this value should be exactly maxParticipants - (countParticipants() + # participants
     * that are currently working on this task)
     */
    @Basic
    public int availability;

    /**
     * Whether the group is disabled (won't be scheduled in most cases) or not
     */
    @JsonIgnore
    @Column(name = "disabled", columnDefinition = "boolean default false")
    public boolean disabled;

    /**
     * The name of the group
     */
    @Basic
    @Column(name = "fileName")
    public String fileName;

    /**
     * The number of the group
     */
    @Basic
    @Column(name = "listNumber")
    public Integer listNumber;

    /**
     * How long it takes approximately to answer this group
     */
    @JsonIgnore
    @Basic
    @Column(name = "maxWorkingTime", columnDefinition = "integer default -1")
    public Integer maxWorkingTime;

    /**
     * The maximum number of participants that should do this task.
     */
    @JsonIgnore
    @Basic
    @Column(name = "maxParticipants", columnDefinition = "integer default -1")
    public Integer maxParticipants;

    /* END OF VARIABLES BLOCK */

    /**
     * Play class that enables us to retrieve groups from the database.
     */
    @JsonIgnore
    @Transient
    private static Finder<Integer, AbstractGroup> finder = new Finder<>(AbstractGroup.class);

    /**
     * Used to randomly select questions from this group
     */
    @JsonIgnore
    @Transient
    protected Random random = new Random();

    /**
     * The questions that belong to this group
     */
    @Transient
    protected List<Question> questions = null;

    public AbstractGroup() {
    }

    /**
     * Publishes this group on Mechanical Turk
     *
     * @param service        The service (either Sandbox, or Mechanical Turk itself)
     * @param publishedId    The unique Id that is the same for all groups of an experiment that are published at the same time
     * @param hitTypeId      The hitTypeId (assigned by Mechanical Turk)
     * @param lifetime       How long the study should be running
     * @param maxAssignments How many participants should participate in this group
     * @return The URL of the study that is assigned by Mechanical turk
     * @throws SQLException Propagated from JDBC
     */
    public abstract String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException;

    /**
     * Checks if this group should currently be scheduled or not. If it should,
     * the availability will be decreased by one.
     *
     * @return Whether the group should be scheduled or not
     * @throws SQLException Propagated from JDBC
     */
    public synchronized boolean decreaseIfAvailable() throws SQLException {
        boolean answer;
        int availability = getAvailability();

        if (disabled || availability <= 0 || (maxParticipants != null && maxParticipants > 0 && countParticipants() > maxParticipants)) {
            answer = false;
        } else {
            answer = true;
            setAvailability(availability - 1);
        }

        System.out.println("Part " + getId() + " availability: " + (availability) + " -> return " + answer);
        return answer;
    }

    /**
     * Checks and returns the current availability value
     *
     * @return The current availability value
     * @throws SQLException Propagated from JDBC
     */
    public synchronized int getAvailability() throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT availability FROM Groups WHERE PartId=" + this.getId());
        ResultSet rs = statement.executeQuery();

        int result = -1;

        if (rs.next()) {
            result = rs.getInt("availability");
        }

        return result;
    }

    /**
     * Updates the current availability value
     *
     * @param availability The value to set
     * @throws SQLException Propagated from JDBC
     */
    public synchronized void setAvailability(int availability) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("UPDATE Groups SET availability = ? WHERE PartId=" + this.getId());
        statement.setInt(1, availability);
        statement.execute();
    }

    /**
     * Inserts this group into the list of published groups
     *
     * @param hitID       The hitId that was assigned from Mechanical Turk to this group
     * @param publishedId The unique Id that is the same for all groups of an experiment that are published at the same time
     * @throws SQLException Propagated from JDBC
     */
    public void insert(String hitID, int publishedId) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("INSERT INTO PartPublishedAs(PartID,mTurkID,publishedId) VALUES(?,?,?)");
        statement.setInt(1, getId());
        statement.setString(2, hitID);
        statement.setInt(3, publishedId);
        statement.execute();
    }

    /**
     * Returns the next question that is available. Returns null if none is currently available
     *
     * @return Returns the next question that is available. Returns null if none is currently available
     * @throws SQLException Propagated from JDBC
     */
    public Question getNextQuestion() throws SQLException {
        List<Question> questions = getQuestions();
        for (Question question : questions) {
            Question actQuestion = question.getIfAvailable();
            if (actQuestion != null) {
                return actQuestion;
            }
        }
        return null;
    }

    /**
     * Counts the number of participants that already participated in this group
     *
     * @return The number of participants that already participated in this group
     * @throws SQLException Propagated from JDBC
     */
    public int countParticipants() throws SQLException {
        Statement s = DatabaseServiceImplementation.staticConnection().createStatement();
        ResultSet resultSet = s.executeQuery("SELECT count(*) FROM (SELECT DISTINCT workerId FROM Results WHERE partId = " + getId() + ") AS tmp");

        if (resultSet.next()) {
            return resultSet.getInt(1);
        }

        s.close();

        return -1;
    }

    /**
     * Loads the experimentId and the JSON encoded data into the group.
     *
     * @param experiment The experiment this question belongs to
     * @param partNode   The JSON encoded data
     */
    public void setJSONData(LingoExpModel experiment, JsonNode partNode) throws SQLException {
        List<Question> questions_tmp = new LinkedList<>();
        // Create Questions
        for (Iterator<JsonNode> questions = partNode.get("questions").iterator(); questions.hasNext(); ) {
            JsonNode questionNode = questions.next();
            questions_tmp.add(QuestionFactory.createQuestion(questionNode.get("_type").asText(), experiment, questionNode));
        }
        this.questions = questions_tmp;

        JsonNode fileName = partNode.get("fileName");
        if (fileName != null) {
            this.fileName = fileName.asText();
        }

        JsonNode numberNode = partNode.get("listNumber");
        if (numberNode != null) {
            this.listNumber = numberNode.asInt();
        }
    }

    /**
     * Adds this group the groups that are used in experiment {@code exp}
     *
     * @param exp The experiment that this group shold be added to
     * @throws SQLException Propagated from JDBC
     */
    public void addExperimentUsedIn(LingoExpModel exp) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement(
                "INSERT INTO LingoExpModels_contain_Parts(LingoExpModelID,PartID) " +
                        "SELECT " + exp.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM LingoExpModels_contain_Parts WHERE LingoExpModelID=" + exp.getId() + " AND PartID=" + this.getId() +
                        ")");

        statement.execute();
    }

    /**
     * Looks up and returns the Id of the experiment that this group belongs to
     *
     * @return The Id of the experiment that this group belongs to
     * @throws SQLException Propagated from JDBC
     */
    public Integer getExperimentUsedIn() throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM LingoExpModels_contain_Parts WHERE PartID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        int result = -1;
        if (rs.next()) {
            result = rs.getInt("LingoExpModelID");
        }
        return result;
    }

    /**
     * Selects and renders a random question that belongs to this group
     *
     * @param worker       The worker that is doing the experiment
     * @param assignmentId The assignmentId that was assigned by Mechanical Turk
     * @param hitId        The hitId that was assigned by Mechanical Turk
     * @param turkSubmitTo The url to submit the results to (i.e. either the Sandbox or Mechanical Turk itself)
     * @param exp          The experiment that this group belongs to
     * @param df           The DynamicForm, possibly containing more parameters
     * @return Returns a random question that belongs to this group
     * @throws SQLException Propagated from JDBC
     */
    public Result getRandomQuestion(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) throws SQLException {
        int nr = random.nextInt(getQuestions().size());
        return getQuestions().get(nr).renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
    }

    /**
     * Renders this group for participants coming from Mechanical Turk
     *
     * @param worker       The worker that is doing the experiment
     * @param assignmentId The assignmentId that was assigned by Mechanical Turk
     * @param hitId        The hitId that was assigned by Mechanical Turk
     * @param turkSubmitTo The url to submit the results to (i.e. either the Sandbox or Mechanical Turk itself)
     * @param exp          The experiment that this group belongs to
     * @param df           df The DynamicForm, possibly containing more parameters
     * @return Returns a random question that belongs to this group
     */
    public abstract Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df);

    /**
     * Converts this group to JSON and returns it
     *
     * @return This group encoded as JSON
     * @throws SQLException Propagated from JDBC
     */
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder partBuilder = Json.createObjectBuilder();
        JsonArrayBuilder questionsBuilder = Json.createArrayBuilder();

        for (Question partQuestion : getQuestions()) {
            questionsBuilder.add(partQuestion.returnJSON());
        }

        partBuilder.add("id", id);
        if (fileName != null) {
            partBuilder.add("fileName", fileName);
        }
        if (listNumber != null) {
            partBuilder.add("listNumber", listNumber);
        }
        partBuilder.add("questions", questionsBuilder.build());

        return partBuilder.build();
    }

    /**
     * Looks up and returns all questions that belong to this group
     *
     * @return The list of questions that belong to this group
     * @throws SQLException Propagated from JDBC
     */
    public List<Question> getQuestions() throws SQLException {
        if (questions == null) {
            PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM Parts_contain_Questions WHERE PartID=" + this.getId());
            ResultSet rs = statement.executeQuery();

            List<Question> result = new LinkedList<>();
            while (rs.next()) {
                result.add(Question.byId(rs.getInt("QuestionID")));
            }
            this.questions = result;
            return result;
        }

        return this.questions;
    }

    /**
     * Stores the connection between this group and its questions in the database
     *
     * @throws SQLException Propagated from JDBC
     */
    public void saveQuestions() throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("INSERT INTO Parts_contain_Questions(PartID,QuestionID) SELECT " + getId() + ", ? " +
                "WHERE NOT EXISTS (" +
                "SELECT * FROM Parts_contain_Questions WHERE PartID= " + getId() + " AND QuestionID= ? " +
                ")");

        for (Question question : questions) {
            statement.setInt(1, question.getId());
            statement.setInt(2, question.getId());
            statement.execute();
        }
    }

    /**
     * Deletes this group
     *
     * @return true iff the group was deleted successfully
     */
    @Override
    public boolean delete() {
        try {
            for (Question question : getQuestions()) {
                question.delete();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return super.delete();
    }

    /**
     * Retrieves a group from the database and returns it. If the group does not exist, null is returned instead
     *
     * @param id The id of the group to retrieve
     * @return The retrieved group, if existent. null otherwise
     */
    public static AbstractGroup byId(int id) {
        return finder.byId(id);
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Id: " + id;
    }
}
