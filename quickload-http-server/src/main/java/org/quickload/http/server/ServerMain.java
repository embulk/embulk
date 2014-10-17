package org.quickload.http.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

public class ServerMain
{
    private static class ApplicationBase extends Application
    {
        @Override
        public Set<Class<?>> getClasses() {
            Set<Class<?>> routes = new HashSet<Class<?>>();
            routes.add(Sample.class);
            return routes;
        }
    }

    public static void main(String[] args) throws Exception
    {
        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        servletContextHandler.setContextPath("/");
        ServletHolder servletHolder = new ServletHolder(ServletContainer.class);
        servletHolder.setInitParameter("javax.ws.rs.Application", ApplicationBase.class.getName());
        servletContextHandler.addServlet(servletHolder, "/*");

        Server server = new Server(8080);
        server.setHandler(servletContextHandler);
        server.start();
        server.join();
    }
}
