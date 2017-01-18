package models;

import controllers.DatabaseController;
import models.Groups.AbstractGroup;
import models.Questions.Question;
import play.data.validation.Constraints;
import play.db.ebean.Model;

import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Entity
@Table(name="Workers")
public class Worker extends Model {

    @Column(name = "WorkerID")
    @Id
    String id;

    @Column(name = "banned")
    @Constraints.Required
    boolean banned;

    private static Model.Finder<String,Worker> finder = new Model.Finder<String,Worker>(String.class,Worker.class);

    protected Worker(String workerID, boolean banned){
        this.banned = banned;
        id= workerID;
    }

    /*

    Database Operations

     */

    public void addParticipatesInPart(AbstractGroup group, String assignmentId, String hitID) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                "INSERT INTO Workers_participateIn_Parts(WorkerID,PartID,assignmentID, hitID) VALUES (?,?,?,?)");
        statement.setString(1, this.getId());
        statement.setInt(2, group.getId());
        statement.setString(3, assignmentId);
        statement.setString(4,hitID);
        statement.execute();
    }

    public void addParticipatesInPart(AbstractGroup group, Question question, Question question2, String assignmentId, String hitID) throws SQLException {
        if(question != null && question2 == null){
            PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                    "INSERT INTO Workers_participateIn_Parts(WorkerID,PartID,assignmentID, hitID,QuestionID) VALUES (?,?,?,?,?)");
            statement.setString(1, this.getId());
            if(group != null){
                statement.setInt(2, group.getId());
            }else{
                statement.setInt(2, -1);

            }
            statement.setString(3, assignmentId);
            statement.setString(4,hitID);
            statement.setInt(5, question.getId());
            statement.execute();
        }else if(question != null){
            PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                    "INSERT INTO Workers_participateIn_Parts(WorkerID,assignmentID, hitID,QuestionID,QuestionID2) " +
                            "SELECT ?,?,?,?,? WHERE NOT EXISTS (SELECT * FROM Workers_participateIn_Parts WHERE WorkerId = ? AND assignmentId = ?)");
            statement.setString(1, this.getId());
            statement.setString(2, assignmentId);
            statement.setString(3,hitID);


            statement.setInt(4, question.getId());
            statement.setInt(5,question2.getId());
            statement.setString(6,this.getId());
            statement.setString(7,assignmentId);
            statement.execute();
        } else{
            addParticipatesInPart(group,assignmentId,hitID);
        }
    }

    public static void updateParticipatesInPart(String oldWorkerId, AbstractGroup group, String newWorkerId) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("UPDATE Workers_participateIn_Parts SET workerId = ? WHERE workerId = ? AND PartId = ?");
        statement.setString(1,newWorkerId);
        statement.setString(2,oldWorkerId);
        statement.setInt(3, group.getId());
        statement.execute();
        statement.close();
        System.out.println("Update workerId: " + oldWorkerId + " changed name to " + newWorkerId + " for Part " + group.getId());
    }

    public void addParticipatesInCD_Question(int questionID, String assignmentID, boolean answerCorrect) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                "INSERT INTO participatesInCD_Question(QuestionID,workerID,assignmentID,answerCorrect) " +
                        "SELECT ?,?,?,? WHERE NOT EXISTS (SELECT * FROM participatesInCD_Question WHERE QuestionID = ? AND WorkerID = ?)");
        statement.setInt(1, questionID);
        statement.setString(2, this.getId());
        statement.setString(3, assignmentID);
        statement.setBoolean(4, answerCorrect);
        statement.setInt(5, questionID);
        statement.setString(6, this.getId());
        statement.execute();
    }

    public static void addStatistics(String workerId, int expId, String origin, String statistics) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                "INSERT INTO ParticipantStatistics(id,workerId, origin, expId, statistics) VALUES (nextval('ParticipantStatistics_seq'),?,?,?,?)");

        statement.setString(1, workerId);
        statement.setString(2, origin);
        statement.setInt(3, expId);
        statement.setString(4, statistics);

        statement.execute();
    }

    public int countFalseAnswers() throws SQLException {
        Statement statement = DatabaseController.getConnection().createStatement();
        ResultSet rs = statement.executeQuery("SELECT count(*) AS falseCount FROM participatesInCD_Question WHERE WorkerID='" + this.getId() +"' AND answerCorrect = false");

        int result = -1;
        while(rs.next()){
            result = rs.getInt("falseCount");
        }

        return result;
    }

    /**
     * Test if a worker has participated in an specific part and returns - if found - the assignmentID else null
     * @param group The part to look for
     * @return the ID of HIT, the worker participated in
     * @throws SQLException
     */
    public Participation getParticipatesInPart(AbstractGroup group) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM Workers_participateIn_Parts WHERE WorkerID=? AND PartID=?");
        statement.setString(1,this.getId());
        statement.setInt(2, group.getId());
        ResultSet rs = statement.executeQuery();

        Participation result = null;
        if(rs.next()){
            result = new Participation(group.getId(),rs.getString("assignmentID"),rs.getString("hitID"),rs.getInt("QuestionID"));
        }

        return result;
    }

    public Participation getParticipatesInExperiment(LingoExpModel exp) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM Workers_participateIn_Parts JOIN LingoExpModels_contain_Parts USING (partId) WHERE WorkerID=? AND LingoExpModelId=?");
        statement.setString(1,this.getId());
        statement.setInt(2,exp.getId());
        ResultSet rs = statement.executeQuery();

        Participation result = null;
        if(rs.next()){
            result = new Participation(rs.getInt("partId"),rs.getString("assignmentID"),rs.getString("hitID"),rs.getInt("QuestionID"));
        }

        return result;
    }



    public class Participation{
        private Participation(int pID,String aID, String hID, int QuestionID){
            partID=pID;
            assignmentID = aID;
            hitID=hID;
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

        public int getPartID() { return partID; }
    }

    public void addIsBlockedFor(LingoExpModel exp) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement(
                "INSERT INTO Workers_areBlockedFor_LingoExpModels(WorkerID,LingoExpModelID) SELECT '" + this.getId() + "',? " +
                        "WHERE NOT EXISTS (" +
                        "SELECT * FROM  Workers_areBlockedFor_LingoExpModels WHERE WorkerID ='" + this.getId() + "' AND LingoExpModelID=?" +
                        ")");
        statement.setInt(1,exp.getId());
        statement.setInt(2,exp.getId());
        statement.execute();
    }

    public boolean getIsBlockedFor(int expId) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM Workers_areBlockedFor_LingoExpModels WHERE WorkerID=? AND LingoExpModelId=?");
        statement.setString(1,getId());
        statement.setInt(2,expId);


        if(statement.executeQuery().next()){
            return true;
        }else{
            return false;
        }
    }

    public void isBanned(boolean isBlocked) throws SQLException {
        this.banned = isBlocked;
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("UPDATE Workers SET banned = ? WHERE WorkerID ='" + this.getId() + "'");
        statement.setBoolean(1,isBlocked);
        statement.execute();
    }

    public static Worker createWorker(String id) {
        Worker worker = new Worker(id,false);
        worker.save();

        return worker;
    }

    public static Worker getWorkerById(String id) {
        return finder.byId(id);
    }

    /*

    Getter / Setter

     */

    public boolean getIsBanned(){
        return banned;
    }

    public String getId(){
        return id;
    }

    @Override
    public String toString(){
        return "WorkerID:  " + id + "\tBlocked: " + getIsBanned();
    }

}
