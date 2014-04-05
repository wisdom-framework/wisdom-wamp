# Wisdom-Wamp

Wisdom-Wamp is an extension to Wisdom-Framework to expose services using the WAMP v1 protocol. It supports:

* RPC invocation (client to server)
* Event notification (client to server and server to client)

Notice that WAMP(http://wamp.ws/) is based is a Web Socket sub-protocol. The WAMP v1 specification is available here
(http://wamp.ws/spec/wamp1/).

It uses the OSGi Event Admin to deal with WAMP events.


## Installation

Add the following dependency to your `pom.xml` file:

````
<dependency>
    <groupId>org.wisdom-framework</groupId>
    <artifactId>wisdom-wamp</artifactId>
    <version>${project.version}</version>
</dependency>
````

*Or* copy the jar file to the Wisdom's `Application` directory, as well an implementation of the OSGi Event Admin
such as [Apache Felix Event Admin](http://felix.apache.org/site/apache-felix-event-admin.html) (downloadable from the
 [Apache Felix Download Page](http://felix.apache.org/downloads.cgi).


We also recommend the use of the Autobahn JavaScript library to interact using the WAMP protocol from JavaScript.
Autobahn is available within a WebJar:

```
<dependency>
    <groupId>org.webjars</groupId>
    <artifactId>autobahnjs</artifactId>
    <version>0.8.2</version>
</dependency>
```

**NOTE**: the 0.8.2 is the last version of autobahn implementing WAMP v1.

## Configuration

The only thing to do is to allow the Wamp subprotocol. In the `src/main/configuration/application.conf` file, add:

````
# Websocket subprotocols
wisdom.websocket.subprotocols = wamp
````

### Publishing a service using WAMP

The WAMP support provides a `org.wisdom.wamp.services.Wamp` service that let you register and unregister object
published using WAMP. In the following example, an instance of 'Calc' is registered.

````
package org.wisdom.wamp.sample;

import org.apache.felix.ipojo.annotations.*;
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

    private ExportedService ref;

    private final static Logger LOGGER = LoggerFactory.getLogger(WampController.class);

    @Validate
    public void start() throws RegistryException {
        LOGGER.debug("Published service: " + wamp.getServices());
        ref = wamp.register(new Calc(), "/calc");
    }

    @Invalidate
    public void stop() {
        if (ref != null) {
            wamp.unregister(ref);
        }
    }
}
````

Registration is made using the `register` method, taking as parameter the object to expose and the url. Notice that
you must unregister your service. The registration gives you an `ExportService` that you have to use to unregister it.

### Sending and Receiving events

The WAMP events are bridged to the OSGi Event Admin. So if we extend the previous example to receive and send events,
 there result looks like:

````
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
````

The Event Admin service is retrieved using `@Requires EventAdmin ea;`. Sending event is done using the `EventAdmin
.postEvent()` method. Events must be send on topics under `/wamp`, as `wamp/data` in the example. This topic is
translated to WAMP by just prefixing it with the server name and HTTP port,
such as : `http://localhost:9000/wamp/data`.

Receiving events is also done using the Event Admin. You component must publish the `EventHandler` service with the
`topics` property selecting the listened topics. Unlike sending, the 'wamp' prefix is removed by the WAMP service.
Event sent on `http://localhost:9000/wamp/data` are received on `/data`.

### The JavaScript client

Here is an example of JavaScript client using autobahn.js:

````
<!DOCTYPE html>
<html>
<head>
    <title>Wamp Example</title>
    <link rel="stylesheet" href="socket.css">
    <script src="/libs/autobahn.js"></script>
    <script>
        // WAMP session object
        var sess;
        var wsuri = "ws://localhost:9000/wamp";

        window.onload = function () {

            // connect to WAMP server
            ab.connect(wsuri,

                    // WAMP session was established
                    function (session) {

                        sess = session;
                        appendTextArea("Connected to " + wsuri);

                        // subscribe to topic, providing an event handler
                        sess.subscribe("http://localhost:9000/wamp/simple", onEvent);
                    },

                    // WAMP session is gone
                    function (code, reason) {

                        sess = null;
                        appendTextArea("Connection lost (" + reason + ")");
                    }
            );
        };

        function onEvent(topic, event) {
            appendTextArea("received event from " + topic + " : " + JSON.stringify(event));
        }

        function publishEvent() {
            sess.publish("http://localhost:9000/wamp/simple", {a: "foo", b: "bar", c: 23});
        }

        function callProcedure() {
            // issue an RPC, returns promise object
            sess.call("http://localhost:9000/wamp/calc#add", 23, 7).then(
                    // RPC success callback
                    function (res) {
                        appendTextArea("got invocation result: " + res);
                    },

                    // RPC error callback
                    function (error, desc) {
                        appendTextArea("got invocation error: " + desc);
                    }
            );
        }

        function appendTextArea(newData) {
            var el = getTextAreaElement();
            el.value = el.value + '\n> ' + newData;
        }

        function getTextAreaElement() {
            return document.getElementById('responseText');
        }
    </script>
    <script src="/assets/jquery-2.0.3.min.js"></script>
    <link rel="stylesheet" href="/libs/css/bootstrap.min.css"/>
    <link rel="stylesheet" href="/libs/css/bootstrap-theme.min.css"/>
    <script src="/libs/jquery.js"></script>
    <script src="/libs/js/bootstrap.min.js"></script>
</head>
<body>
<div class="container">

    <div class="starter-template">
        <h1>AutobahnJS WAMP Client</h1>
        <button onclick="publishEvent();">Publish Event</button>
        <button onclick="callProcedure();">Call Procedure</button>
        <h2>Interaction</h2>
        <textarea id="responseText"></textarea>
    </div>
</div>
</body>
</html>
````

### Eligible and Exclusion lists

Event sender can configure the eligible and exclusion list by setting the two following properties in the sent event:

* `wamp.exclusions` : the list of excluded clients (`List<String>`)
* `wamp.eligible` : the list of eligible clients (`List<String>`)
