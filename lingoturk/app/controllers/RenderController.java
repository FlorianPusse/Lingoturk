package controllers;

import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Questions.ExampleQuestion;
import models.Questions.Question;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import play.mvc.Security;
import play.twirl.api.Html;

public class RenderController extends Controller {

    private static LeastUsedChunksAnswer getLeastUsedChunk(int expId) throws SQLException {
        PreparedStatement statement = DatabaseController.getConnection().prepareStatement("SELECT * FROM (SELECT partId,chunkId,floor(count(*)/31) AS occurences FROM (SELECT * FROM PictureNamingResult JOIN LingoExpModels_contain_Parts USING (PartId) WHERE LingoExpModelId = ? ) AS tmp GROUP BY partId, chunkId) AS tmp2 ORDER BY occurences ASC LIMIT 1;");
        statement.setInt(1, expId);

        ResultSet rs = statement.executeQuery();

        LeastUsedChunksAnswer answer = null;
        if (rs.next()) {
            answer = new LeastUsedChunksAnswer(rs.getInt("chunkId"), rs.getInt("partId"));
        }
        return answer;
    }

    private static class LeastUsedChunksAnswer {
        int chunkId;
        int partId;

        public LeastUsedChunksAnswer(int chunkId, int partId) {
            this.chunkId = chunkId;
            this.partId = partId;
        }
    }

    /**
     * Renders an experiment for the given origin.
     *
     * @param expId The experiment to display
     * @return Result object containing the page.
     */
    public static Result render(Integer expId, Integer partId, Integer questionId, String workerId, String origin) {
        DynamicForm df = new DynamicForm().bindFromRequest();
        LingoExpModel lingoExpModel = LingoExpModel.byId(expId);
        if (lingoExpModel == null) {
            return internalServerError("Unknown experiment Id");
        }

        Worker w = null;
        if(workerId != null){
            w = Worker.getWorkerById(workerId);
        }

        try {
            Method m = getRenderMethod(lingoExpModel.getExperimentType());
            Html webpage = (Html) m.invoke(null, (questionId == null ? null : Question.byId(questionId)), (partId == null ? null : AbstractGroup.byId(partId)), w, null, null, null, lingoExpModel, df, origin);
            return ok(webpage);
        } catch (ClassNotFoundException e) {
            return internalServerError("Unknown experiment name: " + lingoExpModel.getExperimentType());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassCastException e) {
            e.printStackTrace();
            return internalServerError("Wrong type for name: " + lingoExpModel.getExperimentType());
        }
    }

    public static Method getRenderMethod(String experimentType) throws NoSuchMethodException, ClassNotFoundException {
        Class<?> c = Class.forName("views.html.ExperimentRendering." + experimentType + "." + experimentType + "_render");
        return c.getMethod("render", Question.class, AbstractGroup.class, Worker.class, String.class, String.class, String.class, LingoExpModel.class, DynamicForm.class, String.class);
    }

    public static Method getPreviewMethod(String experimentType) throws NoSuchMethodException, ClassNotFoundException {
        Class<?> c = Class.forName("views.html.ExperimentRendering." + experimentType + "." + experimentType + "_preview");
        return c.getMethod("render",LingoExpModel.class);
    }

    /**
     * Renders an experiment's worker-view of any type.
     *
     * @param id           The id in the database
     * @param assignmentId The assignment ID submitted by AMT
     * @param hitId        The HIT ID submitted by AMT
     * @param workerId     The worker ID submitted by AMT or "null" if none submitted
     * @return The rendered page
     */
    public static Result renderAMT(String Type, int id, String assignmentId, String hitId, String workerId, String turkSubmitTo) {
        try {
            DynamicForm df = new DynamicForm().bindFromRequest();

            if(!(Type.equals("question") || Type.equals("part"))){
                return internalServerError("Unknown Type specifier: " + Type);
            }

            // Get right experiment model
            Question question = Type.equals("question") ? Question.byId(id) : null;
            AbstractGroup group = Type.equals("part") ? AbstractGroup.byId(id) : null;
            LingoExpModel exp = LingoExpModel.byId(Type.equals("question") ? question.getExperimentID() : group.getExperimentUsedIn());

            // if worker id is not available (null) the site just shows an preview
            if (assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") || workerId == null) {
                try {
                    return ok((Html) getPreviewMethod(exp.getExperimentType()).invoke(null,exp));
                }catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | ClassNotFoundException e) {
                    // No preview available, try to continue
                    workerId = (workerId != null) ? workerId : "NA";
                }
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

    @Security.Authenticated(Secured.class)
    public static Result previewLists(int expId){
        LingoExpModel exp = LingoExpModel.byId(expId);
        if(exp == null){
            return internalServerError("Experiment Id: " + expId + " does not exist");
        }

        return ok(views.html.ExperimentRendering.previewLists.render(exp));
    }

}
