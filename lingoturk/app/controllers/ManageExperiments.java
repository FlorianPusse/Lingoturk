package controllers;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.LingoExpModel;
import models.Groups.AbstractGroup;
import models.Groups.GroupFactory;
import models.Questions.DiscourseConnectivesExperiment.CheaterDetectionQuestion;
import models.Questions.ExampleQuestion;
import models.Questions.LinkingExperimentV1.Prolific.Combination;
import models.Questions.LinkingExperimentV1.Prolific.LinkingGroup;
import models.Questions.PartQuestion;
import models.Questions.Question;
import models.Questions.QuestionFactory;
import models.Repository;
import models.Worker;
import play.api.templates.Html;
import play.data.DynamicForm;
import play.data.Form;
import play.libs.Crypto;
import play.libs.Json;
import play.mvc.*;

import com.fasterxml.jackson.databind.JsonNode;
import play.mvc.BodyParser;
import views.html.index;

import static models.AbstractFactory.convertToClassIdentifier;
import static org.apache.commons.io.FileUtils.listFiles;

public class ManageExperiments extends Controller {

    /* Render experiment creation page */
    @BodyParser.Of(value = BodyParser.Json.class, maxLength = 200 * 1024 * 10)
    public static Result submitResults() {
        JsonNode json = request().body().asJson();
        String experimentType = json.get("experimentType").asText();
        Class<?> experimentClass = ManageExperiments.lookupExperimentType(experimentType);

        if (experimentClass == null) {
            return internalServerError("Unknown experiment type: " + experimentType);
        }

        try {
            Question question = (Question) experimentClass.newInstance();
            Method m = experimentClass.getMethod("writeResults", JsonNode.class);
            m.invoke(question, json);
        } catch (NoSuchMethodException e) {
            return internalServerError("Function write: " + experimentType);
        } catch (InstantiationException e) {
            return internalServerError("No default constructor for: " + experimentType);
        } catch (IllegalAccessException e) {
            return internalServerError("writeResults method is not accessible: " + experimentType);
        } catch (InvocationTargetException e) {
            return internalServerError("writeResults method throws exception: \n" + e.getMessage());
        }

        return ok();
    }

    @BodyParser.Of(BodyParser.Json.class)
    public static Result submitResult() throws SQLException {
        JsonNode json = request().body().asJson();
        if (json != null) {
            String assignmentID = json.get("assignmentID").asText();
            if (assignmentID != null && !assignmentID.equals("ASSIGNMENT_ID_NOT_AVAILABLE_TEST")) {
                AsynchronousJob.addAssignmentID(assignmentID);
            }
        }

        return ok();
    }

    public static Result createExperiment(String name) {
        try {
            Class<?> c = Class.forName("views.html.CreateExperiments." + name + ".create" + name);
            Method m = c.getMethod("render");
            Html webpage = (Html) m.invoke(null);
            return ok(webpage);
        } catch (ClassNotFoundException e) {
            return internalServerError("Unknown experiment name: " + name);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
            return internalServerError("Wrong type for name: " + name);
        }
    }

    private static Map<String, Class> classMap = new HashMap<>();

    public static Class lookupExperimentType(String experimentName) {
        if (classMap.containsKey(experimentName)) {
            return classMap.get(experimentName);
        }

        File folder = new File("app/models/Questions/" + experimentName + "/");
        if (folder.exists() && folder.isDirectory()) {
            Collection<File> files = listFiles(folder, new String[]{"java"}, false);
            for (File f : files) {
                String classIdentifier = convertToClassIdentifier(f.getPath());
                Class<?> c = null;
                try {
                    c = Class.forName(classIdentifier);
                } catch (ClassNotFoundException e) {
                    // Is not a correct implemented file -> Play will take care of that
                }

                if (PartQuestion.class.isAssignableFrom(c)) {
                    classMap.put(experimentName, c);
                    return c;
                }
            }
        }

        return null;
    }

    public static Result editInstructions(int expId) {
        LingoExpModel expModel = LingoExpModel.byId(expId);
        return ok(views.html.ManageExperiments.editInstructions.render(expId, expModel.getAdditionalExplanations()));
    }

    public static Result submitNewInstructions() throws SQLException {
        DynamicForm requestData = Form.form().bindFromRequest();
        int expId = Integer.parseInt(requestData.get("expId"));
        String instructions = requestData.get("instructions");
        LingoExpModel expModel = LingoExpModel.byId(expId);
        expModel.setAdditionalExplanations(instructions);
        return ok(views.html.ManageExperiments.editInstructions.render(expId, expModel.getAdditionalExplanations()));
    }

    public static List<String> getExperimentNames() {
        List<String> experimentNames = new LinkedList<>();

        File folder = new File("app/models/Questions");
        File[] folders = folder.listFiles(File::isDirectory);
        for (File f : folders) {
            experimentNames.add(f.getName());
        }

        return experimentNames;
    }

    public static Result returnJSON(int expID) throws SQLException, IOException {
        LingoExpModel exp = LingoExpModel.byId(expID);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(exp.returnJSON().toString());
        return ok(Json.stringify(actualObj));
    }

    public static Result getQuestion(int id) throws SQLException, IOException {
        Question question = Question.byId(id);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(question.returnJSON().toString());
        return ok(actualObj);
    }

    public static Result returnPart(int partId) throws SQLException, IOException {
        AbstractGroup p = AbstractGroup.byId(partId);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(p.returnJSON().toString());
        return ok(Json.stringify(actualObj));
    }

    public static String getStoredName(Http.Request r, int expId) {
        Http.Cookie cookie = r.cookie("name_" + expId);
        if (cookie != null) {
            String name = Crypto.decryptAES(cookie.value());
            System.out.println("Cookie set for experiment " + expId + ". Stored name: " + name);
            return name;
        } else {
            return null;
        }
    }

    static int fallBackCounter = 0;

    public static Result returnPartAsJSON(int expId, String workerId) throws SQLException, IOException {
        LingoExpModel exp = LingoExpModel.byId(expId);

        Worker worker = Worker.getWorkerById(workerId);
        if (worker == null) {
            worker = Worker.createWorker(workerId);
        }

        String stored_workerId = getStoredName(request(), exp.getId());
        Worker stored_worker = null;
        if (stored_workerId != null) {
            stored_worker = Worker.getWorkerById(stored_workerId);
        }
        AbstractGroup group = null;

        // Worker didn't accept this hit yet
        if (stored_workerId == null || stored_worker == null) {
            Worker.Participation participation = worker.getParticipatesInExperiment(exp);

            if (participation == null) {
                for (AbstractGroup p : exp.getParts()) {
                    if (p.decreaseIfAvailable()) {
                        group = p;
                        break;
                    }
                }
                if (group == null) {
                    group = exp.getParts().get(fallBackCounter);
                    System.out.println("No Parts available. Fallback to Group Nr. " + group.getId());
                    fallBackCounter++;
                    if (fallBackCounter >= exp.getParts().size()) {
                        fallBackCounter = 0;
                    }
                }
                worker.addParticipatesInPart(group, null, null, "Prolific", null);
            } else {
                group = AbstractGroup.byId(participation.getPartID());
            }
        } else {
            if (!stored_workerId.equals(workerId)) {
                System.out.println(stored_workerId + " tries to open experiment " + expId + " again with new workerId " + workerId);
            }

            Worker.Participation participation = stored_worker.getParticipatesInExperiment(exp);

            if (participation == null) {
                throw new RuntimeException();
            } else {
                group = AbstractGroup.byId(participation.getPartID());

                if (!stored_workerId.equals(workerId)) {
                    Worker.updateParticipatesInPart(stored_workerId, group, workerId);
                }
            }
        }

        response().setCookie("name_" + expId, Crypto.encryptAES(workerId), 999999);

        if (group instanceof LinkingGroup) {
            PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT DISTINCT lhs_script, rhs_script FROM LinkingResult WHERE workerId = ?");
            statement.setString(1, workerId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                int lhs_script = rs.getInt("lhs_script");
                int rhs_script = rs.getInt("rhs_script");

                for (Iterator<PartQuestion> combinationIterator = group.getQuestions().iterator(); combinationIterator.hasNext(); ) {
                    Combination c = (Combination) combinationIterator.next();
                    if (c.getLhs() == lhs_script && c.getRhs() == rhs_script) {
                        combinationIterator.remove();
                    }
                }
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(group.returnJSON().toString());
        return ok(Json.stringify(actualObj));
    }

    /**
     * Submits a new DragNDrop experiment and saves it into the database.
     * It collects all data, which was submitted via POST.
     *
     * @return the index-page
     */
    @BodyParser.Of(value = BodyParser.Json.class, maxLength = 200 * 1024 * 10)
    public static Result submitNewExperiment() throws SQLException {
        JsonNode json = request().body().asJson();

        LingoExpModel experiment;
        String name = json.get("name").asText();
        String description = json.get("description").asText();
        String nameOnAmt = json.get("nameOnAmt").asText();
        String additionalExplanations = json.get("additionalExplanations").asText();
        String experimentType = json.get("type").asText();

        // new experiment
        if (json.get("id").asInt() == -1) {
            experiment = LingoExpModel.createLingoExpModel(name, description, additionalExplanations, nameOnAmt, experimentType);
        } else {
            experiment = LingoExpModel.byId(json.get("id").asInt());
            experiment.setName(name);
            experiment.setDescription(description);
            experiment.setNameOnAmt(nameOnAmt);
            experiment.setAdditionalExplanations(additionalExplanations);
            experiment.setExperimentType(experimentType);
        }

        experiment.update();

        // Create example question
        List<ExampleQuestion> exampleQuestions_tmp = new LinkedList<>();
        for (Iterator<JsonNode> exampleQuestions = json.get("exampleQuestions").iterator(); exampleQuestions.hasNext(); ) {
            JsonNode question = exampleQuestions.next();
            String type = question.get("type").asText();
            ExampleQuestion exampleQuestion = QuestionFactory.createExampleQuestion(type, experiment, question);
            exampleQuestions_tmp.add(exampleQuestion);
        }

        experiment.setExampleQuestions(exampleQuestions_tmp);


        // Create cheater detection questions
        JsonNode cheaterDetectionNode = json.get("cheaterDetectionQuestions");
        if (cheaterDetectionNode != null) {
            List<CheaterDetectionQuestion> cheaterDetectionQuestions_tmp = CheaterDetectionQuestion.createCheaterDetectionQuestions(experiment, json.get("cheaterDetectionQuestions"));
            experiment.setCheaterDetectionQuestions(cheaterDetectionQuestions_tmp);
        }

        // Create Parts
        List<AbstractGroup> groups = new LinkedList<>();
        for (Iterator<JsonNode> partIterator = json.get("parts").iterator(); partIterator.hasNext(); ) {
            JsonNode partNode = partIterator.next();
            AbstractGroup p = GroupFactory.createPart(partNode.get("type").asText(), experiment, partNode);
            if (p != null) {
                groups.add(p);
            } else {
                return internalServerError("Unknown Group type: " + partNode.get("type").asText());
            }
        }

        // Redirect
        return ok(index.render());
    }
}