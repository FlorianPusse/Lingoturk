package models;

import models.Groups.AbstractGroup;
import models.Questions.Question;
import play.data.validation.Constraints;
import io.ebean.*;
import services.DatabaseServiceImplementation;

import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Worker class represents a participant of our experiments.
 */
@Entity
@Table(name = "Workers")
public class Worker extends Model {

    /**
     * The id of the worker. Needs to be unique
     */
    @Column(name = "WorkerID")
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "workers_seq")
    String id;

    /**
     * The current status of the worker. Will be be set to true, if the worker is not
     * allowed to aprticipate anymore. Default is false
     */
    @Column(name = "banned")
    @Constraints.Required
    boolean banned;

    /**
     * Play class to retrieve workers from the database
     */
    private static Finder<String, Worker> finder = new Finder<>(Worker.class);

    protected Worker(String workerID, boolean banned) {
        this.banned = banned;
        id = workerID;
    }

    /**
     * Stores the participation of a worker for a specific part and assignmentId
     *
     * @param group        The group the worker participated in
     * @param assignmentId The id that assigned to this participation (only available for mechanical turk)
     * @param hitID        The id assigned to the task (only available for mechanical turk)
     * @throws SQLException Propagated from JDBC
     */
    public void addParticipatesInPart(AbstractGroup group, String assignmentId, String hitID) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement(
                "INSERT INTO Workers_participateIn_Parts(WorkerID,PartID,assignmentID, hitID) VALUES (?,?,?,?)");
        statement.setString(1, this.getId());
        statement.setInt(2, group.getId());
        statement.setString(3, assignmentId);
        statement.setString(4, hitID);
        statement.execute();
    }

    /**
     * Stores the participation of a worker for a specific part and assignmentId
     *
     * @param group        The group the worker participated in
     * @param assignmentId The id that assigned to this participation (only available for mechanical turk)
     * @param hitID        The id assigned to the task (only available for mechanical turk)
     * @param question     The actual question that the participant answers
     * @param question2    (optional:) If two questions are compared: The id of the second question
     * @throws SQLException Propagated from JDBC
     */
    public void addParticipatesInPart(AbstractGroup group, Question question, Question question2, String assignmentId, String hitID) throws SQLException {
        if (question != null && question2 == null) {
            PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement(
                    "INSERT INTO Workers_participateIn_Parts(WorkerID,PartID,assignmentID, hitID,QuestionID) VALUES (?,?,?,?,?)");
            statement.setString(1, this.getId());
            if (group != null) {
                statement.setInt(2, group.getId());
            } else {
                statement.setInt(2, -1);

            }
            statement.setString(3, assignmentId);
            statement.setString(4, hitID);
            statement.setInt(5, question.getId());
            statement.execute();
        } else if (question != null) {
            PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement(
                    "INSERT INTO Workers_participateIn_Parts(WorkerID,assignmentID, hitID,QuestionID,QuestionID2) " +
                            "SELECT ?,?,?,?,? WHERE NOT EXISTS (SELECT * FROM Workers_participateIn_Parts WHERE WorkerId = ? AND assignmentId = ?)");
            statement.setString(1, this.getId());
            statement.setString(2, assignmentId);
            statement.setString(3, hitID);


            statement.setInt(4, question.getId());
            statement.setInt(5, question2.getId());
            statement.setString(6, this.getId());
            statement.setString(7, assignmentId);
            statement.execute();
        } else {
            addParticipatesInPart(group, assignmentId, hitID);
        }
    }

    /**
     * Updates the participation of a worker that changed its name from {@code oldWorkerId } to {@code id}
     * for a specific group {@code group}
     *
     * @param oldWorkerId The old name
     * @param group       The group the worker participated in
     * @throws SQLException SQLException Propagated from JDBC
     */
    public void updateParticipatesInPart(String oldWorkerId, AbstractGroup group) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("UPDATE Workers_participateIn_Parts SET workerId = ? WHERE workerId = ? AND PartId = ?");
        statement.setString(1, id);
        statement.setString(2, oldWorkerId);
        statement.setInt(3, group.getId());
        statement.execute();
        statement.close();
        System.out.println("Update workerId: " + oldWorkerId + " changed name to " + id + " for Part " + group.getId());
    }

    /**
     * Adds statistics about this worker for a given experiment {@code expId} and an {@code origin}
     *
     * @param expId      The id of the experiment these statistics were collected for
     * @param origin     The origin of the worker (either Prolific, or Mechanical Turk in most cases)
     * @param statistics The actual JSON encoded statistics
     * @throws SQLException SQLException Propagated from JDBC
     */
    public void addStatistics(int expId, String origin, String statistics) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement(
                "INSERT INTO ParticipantStatistics(id,workerId, origin, expId, statistics) VALUES (nextval('ParticipantStatistics_seq'),?,?,?,?)");

        statement.setString(1, id);
        statement.setString(2, origin);
        statement.setInt(3, expId);
        statement.setString(4, statistics);

        statement.execute();
    }

    /**
     * Test if a worker has participated in an specific part and returns - if found - the assignmentID else null
     *
     * @param group The part to look for
     * @return the ID of HIT, the worker participated in
     * @throws SQLException SQLException Propagated from JDBC
     */
    public Participation getParticipatesInPart(AbstractGroup group) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM Workers_participateIn_Parts WHERE WorkerID=? AND PartID=?");
        statement.setString(1, this.getId());
        statement.setInt(2, group.getId());
        ResultSet rs = statement.executeQuery();

        Participation result = null;
        if (rs.next()) {
            result = new Participation(group.getId(), rs.getString("assignmentID"), rs.getString("hitID"), rs.getInt("QuestionID"));
        }

        return result;
    }

    /**
     * Test if a worker has participated in an specific experiment and returns - if found - the assignmentID else null
     *
     * @param exp The experiment  part to look for
     * @return the ID of HIT, the worker participated in
     * @throws SQLException SQLException Propagated from JDBC
     */
    public Participation getParticipatesInExperiment(LingoExpModel exp) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM Workers_participateIn_Parts JOIN LingoExpModels_contain_Parts USING (partId) WHERE WorkerID=? AND LingoExpModelId=?");
        statement.setString(1, this.getId());
        statement.setInt(2, exp.getId());
        ResultSet rs = statement.executeQuery();

        Participation result = null;
        if (rs.next()) {
            result = new Participation(rs.getInt("partId"), rs.getString("assignmentID"), rs.getString("hitID"), rs.getInt("QuestionID"));
        }

        return result;
    }

    /**
     * Wraps a participation entry into a Java class
     */
    public class Participation {
        private Participation(int pID, String aID, String hID, int QuestionID) {
            partID = pID;
            assignmentID = aID;
            hitID = hID;
            questionID = QuestionID;
        }

        int partID;
        String assignmentID;
        String hitID;
        int questionID;

        public String getAssignmentID() {
            return assignmentID;
        }

        public String getHitID() {
            return hitID;
        }

        public int getQuestionID() {
            return questionID;
        }

        public int getPartID() {
            return partID;
        }
    }

    /**
     * Adds this a worker to the list of blocked people. These won't be able to participate
     * in this experiment (if coming from Mechanical Turk)
     *
     * @param exp The experiment that this participant is blocked for
     * @throws SQLException SQLException SQLException Propagated from JDBC
     */
    public void addIsBlockedFor(LingoExpModel exp) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement(
                "INSERT INTO Workers_areBlockedFor_LingoExpModels(WorkerID,LingoExpModelID) SELECT '" + this.getId() + "',? " +
                        "WHERE NOT EXISTS (" +
                        "SELECT * FROM  Workers_areBlockedFor_LingoExpModels WHERE WorkerID ='" + this.getId() + "' AND LingoExpModelID=?" +
                        ")");
        statement.setInt(1, exp.getId());
        statement.setInt(2, exp.getId());
        statement.execute();
    }

    /**
     * Checks if this participant is blocked for experiment {@code expId}
     *
     * @param expId The id of the experiment that should be checked
     * @return True iff the participant is blocked for experiment {@code expId}
     * @throws SQLException SQLException SQLException Propagated from JDBC
     */
    public boolean getIsBlockedFor(int expId) throws SQLException {
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("SELECT * FROM Workers_areBlockedFor_LingoExpModels WHERE WorkerID=? AND LingoExpModelId=?");
        statement.setString(1, getId());
        statement.setInt(2, expId);


        if (statement.executeQuery().next()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Creates and returns a worker with workerId {@code id}. If the worker already exists,
     * its value will be overwritten
     *
     * @param id The id of the worker to create
     * @return The created worker
     */
    public static Worker createWorker(String id) {
        Worker worker = new Worker(id, false);
        worker.save();

        return worker;
    }

    /**
     * Returns a worker specified by {@code id} from the database. If no such worker exists,
     * it will be created first
     *
     * @param id The id of the worker
     * @return The worker with id {@code id}
     */
    public static Worker getWorkerById(String id) {
        Worker w = finder.byId(id);
        if (w == null) {
            return createWorker(id);
        }
        return finder.byId(id);
    }

    /**
     * Updates the banned value of a participant to the new value {@code isBanned}
     *
     * @param isBanned
     * @throws SQLException SQLException SQLException Propagated from JDBC
     */
    public void isBanned(boolean isBanned) throws SQLException {
        this.banned = isBanned;
        PreparedStatement statement = DatabaseServiceImplementation.staticConnection().prepareStatement("UPDATE Workers SET banned = ? WHERE WorkerID ='" + this.getId() + "'");
        statement.setBoolean(1, isBanned);
        statement.execute();
    }

    public boolean getIsBanned() {
        return banned;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "WorkerID:  " + id + "\tBlocked: " + getIsBanned();
    }

}
