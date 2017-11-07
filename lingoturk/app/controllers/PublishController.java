package controllers;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import com.amazonaws.mturk.requester.Comparator;
import com.amazonaws.mturk.requester.Locale;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.amazonaws.mturk.requester.QualificationType;
import com.amazonaws.mturk.service.axis.RequesterService;
import com.amazonaws.mturk.service.exception.ServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import models.Groups.AbstractGroup;
import models.LingoExpModel;
import models.Worker;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import services.DatabaseService;
import services.LingoturkConfig;
import services.MechanicalTurkService;

import javax.inject.Inject;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/***
 * Controller handling handling requests related to experiment publishing. Experiments
 * can be published either to Amazon Mechanical Turk (via API) or Prolific Academic
 */
public class PublishController extends Controller {

    private final FormFactory formFactory;
    private final DatabaseService databaseService;
    private final LingoturkConfig lingoturkConfig;
    private final MechanicalTurkService mechanicalTurkService;

    @Inject
    public PublishController(final FormFactory formFactory, final DatabaseService databaseService, final LingoturkConfig lingoturkConfig, final MechanicalTurkService mechanicalTurkService) {
        this.formFactory = formFactory;
        this.databaseService = databaseService;
        this.lingoturkConfig = lingoturkConfig;
        this.mechanicalTurkService = mechanicalTurkService;
    }

    /**
     * Lets the user choose an experiment platform to publish an experiment on.
     *
     * @param expId The Id of the experiment that should be published
     * @return the experiment decision page
     */
    @SubjectPresent
    public Result publishingPlatform(int expId) {
        return ok(views.html.publishing.publishingPlatform.render(expId));
    }

    /**
     * Renders the preview page based on the given Experiment. The preview pages
     * shows the current account balance (if available) and allows the user to enter
     * publishing options such as reward, expected running time, ...
     *
     * @param expId: The Id of the experiment that should be published
     * @return the preview page
     */
    @SubjectPresent
    public Result publishOnMturk(int expId) {
        LingoExpModel exp = LingoExpModel.byId(expId);

        RequesterService rs = mechanicalTurkService.getService(MechanicalTurkService.source.MTURK);
        double balance;
        try {
            balance = rs.getAccountBalance();
        } catch (ServiceException se) {
            balance = -1;
        }

        return ok(views.html.publishing.publishOnMturk.render(exp, balance, lingoturkConfig.getStaticIp()));
    }

    /**
     * Renders the publishOnAMT page and publishes the Experiment on AMT. First it reads the input from the
     * request form, then it creates the HIT and publishes it. Infos about the publishing process are stored
     * in the database.
     *
     * @return renders the page
     */
    @SubjectPresent
    public Result publishMturk() throws SQLException {
        DynamicForm df = formFactory.form().bindFromRequest();
        int expId = Integer.parseInt(df.get("eId"));

        // Get experiment
        LingoExpModel experiment = LingoExpModel.byId(expId);
        experiment.setNameOnAmt(df.get("nameOnAmt"));
        experiment.setDescriptionOnAmt(df.get("descriptionOnAmt"));

        String title = experiment.getNameOnAmt();
        String description = experiment.getDescriptionOnAmt();
        double reward = Double.parseDouble(df.get("costsPerAssignment"));


        Long lifetime = Long.parseLong(df.get("duration"));
        // Convert from days to seconds
        lifetime *= 60 * 60 * 24;

        Long maxWorkingTime = Long.parseLong(df.get("maxWorkingTime"));
        // Convert from minutes to seconds
        maxWorkingTime *= 60;

        Long autoApprovalDelay = Long.parseLong(df.get("autoApprovalDelay"));
        // Convert from days to seconds
        autoApprovalDelay *= 60 * 60 * 24;

        String keywords = df.get("keywords");
        Integer maxAssign = Integer.parseInt(df.get("maxAssign"));

        boolean useRequirements = df.get("useRequirements") != null;
        boolean useLocationRequirements = df.get("useLocationRequirements") != null;
        int minHitsApproved = Integer.parseInt(df.get("hitsApprovedRequirement"));
        int minApprovalRate = Integer.parseInt(df.get("approvalRateRequirement"));
        String[] allowedLocales = df.get("allowedLocales").split(",");

        // Create HitType for this Experiment

        RequesterService service;
        if (df.get("destination").equals("sandbox")) {
            service = mechanicalTurkService.getService(MechanicalTurkService.source.SANDBOX);
        } else if (df.get("destination").equals("amt")) {
            service = mechanicalTurkService.getService(MechanicalTurkService.source.MTURK);
        } else {
            return badRequest("Unknown destination.");
        }

        String blockedWorkers = df.get("blockedWorkers");

        // filter out individual participants
        String[] blocked = blockedWorkers.split(",");
        List<Worker> blockedWorkersList = new LinkedList<>();
        for (int i = 0; i < blocked.length; ++i) {
            String w = blocked[i].trim();
            if (!w.isEmpty()) {
                blocked[i] = w;
                blockedWorkersList.add(Worker.getWorkerById(w));
            }
        }
        // add them to the list of blocked participants
        if (!blockedWorkersList.isEmpty()) {
            experiment.addBlockedWorkers(blockedWorkersList);
        }

        List<QualificationRequirement> requirements = new LinkedList<>();
        if (blockedWorkersList.size() > 0) {
            QualificationType qualificationType = null;

            String qualificationName = experiment.getNameOnAmt() + " " + experiment.getId();

            // Check if the qualification already exists
            for (QualificationType type : service.getAllQualificationTypes()) {
                if (type.getName().equals(qualificationName)) {
                    qualificationType = type;
                    break;
                }
            }

            // Qualification does not exist yet, create it right now
            if (qualificationType == null) {
                qualificationType = service.createQualificationType(qualificationName + " Qualification", "", "This qualification is assigned to participants of the " + experiment.getName() + "experiment or similar ones. Unfortunately, they won't be able to participate in the following ones.");
            }

            for (Worker w : blockedWorkersList) {
                service.assignQualification(qualificationType.getQualificationTypeId(), w.getId(), null, false);
            }

            QualificationRequirement blockWorkers = new QualificationRequirement();
            blockWorkers.setQualificationTypeId(qualificationType.getQualificationTypeId());
            blockWorkers.setComparator(Comparator.DoesNotExist);

            requirements.add(blockWorkers);
        }

        if (minApprovalRate > 0 && useRequirements) {
            QualificationRequirement approvalRateRequirement = new QualificationRequirement();
            approvalRateRequirement.setQualificationTypeId(RequesterService.APPROVAL_RATE_QUALIFICATION_TYPE_ID);
            approvalRateRequirement.setComparator(Comparator.GreaterThan);
            approvalRateRequirement.setIntegerValue(new int[]{minApprovalRate});

            requirements.add(approvalRateRequirement);
        }

        if (minHitsApproved > 0 && useRequirements) {
            QualificationRequirement hitsApprovedRequirement = new QualificationRequirement();
            hitsApprovedRequirement.setQualificationTypeId("00000000000000000040");
            hitsApprovedRequirement.setComparator(Comparator.GreaterThan);
            hitsApprovedRequirement.setIntegerValue(new int[]{minHitsApproved});

            requirements.add(hitsApprovedRequirement);
        }

        if (allowedLocales.length > 0 && useLocationRequirements && useRequirements) {
            QualificationRequirement localeRequirement = new QualificationRequirement();
            localeRequirement.setQualificationTypeId(RequesterService.LOCALE_QUALIFICATION_TYPE_ID);
            localeRequirement.setComparator(Comparator.In);

            Locale[] locales = new Locale[allowedLocales.length];
            for (int i = 0; i < allowedLocales.length; ++i) {
                Locale l = new Locale();
                l.setCountry(allowedLocales[i]);
                locales[i] = l;
            }

            localeRequirement.setLocaleValue(locales);
            requirements.add(localeRequirement);
        }

        QualificationRequirement[] qualificationRequirements = requirements.toArray(new QualificationRequirement[requirements.size()]);
        String hitTYPE = service.registerHITType(autoApprovalDelay, maxWorkingTime, reward, title, keywords, description, qualificationRequirements);

        int publishedId = experiment.publish(lifetime, service.getWebsiteURL() + "/mturk/preview?groupId=" + hitTYPE, df.get("destination"));

        String url = "";

        for (AbstractGroup p : experiment.getParts()) {
            url = p.publishOnAMT(service, publishedId, hitTYPE, lifetime, maxAssign);
        }
        return ok(views.html.publishing.publish.render(url));
    }

    /**
     * Renders the preview page based on the given Experiment. The preview page allows
     * the experimenter to enter available options such as callback url and generates
     * the experiment URL based on that info. (Unfortunately, we can't do more here as
     * Prolific does not provide an API yet.
     *
     * @param expId: The Id of the experiment that should be published
     * @return the preview page
     */
    @SubjectPresent
    public Result publishOnProlific(int expId) {
        LingoExpModel expModel = LingoExpModel.byId(expId);
        if (expModel == null) {
            return internalServerError("Experiment ID does not exist!");
        }

        return ok(views.html.publishing.publishOnProlific.render(expModel, lingoturkConfig.getStaticIp()));
    }

    /**
     * Stores the info about the experiment, represented by {code expId}, that should be published
     * Depending on the type, that info will be used for scheduling.
     *
     * @return Returns an {@code OK}, if everything works out, an {@code internalServerError} otherwise.
     */
    @SubjectPresent
    @BodyParser.Of(BodyParser.Json.class)
    public Result publishProlific() {
        JsonNode json = request().body().asJson();
        int expId = json.get("expId").asInt();
        LingoExpModel expModel = LingoExpModel.byId(expId);

        if (expModel == null) {
            return internalServerError("LingoExpModel does not exist.");
        }

        int lifetime = json.get("lifetime").asInt();
        switch (json.get("type").asText()) {
            case "DISJOINT LISTS":
                // convert from minutes to ms
                int maxWorkingTime = json.get("maxWorkingTime").asInt() * 1000 * 60;
                int defaultValue = json.get("defaultValue").asInt();
                boolean useAdvancedMode = json.get("useAdvancedMode").asBoolean();

                for (Iterator<Map.Entry<String, JsonNode>> updateIt = json.get("updates").fields(); updateIt.hasNext(); ) {
                    Map.Entry<String, JsonNode> update = updateIt.next();

                    try {
                        int partId = Integer.parseInt(update.getKey());
                        AbstractGroup group = AbstractGroup.byId(partId);
                        boolean disabled;
                        int maxParticipants;
                        if (useAdvancedMode) {
                            maxParticipants = update.getValue().get("maxParticipants").asInt();
                        } else {
                            maxParticipants = defaultValue;
                        }
                        int availability = maxParticipants - group.countParticipants();
                        disabled = availability <= 0;

                        PreparedStatement ps = databaseService.getConnection().prepareStatement("UPDATE Groups SET disabled = ?, maxParticipants = ?, availability = ?, maxWorkingTime = ? WHERE partId = ?");
                        System.out.println("[info] play - Update Group " + partId + ": Setting disabled to '" + disabled + "' and maxParticipants to '" + maxParticipants + "'");

                        ps.setBoolean(1, disabled);
                        ps.setInt(2, maxParticipants);
                        ps.setInt(3, availability);
                        ps.setInt(4, maxWorkingTime);
                        ps.setInt(5, partId);
                        ps.execute();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        return internalServerError("Can't update Groups: " + e.getMessage());
                    }
                }
                break;
            case "MULTIPLE LISTS":
                try {
                    for (AbstractGroup g : expModel.getParts()) {
                        PreparedStatement ps = databaseService.getConnection().prepareStatement("UPDATE Groups SET disabled = FALSE WHERE partId = ?");
                        ps.setInt(1, g.getId());
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
            expModel.publish(lifetime, null, "PROLIFIC");
        } catch (SQLException e) {
            e.printStackTrace();
            return internalServerError("Could not store publish to DB.");
        }
        System.out.println("[info] play - Published Experiment " + expId + " successfully to Prolific.");

        return ok();
    }
}
