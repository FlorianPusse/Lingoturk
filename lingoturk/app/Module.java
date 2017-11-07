import be.objectify.deadbolt.java.cache.HandlerCache;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import deadbold.TrivialHandlerCache;
import services.*;

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 * <p>
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
public class Module extends AbstractModule {

    @Override
    public void configure() {
        bind(LingoturkConfig.class).asEagerSingleton();
        bind(DatabaseService.class).asEagerSingleton();
        bind(DatabaseModuleService.class).asEagerSingleton();
        bind(AppInitalizationService.class).asEagerSingleton();
        bind(ExperimentWatchService.class).asEagerSingleton();
        bind(MechanicalTurkService.class).asEagerSingleton();

        bind(HandlerCache.class).to(TrivialHandlerCache.class);
        bind(TrivialHandlerCache.class).in(Singleton.class);
    }

}
