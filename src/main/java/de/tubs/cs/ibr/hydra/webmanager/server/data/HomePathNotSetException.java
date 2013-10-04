package de.tubs.cs.ibr.hydra.webmanager.server.data;

import java.io.IOException;

public class HomePathNotSetException extends IOException {

    /**
     * 
     */
    private static final long serialVersionUID = -148628321817885906L;

    public HomePathNotSetException() {
        super("Please set the HOME directory for Hydra using the JVM args -Dconfig.hydra=<hydra-path>.");
    }
}
