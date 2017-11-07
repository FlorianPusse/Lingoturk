package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import services.LingoturkConfig;

import javax.inject.Inject;
import java.io.File;

/***
 * Controller handling handling requests for dynamic assets. These assets can be loaded dynamically when not
 * available via Assets.at (e.g. in case they were created while running in production mode.
 */
public class AssetController extends Controller {

    private final LingoturkConfig lingoturkConfig;

    @Inject
    public AssetController(final LingoturkConfig lingoturkConfig) {
        this.lingoturkConfig = lingoturkConfig;
    }

    /**
     * Loads the asset specified by the path and returns it encapsulated in a result object. If no such
     * asset exists, a 404 not found result is returned instead.
     *
     * @param path The path to the asset
     * @return Returns the result encapsulating the asset, or a 404 not found result, if no asset exists
     */
    public Result at(String path) {
        File f = new File(lingoturkConfig.getPathPrefix() + "public/" + path);
        if (f.exists()) {
            return ok(f);
        }

        return notFound();
    }
}
