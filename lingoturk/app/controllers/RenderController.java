package controllers;

import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Questions.ExampleQuestion;
import models.Questions.Question;
import models.Repository;
import models.Worker;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import play.twirl.api.Html;

public class RenderController extends Controller {

    // TODO: Reimplement
    public static Result renderExampleQuestion(int id) {
        ExampleQuestion exampleQuestion = (ExampleQuestion) Question.byId(id);
        return exampleQuestion.render();
    }

    private static LeastUsedChunksAnswer getLeastUsedChunk(int expId) throws SQLException {
        PreparedStatement statement = Repository.getConnection().prepareStatement("SELECT * FROM (SELECT partId,chunkId,floor(count(*)/31) AS occurences FROM (SELECT * FROM PictureNamingResult JOIN LingoExpModels_contain_Parts USING (PartId) WHERE LingoExpModelId = ? ) AS tmp GROUP BY partId, chunkId) AS tmp2 ORDER BY occurences ASC LIMIT 1;");
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
     * Renders an experiment on Prolific Academic
     *
     * @param expId The experiment to display
     * @return Result object containing the page.
     */
    public static Result renderProlific(int expId) {
        LingoExpModel lingoExpModel = LingoExpModel.byId(expId);
        if (lingoExpModel == null) {
            return internalServerError("Unknown experiment Id");
        }
        try {
            Method m = getRenderMethod(lingoExpModel.getExperimentType());
            Html webpage = (Html) m.invoke(null, null, null, null, null, null, null, lingoExpModel, null, "PROLIFIC");
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

            // Get right experiment model
            LingoExpModel exp;
            Question question;
            AbstractGroup group;

            // if worker id is not available (null) the site just shows an preview
            if (assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") || workerId == null) {
                return ok(views.html.ExperimentRendering.LinkingV2Experiment.LinkingV2Experiment_preview.render());
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
                    question = Question.byId(id);

                    if (question == null) {
                        return internalServerError("Unknown experimentId");
                    }

                    exp = LingoExpModel.byId(question.getExperimentID());

                    if (worker.getIsBlockedFor(exp.getId())) {
                        return ok(views.html.ExperimentRendering.blockedWorker.render());
                    }

                    return question.renderAMT(worker, assignmentId, hitId, turkSubmitTo, exp, df);
                case "part":
                    group = AbstractGroup.byId(id);

                    if (group == null) {
                        return internalServerError("Unknown groupId");
                    }

                    exp = LingoExpModel.byId(group.getExperimentUsedIn());

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

}
