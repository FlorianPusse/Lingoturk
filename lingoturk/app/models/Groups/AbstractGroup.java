package models.Groups;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Questions.Question;
import models.Questions.QuestionFactory;
import models.Repository;
import models.Worker;
import play.data.DynamicForm;
import play.db.ebean.Model;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;


@Entity
@Inheritance
@DiscriminatorColumn(length=50)
@Table(name = "Groups")
@DiscriminatorValue("DistinctGroup")
public abstract class AbstractGroup extends Model {

    /* BEGIN OF VARIABLES BLOCK */

    @Id
    @Column(name = "PartId")
    int id;

    @Basic
    int availability;

    @Basic
    protected
    String fileName;

    /* END OF VARIABLES BLOCK */

    public AbstractGroup(){}

    private static Finder<Integer, AbstractGroup> finder = new Finder<>(Integer.class, AbstractGroup.class);

    private Random random = new Random();

    protected List<PartQuestion> questions = null;

    public abstract String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException;

    public synchronized boolean decreaseIfAvailable() throws SQLException {
        boolean answer;
        int availability = getAvailability();
        if(availability > 0){
            setAvailability(availability - 1);
            answer = true;
        }else{
            answer = false;
        }

        System.out.println("Part " + getId() + " availability: " + (availability) + " -> return " + answer);
        return answer;
    }

    public synchronized int getAvailability() throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT availability FROM Groups WHERE PartId=" + this.getId());
        ResultSet rs = statement.executeQuery();

        int result = -1;

        if (rs.next()) {
            result = rs.getInt("availability");
        }

        return result;
    }

    public synchronized void setAvailability(int availability) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("UPDATE Groups SET availability = ? WHERE PartId=" + this.getId());
        statement.setInt(1,availability);
        statement.execute();
    }

    public void insert(String hitID,int publishedId) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("INSERT INTO PartPublishedAs(PartID,mTurkID,publishedId) VALUES(?,?,?)");
        statement.setInt(1, getId());
        statement.setString(2, hitID);
        statement.setInt(3,publishedId);
        statement.execute();
    }

    public PartQuestion getNextQuestion() throws SQLException {
        List<PartQuestion> questions = getQuestions();
        for(PartQuestion question : questions){
            PartQuestion actQuestion = (PartQuestion) question.getIfAvailable();
            if(actQuestion != null){
                return actQuestion;
            }
        }
        return null;
    }

    public void setJSONData(LingoExpModel experiment, JsonNode partNode) throws SQLException {
        List<PartQuestion> questions_tmp = new LinkedList<>();
        // Create Questions
        for(Iterator<JsonNode> questions =  partNode.get("questions").iterator(); questions.hasNext(); ){
            JsonNode questionNode = questions.next();
            questions_tmp.add(QuestionFactory.createQuestion(questionNode.get("type").asText(),experiment,questionNode));
        }
        this.questions = questions_tmp;
    }

    public static AbstractGroup byId(int id) {
        return finder.byId(id);
    }

    public void addExperimentUsedIn(LingoExpModel exp) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement(
                "INSERT INTO LingoExpModels_contain_Parts(LingoExpModelID,PartID) " +
                        "SELECT " + exp.getId() + ", " + this.getId() +
                        " WHERE NOT EXISTS (" +
                        "SELECT * FROM LingoExpModels_contain_Parts WHERE LingoExpModelID=" + exp.getId() + " AND PartID=" + this.getId() +
                        ")");

        statement.execute();
    }

    public Result getRandomQuestion(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) throws SQLException {
        int nr = random.nextInt(getQuestions().size());
        return getQuestions().get(nr).renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
    }

    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder partBuilder = Json.createObjectBuilder();
        JsonArrayBuilder questionsBuilder = Json.createArrayBuilder();

        for (PartQuestion partQuestion : getQuestions()) {
            questionsBuilder.add(partQuestion.returnJSON());
        }

        partBuilder.add("id", id);
        partBuilder.add("questions", questionsBuilder.build());

        return partBuilder.build();
    }

    public Integer getExperimentUsedIn() throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT * FROM LingoExpModels_contain_Parts WHERE PartID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        int result = -1;
        if (rs.next()) {
            result = rs.getInt("LingoExpModelID");
        }
        return result;
    }

    public List<PartQuestion> getQuestions() throws SQLException {
        if (questions == null){
            PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT * FROM Parts_contain_Questions WHERE PartID=" + this.getId());
            ResultSet rs = statement.executeQuery();

            List<PartQuestion> result = new LinkedList<>();
            while (rs.next()) {
                result.add((PartQuestion) Question.byId(rs.getInt("QuestionID")));
            }
            this.questions = result;
            return result;
        }

        return this.questions;
    }

    public abstract Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df);

    public void saveQuestions() throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("INSERT INTO Parts_contain_Questions(PartID,QuestionID) SELECT " + getId() + ", ? " +
                "WHERE NOT EXISTS (" +
                "SELECT * FROM Parts_contain_Questions WHERE PartID= " + getId() + " AND QuestionID= ? " +
                ")");

        for (Question question : questions) {
            statement.setInt(1, question.getId());
            statement.setInt(2, question.getId());
            statement.execute();
        }
    }

    @Override
    public void delete(){
        try {
            for(Question question : getQuestions()){
                question.delete();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.delete();
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Id: " + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractGroup)) return false;

        AbstractGroup group = (AbstractGroup) o;

        return id == group.id;

    }

    @Override
    public int hashCode() {
        return id;
    }

}
