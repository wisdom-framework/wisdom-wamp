package org.wisdom.wamp.sample;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.ipojo.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wisdom.api.DefaultController;
import org.wisdom.wamp.services.ExportedService;
import org.wisdom.wamp.services.RegistryException;
import org.wisdom.wamp.services.Wamp;

@Component
@Provides
@Instantiate
public class WampController extends DefaultController implements EventHandler {

    @Requires
    private Wamp wamp;

    @Requires
    private EventAdmin ea;

    @ServiceProperty(name = EventConstants.EVENT_TOPIC)
    private String[] topics = new String[]{"simple"};

    private ExportedService ref;

    private final static Logger LOGGER = LoggerFactory.getLogger(WampController.class);

    @Validate
    public void start() throws RegistryException {
        LOGGER.debug("Published service: " + wamp.getServices());
        ref = wamp.register(new Calc(), "/calc");
        // Publishing an event.
        ea.postEvent(new Event("wamp/data", ImmutableMap.of("message", "hello")));
    }

    @Invalidate
    public void stop() {
        if (ref != null) {
            wamp.unregister(ref);
        }
    }

    /**
     * Called by the {@link org.osgi.service.event.EventAdmin} service to notify the listener of an
     * event.
     *
     * @param event The event that occurred.
     */
    @Override
    public void handleEvent(Event event) {
        LOGGER.info("Receiving message from {} with {}", event.getTopic(), event.getProperty(Wamp.WAMP_EVENT_PROPERTY));
    }
}

