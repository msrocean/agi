package io.agi.ef.interprocess.apiInterfaces;

import javax.ws.rs.core.Response;

/**
 * Created by gideon on 30/07/15.
 */
public interface ControlInterface {

    Response command( String entityName, String command );

    Response status( String entityName, String state );
}