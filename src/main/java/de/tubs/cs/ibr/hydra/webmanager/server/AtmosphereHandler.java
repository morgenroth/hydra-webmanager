
package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceFactory;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.cpr.Serializer;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;

import de.tubs.cs.ibr.hydra.webmanager.shared.Event;

public class AtmosphereHandler extends AbstractReflectorAtmosphereHandler {
    static final Logger logger = Logger.getLogger("AtmosphereHandler");

    @Override
    public void onRequest(AtmosphereResource r) throws IOException {
        AtmosphereRequest req = r.getRequest();
        
        if (req.getMethod().equals("GET")) {
            doGet(r);
        } else if (req.getMethod().equals("POST")) {
            doPost(r);
        }
    }

    private void doGet(final AtmosphereResource res) {
        // set encoding for the response
        final String encoding = res.getRequest().getCharacterEncoding();
        res.getResponse().setCharacterEncoding(encoding);
        res.getResponse().setContentType("application/json");

        // subscribe to global /events
        res.setBroadcaster( DefaultBroadcasterFactory.getDefault().lookup("/events", true) );

        // add message serializer
        res.setSerializer(new Serializer() {
            Charset charset = Charset.forName(encoding);

            public void write(OutputStream os, Object o) throws IOException {
                // only process Event messages
                if (o instanceof Event) {
                    AutoBean<Event> bean = AutoBeanUtils.getAutoBean((Event)o);
                    
                    String payload = AutoBeanCodex.encode(bean).getPayload();
                    os.write(payload.getBytes(charset));
                    os.flush();
                }
            }
        });
        
        logger.info("atmosphere registered: " + res.uuid());

        // suspend as long as there are no messages
        res.suspend();
    }

    private void doPost(AtmosphereResource res) throws IOException {
        StringBuilder data = new StringBuilder();
        BufferedReader requestReader;

        requestReader = res.getRequest().getReader();
        char[] buf = new char[5120];
        int read = -1;
        while ((read = requestReader.read(buf)) > 0) {
            data.append(buf, 0, read);
        }

        String message = data.toString();
        
        if (message.startsWith("subscribe ")) {
            String[] msg = message.split(" ");
            
            String atmosphereId = msg[1];
            Long sessionId = Long.valueOf(msg[2]);
            
            AtmosphereResource r = AtmosphereResourceFactory.getDefault().find(atmosphereId);
            Broadcaster b = DefaultBroadcasterFactory.getDefault().lookup("/session/" + sessionId, true);
            
            if ((b != null) && (r != null)) {
                b.addAtmosphereResource(r);
                logger.info("atmosphere " + r.uuid() + " subscribed to " + sessionId);
            }
        } else if (message.startsWith("unsubscribe ")) {
            String[] msg = message.split(" ");
            
            String atmosphereId = msg[1];
            Long sessionId = Long.valueOf(msg[2]);
            
            AtmosphereResource r = AtmosphereResourceFactory.getDefault().find(atmosphereId);
            Broadcaster b = DefaultBroadcasterFactory.getDefault().lookup("/session/" + sessionId);
            
            if ((b != null) && (r != null)) {
                b.removeAtmosphereResource(r);
                logger.info("atmosphere " + r.uuid() + " un-subscribed from " + sessionId);
            }
        }
    }
}
