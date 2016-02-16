package controllers;


import com.amazonaws.mturk.requester.Assignment;
import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import org.dom4j.DocumentException;

import java.sql.SQLException;
import java.util.List;

import static controllers.Extract.getHITs;

public class Service {

    public enum source {SANDBOX,MTURK}

    public static RequesterService getService(source s){
        RequesterService service = null;

        if (s == source.SANDBOX) {
            service = new RequesterService(new PropertiesClientConfig("conf/mturk.properties"));
        }
        else if (s == source.MTURK) {
            service = new RequesterService(new PropertiesClientConfig("conf/mturkToAMT.properties"));
        }

        return service;
    }

    public static void approveAssignments(int publishID) throws SQLException, DocumentException {
        RequesterService requesterService = getService(source.MTURK);
        List<HIT> hits = getHITs(publishID,requesterService);

        for(HIT hit : hits){
            for(Assignment assignment : requesterService.getAllAssignmentsForHIT(hit.getHITId())){
                requesterService.approveAssignment(assignment.getAssignmentId(),"Thank you for participating in our HITs!");
                System.out.println(assignment.getAssignmentId());
            }
        }
    }

}
