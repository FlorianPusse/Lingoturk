package services;


import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.amazonaws.mturk.util.PropertiesClientConfig;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

/**
 * Service providing access to Mechanical Turk services.
 * TODO: Needs upgrade to support multiple users
 */
@Singleton
public class MechanicalTurkService {

    public enum source {SANDBOX, MTURK}

    @Inject
    DatabaseService databaseService;

    /**
     * Returns the Mechanical Turk service for a given source {@code s}
     *
     * @param s The source for the service: Either the Sandbox or the real Mturk
     * @return The Mechanical Turk service
     */
    public RequesterService getService(source s) {
        RequesterService service = null;

        if (s == source.SANDBOX) {
            service = new RequesterService(new PropertiesClientConfig("conf/mturk.properties"));
        } else if (s == source.MTURK) {
            service = new RequesterService(new PropertiesClientConfig("conf/mturkToAMT.properties"));
        }

        return service;
    }

    /**
     * Returns all HITs for a given publishId and a RequesterService
     *
     * @param publishID The publishId that is assigned when an experiment is published
     * @param service   The service that connects to Mechanical Turk
     * @return The list of all HITs that are stored for this publishId at the given {@code service}
     * @throws SQLException If the list of HITs couldn't be stored from the Database
     */
    public List<HIT> getHITs(int publishID, RequesterService service) throws SQLException {
        PreparedStatement statement = databaseService.getConnection().prepareStatement("SELECT mTurkID FROM QuestionPublishedAs WHERE publishedId = ?");
        statement.setInt(1, publishID);
        ResultSet rs = statement.executeQuery();

        List<HIT> results = new LinkedList<>();
        while (rs.next()) {
            String hitID = rs.getString("mturkID");
            HIT hit = null;
            try {
                hit = service.getHIT(hitID);
            } catch (ServiceException e) {
                System.err.println("HIT does not exist or cannot establish connection\n" + e.getMessage());
            }
            results.add(hit);
        }

        return results;
    }

    /**
     * Approves all assignments for a given {@code publishId}
     *
     * @param publishID The publishId that should be checkec
     * @throws SQLException If the list of HITs couldn't be read from the database.
     */
    public void approveAssignments(int publishID) throws SQLException {
        RequesterService requesterService = getService(source.MTURK);
        List<HIT> hits = getHITs(publishID, requesterService);

        for (HIT hit : hits) {
            for (Assignment assignment : requesterService.getAllAssignmentsForHIT(hit.getHITId())) {
                requesterService.approveAssignment(assignment.getAssignmentId(), "Thank you for participating in our HITs!");
                System.out.println(assignment.getAssignmentId());
            }
        }
    }

}
