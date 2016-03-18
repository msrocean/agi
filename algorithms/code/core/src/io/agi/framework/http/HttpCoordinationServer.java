package io.agi.framework.http;

import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

/**
 * Created by dave on 19/02/16.
 */
public class HttpCoordinationServer {

    public static HttpServer create( final HttpCoordination c, int port, String handlerContext, HttpCoordinationHandler h ) {

        try {
            HttpServer server = HttpServer.create( new InetSocketAddress(port), 0 );
            server.createContext(handlerContext, h );
            server.setExecutor(null); // creates a default executor
            return server;
        }
        catch( Exception e ) {
            e.printStackTrace();
            return null;
        }
    }
}