//
// $Id$

package com.threerings.app.server;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.SocketChannel;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.HttpException;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.nio.SelectChannelEndPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import static com.threerings.app.Log.log;

/**
 * Dispatches HTTP requests to servlets.
 */
@Singleton
public class AppHttpServer extends Server
{
   /**
     * Injects and registers the specified servlet at the specified path. Can be called before or
     * after {@link #init}.
     */
    public void serve (Class<? extends HttpServlet> sclass, String path) {
        _context.addServlet(new ServletHolder(_injector.getInstance(sclass)), path);
    }

    /**
     * Initializes the HTTP server with the supplied document root.
     */
    public void init (int port, File docroot)
    {
        // use a custom connector that works around some jetty non-awesomeness
        setConnectors(new Connector[] {
            new SaneChannelConnector(port)
        });

        ContextHandlerCollection handlers = new ContextHandlerCollection();
        setHandler(handlers);

        // wire our root context
        _context.setContextPath("/");
        _context.setResourceBase(docroot.getAbsolutePath());
        _context.setWelcomeFiles(new String[] { "index.html" });
        _context.addServlet(new ServletHolder(new CustomDefaultServlet()), "/*");

        // TODO: allow eror page customization
        // // deliver a static error page regardless of cause; errors are already logged
        // _context.setErrorHandler(new ErrorHandler() {
        //     @Override
        //     protected void writeErrorPageHead (HttpServletRequest request, Writer writer, int code,
        //                                        String message) throws IOException {
        //         writer.write(ERROR_HEAD);
        //     }
        //     @Override
        //     protected void writeErrorPageBody (HttpServletRequest request, Writer writer, int code,
        //                                        String message, boolean stacks) throws IOException {
        //         // TODO: allow app to customize
        //         writer.write("<span>" + code + "</span>: " + message);
        //     }
        // });

        handlers.addHandler(_context);
    }

    /**
     * Instructs the HTTP server to stop listening for new requests. Should be followed by a call
     * to {@link #join} to wait for existing requests to complete.
     */
    public void shutdown ()
    {
        try {
            stop();
        } catch (Exception e) {
            log.warning("Failed to stop HTTP server.", e);
        }
    }

    protected static class SaneChannelConnector extends SelectChannelConnector {
        public SaneChannelConnector (int httpPort) {
            setPort(httpPort);
        }

        @Override // from SelectChannelConnector
        protected Connection newConnection (SocketChannel chan, SelectChannelEndPoint ep) {
            return new HttpConnection(this, ep, getServer()) {
                @Override public Connection handle () throws IOException {
                    try {
                        return super.handle();
                    } catch (NumberFormatException nfe) {
                        // TODO: demote this to log.info in a week or two
                        log.warning("Failing invalid HTTP request", "uri", _uri, "error", nfe);
                        throw new HttpException(400); // bad request
                    } catch (IOException ioe) {
                        if (ioe.getClass() == IOException.class) { // grr
                            log.warning("Failing invalid HTTP request", "uri", _uri, "error", ioe);
                            throw new HttpException(400); // bad request
                        } else {
                            throw ioe;
                        }
                    }
                }
            };
        }
    }

    protected static class CustomDefaultServlet extends DefaultServlet
    {
        @Override // from DefaultServlet
        protected void doGet (HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException
        {
            String path = req.getPathInfo();
            if (path != null) {
                // add a no caching header to GWT foo.nocache.ext files
                if (path.indexOf(".nocache.") != -1) {
                    rsp.addHeader("Cache-Control", "no-cache, no-store");
                // and cache the unchanging files for long time
                } else if (path.indexOf(".cache.") != -1) {
                    rsp.setDateHeader("Expires", System.currentTimeMillis() + ONE_YEAR);
                }
                // provide the proper content-type for (app cache) manifest files
                if (path.endsWith(".manifest")) {
                    rsp.setContentType("text/cache-manifest");
                }
            }
            super.doGet(req, rsp);
        }
    }

    protected final ServletContextHandler _context = new ServletContextHandler();

    @Inject protected Injector _injector;
    // @Inject protected StatusServlet _statusServlet;

    // TODO: allow the app to provide error
    protected static final String ERROR_HEAD =
        "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=ISO-8859-1\"/>\n" +
        "<title>Oh noez!</title>\n";

    protected static final long ONE_YEAR = 365*24*60*60*1000L;
}
