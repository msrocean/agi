package io.agi.ef.core.apiInterfaces;

import javax.ws.rs.core.Response;

/**
 * Created by gideon on 1/08/15.
 */
public interface ConnectInterface {
    Response connectAgent( String contextPath );
    Response connectWorld( String contextPath );
}