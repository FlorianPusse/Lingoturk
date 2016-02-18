package controllers;

// Mturk Requester

import java.io.*;
import java.sql.SQLException;
import java.util.*;

import com.amazonaws.mturk.requester.*;
import models.Groups.*;
import models.Questions.DiscourseConnectivesExperiment.CheaterDetectionQuestion;
import models.Worker;
import play.mvc.*;
import views.html.*;

import models.LingoExpModel;
import play.data.DynamicForm;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;


public class Application extends Controller {

    // TODO: Variables will be refractored and removed
    public final static String PASSWORD = "password";
    public static QualificationRequirement[] qualificationRequirements;
    public static int actCounter = 0;

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
        return ok(views.html.CreateExperiments.overviewPage.render(ManageExperiments.getExperimentNames()));
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
            if(expModels.isCurrentlyRunning()){
                runningExperiments.add(expModels);
            }else{
                offlineExperiments.add(expModels);
            }
        }

        return ok(views.html.ManageExperiments.manage.render(offlineExperiments, runningExperiments));
    }

    /**
     * Lets the user choose an experiment platform to publish an experiment on.
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
        return ok(views.html.publishing.publishOnProlific.render(expId));
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
     * Renders the publishOnAMT page and publishes the Experiment on AMT. First it reads the input from the Form, then it creates the HIT
     * and publishes it.
     * After creating the HIT, it saves the HIT-ID in the Experiment.
     *
     * @return renders the page
     */
    @Security.Authenticated(Secured.class)
    public static Result publish() throws SQLException {
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

            if(w == null){
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
        }else{
            return badRequest("Unknown destination.");
        }

        // Quealification requirements
        QualificationRequirement localeRequirement = new QualificationRequirement();
        localeRequirement.setQualificationTypeId(RequesterService.LOCALE_QUALIFICATION_TYPE_ID);
        localeRequirement.setComparator(com.amazonaws.mturk.requester.Comparator.EqualTo);
        localeRequirement.setLocaleValue(new com.amazonaws.mturk.requester.Locale("US"));

        QualificationRequirement hitsApprovedRequirement = new QualificationRequirement();
        hitsApprovedRequirement.setQualificationTypeId("00000000000000000040");
        hitsApprovedRequirement.setComparator(com.amazonaws.mturk.requester.Comparator.GreaterThan);
        hitsApprovedRequirement.setIntegerValue(1000);

        //qualificationRequirements = new QualificationRequirement[]{localeRequirement,hitsApprovedRequirement};
        qualificationRequirements = new QualificationRequirement[0];

        String hitTYPE = service.registerHITType(7200L, 600L, reward, title, keywords, description, qualificationRequirements);

        int publishedId = experiment.publish(lifetime, service.getWebsiteURL() + "/mturk/preview?groupId=" + hitTYPE, df.get("destination"));

        String url = "";

        for (CheaterDetectionQuestion q : experiment.getCheaterDetectionQuestions()) {
            actCounter++;
            url = q.publish(service, publishedId, hitTYPE, lifetime, maxAssign);
        }

        for (AbstractGroup p : experiment.getParts()) {
            actCounter++;
            url = p.publishOnAMT(service, publishedId, hitTYPE, lifetime, maxAssign);
        }
        return ok(views.html.publishing.publish.render(url));
    }

    /**
     * Renders the contact information page
     * @return Result object containing the page
     */
    @Security.Authenticated(Secured.class)
    public static Result contact() {
        return ok(contact.render());
    }

    /**
     * Renders the settings-page of an experiment
     *
     * @param id the experiments id
     * @return the rendered page
     */
    @Security.Authenticated(Secured.class)
    public static Result changeSettings(String id) {
        return ok(views.html.ManageExperiments.changeSettings.render(id));
    }

    /**
     * Deletes an experiment, out of the database
     *
     * @param id the experiment's- id
     * @return the rendered index-page
     */
    public static Result delete(int id) throws SQLException {
        LingoExpModel exp = LingoExpModel.byId(id);

        if (exp != null) {
            exp.delete();
        }

        return ok(index.render());
    }

    /**
     * Saves the given ip into the "ip.properties" file, which is saved in the conf directory
     *
     * @param ip the ip-string
     */
    public static void setStaticIp(String ip) {
        try {
            File file = new File("conf/ip.properties");
            FileWriter fw = new FileWriter(file);
            fw.write(ip);
            fw.close();
        } catch (Throwable e) {
            ok(errorpage.render("Could not write config.", "/"));
        }
    }

    /**
     * Looks up the saved ip from the "ip.properties" file, which is saved in the conf directory
     *
     * @return the saved IP
     */
    public static String getStaticIp() {
        try {
            File file = new File("conf/ip.properties");
            Scanner scanner = new Scanner(file);
            return scanner.nextLine();
        } catch (Throwable e) {
            ok(errorpage.render("Could not read config.", "/"));
        }
        return null;
    }

}
