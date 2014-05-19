package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.user.client.Window;
import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.Label;
import com.github.gwtbootstrap.client.ui.constants.ButtonType;
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
                                if(checkPassword(userBox.getText(), pwBox.getText()))
                                {
                                    p.hide();
                                }
                                
                            } });
        loginButton.setType(ButtonType.SUCCESS);
        
        buttons.add(cancelButton);
        buttons.add(loginButton);

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
    
    private boolean checkPassword(String username, String password)
    {
        DirContext ctx = null;
        String usergroup = "users";
        String ldap_path = "uid="+username+",ou="+usergroup+",dc=ibr,dc=cs,dc=tu-bs,dc=de";
        
        Hashtable env = new Hashtable();
        boolean authenticated = false;
        
        try {

            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldaps://ldap.ibr.cs.tu-bs.de");

            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            //env.put(Context.SECURITY_PRINCIPAL, "uid="+ userId +",ou=users,dc=ibr,dc=tu-bs,dc=de");
            env.put(Context.SECURITY_PRINCIPAL, ldap_path);
            env.put(Context.SECURITY_CREDENTIALS, password);

            System.out.println("before context");

            // NamingException -> NOT authenticated!
            ctx = new InitialDirContext(env);

            //The user is authenticated.
            authenticated = true;
            System.out.println("after context");

        } catch (NamingException e) {
            //e.printStackTrace();
            authenticated = false;
        }
        
        if(!authenticated)
        {
            noteText.setVisible(true);
            noteText.setText("wrong username/password!");
        }
            
        return authenticated;
    }
}
