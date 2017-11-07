package controllers;

import be.objectify.deadbolt.java.actions.SubjectPresent;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.about;
import views.html.index;

/***
 * Controller handling standart application requests such as the index page or the about page
 */
public class ApplicationController extends Controller {

    /**
     * Renders the index-page.
     *
     * @return Result object (index-page)
     */

    @SubjectPresent
    public Result index() {
        return ok(index.render());
    }

    /**
     * Renders the about information page
     *
     * @return Result object containing the page
     */
    @SubjectPresent
    public Result about() {
        return ok(about.render());
    }
}
