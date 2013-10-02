
package de.tubs.cs.ibr.hydra.webmanager.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.logging.Logger;

import org.atmosphere.cpr.AtmosphereResource;
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
    public void onRequest(AtmosphereResource ar) throws IOException {
        if (ar.getRequest().getMethod().equals("GET")) {
            doGet(ar);
        } else if (ar.getRequest().getMethod().equals("POST")) {
            doPost(ar);
        }
    }

    public void doGet(final AtmosphereResource ar) {

        ar.getResponse().setCharacterEncoding(ar.getRequest().getCharacterEncoding());
        ar.getResponse().setContentType("application/json");

        // lookup the broadcaster, if not found create it. Name is arbitrary
        ar.setBroadcaster(DefaultBroadcasterFactory.getDefault().lookup("events", true));

        ar.setSerializer(new Serializer() {
            Charset charset = Charset.forName(ar.getResponse().getCharacterEncoding());

            public void write(OutputStream os, Object o) throws IOException {
                if (o instanceof Event) {
                    AutoBean<Event> bean = AutoBeanUtils.getAutoBean((Event)o);
                    
                    String payload = AutoBeanCodex.encode(bean).getPayload();
                    os.write(payload.getBytes(charset));
                    os.flush();
                }
                else if (o instanceof String) {
                    String payload = (String)o;
                    os.write(payload.getBytes(charset));
                    os.flush();
                }
                else {
                    String payload = o.toString();
                    os.write(payload.getBytes(charset));
                    os.flush();
                }
            }
        });

        ar.suspend();
    }

    public void doPost(AtmosphereResource ar) throws IOException {
        StringBuilder data = new StringBuilder();
        BufferedReader requestReader;

        requestReader = ar.getRequest().getReader();
        char[] buf = new char[5120];
        int read = -1;
        while ((read = requestReader.read(buf)) > 0) {
            data.append(buf, 0, read);
        }
        logger.info("Received json message from client: " + data.toString());

        String message = data.toString();
        DefaultBroadcasterFactory.getDefault().lookup("notify").broadcast(message);
    }

    @Override
    public void destroy() {

    }
}
