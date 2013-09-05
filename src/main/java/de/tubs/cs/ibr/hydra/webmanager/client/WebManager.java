package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.atmosphere.gwt20.client.Atmosphere;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class WebManager implements EntryPoint {

    public static final Logger logger = Logger.getLogger(WebManager.class.getName());
    
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
      
      Atmosphere atmosphere = Atmosphere.create();
      
      Widget w = new SessionView(atmosphere);
      RootLayoutPanel.get().add(w);
  }
}
