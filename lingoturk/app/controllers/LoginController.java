package controllers;

import models.User;
import play.data.DynamicForm;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.login;

import javax.inject.Inject;

/***
 * Controller handling handling requests related to the login process.
 */
public class LoginController extends Controller {

    private final FormFactory formFactory;

    @Inject
    public LoginController(final FormFactory formFactory) {
        this.formFactory = formFactory;
    }

    /**
     * Renders the login-page
     *
     * @return the rendered page
     */
    public Result login() {
        return ok(login.render());
    }

    /**
     * Tries to log in an user using the {@code username} and {@code password} stored in the
     * submitted form. Redirects the user if the user exists and the username/password combination
     * is valid. Otherwise an error is thrown.
     *
     * @return Redirects to the error page, if no error occurs. Else {@code badRequest} is returned.
     */
    public Result authenticate() {
        DynamicForm df = formFactory.form().bindFromRequest();
        String username = df.get("username");
        String password = df.get("password");

        if (username == null) {
            return badRequest("Username missing");
        }
        if (password == null) {
            return badRequest("Password missing");
        }

        User u = User.byUsername(username);
        if (u == null) {
            return badRequest("Unknown user/password combination");
        }

        String token = u.login(password);
        if (token == null) {
            return badRequest("Unknown user/password combination");
        }

        session("username", df.get("username"));
        session("token", token);

        return redirect(routes.ApplicationController.index());
    }

}
