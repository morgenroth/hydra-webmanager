package de.tubs.cs.ibr.hydra.webmanager.shared;

import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;

public interface EventDataFactory extends AutoBeanFactory {
    AutoBean<EventData> event();
    AutoBean<EventDataExtra> extra();
}
