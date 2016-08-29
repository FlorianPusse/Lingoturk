package models.Groups.LinkingV1Experiment;


import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Questions.LinkingV1Experiment.Prolific.Combination;
import models.Questions.LinkingV1Experiment.Script;
import models.Questions.PartQuestion;
import models.Questions.Question;
import models.Repository;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("LinkingV1Experiment.LinkingV1PoolGroup")
public class LinkingV1PoolGroup extends AbstractGroup {

    /* BEGIN OF VARIABLES BLOCK */

    /* END OF VARIABLES BLOCK */

    List<Script> leftHandSide = null;
    List<Script> rightHandSide = null;

    static long AUTO_APPROVAL_DELAY_IN_SECONDS = 7200L;
    static long ASSIGNMENT_DURATION_IN_SECONDS = 600L;
    static double REWARD = 0.35;
    static String TITLE = "Linking most similar English sentences. #";
    static String KEYWORDS = "script,scripts,activity,activities,linking,aligning, English,events,description,descriptions,most,similar,everyday";
    static String DESCRIPTION = "We require native speakers of English who will be presented with two descriptions of everyday activities and will be required to decide if the highlighted event in the first description is most similar to the highlighted event in the second description.";

    public LinkingV1PoolGroup(){}

    @Override
    public List<ProlificPublish> prepareProlificPublish() {
        return null;
    }

    @Override
    public void publishOnProlific(int maxAssignments) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignmentsPerCombination) throws SQLException {
        String url = null;

        hitTypeId = service.registerHITType(AUTO_APPROVAL_DELAY_IN_SECONDS, ASSIGNMENT_DURATION_IN_SECONDS, REWARD, TITLE + Application.actCounter, KEYWORDS, DESCRIPTION, Application.qualificationRequirements);

        int counter = 0;

        for (Script lhs : getLeftHandSide()) {
            if (!lhs.containsActiveItem()) {
                System.out.println("Warning: " + lhs.getId() + " will not be published. Script does not contain active items.");
                continue;
            }

            ++counter;

            //System.out.println(lhs.getId() +  " will be published. Script contains at least one active item.");

            for (Script rhs : getRightHandSide()) {
                String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                        + "<ExternalURL> " + Application.getStaticIp() + "/render?id=" + lhs.getId() + "&amp;id2=" + rhs.getId() + "&amp;Type=question</ExternalURL>"
                        + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";

                HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignmentsPerCombination, null, null, null, null, null, null);
                url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();
                System.out.println(url);

                insert(hit.getHITId(), publishedId, lhs.getId(), rhs.getId());
            }
        }

        System.out.println("\n" + counter * 3 + "script published for story: " + fileName + "\n");

        return url;
    }

    public void insert(String hitID, int publishedId, int question1, int question2) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("INSERT INTO PartPublishedAs(PartID,mTurkID,publishedId,question1,question2) VALUES(?,?,?,?,?)");
        statement.setInt(1, getId());
        statement.setString(2, hitID);
        statement.setInt(3, publishedId);
        statement.setInt(4, question1);
        statement.setInt(5, question2);
        statement.execute();
    }

    public List<Script> getLeftHandSide() throws SQLException {
        if (leftHandSide != null) {
            return leftHandSide;
        }

        List<Script> tmp = new LinkedList<>();
        for (PartQuestion s : getQuestions()) {
            Script s_tmp = (Script) s;
            if (s_tmp.LinkingV1_side.equals("lhs")) {
                tmp.add(s_tmp);
            }
        }
        return tmp;
    }

    public List<Script> getRightHandSide() throws SQLException {
        if (rightHandSide != null) {
            return rightHandSide;
        }

        List<Script> tmp = new LinkedList<>();
        for (PartQuestion s : getQuestions()) {
            Script s_tmp = (Script) s;
            if (s_tmp.LinkingV1_side.equals("rhs")) {
                tmp.add(s_tmp);
            }
        }
        return tmp;
    }

    @Override
    public JsonObject returnJSON() {
        try{
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
            JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
            for (Question q : getLeftHandSide()) {
                arrayBuilder.add(q.returnJSON());
            }
            objectBuilder.add("lhs", arrayBuilder.build()).build();

            arrayBuilder = Json.createArrayBuilder();
            for (Question q : getRightHandSide()) {
                arrayBuilder.add(q.returnJSON());
            }
            objectBuilder.add("rhs", arrayBuilder.build()).build();

            return objectBuilder.build();
        }catch (SQLException e){
            return null;
        }
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode partNode) throws SQLException {
        // TODO: Exception handling
        try {
            String fileName = partNode.get("fileName").asText();
            System.out.println(fileName);
            this.fileName = fileName;

            String lhs = partNode.get("lhs").asText();
            String rhs = partNode.get("rhs").asText();

            System.out.println("Create LHS.");
            List<Script> leftHandSide = Script.createScripts(lhs, "lhs", experiment, true, Script.class);
            System.out.println("Create RHS.");
            List<Script> rightHandSide = Script.createScripts(rhs, "rhs", experiment, false, Script.class);

            leftHandSide.addAll(rightHandSide);
            questions = new LinkedList<>(leftHandSide);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public Result getRandomQuestion(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        try {
            Random random = new Random();

            PartQuestion lhs = getLeftHandSide().get(random.nextInt(getLeftHandSide().size()));
            PartQuestion rhs = getRightHandSide().get(random.nextInt(getRightHandSide().size()));

            Map<String,String> variableMap = df.data();
            variableMap.put("id1", String.valueOf(lhs.getId()));
            variableMap.put("id2", String.valueOf(rhs.getId()));

            df = df.fill(variableMap);
            return ok(views.html.ExperimentRendering.LinkingV1Experiment.LinkingV1Experiment_render.render(lhs, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
        } catch (SQLException e) {
            return internalServerError("Can't connect to DB.");
        }
    }

    public List<Combination> getQuestionCombinations() throws SQLException {
        List<Combination> combinations = new LinkedList<>();
        for (Script lhs : getLeftHandSide()) {
            for (Script rhs : getRightHandSide()) {
                Combination c = new Combination(new int[]{lhs.getId(), rhs.getId()});
                c.save();
                combinations.add(c);
            }
        }
        return combinations;
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return this.getRandomQuestion(worker, assignmentId, hitId, turkSubmitTo, exp, df);
    }


}
