package controllers;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Questions.Question;
import models.Worker;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;
import views.html.ExperimentRendering.experiment_main;

import javax.inject.Inject;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/***
 * Controller handling handling requests related to experiment rendering
 */
public class RenderController extends Controller {

    private final FormFactory formFactory;
    public static ConcurrentHashMap<String, String> experimentContent = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> previewContent = new ConcurrentHashMap<>();

    @Inject
    public RenderController(final FormFactory formFactory) {
        this.formFactory = formFactory;
    }

    /**
     * Loads the contents of an experiment type. Instructions will be injected at the position
     * marked with @_SHOW_INSTRUCTIONS_@
     *
     * @param expModel The experiment that should be rendered
     * @return Returns the content of the experiment. If the content can't be loaded, an error is returend instead
     */
    public static Html getExperimentContent(LingoExpModel expModel) {
        String type = expModel.getExperimentType();
        String content = experimentContent.get(type);
        if (content != null) {
            content = content.replace("@_SHOW_INSTRUCTIONS_@", expModel.getAdditionalExplanations());
            return Html.apply(content);
        }
        return Html.apply("Could not load experiment content. Please try again in a few seconds.");
    }

    /**
     * Loads the preview of an experiment type. Instructions will be injected at the position
     * marked with @_SHOW_INSTRUCTIONS_@
     *
     * @param expModel The experiment that should be rendered
     * @return Returns the previw of the experiment. If the content can't be loaded, null is returned instead
     */
    public static Html getPreviewContent(LingoExpModel expModel) {
        String type = expModel.getExperimentType();
        String content = previewContent.get(type);
        if (content != null) {
            content = content.replace("@_SHOW_INSTRUCTIONS_@", expModel.getAdditionalExplanations());
            return Html.apply(content);
        }
        return null;
    }

    /**
     * Renders an experiment for the given origin. This method is mostly invoked by participants
     * coming from Prolific Academic
     *
     * @param expId The experiment to display
     * @return Result object containing the page.
     */
    public Result render(Integer expId, Integer partId, Integer questionId, String workerId, String origin) {
        DynamicForm df = formFactory.form().bindFromRequest();
        LingoExpModel lingoExpModel = LingoExpModel.byId(expId);
        if (lingoExpModel == null) {
            return internalServerError("Unknown experiment Id");
        }

        Worker w = null;
        if (workerId != null) {
            w = Worker.getWorkerById(workerId);
        }

        Html html = getExperimentContent(lingoExpModel);
        return ok(experiment_main.render(lingoExpModel.getExperimentType(), (questionId == null ? null : Question.byId(questionId)), (partId == null ? null : AbstractGroup.byId(partId)), w, null, null, null, lingoExpModel, df, origin, html));
    }

    /**
     * Renders an experiment's worker-view.
     *
     * @param id           The id in the database
     * @param assignmentId The assignment ID submitted by AMT
     * @param hitId        The HIT ID submitted by AMT
     * @param workerId     The worker ID submitted by AMT or "null" if none submitted
     * @return The rendered page
     */
    public Result renderAMT(String Type, int id, String assignmentId, String hitId, String workerId, String turkSubmitTo) {
        try {
            DynamicForm df = formFactory.form().bindFromRequest();

            if (!(Type.equals("question") || Type.equals("part"))) {
                return internalServerError("Unknown Type specifier: " + Type);
            }

            // Get right experiment model
            Question question = Type.equals("question") ? Question.byId(id) : null;
            AbstractGroup group = Type.equals("part") ? AbstractGroup.byId(id) : null;
            LingoExpModel exp = LingoExpModel.byId(Type.equals("question") ? question.getExperimentID() : group.getExperimentUsedIn());

            // if worker id is not available (null) the site just shows an preview
            if (assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") || workerId == null) {
                Html content = getPreviewContent(exp);
                if (content != null) {
                    return ok(content);
                }
                workerId = (workerId != null) ? workerId : "NA";
            }

            // Retrieve worker from DB
            Worker worker = Worker.getWorkerById(workerId);
            if (worker == null) {
                worker = Worker.createWorker(workerId);
            }

            // Worker is not allowed to participate in our experiments anymore
            if (worker.getIsBanned()) {
                return ok(views.html.ExperimentRendering.bannedWorker.render());
            }

            switch (Type) {
                case "question":
                    if (question == null) {
                        return internalServerError("Unknown experimentId");
                    }

                    if (worker.getIsBlockedFor(exp.getId())) {
                        return ok(views.html.ExperimentRendering.blockedWorker.render());
                    }

                    return question.renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
                case "part":
                    if (group == null) {
                        return internalServerError("Unknown groupId");
                    }

                    if (worker.getIsBlockedFor(exp.getId())) {
                        return ok(views.html.ExperimentRendering.blockedWorker.render());
                    }

                    return group.renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
                default:
                    return internalServerError("Unknown Type specifier: " + Type);
            }
        } catch (SQLException e) {
            return internalServerError("Can't connect to DB.");
        }
    }

    /**
     * Previews the lists for a given experiment, represented by the {@code expId}
     *
     * @param expId The experiment that should be previewed
     * @return The preview of the experiment
     */
    @SubjectPresent
    public Result previewLists(int expId) {
        LingoExpModel exp = LingoExpModel.byId(expId);
        if (exp == null) {
            return internalServerError("Experiment Id: " + expId + " does not exist");
        }

        return ok(views.html.ExperimentRendering.previewLists.render(exp));
    }

}
