package controllers;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.Groups.AbstractGroup;
import models.Groups.GroupFactory;
import models.LingoExpModel;
import models.Questions.Question;
import models.Worker;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;
import services.LingoturkConfig;
import views.html.index;

import javax.inject.Inject;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

import static play.libs.Json.stringify;

/***
 * Controller handling handling requests related to experiment instances. These requests include
 * creation, modification, deletion of instances as well returning instances as JSON.
 */
public class ExperimentController extends Controller {

    private final FormFactory formFactory;
    private final LingoturkConfig lingoturkConfig;

    @Inject
    public ExperimentController(final FormFactory formFactory, final LingoturkConfig lingoturkConfig) {
        this.formFactory = formFactory;
        this.lingoturkConfig = lingoturkConfig;
    }

    /**
     * Currently not in use. This function will be populated with a new meaning soon.
     * Renders the modify-view for a given experiment.
     *
     * @param id the experiments id
     * @return the rendered page
     */
    @SubjectPresent
    public Result modify(int id) {
        return internalServerError("This function is currently not implemented.");
    }

    /**
     * Retrieves a list of all stored experiments and then renders the managing page listing all experiments
     *
     * @return renders the page
     */
    @SubjectPresent
    public Result manage() throws SQLException {
        return ok(views.html.ExperimentManagement.manage.render(LingoExpModel.getAllExperiments()));
    }

    /**
     * Deletes an experiment, out of the database
     *
     * @param id the experiment's- id
     * @return the rendered index-page
     */
    @SubjectPresent
    public Result delete(int id) throws SQLException {
        LingoExpModel exp = LingoExpModel.byId(id);

        if (exp != null) {
            exp.delete();
        }

        return ok(index.render());
    }

    /**
     * Creates a new experiment instance for the experiment type specified by {@code name}.
     * First checks if a customized template is available. If that is not the case, fall back
     * to the default template. If no such experiment type exists, return an {@code internalServerError}
     *
     * @param name The name of the experiment type to instantiate
     * @return The customized/fallback tempalte, if available. An internalServerError otherwise.
     */
    @SubjectPresent
    public Result createExperiment(String name) {
        try {
            Class<?> c = Class.forName("views.html.ExperimentCreation." + name + ".create" + name);
            Method m = c.getMethod("render");
            Html webpage = (Html) m.invoke(null);
            return ok(webpage);
        } catch (ClassNotFoundException e) {
            // Remove "Experiment" at the end of name
            return ok(views.html.ExperimentCreation.createExperiment.render(name.substring(0, name.length() - "Experiment".length())));
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
            return internalServerError("Wrong type for name: " + name);
        }
    }

    /**
     * Creates and stores a new experiment instance.
     *
     * @return Returns to the index site.
     * @throws SQLException is propagated if it occurs internally.
     */
    @SubjectPresent
    @BodyParser.Of(value = BodyParser.Json.class)
    public Result submitNewExperiment() throws SQLException {
        JsonNode json = request().body().asJson();

        LingoExpModel experiment;
        String name = json.get("name").asText();
        String description = json.get("description").asText();
        String additionalExplanations = json.get("additionalExplanations").asText();
        String experimentType = json.get("type").asText();


        String listType;
        Properties experimentProperties = new Properties();
        try {
            experimentProperties.load(new FileReader("app/models/Questions/" + experimentType + "/experiment.properties"));
            listType = experimentProperties.getProperty("listType");
        } catch (IOException e) {
            e.printStackTrace();
            return internalServerError("Can't find experiment type: " + experimentType);
        }

        // new experiment
        if (json.get("id").asInt() == -1) {
            experiment = LingoExpModel.createLingoExpModel(name, description, additionalExplanations, "", experimentType, listType);
        } else {
            experiment = LingoExpModel.byId(json.get("id").asInt());
            experiment.setName(name);
            experiment.setDescription(description);
            experiment.setAdditionalExplanations(additionalExplanations);
            experiment.setExperimentType(experimentType);
        }

        experiment.update();

        // Create Parts

        for (Iterator<JsonNode> partIterator = json.get("parts").iterator(); partIterator.hasNext(); ) {
            JsonNode partNode = partIterator.next();
            AbstractGroup p = GroupFactory.createPart(partNode.get("_type").asText(), experiment, partNode);
            if (p == null) {
                return internalServerError("Unknown Group type: " + partNode.get("_type").asText());
            }
        }

        // Redirect
        return ok(index.render());
    }

    /**
     * Loads the current instructions for experiment {@code expId} from the database and displays the
     * instruction modification interface.
     *
     * @param expId The Id of the experiment that should be modified
     * @return The instruction modification interface
     */
    @SubjectPresent
    public Result editInstructions(int expId) {
        LingoExpModel expModel = LingoExpModel.byId(expId);
        return ok(views.html.ExperimentManagement.editInstructions.render(expId, expModel.getAdditionalExplanations(), false));
    }

    /**
     * Stores new instructions {@code instructions} for the experiment specified by the {@code expId} value in the request.
     *
     * @return Returns the index page, if the experiment specified by  {@code expId} exists. A badRequest otherwise
     */
    @SubjectPresent
    public Result submitNewInstructions() {
        DynamicForm requestData = formFactory.form().bindFromRequest();
        int expId = Integer.parseInt(requestData.get("expId"));
        String instructions = requestData.get("instructions");

        LingoExpModel expModel = LingoExpModel.byId(expId);
        if (expModel != null) {
            expModel.setAdditionalExplanations(instructions);
            return ok(views.html.ExperimentManagement.editInstructions.render(expId, expModel.getAdditionalExplanations(), true));
        }

        return badRequest("Experiment does not exist.");
    }

    /**
     * Converts the experiment specified by {@code expId} to JSON and returns it. If the experiment does not
     * exist, a {@code badRequest} is returned. If an exception occurs internally, an {@code internalServerError}
     * is returned.
     *
     * @param expID The id of the experiment to return
     * @return The JSON representation of the experiment specified by {@code expId}, if it exists and no error occurs.
     */
    public Result returnJSON(int expID) {
        LingoExpModel exp = LingoExpModel.byId(expID);
        if (exp == null) {
            return badRequest("Experiment does not exist.");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj;
        try {
            actualObj = mapper.readTree(exp.returnJSON().toString());
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return internalServerError("Could not convert to JSON.");
        }
        return ok(stringify(actualObj));
    }

    /**
     * Converts the question specified by {@code questionId} to JSON and returns it. If the question does not
     * exist, a {@code badRequest} is returned. If an exception occurs internally, an {@code internalServerError}
     * is returned.
     *
     * @param questionId The id of the question to return
     * @return The JSON representation of the experiment specified by {@code expId}, if it exists and no error occurs.
     */
    public Result getQuestion(int questionId) {
        Question question = Question.byId(questionId);
        if (question == null) {
            return badRequest("Question does not exist.");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj;
        try {
            actualObj = mapper.readTree(question.returnJSON().toString());
        } catch (IOException e) {
            e.printStackTrace();
            return internalServerError("Could not convert to JSON.");
        }
        return ok(actualObj);
    }

    /**
     * Converts the group specified by {@code groupId} to JSON and returns it. If the group does not
     * exist, a {@code badRequest} is returned. If an exception occurs internally, an {@code internalServerError}
     * is returned.
     *
     * @param groupId The id of the group to return
     * @return The JSON representation of the experiment specified by {@code groupId}, if it exists and no error occurs.
     */
    public Result returnPart(int groupId) {
        AbstractGroup p = AbstractGroup.byId(groupId);
        if (p == null) {
            return badRequest("Group does not exist.");
        }

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj;
        try {
            actualObj = mapper.readTree(p.returnJSON().toString());
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            return internalServerError("Could not convert to JSON.");
        }
        return ok(stringify(actualObj));
    }

    /**
     * Returns a participants name for experiment {@code expId} stored in a cookie of the current request {@code r}.
     * If no value is set, null is returned.
     *
     * @param r     The current request
     * @param expId The Id of the experiment to check
     * @return The participants name, if found, null otherwise
     */
    public String getStoredName(Http.Request r, int expId) {
        Http.Cookie cookie = r.cookie("name_" + expId);
        if (cookie != null) {
            String name = cookie.value();
            System.out.println("Cookie set for experiment " + expId + ". Stored name: " + name);
            return name;
        }

        return null;
    }

    /**
     * Assigns a group of the experiment specified by {@code expId} to participant {@code workerId}.
     * If this participant is already assigned a group, return the same one again.
     * If the participant already entered another workerId (stored in the cookie of this request),
     * update the stored name and return the same group again.
     *
     * @param expId    The Id of the experiment to use
     * @param workerId The Id of the worker that should be assigned a group
     * @return The new group assigned to the worker
     * @throws SQLException
     * @throws IOException
     */
    public synchronized Result returnPartAsJSON(int expId, String workerId) throws SQLException, IOException {
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
        AbstractGroup group;

        // Worker didn't accept this hit yet
        if (stored_workerId == null || stored_worker == null) {
            Worker.Participation participation = worker.getParticipatesInExperiment(exp);

            if (participation == null) {
                group = getNextPart(exp, ScheduleType.AVAILABLE);
                if (group == null) {
                    group = getNextPart(exp, ScheduleType.ENABLED);
                }
                if (group == null) {
                    group = getNextPart(exp, ScheduleType.ALL);
                }
                worker.addParticipatesInPart(group, null, null, "Prolific" + ZonedDateTime.now(), null);
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
                    Worker w = Worker.getWorkerById(workerId);
                    w.updateParticipatesInPart(stored_workerId, group);
                }
            }
        }

        response().setCookie(Http.Cookie.builder("name_" + expId, workerId).withMaxAge(Duration.ofSeconds(3600)).build());

        ObjectMapper mapper = new ObjectMapper();
        JsonNode actualObj = mapper.readTree(group.returnJSON().toString());
        return ok(stringify(actualObj));
    }

    /**
     * Asks the user which kind of experiment s/he wants to create.
     * Lists all available experiment types to choose from.
     *
     * @return Result object (The rendered page)
     */
    @SubjectPresent
    public Result overviewPage() {
        return ok(views.html.ExperimentCreation.overviewPage.render(lingoturkConfig.getExperimentNames()));
    }

    /**
     * Specifies the possible priorities when scheduling groups.
     */
    private enum ScheduleType {
        /**
         * Schedule the current group next, only if it's availability is > 0
         */
        AVAILABLE,

        /**
         * Schedule the current group next, only if it's not disabled
         */
        ENABLED,

        /**
         * Always schedule the current group next.
         */
        ALL
    }

    /**
     * Maps expId -> [0, # groups of experiment expId )
     */
    private static Map<Integer, Integer> lastUsedList = new HashMap<>();


    /**
     * Given an experiment {@code exp}, find the next group that fulfills the schedule type {@code type}
     *
     * @param exp  The experiment to check
     * @param type The schedule type that should be fulfilled
     * @return The next group in the list that fulfills the requirement, or null if no such exists
     * @throws SQLException If one of the internal methods throws an exception
     */
    private static AbstractGroup getNextPart(LingoExpModel exp, ScheduleType type) throws SQLException {
        int oldValue;
        int listCounter;
        AbstractGroup group = null;
        int expId = exp.getId();

        if (lastUsedList.containsKey(expId)) {
            oldValue = listCounter = lastUsedList.get(expId);
        } else {
            oldValue = listCounter = 0;
            lastUsedList.put(expId, 0);
        }

        List<AbstractGroup> lists = exp.getParts();

        boolean found;
        do {
            found = false;
            AbstractGroup g = lists.get(listCounter);

            if (type == ScheduleType.AVAILABLE && g.decreaseIfAvailable()
                    || type == ScheduleType.ENABLED && (!g.disabled)
                    || type == ScheduleType.ALL) {
                group = g;
                found = true;
            }

            listCounter++;
            if (listCounter >= lists.size()) {
                listCounter = 0;
            }
            lastUsedList.put(expId, listCounter);
        } while (!found && listCounter != oldValue);

        if (group != null) {
            System.out.println("Schedule List " + group.getId() + " next for exp: " + expId + " in mode " + type);
        }

        return group;
    }

}
