package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.PasswordTextBox;

public class DefaultPasswordBox extends PasswordTextBox implements FocusHandler{ 
        private String defaultText; 

        public DefaultPasswordBox(String defText) { 
                defaultText = defText; 
                setText(defaultText); 
                addFocusHandler(this); 
        } 

        public void setDefaultText(String defText) { 
                defaultText = defText; 
        } 

        public String getDefaultText() { 
                return defaultText; 
        } 

        @Override
        public void onFocus(FocusEvent event) {
           setText(""); 
        }

}
