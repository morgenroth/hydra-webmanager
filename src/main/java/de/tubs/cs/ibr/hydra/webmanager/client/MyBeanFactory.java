package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;

public interface MyBeanFactory extends AutoBeanFactory {
    AutoBean<Event> event(Event event);
}
