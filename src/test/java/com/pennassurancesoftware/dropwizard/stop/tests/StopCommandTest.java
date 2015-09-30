package com.pennassurancesoftware.dropwizard.stop.tests;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;

import net.sourceforge.argparse4j.inf.Namespace;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.pennassurancesoftware.dropwizard.stop.ConfigurationStrategy;
import com.pennassurancesoftware.dropwizard.stop.StopCommand;
import com.pennassurancesoftware.dropwizard.stop.StopConfiguration;

/**
 * Tests for StopCommand class.
 */
public class StopCommandTest extends AbstractStopTests {

   private TestStopCommand fixture;
   private Socket mockSocket;
   private OutputStream mockOut;
   private int exitErrorCode;
   private TestMeConfiguration configuration;
   private Bootstrap<TestMeConfiguration> bootstrap;

   @Before
   public void setUp() throws IOException {
      ConfigurationStrategy<TestMeConfiguration> strategy = new ConfigurationStrategy<TestMeConfiguration>() {
         @Override
         public StopConfiguration getStopConfiguration( TestMeConfiguration configuration ) {
            return configuration.getStopConfiguration();
         }
      };
      fixture = new TestStopCommand( strategy, TestMeConfiguration.class );

      mockSocket = mock( Socket.class );
      mockOut = mock( OutputStream.class );
      when( mockSocket.getOutputStream() ).thenReturn( mockOut );
      configuration = new TestMeConfiguration();
      configuration.getStopConfiguration().setWait( Duration.seconds( 0 ) );
      bootstrap = new Bootstrap<TestMeConfiguration>( new TestMeService() );
   }

   @Test
   public void runHappyDay() throws Exception {
      fixture.run( bootstrap, null, configuration );

      assertThat( exitErrorCode ).isEqualTo( 0 );

      verify( mockSocket ).getOutputStream();

      ArgumentCaptor<byte[]> captorWrite = ArgumentCaptor.forClass( byte[].class );
      verify( mockOut ).write( captorWrite.capture() );
      byte[] expectedStopCommand = ( configuration.getStopConfiguration().getKey() + "\r\nstop\r\n" ).getBytes();
      assertThat( captorWrite.getValue() ).isEqualTo( expectedStopCommand );

      verify( mockOut ).flush();
      verify( mockSocket ).close();
   }

   @Test
   public void runWithTimeout() throws Exception {
      int waitTime = 1;
      configuration.getStopConfiguration().setWait( Duration.seconds( waitTime ) );
      InputStream mockInputStream = new ByteArrayInputStream( "Stopped".getBytes() );
      when( mockSocket.getInputStream() ).thenReturn( mockInputStream );

      fixture.run( bootstrap, null, configuration );

      verify( mockSocket ).getOutputStream();
      verify( mockSocket ).setSoTimeout( waitTime * 1000 );
      verify( mockSocket ).getInputStream();

      ArgumentCaptor<byte[]> captorWrite = ArgumentCaptor.forClass( byte[].class );
      verify( mockOut ).write( captorWrite.capture() );
      byte[] expectedStopCommand = ( configuration.getStopConfiguration().getKey() + "\r\nstop\r\n" ).getBytes();
      assertThat( captorWrite.getValue() ).isEqualTo( expectedStopCommand );

      verify( mockOut ).flush();
      verify( mockSocket ).close();
   }

   @Test
   public void runWithTimeoutWaitingError() throws Exception {
      int waitTime = 1;
      configuration.getStopConfiguration().setWait( Duration.seconds( waitTime ) );
      when( mockSocket.getInputStream() ).thenThrow( new SocketTimeoutException( "Testing timeout" ) );

      fixture.run( bootstrap, null, configuration );
      assertThat( exitErrorCode ).isEqualTo( 2 );

      verify( mockSocket ).getOutputStream();
      verify( mockSocket ).setSoTimeout( waitTime * 1000 );
      verify( mockSocket ).getInputStream();

      ArgumentCaptor<byte[]> captorWrite = ArgumentCaptor.forClass( byte[].class );
      verify( mockOut ).write( captorWrite.capture() );
      byte[] expectedStopCommand = ( configuration.getStopConfiguration().getKey() + "\r\nstop\r\n" ).getBytes();
      assertThat( captorWrite.getValue() ).isEqualTo( expectedStopCommand );

      verify( mockOut ).flush();
      verify( mockSocket ).close();
   }

   @Test
   public void runWithConnectionException() throws Exception {
      byte[] message = ( configuration.getStopConfiguration().getKey() + "\r\nstop\r\n" ).getBytes();
      doThrow( new ConnectException( "Testing connection exception." ) ).when( mockOut ).write( message );

      fixture.run( bootstrap, null, configuration );
      assertThat( exitErrorCode ).isEqualTo( 3 );
      verify( mockSocket ).close();
   }

   @Test
   public void runWithUnknownException() throws Exception {
      byte[] message = ( configuration.getStopConfiguration().getKey() + "\r\nstop\r\n" ).getBytes();
      doThrow( new RuntimeException( "Testing unknown exception." ) ).when( mockOut ).write( message );

      fixture.run( bootstrap, null, configuration );
      assertThat( exitErrorCode ).isEqualTo( 4 );
      verify( mockSocket ).close();
   }

   private class TestStopCommand extends StopCommand<AbstractStopTests.TestMeConfiguration> {
      private TestStopCommand( ConfigurationStrategy<TestMeConfiguration> strategy, Class<AbstractStopTests.TestMeConfiguration> configurationClass ) {
         super( strategy, configurationClass );
      }

      @Override
      protected void exitNow( int exitCode ) {
         exitErrorCode = exitCode;
      }

      @Override
      protected Socket getSocket( int port ) {
         return mockSocket;
      }

      @Override
      protected void run( Bootstrap<TestMeConfiguration> testMeConfigurationBootstrap, Namespace namespace, TestMeConfiguration configuration ) throws Exception {
         super.run( testMeConfigurationBootstrap, namespace, configuration );
      }
   }

}
