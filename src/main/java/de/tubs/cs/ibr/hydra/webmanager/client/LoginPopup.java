package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.Date;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import de.tubs.cs.ibr.hydra.webmanager.shared.Credentials;
public class LoginPopup extends PopupPanel
{
    
    FormPanel mForm = new FormPanel();
    final LoginPopup mPopup = this;
    DefaultTextBox mUserBox ;
    DefaultPasswordBox mPWBox;
    Label mNoteText;
    AsyncCallback<Credentials> mCallback;
    
    
    public LoginPopup(AsyncCallback<Credentials> callback)
    {
        super(false);
        
        mCallback = callback;
        initForm();
        
        this.setWidget(mForm);
    }
    
    private void initForm()
    {
        VerticalPanel content = new VerticalPanel();
        HorizontalPanel buttons = new HorizontalPanel();
        mNoteText = new Label();
        mNoteText.setVisible(false);
        
        Button cancelButton = new Button("Cancel", new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                mPopup.hide();
                                } });
        cancelButton.setType(ButtonType.DANGER);
        Button loginButton = new Button("Login", new ClickHandler() {
                            public void onClick(ClickEvent event) {

                                MasterControlServiceAsync mcs = (MasterControlServiceAsync)GWT.create(MasterControlService.class);
                                mcs.login(mUserBox.getText(), mPWBox.getText(), new AsyncCallback<Credentials>() {
                                    
                                    @Override
                                    public void onSuccess(Credentials creds) {
                                        if ( creds != null)
                                        {
                                            mPopup.hide();
                                            mCallback.onSuccess(creds);
                                            Date expires = new Date(creds.getSessionExpires());
                                            Cookies.setCookie("hydra_sid", creds.getSessionId(), expires, null, "/", false);
                                        }
                                        else
                                        {
                                            mNoteText.setVisible(true);
                                            mNoteText.setText("wrong username/password!");
                                        }
                                    }
                                    @Override
                                    public void onFailure(Throwable caught) {
                                        mNoteText.setVisible(true);
                                        mNoteText.setText("ERROR: " + caught.getMessage());
                                    }

                                });
                                

                                
                            } });
        loginButton.setType(ButtonType.SUCCESS);
        
        buttons.add(loginButton);
        buttons.add(cancelButton);

        mUserBox = new DefaultTextBox("Username");
        mUserBox.setFocus(true);
        mUserBox.setText("Username");

        mPWBox = new DefaultPasswordBox("password");
        mPWBox.setText("Password");
        mPWBox.addKeyPressHandler(new KeyPressHandler() {
            @Override
            public void onKeyPress(KeyPressEvent event) {
               if ((int)event.getCharCode() == 13 ) { //submit form on enter
                   mForm.submit();
               }
            }
        });

        content.add(mUserBox);
        content.add(mPWBox);
        content.add(buttons);
        content.add(mNoteText);
        
        mForm.setWidget(content);
    }
    

}
