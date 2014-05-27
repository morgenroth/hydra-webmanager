package de.tubs.cs.ibr.hydra.webmanager.client;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public class LDAP {
    
    static boolean authenticate(String username, String password)
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
        
        return authenticated;
    }

}
