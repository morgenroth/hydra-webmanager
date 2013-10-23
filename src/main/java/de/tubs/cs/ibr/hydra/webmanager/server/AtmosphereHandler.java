
package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.cpr.Serializer;
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanUtils;

import de.tubs.cs.ibr.hydra.webmanager.shared.EventData;

public class AtmosphereHandler extends AbstractReflectorAtmosphereHandler {
    static final Logger logger = Logger.getLogger("AtmosphereHandler");

    @Override
    public void onRequest(AtmosphereResource r) throws IOException {
        AtmosphereRequest req = r.getRequest();
        
        if (req.getMethod().equals("GET")) {
            doGet(r);
        } else if (req.getMethod().equals("POST")) {
            // ignore posts
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
                if (o instanceof EventData) {
                    AutoBean<EventData> bean = AutoBeanUtils.getAutoBean((EventData)o);
                    
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
}
