package models;

import io.ebean.Finder;
import io.ebean.Model;
import models.Groups.AbstractGroup;
import play.data.validation.Constraints;
import services.DatabaseServiceImplementation;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.io.FileReader;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

@Entity
@Table(name = "LingoExpModels")
public class LingoExpModel extends Model {

    /* BEGIN OF VARIABLES BLOCK */

    @Id
    @Column(name = "LingoExpModelID")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "lingoexpmodels_seq")
    protected int id;

    @Basic
    @Column(name = "name")
    @Constraints.Required
    protected String name;

    @Basic
    @Column(name = "description", columnDefinition = "TEXT")
    protected String description;

    @Basic
    @Column(name = "nameOnAmt")
    protected String nameOnAmt;

    @Basic
    @Column(name = "descriptionOnAmt")
    protected String descriptionOnAmt;

    @Constraints.Required
    @Column(name = "additionalExplanations", columnDefinition = "TEXT")
    protected String additionalExplanations;

    @Basic
    @Column(name = "experimentType")
    protected String experimentType;

    @Basic
    @Column(name = "listType", columnDefinition = "varchar(255) default 'DISJOINT LISTS'")
    protected String listType;

    @Basic
    @Column(name = "owner")
    private String owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "access")
    private ExperimentAccess access;

    @Basic
    @Column(name = "allowedGroups", columnDefinition = "TEXT")
    protected String allowedGroups;

    @Basic
    @Column(name = "allowedExperimenters", columnDefinition = "TEXT")
    protected String allowedExperimenters;

    private enum ExperimentAccess {PRIVATE, GROUP, ALL}

    /* END OF VARIABLES BLOCK */

    private static Finder<Integer, LingoExpModel> finder = new Finder<>(LingoExpModel.class);

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

    /**
     * Creates and stores a new experiment instance
     *
     * @param name                   The name of the experiment
     * @param description            The description of the experiment
     * @param additionalExplanations The instructions shown to the participants
     * @param nameOnAmt              The name of the experiment that will be shown on Mechanical Turk
     * @param experimentType         The experiment type of the experiment
     * @param listType               The list type of the experiment
     * @return The created experiment
     */
    public static LingoExpModel createLingoExpModel(String name, String description, String additionalExplanations, String nameOnAmt, String experimentType, String listType) {
        LingoExpModel result = new LingoExpModel(name, description, additionalExplanations, nameOnAmt, experimentType, listType);
        result.save();
        return result;
    }


    /**
     * Returns a list of all experiments.
     *
     * @return List of experiments
     */
    public static List<LingoExpModel> getAllExperiments() throws SQLException {
        return finder.query().orderBy("LingoExpModelID").findList();
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

    /**
     * Adds a list of workers to the blocked list of this experiment.
     *
     * @param workerList The list of workers that should be added
     * @throws SQLException if a database access error occurs
     */
    public void addBlockedWorkers(List<Worker> workerList) throws SQLException {
        for (Worker w : workerList) {
            w.addIsBlockedFor(this);
        }
    }

    /**
     * Retrieves the list of workers that are blocked for this experiment from the database
     * and returns it
     *
     * @return Returns the list of workers that are blocked for this experiment
     * @throws SQLException if a database access error occurs
     */
    public List<Worker> getBlockedWorkers() throws SQLException {
        List<Worker> result = new LinkedList<>();

        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM Workers_areBlockedFor_LingoExpModels WHERE LingoExpModelID=" + this.getId());
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            result.add(Worker.getWorkerById(rs.getString("WorkerID")));
        }

        return result;
    }

    /**
     * Retrieves the list of groups for this experiment from the database
     * and returns it
     *
     * @return Returns the list of groups that are blocked for this experiment
     * @throws SQLException if a database access error occurs
     */
    public List<AbstractGroup> getParts() throws SQLException {
        List<AbstractGroup> result = new LinkedList<>();

        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM LingoExpModels_contain_Parts WHERE LingoExpModelID=" + this.getId() + " ORDER BY LingoExpModelID,PartId");
        ResultSet rs = statement.executeQuery();

        while (rs.next()) {
            result.add(AbstractGroup.byId(rs.getInt("PartID")));
        }

        return result;
    }

    /**
     * Deletes this experiment instance.
     *
     * @return Whether the experiment instance was deleted successfully
     */
    @Override
    public boolean delete() {
        try {
            for (AbstractGroup part : getParts()) {
                part.delete();
            }
        } catch (SQLException sqlE) {
            throw new RuntimeException("An error occured while deleteing the experiment:\n" + sqlE.getMessage());
        }
        return super.delete();
    }

    /**
     * Publishes this experiment instance on Mechanical Turk
     *
     * @param lifetime    The lifetime of the experiment
     * @param url         The URL that was assigned to the experiment
     * @param destination Whether the experiment got published to Mechanical Turk or the Sandbox
     * @return The id that was assigned
     * @throws SQLException if a database access error occurs
     */
    public int publish(long lifetime, String url, String destination) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM publishLingoExpModel(?,?,?,?)");
        statement.setInt(1, getId());
        statement.setLong(2, lifetime);
        statement.setString(3, url);
        statement.setString(4, destination);

        ResultSet rs = statement.executeQuery();
        int result = -1;
        if (rs.next()) {
            result = rs.getInt("publishLingoExpModel");
        } else {
            assert false;
        }
        return result;
    }

    /**
     * Returns the the minimum number of participants that a group has. If a SQLException
     * occurs, then -1 will be returned instead.
     *
     * @return The min number of participants, or -1, if an error occurs.
     */
    public int getMinParticipants() {
        try {
            List<AbstractGroup> groups = getParts();
            List<Integer> participants = new LinkedList<>();
            for (AbstractGroup g : groups) {
                participants.add(g.countParticipants());
            }
            return Collections.min(participants);
        } catch (SQLException e) {
            return -1;
        }
    }

    /**
     * Returns this LingoExpModel as JSON
     *
     * @return The JSON encoded LingoExpModel
     * @throws SQLException if a database access error occurs
     */
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder experimentBuilder = Json.createObjectBuilder();
        experimentBuilder.add("id", this.id);
        experimentBuilder.add("name", this.name);
        experimentBuilder.add("nameOnAmt", this.nameOnAmt);
        experimentBuilder.add("description", this.description);
        experimentBuilder.add("additionalExplanations", this.additionalExplanations);

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

    public String getListType() {
        Properties experimentProperties = new Properties();
        try {
            experimentProperties.load(new FileReader("app/models/Questions/" + getExperimentType() + "/experiment.properties"));
        } catch (IOException e) {
            return "DISJOINT LISTS";
        }
        String listType = experimentProperties.getProperty("listType");
        if (listType != null) {
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

    public void setAdditionalExplanations(String additionalExplanations) {
        this.additionalExplanations = additionalExplanations;
        this.update();
    }
}
