package models.Questions.DiscourseConnectivesExperiment;

import au.com.bytecode.opencsv.CSVWriter;
import com.amazonaws.mturk.requester.Assignment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.LingoExpModel;
import models.Questions.Question;
import models.Results.AssignmentResult;
import models.Questions.PartQuestion;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import play.mvc.Result;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Iterator;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.ok;


@Entity
@Inheritance
@DiscriminatorValue("DCQuestion")
public class DiscourseConnectivesQuestion extends PartQuestion {

    @Basic
    @Column(name="Sentence1", columnDefinition = "TEXT")
    protected String sentence1;

    @Basic
    @Column(name="Sentence2", columnDefinition = "TEXT")
    protected String sentence2;

    @Basic
    @Column(name="innerID")
    protected String innerID;

    @Basic
    @Column(name="SentenceType")
    protected String sentenceType;

    public DiscourseConnectivesQuestion(){}

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + sentence1.hashCode();
        result = 31 * result + sentence2.hashCode();
        return result;
    }

    @Override
    public AssignmentResult parseAssignment(Assignment assignment) throws DocumentException {
        String workerId = assignment.getWorkerId();
        String assignment_String = assignment.getAnswer();

        String category = null;
        String[] manualAnswer = null;
        String[] notRelevant = null;
        String[] validConnectives = null;
        String[] cantDecide = null;
        Calendar assignmentTime = assignment.getAcceptTime();
        Calendar submitTime = assignment.getSubmitTime();
        long workingTime_tmp = submitTime.getTimeInMillis() - assignmentTime.getTimeInMillis();
        long workingTime_seconds = workingTime_tmp / 1000;

        org.dom4j.Document document = DocumentHelper.parseText(assignment_String);
        //Root
        Element root = document.getRootElement();
        for (Iterator i = root.elementIterator("Answer"); i.hasNext(); ) {
            Element elem = (Element) i.next();
            String questions = elem.element("QuestionIdentifier").getStringValue();
            if (questions.equals("category")) {
                category = elem.element("FreeText").getStringValue().replace(" ","_");
            }
            if (questions.equals("manualAnswer")) {
                manualAnswer = replaceOccurrences(elem.element("FreeText").getStringValue().split(",")," ","_");
            }
            if (questions.equals("notRelevant")) {
                notRelevant = replaceOccurrences(elem.element("FreeText").getStringValue().split(",")," ","_");
            }
            if (questions.equals("validConnectives")) {
                validConnectives = replaceOccurrences(elem.element("FreeText").getStringValue().split(",")," ","_");
            }
            if (questions.equals("cantDecide")) {
                cantDecide = replaceOccurrences(elem.element("FreeText").getStringValue().split(",")," ","_");
            }
        }

        return new AssignmentResult(assignment.getAssignmentId(),workerId,category,manualAnswer,notRelevant,validConnectives,cantDecide, workingTime_seconds);
    }

    public static String[] replaceOccurrences(String[] array, String expression, String replacement){
        for(int i = 0; i < array.length; i++){
            array[i] = array[i].replace(expression,replacement);
        }
        return array;
    }

    @Override
    public void setJSONData(LingoExpModel experiment, JsonNode questionNode) throws SQLException {
        String sentence1 = questionNode.get("sentence1").asText();
        String sentence2 = questionNode.get("sentence2").asText();
        String innerID = questionNode.get("id").asText();
        String sentenceType = questionNode.get("sentenceType").asText();

        this.sentence1 = sentence1;
        this.sentence2 = sentence2;
        this.innerID = innerID;
        this.sentenceType = sentenceType;
        this.experimentID = experiment.getId();
    }

    @Override
    public Result render(String assignmentId, String hitId, String workerId, String turkSubmitTo, String additionalExplanations) {
        return ok(views.html.renderExperiments.DiscourseConnectivesExperiment.dc_dragndrop.render(this, assignmentId, workerId, turkSubmitTo,additionalExplanations));
    }

    @Override
    public JsonObject returnJSON() throws SQLException {
        JsonObjectBuilder dragAndDropQuestionBuilder = Json.createObjectBuilder();
        dragAndDropQuestionBuilder.add("id", innerID);
        dragAndDropQuestionBuilder.add("sentence1", sentence1);
        dragAndDropQuestionBuilder.add("sentence2", sentence2);
        dragAndDropQuestionBuilder.add("type","DND_Q");
        dragAndDropQuestionBuilder.add("sentenceType",this.getSentenceType());

        return dragAndDropQuestionBuilder.build();
    }

    @Override
    public String toString(){
        return "DragAndDrop Question";
    }

    public static void addConnectives(JsonArrayBuilder builder, String connectives[]) {
        for (String c : connectives) {
            builder.add(c);
        }
    }

    public static JsonObject createConjunctions() {
        JsonObjectBuilder all = Json.createObjectBuilder();
        JsonArrayBuilder allArray = Json.createArrayBuilder();

        JsonObjectBuilder categoryBuilder = Json.createObjectBuilder();
        JsonArrayBuilder connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"still", "if", "while", "meanwhile", "by contrast", "though", "however", "because", "so", "in addition"});
        categoryBuilder.add("category", "although");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"while", "also", "in addition", "in particular", "but", "so", "before", "then", "if", "because"});
        categoryBuilder.add("category", "and");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"since", "as", "in fact", "but", "while", "after", "until"});
        categoryBuilder.add("category", "because");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"still", "if", "however", "while", "by contrast", "although", "in addition", "for example", "meanwhile"});
        categoryBuilder.add("category", "but");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"for instance", "specifically", "in fact", "until", "after", "meanwhile"});
        categoryBuilder.add("category", "for example");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"if then", "until", "when", "although", "while", "before", "since"});
        categoryBuilder.add("category", "if");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"also", "indeed", "specifically", "for example", "that is", "in other words", "in short", "because", "therefore"});
        categoryBuilder.add("category", "in fact");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"rather", "but", "after", "because", "while", "as"});
        categoryBuilder.add("category", "instead");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"thus", "consequently", "while", "when", "after"});
        categoryBuilder.add("category", "so");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"while", "as", "after", "once", "before", "because", "although", "if"});
        categoryBuilder.add("category", "when");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"previously", "while", "since", "although", "until"});
        categoryBuilder.add("category", "before");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"once", "although", "as", "while", "until"});
        categoryBuilder.add("category", "after");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        all.add("categories", allArray.build());
        return all.build();
    }

    public static JsonObject createConjunctiveAdverbs() {
        JsonObjectBuilder all = Json.createObjectBuilder();
        JsonArrayBuilder allArray = Json.createArrayBuilder();

        JsonObjectBuilder categoryBuilder = Json.createObjectBuilder();
        JsonArrayBuilder connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"still", "meanwhile", "by contrast", "on the other hand", "nevertheless", "by comparison", "moreover", "specifically"});
        categoryBuilder.add("category", "however");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"moreover", "in addition", "in particular", "therefore", "meanwhile", "by contrast", "specifically"});
        categoryBuilder.add("category", "also");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"since", "as", "in fact", "indeed", "by contrast"});
        categoryBuilder.add("category", "because");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"for instance", "specifically", "in fact", "nevertheless", "therefore", "moreover"});
        categoryBuilder.add("category", "for example");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"indeed", "specifically", "for example", "that is", "in other words", "in short", "in addition", "therefore"});
        categoryBuilder.add("category", "in fact");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"rather", "by contrast", "meanwhile", "moreover", "specifically"});
        categoryBuilder.add("category", "instead");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"as a result", "consequently", "for instance", "in particular", "still", "nevertheless"});
        categoryBuilder.add("category", "thus");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"at the same time", "simultaneously", "however", "by contrast", "moreover"});
        categoryBuilder.add("category", "meanwhile");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"earlier", "however", "indeed", "by contrast", "specifically"});
        categoryBuilder.add("category", "previously");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"later", "simultaneously", "moreover", "by contrast", "specifically", "moreover"});
        categoryBuilder.add("category", "then");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        categoryBuilder = Json.createObjectBuilder();
        connectiveBuilder = Json.createArrayBuilder();
        addConnectives(connectiveBuilder, new String[]{"if then", "when", "until", "although", "while", "in addition"});
        categoryBuilder.add("category", "if");
        categoryBuilder.add("connectives", connectiveBuilder.build());
        allArray.add(categoryBuilder.build());

        all.add("categories", allArray.build());
        return all.build();
    }

    public static Result getConnectives(int questionID) throws IOException, SQLException {
        JsonObject object;

        DiscourseConnectivesQuestion question = (DiscourseConnectivesQuestion) Question.byId(questionID);
        switch (question.getSentenceType()) {
            case "Conjunction":
                object = createConjunctions();
                break;
            case "Conjunctive adverb":
                object = createConjunctiveAdverbs();
                break;
            default:
                return internalServerError("Unknown SentenceType");
        }


        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(object.toString());
        return ok(actualObj);
    }

    public int getId() {
        return id;
    }

    public String getSentence1() {
        return sentence1;
    }

    public String getSentence2() {
        return sentence2;
    }

    public int getExperimentID() {
        return experimentID;
    }

    @Override
    public void writeResults(JsonNode resultNode) {
        // TODO: Implement
    }

    public String getSentenceType() {
        return sentenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DiscourseConnectivesQuestion)) return false;
        if (!super.equals(o)) return false;

        DiscourseConnectivesQuestion question = (DiscourseConnectivesQuestion) o;

        return sentence1.equals(question.sentence1) && sentence2.equals(question.sentence2);
    }
}
