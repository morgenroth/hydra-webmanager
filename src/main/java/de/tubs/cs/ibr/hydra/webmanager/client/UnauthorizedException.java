package de.tubs.cs.ibr.hydra.webmanager.client;

public class UnauthorizedException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 3377374715626403974L;
    
    public UnauthorizedException()
    {
        super("user is not authorized!");
    }

}
