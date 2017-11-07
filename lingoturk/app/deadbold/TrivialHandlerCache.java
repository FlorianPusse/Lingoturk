package deadbold;

import be.objectify.deadbolt.java.DeadboltHandler;
import be.objectify.deadbolt.java.cache.HandlerCache;
import com.google.inject.Singleton;

/**
 * A trivial HandlerCache implementation that uses only the CustomDeadboltHandler
 */
@Singleton
public class TrivialHandlerCache implements HandlerCache
{
    private final DeadboltHandler defaultHandler = new CustomDeadboltHandler();

    @Override
    public DeadboltHandler apply(final String key)
    {
        return defaultHandler;
    }

    @Override
    public DeadboltHandler get()
    {
        return defaultHandler;
    }
}
