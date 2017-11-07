package deadbold;

import be.objectify.deadbolt.java.DeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;
import be.objectify.deadbolt.java.models.Subject;
import models.User;
import play.mvc.Http;
import play.mvc.Result;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.redirect;

/**
 * Implements the DeadboltHandler in order to provide authorization to our application
 */
public class CustomDeadboltHandler implements DeadboltHandler {

    /**
     * For now we don't want to do anything before the authorization happens
     */
    @Override
    public CompletionStage<Optional<Result>> beforeAuthCheck(Http.Context context) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Loads the current user from the {@code context}
     * @param context The current requests context
     * @return The current user, if available. Empty {@code Optional} otherwise
     */
    @Override
    public CompletionStage<Optional<? extends Subject>> getSubject(Http.Context context) {
        Http.Session session = context.session();
        String username = session.get("username");
        String token = session.get("token");

        if(username == null || token == null){
            session.put("reason","nologin");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // Check if user exists. If not, redirect to login page
        User u = User.byUsername(username);
        if(u == null){
            session.put("reason","nologin");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        // Token is not valid. Remove them from session and force user to refresh login
        if(!u.verifyToken(token)){
            session.remove("username");
            session.remove("token");
            session.put("reason","nologin");
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.completedFuture(Optional.of(u));
    }

    /**
     * If authorization fails, we either redirect the user to the login page (in case that was the problem)
     * or we return an error message.
     * @param context The current requests context
     * @param content The content type hint that can be used to determine what format the result should have
     * @return Either an error message, or a redirect to the login page (if no user is logged in)
     */
    @Override
    public CompletionStage<Result> onAuthFailure(Http.Context context, Optional<String> content) {
        Http.Session session = context.session();
        String reason = session.get("reason");
        session.remove("reason");
        if(reason != null && reason.equals("nologin")){
            return CompletableFuture.completedFuture(redirect(controllers.routes.LoginController.login()));
        }
        return CompletableFuture.completedFuture(internalServerError("Authorization error"));
    }

    @Override
    public CompletionStage<Optional<DynamicResourceHandler>> getDynamicResourceHandler(Http.Context context) {
        return null;
    }
}
