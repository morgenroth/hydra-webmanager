package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Style.Float;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.TextBox;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/*
 * the form in this popup ist not implemented with gwt-bootstrap form, because
 * this ALWAYS brings the error message: "Module .. need to be (re)compiled"
 * sounds like this bug: https://github.com/gwtbootstrap/gwt-bootstrap/issues/118
 */
import de.tubs.cs.ibr.hydra.webmanager.shared.Credentials;
public class LoginPopup extends PopupPanel
{
    
    SimplePanel mForm = new SimplePanel();
    final LoginPopup mPopup = this;
    Label mUsernameLabel;
    TextBox mUserBox;
    Label mPasswordLabel;
    PasswordTextBox mPWBox;
    Label mNoteText;
    AsyncCallback<Credentials> mCallback;
    
    
    public LoginPopup(AsyncCallback<Credentials> callback)
    {
        super(false);
        
        mCallback = callback;
        initForm();
        
        this.setWidget(mForm);

        Scheduler.get().scheduleDeferred(new ScheduledCommand() {

            @Override
            public void execute() {
               mUserBox.setFocus(true);
            }
         });

    }
    
    private void initForm()
    {
        VerticalPanel content = new VerticalPanel();
        HorizontalPanel buttons = new HorizontalPanel();
        mNoteText = new Label();
        mUsernameLabel= new Label();
        mPasswordLabel= new Label();
        mNoteText.setVisible(false);
        
        Button loginButton = new Button("Login", new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                doLogin();
            }
        });
        
        Button cancelButton = new Button("Cancel", new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                doReset();
            }
        });
        
        loginButton.setType(ButtonType.SUCCESS);
        cancelButton.setType(ButtonType.DANGER);
        cancelButton.getElement().getStyle().setFloat(Float.RIGHT);
        
        buttons.add(loginButton);
        buttons.add(cancelButton);
        
        mUsernameLabel.setText("Username:");
        mUserBox = new TextBox();
        mUserBox.addKeyDownHandler(new EnterEscapePressHandler());

        mPasswordLabel.setText("Password:");
        mPWBox = new PasswordTextBox();
        mPWBox.addKeyDownHandler(new EnterEscapePressHandler());

        content.add(mUsernameLabel);
        content.add(mUserBox);
        content.add(mPasswordLabel);
        content.add(mPWBox);
        content.add(buttons);
        content.add(mNoteText);
        
        mForm.setWidget(content);
    }
    
    private void doLogin()
    {
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
    }
    
    private void doReset()
    {
        mUserBox.setText("");
        mPWBox.setText("");
        mNoteText.setVisible(false);
        mNoteText.setText("");
        mPopup.hide();
    }
    
    private class EnterEscapePressHandler implements KeyDownHandler
    {
        @Override
        public void onKeyDown(KeyDownEvent event) {
            switch ((int)event.getNativeKeyCode()) {
            case KeyCodes.KEY_ENTER:
                doLogin();
                break;
            case KeyCodes.KEY_ESCAPE:
                doReset();
                break;
            default:
                //do not display "wrong password" msg when user is typing again
                mNoteText.setVisible(false);
                break;
            }
        }
    }
}