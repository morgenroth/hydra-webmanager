package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebManager implements EntryPoint {

    public static final Logger logger = Logger.getLogger(WebManager.class.getName());
    
//  private MyBeanFactory beanFactory = GWT.create(MyBeanFactory.class);
    
//  private AtmosphereRequest jsonRequest;
    
  /**
   * This is the entry point method.
   */
  public void onModuleLoad() {
      GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {
          @Override
          public void onUncaughtException(Throwable e) {
              logger.log(Level.SEVERE, "Uncaught exception", e);
          }
      });
      
//      Atmosphere atmosphere = Atmosphere.create();
      
//    AutoBeanClientSerializer json_serializer = new AutoBeanClientSerializer();
//    json_serializer.registerBeanFactory(beanFactory, Event.class);
//                   
//    // setup JSON Atmosphere connection
//    AtmosphereRequestConfig jsonRequestConfig = AtmosphereRequestConfig.create(json_serializer);
//    jsonRequestConfig.setUrl(GWT.getModuleBaseURL() + "atmosphere/json");
//    jsonRequestConfig.setContentType("application/json; charset=UTF-8");
//    jsonRequestConfig.setTransport(AtmosphereRequestConfig.Transport.STREAMING);
//    jsonRequestConfig.setFallbackTransport(AtmosphereRequestConfig.Transport.LONG_POLLING);
//    jsonRequestConfig.setOpenHandler(new AtmosphereOpenHandler() {
//        @Override
//        public void onOpen(AtmosphereResponse response) {
//            WebManager.logger.info("JSON Connection opened");
//        }
//    });
//    jsonRequestConfig.setCloseHandler(new AtmosphereCloseHandler() {
//        @Override
//        public void onClose(AtmosphereResponse response) {
//            WebManager.logger.info("JSON Connection closed");
//        }
//    });
//    jsonRequestConfig.setMessageHandler(new AtmosphereMessageHandler() {
//        @Override
//        public void onMessage(AtmosphereResponse response) {
//            List<Event> events = response.getMessages();
//            for (Event event : events) {
//                WebManager.logger.info("received message through JSON: " + event.getData());
//                Window.alert(event.getData());
//            }
//        }
//    });
//    
//    jsonRequest = atmosphere.subscribe(jsonRequestConfig);
      
      Widget w = new HydraView();
      RootPanel.get().add(w);
  }
}
