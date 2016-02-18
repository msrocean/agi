package io.agi.ef;

import io.agi.ef.serialization.JsonEntity;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by dave on 14/02/16.
 */
public class Node {

    protected String _name;
    protected String _host;
    protected int _port;
    protected EntityFactory _ef;
    protected Coordination _c;
    protected Persistence _p;

    protected HashMap< String, ArrayList< EntityListener > > _entityListeners = new HashMap< String, ArrayList< EntityListener >>();

    public Node() {

    }

    /**
     * Sets up the interfaces, which cannot me modified afterwards.
     *
     * @param c
     * @param p
     */
    public void setup(
        String name,
        String host,
        int port,
        EntityFactory ef,
        Coordination c,
        Persistence p ) {

        _name = name;
        _host = host;
        _port = port;

        _ef = ef;
        _c = c;
        _p = p;
    }

    public String getName() {
        return _name;
    }
    public String getHost() {
        return _host;
    }
    public int getPort() {
        return _port;
    }

    /**
     * Returns the persistence layer
     * @return
     */
    public Persistence getPersistence() {
        return _p;
    }

    /**
     * Returns the coordination layer
     * @return
     */
    public Coordination getCoordination() {
        return _c;
    }

    /**
     * A callback that is called when an Entity has been updated, including all its children.
     * @param entityName
     */
    public void isUpdated( String entityName ) {
//        int count = _p.getEntityAge(entityName);
//        count += 1;
//        _p.setEntityAge(entityName, count);

        // broadcast to any distributed listeners:
        _c.notifyUpdated(entityName);
    }

    /**
     * Called by the distributed system when an entity has been updated.
     * @param entityName
     */
    public void onUpdated( String entityName ) {
        callEntityListeners(entityName);
    }

    /**
     * This method requests the distributed system to update the specified entity.
     * We don't know which Node hosts the Entity - it could be this, it could be another.
     * So, broadcast (or directly send) the update requet to another Node[s].
     * @param entityName
     */
    public void requestUpdate(String entityName) {
        // TODO: this should broadcast to the wider system the update request, in case it is handled by another Node
        //doUpdate(entityName); // monolithic only variant
        _c.requestUpdate(entityName);
    }

    /**
     * This method is called when the distributed system has received a request for an update of an Entity.
     * @param entityName
     */
    public void doUpdate(String entityName) {

        JsonEntity je = _p.getEntity(entityName);
        //String nodeName = _p.getNodeName(entityName);

        if( !je._node.equals( getName() ) ) {
            return;
        }

        //String entityType = _p.getEntityType( entityName );

        Entity e = _ef.create( entityName, je._type );

        forkUpdate(e); // returns immediately
    }

    /**
     * Creates a thread to actually do the work of updating the entity
     * @param e
     */
    protected void forkUpdate( final Entity e ) {
        Thread t = new Thread( new Runnable() {
            @Override public void run() {
                e.update();
            }
        } );
        t.start();
    }

    /**
     * Adds a listener to the specified Entity.
     * It will persist for only one call.
     * @param entity
     * @param listener
     */
    public void addEntityListener( String entity, EntityListener listener ) {
        synchronized( _entityListeners ) {
            ArrayList< EntityListener > al = _entityListeners.get( entity );
            if( al == null ) {
                al = new ArrayList();
                _entityListeners.put( entity, al );
            }
            al.add( listener );
        }
    }

    /**
     * Call any listeners associated with this Entity, and then remove them.
     * @param entity
     */
    public void callEntityListeners( String entity ) {
        synchronized( _entityListeners ) {
            ArrayList< EntityListener > al = _entityListeners.get( entity );
            if( al == null ) {
                al = new ArrayList< EntityListener >();
                _entityListeners.put( entity, al );
            }

            for( EntityListener listener : al ) {
                listener.onEntityUpdated( entity );
            }

            al.clear(); // remove references, it doesn't need calling twice.
        }
    }

}