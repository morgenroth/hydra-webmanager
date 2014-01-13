package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitCompleteHandler;
import com.google.gwt.user.client.ui.FormPanel.SubmitEvent;
import com.google.gwt.user.client.ui.FormPanel.SubmitHandler;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class LoginPopup extends PopupPanel
{
    
    FormPanel form = new FormPanel();
    final LoginPopup p = this;
    TextBox userBox = null;
    TextBox pwBox = null;
    
    public LoginPopup()
    {
        super(false);
        

        initForm();
        
        this.setWidget(form);
    }
    
    private void initForm()
    {
        //form.setAction("/myFormHandler");
        //form.setEncoding(FormPanel.ENCODING_MULTIPART);
        //form.setMethod(FormPanel.METHOD_POST);

        VerticalPanel content = new VerticalPanel();
        
        Button cancelButton = new Button("Cancel", new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                p.hide();
                                } });
        Button loginButton = new Button("Login", new ClickHandler() {
                            public void onClick(ClickEvent event) {
                                //form.submit();
                                if(checkPassword(userBox.getText(), pwBox.getText()))
                                    p.hide();
                                
                            } });
        
        userBox = new TextBox();
        userBox.setFocus(false);
        pwBox = new PasswordTextBox();
        content.add(userBox);
        content.add(pwBox);
        content.add(cancelButton);
        content.add(loginButton);
        
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
        
        if(authenticated)
            Window.alert("YAY");
        else
            Window.alert("NOPE");
            
        return authenticated;
    }
}
