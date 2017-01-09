package controllers;

// Mturk Requester

import java.io.*;
import java.sql.*;
import java.util.*;

import com.amazonaws.mturk.requester.*;
import com.fasterxml.jackson.databind.JsonNode;
import models.Groups.*;
import models.Worker;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;

import models.LingoExpModel;
import play.data.DynamicForm;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;


public class Application extends Controller {

    // TODO: Variables will be refractored and removed
    public final static String PASSWORD = "password";
    public final static String DEFAULT_PASSWORD = "admin";
    public static QualificationRequirement[] qualificationRequirements;
    public static int actCounter = 0;

    public static Properties properties = null;
    public static final String propertiesLocation = "conf/lingoturk.properties";

    public static Result prototype(){ return ok(views.html.ExperimentRendering.prototype.render()); }

    /**
     * Renders the index-page.
     *
     * @return Result object (index-page)
     */

    @Security.Authenticated(Secured.class)
    public static Result index() {
        return ok(index.render());
    }

    /**
     * Asks the user which kind of experiment s/he wants to create.
     * Lists all available experiment types to choose from.
     *
     * @return Result object (The rendered page)
     */
    @Security.Authenticated(Secured.class)
    public static Result overviewPage() {
        return ok(views.html.ExperimentCreation.overviewPage.render(ManageExperiments.getExperimentNames()));
    }

	/*
     * Manage / Publish
	 */

    /**
     * Renders the manage page, first it gets all experiments, splits them into the ones which are on currently published
     * and the ones which are not. Then it renders the page with two different tables based on the splitted lists.
     *
     * @return renders the page
     */
    @Security.Authenticated(Secured.class)
    public static Result manage() throws SQLException {
        List<LingoExpModel> offlineExperiments = new LinkedList<>();
        List<LingoExpModel> runningExperiments = new LinkedList<>();

        for (LingoExpModel expModels : LingoExpModel.getAllExperiments()) {
            if (expModels.isCurrentlyRunning()) {
                runningExperiments.add(expModels);
            } else {
                offlineExperiments.add(expModels);
            }
        }

        return ok(views.html.ManageExperiments.manage.render(offlineExperiments, runningExperiments));
    }

    /**
     * Lets the user choose an experiment platform to publish an experiment on.
     *
     * @param expId The Id of the experiment that should be published
     * @return the experiment decision page
     */
    @Security.Authenticated(Secured.class)
    public static Result publishingPlatform(int expId) {
        return ok(views.html.publishing.publishingPlatform.render(expId));
    }

    /**
     * Renders the preview page based on the given Experiment given by the Parameters
     *
     * @param expId: The Id of the experiment that should be published
     * @return the preview page
     */
    @Security.Authenticated(Secured.class)
    public static Result publishOnMturk(int expId) {
        LingoExpModel exp = LingoExpModel.byId(expId);

        RequesterService rs = Service.getService(Service.source.MTURK);
        double balance;
        try {
            balance = rs.getAccountBalance();
        } catch (ServiceException se) {
            balance = -1;
        }

        return ok(views.html.publishing.publishOnMturk.render(exp, balance));
    }

    @Security.Authenticated(Secured.class)
    public static Result publishOnProlific(int expId) {
        LingoExpModel expModel = LingoExpModel.byId(expId);
        if (expModel == null) {
            return internalServerError("Experiment ID does not exist!");
        }

        return ok(views.html.publishing.publishOnProlific.render(expModel));
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Result publishProlific() {
        JsonNode json = request().body().asJson();
        int expId = json.get("expId").asInt();
        LingoExpModel expModel = LingoExpModel.byId(expId);

        if(expModel == null){
            return internalServerError("LingoExpModel does not exist.");
        }

        int lifetime = json.get("lifetime").asInt();
        switch (json.get("type").asText()){
            case "DISJOINT LISTS":
                int maxWorkingTime = json.get("maxWorkingTime").asInt();
                int defaultValue = json.get("defaultValue").asInt();
                boolean useAdvancedMode = json.get("useAdvancedMode").asBoolean();

                for(Iterator<Map.Entry<String,JsonNode>> updateIt = json.get("updates").fields(); updateIt.hasNext(); ){
                    Map.Entry<String,JsonNode> update = updateIt.next();

                    try {
                        int partId = Integer.parseInt(update.getKey());
                        AbstractGroup group = AbstractGroup.byId(partId);
                        boolean disabled;
                        int maxParticipants;
                        if(useAdvancedMode) {
                            maxParticipants = update.getValue().get("maxParticipants").asInt();
                        }else{
                            maxParticipants = defaultValue;
                        }
                        int availability = maxParticipants - group.countParticipants();
                        disabled = availability <= 0;

                        PreparedStatement ps = DatabaseController.getConnection().prepareStatement("UPDATE Groups SET disabled = ?, maxParticipants = ?, availability = ?, maxWorkingTime = ? WHERE partId = ?");
                        System.out.println("[info] play - Update Group " + partId + ": Setting disabled to '" + disabled + "' and maxParticipants to '" + maxParticipants + "'");

                        ps.setBoolean(1,disabled);
                        ps.setInt(2,maxParticipants);
                        ps.setInt(3,availability);
                        ps.setInt(4,maxWorkingTime);
                        ps.setInt(5,partId);
                        ps.execute();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return internalServerError("Can't update Groups: " + e.getMessage());
                    }
                }
                break;
            case "MULTIPLE LISTS":
                try {
                    for(AbstractGroup g : expModel.getParts()){
                        PreparedStatement ps = DatabaseController.getConnection().prepareStatement("UPDATE Groups SET disabled = false WHERE partId = ?");
                        ps.setInt(1,g.getId());
                        ps.execute();
                        System.out.println("[info] play - Update Group " + g.getId() + ": Setting disabled to '" + false + "'");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return internalServerError("Could not load Groups: " + e.getMessage());
                }
            default:
        }

        try {
            expModel.publish(lifetime,null,"PROLIFIC");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerError("Could not store publish to DB.");
        }
        System.out.println("[info] play - Published Experiment " + expId + " successfully to Prolific.");

        return ok();
    }

    /**
     * Renders the publishOnAMT page and publishes the Experiment on AMT. First it reads the input from the Form, then it creates the HIT
     * and publishes it.
     * After creating the HIT, it saves the HIT-ID in the Experiment.
     *
     * @return renders the page
     */
    @Security.Authenticated(Secured.class)
    public static Result publishMturk() throws SQLException {
        DynamicForm df = new DynamicForm().bindFromRequest();
        int expId = Integer.parseInt(df.get("eId"));

        // Get experiment
        LingoExpModel experiment = LingoExpModel.byId(expId);

        String title = experiment.getNameOnAmt();
        String description = experiment.getDescription();
        double reward = Double.parseDouble(df.get("costsPerAssignment"));


        Long lifetime = Long.parseLong(df.get("duration"));
        // Convert from days to seconds
        lifetime *= 60 * 60 * 24;

        String keywords = df.get("keywords");
        Integer maxAssign = Integer.parseInt(df.get("maxAssign"));

        String blockedWorkers = df.get("blockedWorkers");

        // einzelne worker rausfiltern
        List<Worker> blocked = new LinkedList<>();
        Scanner scanner = new Scanner(blockedWorkers);
        scanner.useDelimiter(",");

        while (scanner.hasNext()) {
            String workerId = scanner.next().trim();
            Worker w = Worker.getWorkerById(workerId);

            if (w == null) {
                w = Worker.createWorker(workerId);
            }

            blocked.add(w);
        }
        scanner.close();

        // BlockedWorkerListe ergänzen
        if (!blocked.isEmpty()) {
            experiment.addBlockedWorkers(blocked);
        }

        // Create HitType for this Experiment

        RequesterService service;
        if (df.get("destination").equals("sandbox")) {
            service = Service.getService(Service.source.SANDBOX);
        } else if (df.get("destination").equals("amt")) {
            service = Service.getService(Service.source.MTURK);
        } else {
            return badRequest("Unknown destination.");
        }

        //qualificationRequirements = new QualificationRequirement[]{localeRequirement,hitsApprovedRequirement};
        qualificationRequirements = new QualificationRequirement[0];

        String hitTYPE = service.registerHITType(7200L, 600L, reward, title, keywords, description, qualificationRequirements);

        int publishedId = experiment.publish(lifetime, service.getWebsiteURL() + "/mturk/preview?groupId=" + hitTYPE, df.get("destination"));

        String url = "";

        for (AbstractGroup p : experiment.getParts()) {
            actCounter++;
            url = p.publishOnAMT(service, publishedId, hitTYPE, lifetime, maxAssign);
        }
        return ok(views.html.publishing.publish.render(url));
    }

    /**
     * Renders the modify-view for a given experiment
     *
     * @param id the experiments id
     * @return the rendered page
     */
    @Security.Authenticated(Secured.class)
    public static Result modify(int id) {
        LingoExpModel exp = LingoExpModel.byId(id);
        return exp.modify();
    }

    /**
     * Renders the about information page
     *
     * @return Result object containing the page
     */
    @Security.Authenticated(Secured.class)
    public static Result about() {
        return ok(about.render());
    }

    /**
     * Deletes an experiment, out of the database
     *
     * @param id the experiment's- id
     * @return the rendered index-page
     */
    @Security.Authenticated(Secured.class)
    public static Result delete(int id) throws SQLException {
        LingoExpModel exp = LingoExpModel.byId(id);

        if (exp != null) {
            exp.delete();
        }

        return ok(index.render());
    }

    /**
     * Saves the given ip into the "lingoturk.properties" file, which is saved in the conf directory
     *
     * @param ip the ip-string
     */
    public static void setStaticIp(String ip) {
        try {
            properties.setProperty("serverip",ip);
            properties.store(new FileWriter(propertiesLocation),null);
        } catch (Throwable e) {
            ok(errorpage.render("Could not write config.", "/"));
        }
    }

    /**
     * Looks up the saved ip from the "lingoturk.properties" file, which is saved in the conf directory
     *
     * @return the saved IP
     */
    public static String getStaticIp() {
        return properties.getProperty("serverip");
    }

}
