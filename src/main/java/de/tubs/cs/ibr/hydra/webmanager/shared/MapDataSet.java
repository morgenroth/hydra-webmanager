package de.tubs.cs.ibr.hydra.webmanager.shared;

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.IsSerializable;

public class MapDataSet implements IsSerializable {
    public ArrayList<Node> nodes;
    public ArrayList<Link> links;
}
