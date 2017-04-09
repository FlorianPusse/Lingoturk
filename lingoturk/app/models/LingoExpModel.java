package models;

import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;

import controllers.DatabaseController;
import models.Groups.AbstractGroup;
import models.Questions.ExampleQuestion;

import models.Questions.Question;
import play.data.validation.Constraints;
import play.db.ebean.Model;
import play.mvc.Result;

import static play.mvc.Results.ok;

@Entity
@Table(name="LingoExpModels")
public class LingoExpModel extends Model {

    /* BEGIN OF VARIABLES BLOCK */

    @Id
    @Column(name="LingoExpModelID")
    protected int id;

    @Basic
    @Column(name="name")
    @Constraints.Required
    protected String name;

    @Basic
    @Column(name="description", columnDefinition = "TEXT")
    protected String description;

    @Basic
    @Column(name="nameOnAmt")
    protected String nameOnAmt;

    @Basic
    @Column(name="descriptionOnAmt")
    protected String descriptionOnAmt;

    @Constraints.Required
    @Column(name="additionalExplanations", columnDefinition = "TEXT")
    protected String additionalExplanations;

    @Basic
    @Column(name="experimentType")
    protected String experimentType;

    @Basic
    @Column(name="listType", columnDefinition = "varchar(255) default 'DISJOINT LISTS'")
    protected String listType;

    /* END OF VARIABLES BLOCK */

    private static Finder<Integer,LingoExpModel> finder = new Finder<Integer,LingoExpModel>(Integer.class,LingoExpModel.class);

    public LingoExpModel(String name, String description, String additionalExplanations, String nameOnAmt, String experimentType, String listType) {
        this.name = name;
        this.description = description;
        this.additionalExplanations = additionalExplanations;

        if (nameOnAmt.isEmpty()) {
            nameOnAmt = name;
        }

        this.nameOnAmt = nameOnAmt;
        this.experimentType = experimentType;
    }

    public static LingoExpModel createLingoExpModel(String name, String description, String additionalExplanations, String nameOnAmt, String experimentType, String listType) {
        LingoExpModel result = new LingoExpModel(name,description,additionalExplanations,nameOnAmt,experimentType, listType);
        result.save();
        return result;
    }

    /**
     * Returns a list of all experiments.
     *
     * @return List of experiments
     */
    public static List<LingoExpModel> getAllExperiments() throws SQLException {
        return finder.all();
    }

    public Result modify() {
        return ok(views.html.ManageExperiments.modifyDND.render(this.id));
    }

    /**
     * Searchs for an entity of the LingoExpModel class and an id, which is saved in the DB
     *
     * @param id The entities id
     * @return Object of type LingoExpModel
     */
    public static LingoExpModel byId(int id) {
        return finder.byId(id);
    }

    public void setBlockedWorkers(List<Worker> workerList) throws SQLException {
        Statement statement = DatabaseController.getConnection().createStatement();
        statement.execute("DELETE FROM Workers_areBlockedFor_LingoExpModels WHERE LingoExpModelID=" + this.getId());
        addBlockedWorkers(workerList);
    }

    public void addBlockedWorkers(List<Worker> workerList) throws SQLException {
        for (Worker w : workerList) {
            w.addIsBlockedFor(this);
        }
    }

    /*public List<CheaterDetectionQuestion> getCheaterDetectionQuestions() throws SQLException {
        List<CheaterDetectionQuestion> result = new LinkedList<CheaterDetectionQuestion>();

        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM LingoExpModels_contain_CheaterDetectionQuestions WHERE LingoExpModelID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            result.add((CheaterDetectionQuestion) Question.byId(rs.getInt("QuestionID")));
        }

        return result;
    }

    public void setCheaterDetectionQuestions(List<CheaterDetectionQuestion> cheaterDetectionQuestions) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("DELETE FROM LingoExpModels_contain_CheaterDetectionQuestions WHERE LingoExpModelID=" + this.getId());
        statement.execute();

        for (CheaterDetectionQuestion cdq : cheaterDetectionQuestions) {
            cdq.addUsedInExperiments(this);
        }
    }*/

    public List<ExampleQuestion> getExampleQuestions() throws SQLException {
        List<ExampleQuestion> result = new LinkedList<ExampleQuestion>();

        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM LingoExpModels_contain_ExampleQuestions WHERE LingoExpModelID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            result.add((ExampleQuestion) Question.byId(rs.getInt("QuestionID")));
        }

        return result;
    }

    public void setExampleQuestions(List<ExampleQuestion> exampleQuestions) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("DELETE FROM LingoExpModels_contain_ExampleQuestions WHERE LingoExpModelID=" + this.getId());
        statement.execute();

        for (ExampleQuestion eq : exampleQuestions) {
            ExampleQuestion.EQ.addUsedInExperiments(this,eq);
        }
    }

    public List<Worker> getBlockedWorkers() throws SQLException {
        List<Worker> result = new LinkedList<>();

        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM Workers_areBlockedFor_LingoExpModels WHERE LingoExpModelID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            result.add(Worker.getWorkerById(rs.getString("WorkerID")));
        }

        return result;
    }

    public List<Worker> getParticipatingWorkers() throws SQLException {
        List<Worker> result = new LinkedList<>();

        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM Workers_participateIn_LingoExpModels WHERE LingoExpModelID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            result.add(Worker.getWorkerById(rs.getString("WorkerID")));
        }

        return result;
    }

    public List<AbstractGroup> getParts() throws SQLException {
        List<AbstractGroup> result = new LinkedList<>();

        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM LingoExpModels_contain_Parts WHERE LingoExpModelID=" + this.getId() + " ORDER BY LingoExpModelID,PartId");
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            result.add(AbstractGroup.byId(rs.getInt("PartID")));
        }

        return result;
    }

    @Override
    public void delete(){
        try {
            /*for (CheaterDetectionQuestion cheaterDetectionQuestion : getCheaterDetectionQuestions()) {
                cheaterDetectionQuestion.delete();
            }*/
            for (ExampleQuestion exampleQuestion : getExampleQuestions()) {
                Question question = (Question) exampleQuestion;
                question.delete();
            }
            for (AbstractGroup part : getParts()) {
                part.delete();
            }
        } catch (SQLException sqlE){
            throw new RuntimeException("An error occured while deleteing the experiment:\n" + sqlE.getMessage());
        }
        super.delete();
    }

    public int publish(long lifetime, String url, String destination) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM publishLingoExpModel(?,?,?,?)");
        statement.setInt(1,getId());
        statement.setLong(2,lifetime);
        statement.setString(3,url);
        statement.setString(4,destination);

        ResultSet rs = statement.executeQuery();
        int result = -1;
        if (rs.next()) {
            result = rs.getInt("publishLingoExpModel");
        } else {
            assert false;
        }
        return result;
    }

    public boolean isCurrentlyRunning() throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT (timestamp +  INTERVAL '1 second' * lifetime) > NOW() AS running FROM LingoExpModelPublishedAs WHERE LingoExpModelID = ?");
        statement.setInt(1,getId());
        ResultSet rs = statement.executeQuery();

        boolean result = false;
        if (rs.next()) {
            result = rs.getBoolean("running");
        } else {
            assert false;
        }
        return result;
    }

    public static int countExperiments() throws SQLException {
        Statement s = DatabaseController.getConnection().createStatement();
        ResultSet resultSet = s.executeQuery("SELECT count(*) FROM LingoExpModels");
        int result = -1;
        if (resultSet.next()) {
            result =  resultSet.getInt(1);
        }
        resultSet.close();
        s.close();
        return result;
    }

    public int getMinParticipants(){
        try {
            List<AbstractGroup> groups = getParts();
            List<Integer> participants = new LinkedList<>();
            for(AbstractGroup g: groups){
                participants.add(g.countParticipants());
            }
            return Collections.min(participants);
        } catch (SQLException e) {
            return -1;
        }
    }

    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder experimentBuilder = Json.createObjectBuilder();
        experimentBuilder.add("id", this.id);
        experimentBuilder.add("name", this.name);
        experimentBuilder.add("nameOnAmt", this.nameOnAmt);
        experimentBuilder.add("description", this.description);
        experimentBuilder.add("additionalExplanations", this.additionalExplanations);

        JsonArrayBuilder exampleQuestionsBuilder = Json.createArrayBuilder();
        for (ExampleQuestion eq : getExampleQuestions()) {
            exampleQuestionsBuilder.add(eq.returnJSON());
        }
        experimentBuilder.add("exampleQuestions", exampleQuestionsBuilder.build());

        /*JsonArrayBuilder cheaterDetectionQuestionsBuilder = Json.createArrayBuilder();
        for (CheaterDetectionQuestion cdq : getCheaterDetectionQuestions()) {
            cheaterDetectionQuestionsBuilder.add(cdq.returnJSON());
        }
        experimentBuilder.add("cheaterDetectionQuestions", cheaterDetectionQuestionsBuilder.build());
        */

        JsonArrayBuilder partBuilder = Json.createArrayBuilder();
        for (AbstractGroup p : getParts()) {
            partBuilder.add(p.returnJSON());
        }
        experimentBuilder.add("parts", partBuilder.build());

        return experimentBuilder.build();
    }

    public int getId() {
        return id;
    }

    public String getExperimentType() {
        return experimentType;
    }

    public void setExperimentType(String experimentType) {
        this.experimentType = experimentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.update();
    }

    public String getListType(){
        Properties experimentProperties = new Properties();
        try {
            experimentProperties.load(new FileReader("app/models/Questions/" + getExperimentType() + "/experiment.properties"));
        } catch (IOException e) {
            return "DISJOINT LISTS";
        }
        String listType = experimentProperties.getProperty("listType");
        if(listType != null){
            return listType.trim();
        }
        return "DISJOINT LISTS";
    }

    public String getNameOnAmt() {
        return nameOnAmt;
    }

    public void setNameOnAmt(String nameOnAmt) {
        this.nameOnAmt = nameOnAmt;
        this.update();
    }

    public String getDescriptionOnAmt() {
        return descriptionOnAmt;
    }

    public void setDescriptionOnAmt(String descriptionOnAmt) {
        this.descriptionOnAmt = descriptionOnAmt;
        this.update();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) throws SQLException {
        this.description = description;
        this.update();
    }

    public String getAdditionalExplanations() {
        return additionalExplanations;
    }

    public void setAdditionalExplanations(String additionalExplanations) throws SQLException {
        this.additionalExplanations = additionalExplanations;
        this.update();
    }
}
