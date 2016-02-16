package controllers;

import play.api.libs.Crypto;
import play.data.DynamicForm;
import play.mvc.*;
import play.mvc.Http.*;
import views.html.login;

import java.io.IOException;
import java.util.Properties;

import static play.mvc.Controller.session;

public class Secured extends Security.Authenticator{

    @Override
    public String getUsername(Context ctx) {
        String cookieValue = ctx.session().get(Application.PASSWORD);
        return validate(cookieValue) ? "okay" : null;
    }

    @Override
    public Result onUnauthorized(Context ctx) {
        return redirect(routes.Secured.login());
    }

    public static boolean validate(String cookieValue) {
        if (cookieValue == null) {
            return false;
        }

        Properties props = new java.util.Properties();
        try {
            props.load(new java.io.FileInputStream(new java.io.File("conf/mturk.properties")));
        } catch (IOException e) {
            System.err.println("There was a problem reading your properties file from " + "mturk.properties");
            System.err.println("The exception was " + e.toString());
            throw new RuntimeException("Cannot load configuration properties file from " + "mturk.properties", e);
        }
        String store = props.getProperty(Application.PASSWORD);
        if (store.equals(cookieValue)) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * Renders the login-view
     *
     * @return the rendered page
     */
    public static Result login() {
        return ok(login.render());
    }

    public static Result authenticate() {
        DynamicForm df = new DynamicForm().bindFromRequest();
        session().clear();
        session(Application.PASSWORD, Crypto.encryptAES(df.get("password")));
        return redirect(routes.Application.index());
    }

}
