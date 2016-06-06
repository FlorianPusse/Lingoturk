package models.Questions.LinkingV1Experiment;

import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import models.LingoExpModel;
import models.Questions.LinkingV2Experiment.ScriptV2;
import models.Questions.PartQuestion;
import models.Repository;
import models.Results.AssignmentResult;
import models.Worker;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import play.data.DynamicForm;
import play.mvc.Result;

import javax.json.*;
import javax.persistence.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static play.mvc.Results.ok;

@Entity
@Inheritance
@DiscriminatorValue("LinkingV1Experiment.ScriptV1")
@MappedSuperclass
public class Script extends PartQuestion {

    /* BEGIN OF VARIABLES BLOCK */

    @Basic
    @Column(name = "LinkingV1_scriptId", columnDefinition = "TEXT")
    public String scriptId;

    @Basic
    @Column(name = "LinkingV1_side", columnDefinition = "TEXT")
    public String side;

    @OneToMany(cascade = CascadeType.ALL)
    public List<Item> itemList = new LinkedList<>();

    /* END OF VARIABLES BLOCK */

    private static final int MAX_FALSE_ANSWERS = 20;

    public Script(String scriptId, String side, LingoExpModel exp) {
        this.scriptId = scriptId;
        this.side = side;
        this.experimentID = exp.getId();
    }

    public String toString() {
        return "id : " + id + "\nitems : " + itemList;
    }

    @Override
    public void writeResults(JsonNode resultNode) throws SQLException {
        String assignmentId = resultNode.get("assignmentId").asText();
        String hitId = resultNode.get("hitId").asText();
        String workerId = resultNode.get("workerId").asText();
        int workingTimes = resultNode.get("workingTimes").asInt();
        int lhs_script = resultNode.get("script_lhsId").asInt();
        int rhs_script = resultNode.get("script_rhsId").asInt();

        //Script script = (Script) Script.byId(lhs_script);

        for (Iterator<JsonNode> resultIterator = resultNode.get("results").iterator(); resultIterator.hasNext(); ) {
            JsonNode result = resultIterator.next();
            int lhs_item = result.get("lhs").asInt();
            String rhs_item = result.get("rhs").asText();

            //script.verify(rhs_script,lhs_item,rhs_item);

            if (rhs_item.startsWith("before")) {
                String[] answer = rhs_item.split("_");
                PreparedStatement statement = Repository.getConnection().prepareStatement(
                        "INSERT INTO LinkingV1Results(WorkerId,AssignmentId,HitId,lhs_script,rhs_script,lhs_item,before,workingTimes) VALUES(?,?,?,?,?,?,?,?)"
                );

                statement.setString(1, workerId);
                statement.setString(2, assignmentId);
                statement.setString(3, hitId);
                statement.setInt(4, lhs_script);
                statement.setInt(5, rhs_script);
                statement.setInt(6, lhs_item);
                statement.setInt(7, Integer.parseInt(answer[1]));
                statement.setInt(8, workingTimes);

                statement.execute();
                statement.close();
            } else if (rhs_item.startsWith("after")) {
                String[] answer = rhs_item.split("_");
                PreparedStatement statement = Repository.getConnection().prepareStatement(
                        "INSERT INTO LinkingV1Results(WorkerId,AssignmentId,HitId,lhs_script,rhs_script,lhs_item,after,workingTimes) VALUES(?,?,?,?,?,?,?,?)"
                );

                statement.setString(1, workerId);
                statement.setString(2, assignmentId);
                statement.setString(3, hitId);
                statement.setInt(4, lhs_script);
                statement.setInt(5, rhs_script);
                statement.setInt(6, lhs_item);
                statement.setInt(7, Integer.parseInt(answer[1]));
                statement.setInt(8, workingTimes);

                statement.execute();
                statement.close();
            } else if (rhs_item.startsWith("between")) {
                String[] answer = rhs_item.split("_");
                PreparedStatement statement = Repository.getConnection().prepareStatement(
                        "INSERT INTO LinkingV1Results(WorkerId,AssignmentId,HitId,lhs_script,rhs_script,lhs_item,after,before,workingTimes) VALUES(?,?,?,?,?,?,?,?,?)"
                );

                statement.setString(1, workerId);
                statement.setString(2, assignmentId);
                statement.setString(3, hitId);
                statement.setInt(4, lhs_script);
                statement.setInt(5, rhs_script);
                statement.setInt(6, lhs_item);
                statement.setInt(7, Integer.parseInt(answer[1]));
                statement.setInt(8, Integer.parseInt(answer[2]));
                statement.setInt(9, workingTimes);

                statement.execute();
                statement.close();
            } else if (rhs_item.startsWith("noLinkingPossible")) {
                PreparedStatement statement = Repository.getConnection().prepareStatement(
                        "INSERT INTO LinkingV1Results(WorkerId,AssignmentId,HitId,lhs_script,rhs_script,lhs_item,noLinkingPossible,workingTimes) VALUES(?,?,?,?,?,?,?,?)"
                );

                statement.setString(1, workerId);
                statement.setString(2, assignmentId);
                statement.setString(3, hitId);
                statement.setInt(4, lhs_script);
                statement.setInt(5, rhs_script);
                statement.setInt(6, lhs_item);
                statement.setBoolean(7, true);
                statement.setInt(8, workingTimes);

                statement.execute();
                statement.close();
            } else {
                PreparedStatement statement = Repository.getConnection().prepareStatement(
                        "INSERT INTO LinkingV1Results(WorkerId,AssignmentId,HitId,lhs_script,rhs_script,lhs_item,rhs_item,workingTimes) VALUES(?,?,?,?,?,?,?,?)"
                );

                statement.setString(1, workerId);
                statement.setString(2, assignmentId);
                statement.setString(3, hitId);
                statement.setInt(4, lhs_script);
                statement.setInt(5, rhs_script);
                statement.setInt(6, lhs_item);
                statement.setInt(7, Integer.parseInt(rhs_item));
                statement.setInt(8, workingTimes);

                statement.execute();
                statement.close();
            }
        }
    }

    public JsonObject returnJSON() {
        Collections.sort(itemList, new Item.ItemSlotComparator());

        JsonArrayBuilder array = Json.createArrayBuilder();
        for (Item item : itemList) {
            array.add(item.returnJSON());
        }

        return Json.createObjectBuilder().add("id", id).add("items", array.build()).build();
    }

    @Override
    public Result renderAMT(Worker worker, String assignmentId, String hitId, String turkSubmitTo, LingoExpModel exp, DynamicForm df) {
        return ok(views.html.ExperimentRendering.LinkingV1Experiment.LinkingV1Experiment_render.render(this, null, worker, assignmentId, hitId, turkSubmitTo, exp, df, "MTURK"));
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        throw new RuntimeException("Method \"setJSONData\" not implemented for class \"Script\"");
    }

    public void addItem(Item item) {
        itemList.add(item);
    }

    public static List<Script> createScripts(String xmlString, String side, LingoExpModel exp, boolean deleteEmpty) throws ParserConfigurationException, IOException, SAXException {

        List<Script> scriptList = new LinkedList<>();
        Script currentScript = null;

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = builderFactory.newDocumentBuilder();

        Document document = documentBuilder.parse(new InputSource(new StringReader(xmlString)));
        document.normalizeDocument();

        // get stories
        NodeList scripts = document.getElementsByTagName("script");
        for (int i = 0; i < scripts.getLength(); i++) {
            Node script = scripts.item(i);

            if (script.getNodeType() == Node.ELEMENT_NODE) {
                Element scriptElement = (Element) script;

                if (currentScript != null) {
                    scriptList.add(currentScript);
                }

                if(side == null){
                    currentScript = new ScriptV2(scriptElement.getAttribute("id"), null, exp);
                }else{
                    currentScript = new Script(scriptElement.getAttribute("id"), side, exp);
                }

                Item currentItem;

                NodeList itemList = scriptElement.getElementsByTagName("item");
                for (int j = 0; j < itemList.getLength(); j++) {
                    Node item = itemList.item(j);
                    Element itemElement = (Element) item;
                    String item_hAttribute = itemElement.getAttribute("h");
                    String item_headAttribute = itemElement.getAttribute("head");
                    String item_originalAttribute = itemElement.getAttribute("original");
                    String item_slotAttribute = itemElement.getAttribute("slot");
                    String item_textAttribute = itemElement.getAttribute("text");
                    String item_pair = itemElement.getAttribute("pair");

                    currentItem = new Item(item_hAttribute, item_pair, item_textAttribute, item_slotAttribute, item_originalAttribute, item_headAttribute);

                    NodeList ptcpList = itemElement.getElementsByTagName("ptcp");
                    for (int z = 0; z < ptcpList.getLength(); z++) {
                        Node ptcp = ptcpList.item(z);
                        Element ptcpElement = (Element) ptcp;
                        String ptcp_headAttribute = ptcpElement.getAttribute("head");
                        String ptcp_textAttribute = ptcpElement.getAttribute("text");

                        currentItem.addPTCP(new PTCP(ptcp_headAttribute, ptcp_textAttribute));
                    }

                    currentScript.addItem(currentItem);
                }
            } else {
                throw new RuntimeException("wrong format");
            }
        }

        if (currentScript != null) {
            scriptList.add(currentScript);
        }

        for (Iterator<Script> scriptIterator = scriptList.iterator(); scriptIterator.hasNext(); ) {
            Script s = scriptIterator.next();
            if (deleteEmpty) {
                boolean containsActive = false;
                for (Item item : s.getItemList()) {
                    if (!item.getH().equals("")) {
                        containsActive = true;
                        break;
                    }
                }
                if (!containsActive) {
                    scriptIterator.remove();
                    continue;
                }
            }
            s.save();
        }

        return scriptList;
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        return null;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public String getScriptId() {
        return scriptId;
    }

    public boolean containsActiveItem() {
        for (Item item : getItemList()) {
            if (!item.getH().equals("")) {
                return true;
            }
        }
        return false;
    }

    private static int countFalseAnswers(String workerId) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT wrongAnswersCount(?)");
        statement.setString(1, workerId);
        ResultSet resultSet = statement.executeQuery();

        int count = -1;
        if (resultSet.next()) {
            count = resultSet.getInt(1);
        }

        return count;
    }

}
