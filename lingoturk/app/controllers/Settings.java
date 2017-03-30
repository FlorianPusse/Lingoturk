package controllers;

import com.amazonaws.mturk.util.PropertiesClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import play.api.libs.Crypto;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import java.io.IOException;
import java.util.Properties;

public class Settings extends Controller {

    /**
     * Renders the settings page, which allows the user to change Settings like keys or ip.
     *
     * @return the rendered page
     */
    @Security.Authenticated(Secured.class)
    public static Result settings() {
        PropertiesClientConfig pcc = new PropertiesClientConfig("conf/mturk.properties");
        return ok(views.html.Settings.settings.render(pcc.getAccessKeyId(), pcc.getSecretAccessKey(), Application.getStaticIp()));
    }

    /**
     * Saves the changes, which were made by the User
     *
     * @return the error/success-page
     */
    @Security.Authenticated(Secured.class)
    @BodyParser.Of(value = BodyParser.Json.class)
    public static Result globalSettingsChanged() {
        JsonNode json = request().body().asJson();
        String ipAddress = json.get("ipAddress").asText().trim();
        String accessKey = json.get("accessKey").asText().trim();
        String secretKey = json.get("secretKey").asText().trim();
        String oldPassword = json.get("oldPassword").asText().trim();
        String newPassword1 = json.get("newPassword1").asText().trim();
        String newPassword2 = json.get("newPassword2").asText().trim();

        Properties props_Sandbox = new Properties();
        Properties props_AMT = new Properties();

        if (!ipAddress.equals("")) {
            if(ipAddress.charAt(ipAddress.length() - 1) == '/'){
                ipAddress = ipAddress.substring(0,ipAddress.length() - 1);
            }
            Application.setStaticIp(ipAddress);
        }

        if ((!accessKey.equals("") && secretKey.equals("")) || accessKey.equals("") && !secretKey.equals("")) {
            return badRequest("AccessKey/SecretKey is empty while the other one is not!");
        }

        try {
            props_Sandbox.load(new java.io.FileInputStream(new java.io.File("conf/mturk.properties")));
            props_AMT.load(new java.io.FileInputStream(new java.io.File("conf/mturkToAMT.properties")));
        } catch (IOException e) {
            return internalServerError("There was a problem reading your properties file from mturk.properties/mturkToAMT.properties");
        }

        if (!accessKey.equals("") && !secretKey.equals("")) {
            props_Sandbox.setProperty(PropertiesClientConfig.ACCESS_KEY_ID, accessKey);
            props_Sandbox.setProperty(PropertiesClientConfig.SECRET_ACCESS_KEY, secretKey);

            props_AMT.setProperty(PropertiesClientConfig.ACCESS_KEY_ID, accessKey);
            props_AMT.setProperty(PropertiesClientConfig.SECRET_ACCESS_KEY, secretKey);
        }

        if ((!oldPassword.equals("") && (newPassword1.equals("") || newPassword2.equals(""))) ||
                (!newPassword1.equals("") && (oldPassword.equals("") || newPassword2.equals(""))) ||
                (!newPassword2.equals("") && (newPassword1.equals("") || oldPassword.equals("")))
                ) {
            return badRequest("Old password/New password is empty while the other ones are not!");
        }

        if (!oldPassword.equals("") && !newPassword1.equals("") && !newPassword2.equals("")) {
            if (!newPassword1.equals(newPassword2)) {
                return badRequest("New passwords don't match!");
            }

            String store = props_Sandbox.getProperty(Application.PASSWORD, Crypto.encryptAES("admin"));
            if (store.equals(Crypto.encryptAES(oldPassword))) {
                String newStore = Crypto.encryptAES(newPassword1);
                props_Sandbox.setProperty(Application.PASSWORD, newStore);
                session(Application.PASSWORD, newStore);
            } else {
                return badRequest("Old password is incorrect!");
            }
        }

        try {
            props_Sandbox.store(new java.io.FileOutputStream(new java.io.File("conf/mturk.properties")), "");
            props_AMT.store(new java.io.FileOutputStream(new java.io.File("conf/mturkToAMT.properties")), "");
        } catch (IOException e) {
            return internalServerError("There was a problem writing your properties file back to mturk.properties/mturkToAMT.properties");
        }

        return ok();
    }
}
