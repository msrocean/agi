package io.agi.ef.entities;

import io.agi.ef.Entity;
import io.agi.ef.http.node.Node;
import io.agi.ef.Persistence;
import io.agi.ef.http.servlets.DataEventServlet;
import io.agi.ef.http.servlets.EntityEventServlet;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This is a special Entity, part of the Coordinator service[s]. On receipt of events, it broadcasts them to all nodes.
 * This avoids the need for other Nodes to poll the database to discover changes.
 * This Entity should only be created on one Node. But that can be any Node.
 *
 * Created by dave on 12/09/15.
 */
public class Relay extends Entity {

    public static final String ENTITY_TYPE = "relay";
    public static final String ENTITY_NAME = "relay"; // only ever allow one

    public void setup() {
        super.setup( ENTITY_NAME, ENTITY_TYPE, null, null );
    }

    public void onDataEvent( String data, String action ) {
        HashMap< String, String > parameters = new HashMap<>();

        parameters.put( DataEventServlet.PARAMETER_NAME, data );
        parameters.put( DataEventServlet.PARAMETER_ACTION, action );

        // 2. Query database for all the nodes.
        HashSet< String > nodes = Persistence.getNodes();

        // 3. Broadcast event to each node.
        Node n = Node.getInstance();

        for( String nodeName : nodes ) {

            if ( n.isSelf( nodeName ) ) {
                continue; // prevent endless loop
            }

            n.postDataEvent(nodeName, data, action);
        }
    }

    public void onEvent( String entity, String action ) {

        HashMap< String, String > parameters = new HashMap<>();

        parameters.put( EntityEventServlet.PARAMETER_NAME, entity );
        parameters.put( EntityEventServlet.PARAMETER_ACTION, action );

        // 2. Query database for all the nodes.
        HashSet< String > nodes = Persistence.getNodes();

        // 3. Broadcast event to each node.
        Node n = Node.getInstance();

        for( String nodeName : nodes ) {

            if ( n.isSelf( nodeName ) ) {
                continue; // prevent endless loop
            }

            n.postEntityEvent(nodeName, entity, action);
        }
    }

}