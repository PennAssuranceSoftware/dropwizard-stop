package com.pennassurancesoftware.dropwizard.stop;

import io.dropwizard.Configuration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.Duration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;

import net.sourceforge.argparse4j.inf.Namespace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add a 'stop' command to the list of commands for the server.
 * This command will allow a fairly graceful shutdown of the service without using SIGINT.
 * Users would not use this class directly, but instead add {@link StopBundle} during the
 * service initialization.  See {@link StopBundle} for an example of how to set this up.
 */
public class StopCommand<T extends Configuration> extends ConfiguredCommand<T> {
   private static final Logger LOGGER = LoggerFactory.getLogger( StopCommand.class );
   private static final int ERR_LOCALHOST = 1;
   private static final int ERR_TIMEDOUT_WAITING = 2;
   private static final int ERR_NOT_STOPPED = 3;
   private static final int ERR_UNKNOWN = 4;
   private final ConfigurationStrategy<T> strategy;
   private final Class<T> configurationClass;

   protected StopCommand( ConfigurationStrategy<T> strategy, Class<T> configurationClass ) {
      super( "stop", "Stop the server." );
      this.strategy = strategy;
      this.configurationClass = configurationClass;
   }

   /**
    * Returns the {@link Class} of the configuration type.
    *
    * @return the {@link Class} of the configuration type
    */
   protected Class<T> getConfigurationClass() {
      return this.configurationClass;
   }

   @Override
   protected void run( Bootstrap<T> bootstrap, Namespace namespace, T configuration ) throws Exception {
      final StopConfiguration stopConfig = strategy.getStopConfiguration( configuration );
      stop( stopConfig.getPort(), stopConfig.getKey(), stopConfig.getWait() );
   }

   /**
    * Create a socket to connect to the running server and issue the 'stop' command.
    *
    * @param port that the {@link StopMonitor} is on.
    * @param key that the {@link StopMonitor} will expect before performing the stop command.
    * @param timeout in seconds for how long to wait for a response from the {@link StopMonitor} before exiting.
    *                if <= zero no waiting will occur.
    */
   private void stop( int port, String key, Duration timeout ) {
      stop( getSocket( port ), key, timeout );
   }

   // Make a seam so that this class can be tested, mostly.
   protected Socket getSocket( int port ) {
      Socket s = null;
      try {
         s = new Socket( InetAddress.getByName( "127.0.0.1" ), port );
      }
      catch( IOException e ) {
         exitWithError( "Error trying to open a socket on local host port=" + port, e, ERR_LOCALHOST );
      }
      return s;
   }

   private void stop( Socket s, String key, Duration timeout ) {
      try {
         long quantity = timeout.getQuantity();
         if( quantity > 0 ) {
            s.setSoTimeout( ( int )timeout.toMilliseconds() );
         }
         try {
            OutputStream out = s.getOutputStream();
            out.write( ( key + "\r\nstop\r\n" ).getBytes( Charset.forName( "UTF-8" ) ) );
            out.flush();

            if( quantity > 0 ) {
               LOGGER.info( "Waiting " + ( quantity > 0 ? ( "up to " + timeout ) : "" ) + " for server to stop" );
               LineNumberReader lin = new LineNumberReader( new InputStreamReader( s.getInputStream(), Charset.forName( "UTF-8" ) ) );
               String response = lin.readLine();
               if( "Stopped".equals( response ) ) {
                  LOGGER.info( "Stopped" );
               }
            }
         }
         finally {
            s.close();
         }
      }
      catch( SocketTimeoutException e ) {
         exitWithError( "Timed out waiting for stop confirmation.", null, ERR_TIMEDOUT_WAITING );
      }
      catch( ConnectException e ) {
         exitWithError( "Connection issue trying to issue stop command.  " +
               "Are you sure the server is running?", e, ERR_NOT_STOPPED );
      }
      catch( Exception e ) {
         exitWithError( "Unknown issue trying to issue stop command.", e, ERR_UNKNOWN );
      }
   }

   private void exitWithError( String msg, Throwable e, int exitCode ) {
      if( e != null ) {
         LOGGER.error( msg, e );
      }
      else {
         LOGGER.error( msg );
      }
      exitNow( exitCode );
   }

   /**
    * Add a seam for testing.  I.e. wouldn't be good to actually exit the JVM during testing.
    * @param exitCode that will be reported.
    */
   protected void exitNow( int exitCode ) {
      System.exit( exitCode );
   }
}
