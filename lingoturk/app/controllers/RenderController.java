package controllers;

import models.Groups.DisjointGroup;
import models.Groups.AbstractGroup;
import models.Groups.FullGroup;
import models.Groups.LinkingExperimentV2.PoolGroupV2;
import models.LingoExpModel;
import models.Questions.ExampleQuestion;
import models.Questions.LinkingExperimentV1.Script;
import models.Questions.Question;
import models.Repository;
import models.Worker;
import play.mvc.Controller;
import play.mvc.Result;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Random;


public class RenderController extends Controller {

    public static Result renderExampleQuestion(int id) {
        ExampleQuestion exampleQuestion = (ExampleQuestion) Question.byId(id);
        return exampleQuestion.render();
    }

    private static Random randomGenerator = new Random();
    public static Result renderPictureNaming(int expId, String origin) throws SQLException {
        if (origin != null) {
            LingoExpModel exp = LingoExpModel.byId(expId);
            List<AbstractGroup> groups = exp.getParts();

            int index = randomGenerator.nextInt(groups.size());
            AbstractGroup group = groups.get(index);

            if (!(group instanceof DisjointGroup)) {
                throw new RuntimeException("Wrong type");
            } else {
                DisjointGroup disjointGroup = (DisjointGroup) group;
                return disjointGroup.render(origin, -1);
            }
        } else {
            LeastUsedChunksAnswer leastUsedChunk = getLeastUsedChunk(expId);
            DisjointGroup part = (DisjointGroup) AbstractGroup.byId(leastUsedChunk.partId);
            return part.render(origin, leastUsedChunk.chunkId);
        }
    }

    static int storyCompletionCounter = 0;
    public static synchronized Result renderStoryCompletion(int expId, String origin) throws SQLException {
        if (origin != null) {
            LingoExpModel exp = LingoExpModel.byId(expId);
            if(exp != null){
                return ok(views.html.renderExperiments.SentenceCompletionExperiment.SentenceCompletionExperiment.render(exp));
            }
        } else {
            LeastUsedChunksAnswer leastUsedChunk = getLeastUsedChunk(expId);
            DisjointGroup part = (DisjointGroup) AbstractGroup.byId(leastUsedChunk.partId);
            return part.render(origin, leastUsedChunk.chunkId);
        }
        return notFound();
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

    static int plausiblityTestCounter = 0;

    public static synchronized Result renderPB(int expId, String origin) throws SQLException {
        LingoExpModel exp = LingoExpModel.byId(expId);
        List<AbstractGroup> groups = exp.getParts();

        AbstractGroup group = groups.get(plausiblityTestCounter);
        plausiblityTestCounter++;
        if (plausiblityTestCounter == groups.size()) {
            plausiblityTestCounter = 0;
        }

        if (!(group instanceof FullGroup)) {
            throw new RuntimeException("Wrong type");
        } else {
            FullGroup plausibilityGroup = (FullGroup) group;
            // TODO: redo
            return null;
            //return plausibilityGroup.render(origin);
        }
    }

    public static Result renderAMT(int questionId, int questionId2, int slot1, int slot2, String assignmentId, String hitId, String workerId, String turkSubmitTo) {
        if (assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") || workerId == null) {
            return ok(views.html.renderExperiments.LinkingExperimentV2.linking_experimentV2_preview.render());
        }

        return PoolGroupV2.renderAMT(questionId, questionId2, slot1, slot2, assignmentId, hitId, workerId, turkSubmitTo);
    }

    public static Result renderProlific(int expId) {
        LingoExpModel lingoExpModel = LingoExpModel.byId(expId);
        return ok(views.html.renderExperiments.StoryCompletionExperiment.storyCompletionExperiment.render(lingoExpModel));
    }

    /**
     * Renders an experiment's worker-view of any type.
     *
     * @param questionId   The question id in the database
     * @param assignmentId The assignment ID submitted by AMT
     * @param hitId        The HIT ID submitted by AMT
     * @param workerId     The worker ID submitted by AMT or "null" if none submitted
     * @return The rendered page
     */
    public static Result render(String Type, int questionId, int questionId2, String assignmentId, String hitId, String workerId, String turkSubmitTo) throws SQLException {
        // Get right experiment model
        LingoExpModel exp = null;
        Question question = null;
        AbstractGroup group = null;

        if (Type.equals("question")) {
            question = Question.byId(questionId);
            exp = LingoExpModel.byId(question.getExperimentID());
        } else if (Type.equals("part")) {
            group = AbstractGroup.byId(questionId);
            exp = LingoExpModel.byId(group.getExperimentUsedIn());
        }

        // if worker id is not available (null) the site just shows an preview
        if (assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") || workerId == null) {
            return ok(views.html.renderExperiments.LinkingExperimentV2.linking_experimentV2_preview.render());
        }

        Worker worker = Worker.getWorkerById(workerId);

        if (worker == null) {
            worker = Worker.createWorker(workerId);
        }

        // Worker is not allowed to participate in our experiments anymore
        if (worker.getIsBlocked()) {
            return ok(views.html.renderExperiments.bannedWorker.render());
        }

        if (Type.equals("question")) {
            if (!(question instanceof Script)) {
                return question.render(assignmentId, hitId, workerId, turkSubmitTo, exp.getAdditionalExplanations());
            } else {
                Script rhs = (Script) Question.byId(questionId2);
                return Script.renderScripts((Script) question, rhs, assignmentId, hitId, workerId, turkSubmitTo, exp.getAdditionalExplanations());
            }
        } else if (Type.equals("part")) {
            return group.render(worker, assignmentId, hitId, workerId, turkSubmitTo, exp);
        } else {
            return Controller.badRequest();
        }
    }

    public static Result generalRender(String Type, int questionId, int questionId2, String assignmentId, String hitId, String workerId, String turkSubmitTo) throws SQLException {
        // Get right experiment model
        LingoExpModel exp = null;
        Question question = null;
        AbstractGroup group = null;

        if (Type.equals("question")) {
            question = Question.byId(questionId);
            exp = LingoExpModel.byId(question.getExperimentID());
        } else if (Type.equals("part")) {
            group = AbstractGroup.byId(questionId);
            exp = LingoExpModel.byId(group.getExperimentUsedIn());
        }

        // if worker id is not available (null) the site just shows an preview
        if (assignmentId.equals("ASSIGNMENT_ID_NOT_AVAILABLE") || workerId == null) {
            return ok(views.html.renderExperiments.experiment_preview.render(exp));
        }

        Worker worker = Worker.getWorkerById(workerId);

        if (worker == null) {
            worker = Worker.createWorker(workerId);
        }

        // Worker is not allowed to participate in our experiments anymore
        if (worker.getIsBlocked()) {
            return ok(views.html.renderExperiments.bannedWorker.render());
        }

        if (Type.equals("question")) {
            if (!(question instanceof Script)) {
                return question.render(assignmentId, hitId, workerId, turkSubmitTo, exp.getAdditionalExplanations());
            } else {
                Script rhs = (Script) Question.byId(questionId2);
                return Script.renderScripts((Script) question, rhs, assignmentId, hitId, workerId, turkSubmitTo, exp.getAdditionalExplanations());
            }
        } else if (Type.equals("part")) {
            return group.render(worker, assignmentId, hitId, workerId, turkSubmitTo, exp);
        } else {
            return Controller.badRequest();
        }
    }
}
