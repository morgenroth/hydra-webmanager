package de.tubs.cs.ibr.hydra.webmanager.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LoginPopup extends PopupPanel
{
    
    FormPanel form = new FormPanel();
    final LoginPopup p = this;
    DefaultTextBox userBox = null;
    DefaultPasswordBox pwBox = null;
    Label noteText = null;
    
    public LoginPopup()
    {
        super(false);
        
        initForm();
        
        this.setWidget(form);
    }
    
    private void initForm()
    {
        VerticalPanel content = new VerticalPanel();
        HorizontalPanel buttons = new HorizontalPanel();
        noteText = new Label();
        noteText.setVisible(false);
        
        Button cancelButton = new Button("Cancel", new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                p.hide();
                                } });
        cancelButton.setType(ButtonType.DANGER);
        Button loginButton = new Button("Login", new ClickHandler() {
                            public void onClick(ClickEvent event) {

                                MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
                                mcs.login(userBox.getText(), pwBox.getText(), new AsyncCallback<Boolean>() {
                                    
                                    @Override
                                    public void onSuccess(Boolean result) {
                                        if ( result )
                                        {
                                            p.hide();
                                        }
                                        else
                                        {
                                            noteText.setVisible(true);
                                            noteText.setText("wrong username/password!");
                                        }
                                    }
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        noteText.setVisible(true);
                                        noteText.setText("ERROR: " + caught.getMessage());
                                    }

                                });
                                

                                
                            } });
        loginButton.setType(ButtonType.SUCCESS);
        
        buttons.add(loginButton);
        buttons.add(cancelButton);

        userBox = new DefaultTextBox("Username");
        userBox.setFocus(true);
        userBox.setText("Username");

        pwBox = new DefaultPasswordBox("password");
        pwBox.setText("Password");
        pwBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
               if ((int)event.getCharCode() == 13 ) { //submit form on enter
                   form.submit();
               }
            }
        });

        content.add(userBox);
        content.add(pwBox);
        content.add(buttons);
        content.add(noteText);
        
        form.setWidget(content);
    }
    

}
