package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LoginPopup extends PopupPanel
{
    
    FormPanel form = new FormPanel();
    final LoginPopup p = this;
    public LoginPopup()
    {
        super(false);
        

        initForm();
        
        this.setWidget(form);
    }
    
    private void initForm()
    {
        form.setAction("/myFormHandler");

        VerticalPanel content = new VerticalPanel();
        
        Button cancelButton = new Button("Cancel", new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                p.hide();
                                } });
        Button loginButton = new Button("Login", new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                form.submit();
                            } });
        
        TextBox userBox = new TextBox();
        userBox.setFocus(false);
        PasswordTextBox pwBox = new PasswordTextBox();
        content.add(userBox);
        content.add(pwBox);
        content.add(cancelButton);
        content.add(loginButton);

        form.setWidget(content);
    }
}