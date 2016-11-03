package controllers;

import akka.actor.UntypedActor;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static play.mvc.Results.ok;


public class AsynchronousJob extends UntypedActor {

    private static ConcurrentLinkedQueue<String> waitingAssignmentIDs = new ConcurrentLinkedQueue();
    private static List<String> toRemove = new LinkedList<>();
    private static Map<String,Integer> failedTries = new HashMap<>();

    @Override
    public void onReceive(Object message) throws Exception {

    }

    /*@Override
    public void onReceive(Object message) throws Exception {
        for (String assignmentID : waitingAssignmentIDs) {
            System.out.println("Test assignment with ID: " + assignmentID + " ...");
            RequesterService requesterService = Service.getService(Service.source.MTURK);
            try {
                GetAssignmentResult gAR = requesterService.getAssignment(assignmentID);
                HIT hit = gAR.getHIT();
                CheaterDetectionQuestion question = (CheaterDetectionQuestion) PublishableQuestion.byHITId(hit.getHITId());
                Assignment assignment = gAR.getAssignment();

                AssignmentResult assignmentResult = question.parseAssignment(assignment);
                String workerId = assignment.getWorkerId();

                List<String> connectives = null;
                if (assignmentResult.getManualAnswer() != null && assignmentResult.getCategory() != null && assignmentResult.getValidConnectives() != null) {
                    connectives = new LinkedList<>(Arrays.asList(assignmentResult.getManualAnswer()));
                    connectives.addAll(Arrays.asList(assignmentResult.getValidConnectives()));
                    connectives.add(assignmentResult.getCategory());
                }

                List<String> notValid = Arrays.asList(assignmentResult.getNotRelevant());

                boolean actConnectivesAndMustNotHavesDisjoint = Collections.disjoint(connectives, question.getMustNotHaveConnectives_asString());
                boolean notValidAndMustHaveDisjoint = Collections.disjoint(notValid,question.getProposedConnectives());
                List<String> remainingProposedConnectives = question.getProposedConnectives_asString();
                remainingProposedConnectives.removeAll(connectives);
                boolean containsAllProposedConnectives = remainingProposedConnectives.isEmpty();

                Worker worker = Worker.getWorkerById(workerId);
                if (worker != null){
                    if (notValidAndMustHaveDisjoint && actConnectivesAndMustNotHavesDisjoint && containsAllProposedConnectives) {
                        worker.addParticipatesInCD_Question(question.getId(),assignmentID,true);
                    } else {
                        worker.addParticipatesInCD_Question(question.getId(),assignmentID,false);
                    }
                }
                if(worker.countFalseAnswers() >= 2){
                    worker.isBanned(true);
                }
                System.out.println("AssignmentID " + assignmentID + " successfully processed!");
                toRemove.add(assignmentID);
            } catch (InvalidStateException ise) {

            } catch (DocumentException e) {
                e.printStackTrace();
            } catch (ObjectDoesNotExistException oe){
                System.out.println("AssignmentID " + assignmentID + " does not exist yet!");
                if(!failedTries.containsKey(assignmentID)){
                    failedTries.put(assignmentID,1);
                }else{
                    int tries = failedTries.get(assignmentID);
                    if(tries >= 10){
                        toRemove.add(assignmentID);
                        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("INSERT INTO failedAssignments(assignmentID) " +
                                "SELECT ? WHERE NOT EXISTS (SELECT * FROM failedAssignments WHERE assignmentID = ?)");
                        statement.setString(1,assignmentID);
                        statement.setString(2,assignmentID);
                        statement.execute();
                        System.out.println("AssignmentID " + assignmentID + " failed 10 times. Abort.");
                    }else{
                        failedTries.put(assignmentID,tries + 1);
                    }
                }
            } catch (Throwable e){
                e.printStackTrace();
            }
        }
        waitingAssignmentIDs.removeAll(toRemove);
        toRemove = new LinkedList<String>();
    }

    public static void addAssignmentID(String assignmentID) {
        waitingAssignmentIDs.add(assignmentID);
    }

    public static void loadQueue() throws SQLException {
        Connection connection = DatabaseController.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT assignmentID FROM pendingAssignments");
        ResultSet rs = statement.executeQuery();

        ConcurrentLinkedQueue<String> pendingAssignments = new ConcurrentLinkedQueue<String>();
        while(rs.next()){
            pendingAssignments.add(rs.getString("assignmentID"));
        }

        waitingAssignmentIDs = pendingAssignments;
        statement = connection.prepareStatement("DELETE FROM pendingAssignments");
        statement.execute();
    }

    public static Result failedAssignments() throws SQLException, IOException {
        Connection connection = DatabaseController.getConnection();
        PreparedStatement statement = connection.prepareStatement("SELECT assignmentID FROM failedAssignments");
        ResultSet rs = statement.executeQuery();

        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        while(rs.next()){
           arrayBuilder.add(rs.getString("assignmentID"));
        }
        objectBuilder.add("assignmentIDs", arrayBuilder.build());
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(objectBuilder.build().toString());
        return ok(actualObj);
    }

    public static void saveQueue() throws SQLException {
        Connection connection = DatabaseController.getConnection();
        for(String assignmentID : waitingAssignmentIDs){
            PreparedStatement statement = connection.prepareStatement("INSERT INTO pendingAssignments(assignmentID) " +
                    "SELECT ? WHERE NOT EXISTS (SELECT * FROM pendingAssignments WHERE assignmentID = ?)");
            statement.setString(1,assignmentID);
            statement.setString(2,assignmentID);
            statement.execute();
        }
    }*/

}
