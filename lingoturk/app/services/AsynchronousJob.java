package services;

import io.ebean.EbeanServer;
import models.Groups.AbstractGroup;
import models.LingoExpModel;

import javax.inject.Inject;
import javax.persistence.Transient;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Background process that is executed periodically in order to keep track of current participants,
 * update the availabilities, if participants don't hand in results.
 */
@SuppressWarnings("ALL")
class AsynchronousJob implements Runnable {

    private static ConcurrentLinkedQueue<String> waitingAssignmentIDs = new ConcurrentLinkedQueue();
    private static List<String> toRemove = new LinkedList<>();
    private static Map<String, Integer> failedTries = new HashMap<>();

    @Transient
    private final DatabaseService databaseService;

    @Transient
    EbeanServer ebeanServer;

    @Inject
    public AsynchronousJob(DatabaseService databaseService) {
        this.databaseService = databaseService;
        this.ebeanServer = ebeanServer;
    }

    /**
     * Updates the current number of available participants for part {@code partId}
     *
     * @param partId The Id of the part that should be updated
     * @throws SQLException If an internal error occured
     */
    private void updateAvailabilities(int partId) throws SQLException {
        AbstractGroup group = AbstractGroup.byId(partId);
        if (group != null && group.maxParticipants != null && group.maxParticipants >= 0) {
            int maxParticipants = group.maxParticipants;
            int availability = maxParticipants - group.countParticipants();
            if (availability != group.availability) {
                boolean disabled = availability <= 0;
                PreparedStatement ps = databaseService.getConnection().prepareStatement("UPDATE Groups SET disabled = ?, maxParticipants = ?, availability = ? WHERE partId = ?");
                System.out.println("[info] play - Update Group " + partId + ": Setting disabled to '" + disabled + "' and availability to '" + availability + "/" + maxParticipants + "'");

                ps.setBoolean(1, disabled);
                ps.setInt(2, maxParticipants);
                ps.setInt(3, availability);
                ps.setInt(4, partId);
                ps.execute();
                ps.close();
            }
        }
    }

    /**
     * Deletes a participation entry for worker {@code workerId} at part {@code partId}
     *
     * @param workerId The Id of the worker
     * @param partId   The Id of the part that should be updated
     * @throws SQLException If an internal error occured
     */
    private void deleteEntry(String workerId, int partId) throws SQLException {
        Statement deleteStatement = databaseService.getConnection().createStatement();
        deleteStatement.execute("DELETE FROM Workers_participateIn_Parts WHERE workerId = '" + workerId + "' AND PartId = " + partId);
        deleteStatement.close();
    }

    /**
     * Verifies a participation entry for worker {@code workerId} at part {@code partId}
     *
     * @param workerId The Id of the worker
     * @param partId   The Id of the part that should be updated
     * @throws SQLException If an internal error occured
     */
    private void verifyEntry(String workerId, int partId) throws SQLException {
        Statement verifyStatement = databaseService.getConnection().createStatement();
        verifyStatement.execute("UPDATE Workers_participateIn_Parts SET verified = true WHERE workerId = '" + workerId + "' AND PartId = " + partId);
        verifyStatement.close();
    }

    /**
     * The function that will be executed periodically by the Play Framework.
     * - Checks if a participant didn't enter a result for an assignment and deleted the assignment if a threshold
     * is passed
     * - Updates groups that nobody worked on lately
     * - Updates parts that don't even appear in the list of assignments
     * - Disables parts that already have enough participants
     */
    @Override
    public void run() {
        Statement s = null;
        ResultSet rs = null;

        try {
            s = databaseService.getConnection().createStatement();
            rs = s.executeQuery("SELECT Workers_participateIn_Parts.*,LingoExpModels_contain_Parts.*, maxworkingtime FROM Workers_participateIn_Parts JOIN LingoExpModels_contain_Parts USING (PartId) JOIN Groups USING (PartId) WHERE verified = FALSE");

            // Check if we can delete entries in our list of participants per list
            while (rs.next()) {
                LingoExpModel expModel = LingoExpModel.byId(rs.getInt("LingoExpModelID"));
                if (expModel != null) {
                    int partId = rs.getInt("partId");
                    Timestamp timestamp = rs.getTimestamp("timestamp");
                    int maxWorkingTime = rs.getInt("maxWorkingTime");
                    Timestamp endTime = new Timestamp(timestamp.getTime() + maxWorkingTime);
                    if (maxWorkingTime > 0 && (new Timestamp(System.currentTimeMillis())).after(endTime)) {
                        String expType = expModel.getExperimentType().substring(0, expModel.getExperimentType().lastIndexOf("Experiment"));
                        String workerId = rs.getString("workerId");

                        boolean exists = false;
                        Statement answerStatement = databaseService.getConnection().createStatement();
                        ResultSet answerResult = answerStatement.executeQuery("SELECT * FROM " + expType + "Results WHERE workerId = '" + workerId + "' AND PartId = " + partId);
                        if (!answerResult.next()) {
                            exists = true;
                            deleteEntry(workerId, partId);
                        } else {
                            verifyEntry(workerId, partId);
                        }
                        answerStatement.close();
                    }
                }
            }
            rs.close();

            // Free space if part hasn't been touched lately
            rs = s.executeQuery("SELECT * FROM (SELECT partId, max(timestamp) AS maxTime FROM Workers_participateIn_Parts GROUP BY (partId)) AS tmp JOIN LingoExpModels_contain_Parts USING (PartId) JOIN Groups USING (PartId)");
            while (rs.next()) {
                int partId = rs.getInt("partId");
                int maxWorkingTime = rs.getInt("maxWorkingTime");
                Timestamp maxTime = rs.getTimestamp("maxTime");
                Timestamp endTime = new Timestamp(maxTime.getTime() + maxWorkingTime);
                LingoExpModel expModel = LingoExpModel.byId(rs.getInt("LingoExpModelID"));
                if (expModel != null) {
                    if (maxWorkingTime > 0 && (new Timestamp(System.currentTimeMillis())).after(endTime)) {
                        updateAvailabilities(partId);
                    }
                }
            }
            rs.close();

            // Also check parts that aren't in the list at all (possibly because all entries have been deleted)
            rs = s.executeQuery("SELECT PartId FROM Groups WHERE partId NOT IN (SELECT DISTINCT partId FROM Workers_participateIn_Parts WHERE partId IS NOT NULL)");
            while (rs.next()) {
                updateAvailabilities(rs.getInt("PartId"));
            }

            // Check if we can disable some part
            rs = s.executeQuery("SELECT DISTINCT partId,maxParticipants FROM Workers_participateIn_Parts JOIN Groups USING (partId) WHERE disabled = FALSE");
            while (rs.next()) {
                int partId = rs.getInt("partId");
                int maxParticipants = rs.getInt("maxParticipants");
                AbstractGroup group = AbstractGroup.byId(partId);
                if (group != null && maxParticipants > 0) {
                    if (group.countParticipants() >= maxParticipants) {
                        updateAvailabilities(partId);
                    }
                }
            }
            rs.close();

            s.close();
        } catch (SQLException e) {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e1) {
                }
            }
            if (s != null) {
                try {
                    s.close();
                } catch (SQLException e1) {
                }
            }
        }
    }
}
