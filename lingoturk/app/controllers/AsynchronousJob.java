package controllers;

import akka.actor.UntypedActor;
import models.Groups.AbstractGroup;
import models.LingoExpModel;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;


@SuppressWarnings("ALL")
public class AsynchronousJob extends UntypedActor {

    private static ConcurrentLinkedQueue<String> waitingAssignmentIDs = new ConcurrentLinkedQueue();
    private static List<String> toRemove = new LinkedList<>();
    private static Map<String,Integer> failedTries = new HashMap<>();

    private static void updateAvailabilities(int partId) throws SQLException {
        AbstractGroup group = AbstractGroup.byId(partId);
        if (group != null && group.maxParticipants != null && group.maxParticipants >= 0){
            int maxParticipants = group.maxParticipants;
            int availability = maxParticipants - group.countParticipants();
            if(availability != group.availability) {
                boolean disabled = availability <= 0;
                PreparedStatement ps = DatabaseController.getConnection().prepareStatement("UPDATE Groups SET disabled = ?, maxParticipants = ?, availability = ? WHERE partId = ?");
                System.out.println("[info] play - Update Group " + partId + ": Setting disabled to '" + disabled + "' and availability to '" + availability + "/" + maxParticipants + "'");

                ps.setBoolean(1, disabled);
                ps.setInt(2, maxParticipants);
                ps.setInt(3, availability);
                ps.setInt(4, partId);
                ps.execute();
            }
        }
    }

    private static void deleteEntry(String workerId, int partId) throws SQLException {
        Statement deleteStatement = DatabaseController.getConnection().createStatement();
        deleteStatement.execute("DELETE FROM Workers_participateIn_Parts WHERE workerId = '" + workerId + "' AND PartId = " + partId);
        deleteStatement.close();
    }

    private static void verifyEntry(String workerId, int partId) throws SQLException{
        Statement verifyStatement = DatabaseController.getConnection().createStatement();
        verifyStatement.execute("UPDATE Workers_participateIn_Parts SET verified = true WHERE workerId = '" + workerId + "' AND PartId = " + partId);
        verifyStatement.close();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        Statement s = DatabaseController.getConnection().createStatement();
        ResultSet rs = s.executeQuery("SELECT Workers_participateIn_Parts.*,LingoExpModels_contain_Parts.*, maxworkingtime FROM Workers_participateIn_Parts JOIN LingoExpModels_contain_Parts USING (PartId) JOIN Groups USING (PartId) WHERE verified = false");

        // Check if we can delete entries in our list of participants per list
        while(rs.next()){
            LingoExpModel expModel = LingoExpModel.byId(rs.getInt("LingoExpModelID"));
            if (expModel != null){
                int partId = rs.getInt("partId");
                Timestamp timestamp = rs.getTimestamp("timestamp");
                int maxWorkingTime = rs.getInt("maxWorkingTime");
                Timestamp endTime = new Timestamp(timestamp.getTime() + maxWorkingTime);
                if(maxWorkingTime > 0 && (new Timestamp(System.currentTimeMillis())).after(endTime)){
                    String expType = expModel.getExperimentType().substring(0,expModel.getExperimentType().lastIndexOf("Experiment"));
                    String workerId = rs.getString("workerId");

                    boolean exists = false;
                    Statement answerStatement = DatabaseController.getConnection().createStatement();
                    ResultSet answerResult = answerStatement.executeQuery("SELECT * FROM " + expType + "Results WHERE workerId = '" + workerId + "' AND PartId = " + partId);
                    if(!answerResult.next()){
                        exists = true;
                        deleteEntry(workerId,partId);
                    }else{
                        verifyEntry(workerId,partId);
                    }
                    answerStatement.close();
                }
            }
        }

        // Free space if part hasn't been touched lately
        rs = s.executeQuery("SELECT * FROM (SELECT partId, max(timestamp) as maxTime FROM Workers_participateIn_Parts GROUP BY (partId)) as tmp JOIN LingoExpModels_contain_Parts USING (PartId) JOIN Groups USING (PartId)");
        while(rs.next()){
            int partId = rs.getInt("partId");
            int maxWorkingTime = rs.getInt("maxWorkingTime");
            Timestamp maxTime = rs.getTimestamp("maxTime");
            Timestamp endTime = new Timestamp(maxTime.getTime() + maxWorkingTime);
            LingoExpModel expModel = LingoExpModel.byId(rs.getInt("LingoExpModelID"));
            if (expModel != null) {
                if(maxWorkingTime > 0 && (new Timestamp(System.currentTimeMillis())).after(endTime)) {
                    updateAvailabilities(partId);
                }
            }
        }

        // Also check parts that aren't in the list at all (possibly because all entries have been deleted)
        rs = s.executeQuery("SELECT PartId FROM Groups WHERE partId NOT IN (SELECT DISTINCT partId FROM Workers_participateIn_Parts WHERE partId IS NOT NULL)");
        while(rs.next()){
            updateAvailabilities(rs.getInt("PartId"));
        }

        // Check if we can disably some part
        rs = s.executeQuery("SELECT DISTINCT partId,maxParticipants FROM Workers_participateIn_Parts JOIN Groups USING (partId) WHERE disabled = false");
        while(rs.next()){
            int partId = rs.getInt("partId");
            int maxParticipants = rs.getInt("maxParticipants");
            AbstractGroup group = AbstractGroup.byId(partId);
            if(group != null && maxParticipants > 0){
                if (group.countParticipants() >= maxParticipants){
                    updateAvailabilities(partId);
                }
            }
        }

    }
}
