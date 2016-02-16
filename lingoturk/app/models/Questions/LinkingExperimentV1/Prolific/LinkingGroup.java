package models.Questions.LinkingExperimentV1.Prolific;


import com.amazonaws.mturk.service.axis.RequesterService;
import models.Groups.AbstractGroup;
import models.Groups.LinkingExperimentV1.PoolGroup;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import models.Repository;
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

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("LinkingGroup")
public class LinkingGroup extends AbstractGroup {

    public static final int AVAILABLE_ASSIGNMENTS = 3;

    public List<PartQuestion> q = null;

    public static LinkingGroup createPart(List<PartQuestion> combinations) throws SQLException {
        LinkingGroup part = new LinkingGroup();
        part.save();
        part.setAvailability(AVAILABLE_ASSIGNMENTS);

        PreparedStatement statement = Repository.getConnection().prepareStatement("INSERT INTO Parts_contain_Questions(PartID,QuestionID) SELECT " + part.getId() + ", ? " +
                "WHERE NOT EXISTS (" +
                "SELECT * FROM Parts_contain_Questions WHERE PartID= " + part.getId() + " AND QuestionID= ? " +
                ")");

        for (PartQuestion question : combinations) {
            statement.setInt(1, question.getId());
            statement.setInt(2, question.getId());
            statement.execute();
        }

        return part;
    }

    @Override
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException {
        return null;
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("id", getId());

        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

        if(q == null){
            q = getQuestions();
        }

        for(PartQuestion c : q){
            arrayBuilder.add(c.returnJSON());
        }

        objectBuilder.add("questions", arrayBuilder.build());

        return objectBuilder.build();
    }

    public static Result publishProlific(int expId) throws SQLException {
        LingoExpModel expModel = LingoExpModel.byId(expId);
        List<PartQuestion> allCombinations = new LinkedList<>();

        for (AbstractGroup p : expModel.getParts()) {
            PoolGroup tmp = (PoolGroup) p;
            allCombinations.addAll(tmp.getQuestionCombinations());
        }

        final int MAX_GROUP_SIZE = 15;
        final int MAX_GROUP_COLLECTION_SIZE = 10;

        List<LinkingGroup> groupOfCombinations = new LinkedList<>();
        List<PartQuestion> currentCombinations = new LinkedList<>();

        for (int i = 0; i < allCombinations.size(); i++) {
            if (i % MAX_GROUP_SIZE == 0 && (i != 0)) {
                groupOfCombinations.add(LinkingGroup.createPart(currentCombinations));
                currentCombinations = new LinkedList<>();
            }
            currentCombinations.add(allCombinations.get(i));
        }
        if (!currentCombinations.isEmpty()) {
            groupOfCombinations.add(LinkingGroup.createPart(currentCombinations));
        }

        List<LingoExpModel> groupCollections = new LinkedList<>();
        List<LinkingGroup> currentGroups = new LinkedList<>();

        for (int i = 0; i < groupOfCombinations.size(); i++) {
            LinkingGroup g = groupOfCombinations.get(i);
            if (i % MAX_GROUP_COLLECTION_SIZE == 0 && (i != 0)) {
                LingoExpModel exp = new LingoExpModel(expModel.getName() + "_publish_" + (groupCollections.size() + 1), expModel.getDescription(), expModel.getAdditionalExplanations(), expModel.getNameOnAmt(),"LinkingExperimentV1");
                exp.save();
                for (LinkingGroup lg : currentGroups) {
                    System.out.println("LinkingGroup, Id: " + lg.getId() + ", Size: " + g.getQuestions().size());
                    lg.addExperimentUsedIn(exp);
                }

                groupCollections.add(exp);
                currentGroups = new LinkedList<>();
            }
            currentGroups.add(g);
        }
        if (!currentGroups.isEmpty()) {
            LingoExpModel exp = new LingoExpModel(expModel.getName() + "_publish_" + (groupCollections.size() + 1), expModel.getDescription(), expModel.getAdditionalExplanations(), expModel.getNameOnAmt(),"LinkingExperimentV1");
            exp.save();
            for (LinkingGroup lg : currentGroups) {
                System.out.println("LinkingGroup, Id: " + lg.getId() + ", Size: " + lg.getQuestions().size());
                lg.addExperimentUsedIn(exp);
            }
            groupCollections.add(exp);
        }

        System.out.println("Totalsize: " + allCombinations.size());

        return ok();
    }

    public static Result renderLinkingProlific(int expId) {
        return ok(views.html.renderExperiments.LinkingExperimentV1.linkingExperimentProlific.render(expId));
    }

}
