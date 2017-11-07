package controllers;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import com.amazonaws.mturk.util.PropertiesClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import models.User;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.LingoturkConfig;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/***
 * Controller handling handling requests related user settings.
 * TODO: This function still needs to be updated in order to handle multiple users
 */
public class SettingsController extends Controller {

    private final LingoturkConfig lingoturkConfig;

    @Inject
    public SettingsController(final LingoturkConfig lingoturkConfig) {
        this.lingoturkConfig = lingoturkConfig;
    }

    /**
     * Renders the settings page, which allows the user to change SettingsController like keys or ip.
     *
     * @return the rendered page
     */
    @SubjectPresent
    public Result settings() {
        PropertiesClientConfig pcc = new PropertiesClientConfig(lingoturkConfig.getPathPrefix() + "conf/mturk.properties");
        return ok(views.html.Settings.settings.render(pcc.getAccessKeyId(), pcc.getSecretAccessKey(), lingoturkConfig.getStaticIp()));
    }

    /**
     * Saves the changes, which were made by the User.
     * For now, they include the server IP, the Mechanical Turk keys and the password
     * TODO: Handle multiple users; Refactor into multiple functions
     *
     * @return the error/success-page
     */
    @SubjectPresent
    @BodyParser.Of(value = BodyParser.Json.class)
    public Result globalSettingsChanged() {
        Http.Session session = session();
        JsonNode json = request().body().asJson();
        String ipAddress = json.get("ipAddress").asText().trim();
        String accessKey = json.get("accessKey").asText().trim();
        String secretKey = json.get("secretKey").asText().trim();
        String oldPassword = json.get("oldPassword").asText().trim();
        String newPassword1 = json.get("newPassword1").asText().trim();
        String newPassword2 = json.get("newPassword2").asText().trim();

        Properties props_Sandbox = new Properties();
        Properties props_AMT = new Properties();

        try {

            if (!ipAddress.isEmpty()) {
                if (ipAddress.charAt(ipAddress.length() - 1) == '/') {
                    ipAddress = ipAddress.substring(0, ipAddress.length() - 1);
                }
                lingoturkConfig.setStaticIp(ipAddress);
            }

            if ((!accessKey.isEmpty() && secretKey.isEmpty()) || accessKey.isEmpty() && !secretKey.isEmpty()) {
                return badRequest("AccessKey/SecretKey is empty while the other one is not!");
            }

            try {
                props_Sandbox.load(new FileInputStream(new File(lingoturkConfig.getPathPrefix() + "conf/mturk.properties")));
                props_AMT.load(new FileInputStream(new File(lingoturkConfig.getPathPrefix() + "conf/mturkToAMT.properties")));
            } catch (IOException e) {
                return internalServerError("There was a problem reading your properties file from mturk.properties/mturkToAMT.properties");
            }

            if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
                props_Sandbox.setProperty(PropertiesClientConfig.ACCESS_KEY_ID, accessKey);
                props_Sandbox.setProperty(PropertiesClientConfig.SECRET_ACCESS_KEY, secretKey);

                props_AMT.setProperty(PropertiesClientConfig.ACCESS_KEY_ID, accessKey);
                props_AMT.setProperty(PropertiesClientConfig.SECRET_ACCESS_KEY, secretKey);
            }

            if ((!oldPassword.isEmpty() && (newPassword1.isEmpty() || newPassword2.isEmpty())) ||
                    (!newPassword1.isEmpty() && (oldPassword.isEmpty() || newPassword2.isEmpty())) ||
                    (!newPassword2.isEmpty() && (newPassword1.isEmpty() || oldPassword.isEmpty()))
                    ) {
                return badRequest("Old password/New password is empty while the other ones are not!");
            }

            if (!oldPassword.isEmpty() && !newPassword1.isEmpty() && !newPassword2.isEmpty()) {
                if (!newPassword1.equals(newPassword2)) {
                    return badRequest("New passwords don't match!");
                }

                String username = session.get("username");
                if (username == null) {
                    return badRequest("No login");
                }

                User u = User.byUsername(session.get("username"));
                if (u == null) {
                    return badRequest("Unauthorized user.");
                }

                if (!u.updatePassword(oldPassword, newPassword1)) {
                    return badRequest("Old password is incorrect!");
                }
            }

            props_Sandbox.store(new FileOutputStream(new File(lingoturkConfig.getPathPrefix() + "conf/mturk.properties")), "");
            props_AMT.store(new FileOutputStream(new File(lingoturkConfig.getPathPrefix() + "conf/mturkToAMT.properties")), "");
        } catch (IOException e) {
            return internalServerError("There was a problem writing your properties file back to mturk.properties/mturkToAMT.properties");
        }

        return ok();
    }
}
