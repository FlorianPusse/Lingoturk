package models.Groups;

import com.amazonaws.mturk.requester.HIT;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.fasterxml.jackson.databind.JsonNode;
import controllers.Application;
import models.LingoExpModel;
import models.Questions.PartQuestion;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import java.sql.SQLException;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("DisjointGroup")
public class DisjointGroup extends AbstractGroup {

    public DisjointGroup(){}

    @Override
    public String publishOnAMT(RequesterService service, int publishedId, String hitTypeId, Long lifetime, Integer maxAssignments) throws SQLException {
        String question = "<ExternalQuestion xmlns=\"http://mechanicalturk.amazonaws.com/AWSMechanicalTurkDataSchemas/2006-07-14/ExternalQuestion.xsd\">"
                + "<ExternalURL> " + Application.getStaticIp() + "/render?id=" + getId() + "&amp;Type=part</ExternalURL>"
                + "<FrameHeight>" + 800 + "</FrameHeight>" + "</ExternalQuestion>";
        HIT hit = service.createHIT(hitTypeId, null, null, null, question, null, null, null, lifetime, maxAssignments, null, null, null, null, null, null);
        String url = service.getWebsiteURL() + "/mturk/preview?groupId=" + hit.getHITTypeId();

        insert(hit.getHITId(), publishedId);

        availability = maxAssignments;
        this.update();

        return url;
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode partNode) throws SQLException {
        super.setJSONData(experiment,partNode);

        JsonNode numberNode = partNode.get("number");
        if(numberNode != null){
            this.number = numberNode.asInt();
        }
    }


    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        objectBuilder.add("id",id);
        objectBuilder.add("number",number);
        JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
        for(PartQuestion question : getQuestions()){
            arrayBuilder.add(question.returnJSON());
        }
        objectBuilder.add("questions",arrayBuilder.build());
        return objectBuilder.build();
    }

    public Result render(String origin, int imporantChunk) {
        // TODO: NOT ONLY PICUTRE NAMING?
        return ok(views.html.renderExperiments.PictureNamingExperiment.pictureNaming.render(origin,this.getId(),imporantChunk));
    }
}
