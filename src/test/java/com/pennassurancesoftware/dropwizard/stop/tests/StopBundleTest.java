package com.pennassurancesoftware.dropwizard.stop.tests;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import io.dropwizard.cli.Command;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.common.collect.ImmutableList;
import com.pennassurancesoftware.dropwizard.stop.StopBundle;
import com.pennassurancesoftware.dropwizard.stop.StopConfiguration;

/**
 * Test the StopBundle class.
 */
public class StopBundleTest extends AbstractStopTests {
   private StopBundle<TestMeConfiguration> fixture;

   @Before
   public void setUp() throws Exception {
      fixture = new StopBundle<TestMeConfiguration>() {
         @Override
         public StopConfiguration getStopConfiguration( TestMeConfiguration configuration ) {
            return configuration.getStopConfiguration();
         }
      };
   }

   @Test
   public void initialize() {
      Bootstrap<TestMeConfiguration> bootstrap = new Bootstrap<TestMeConfiguration>( new TestMeService() );
      fixture.initialize( bootstrap );

      ImmutableList<Command> commands = bootstrap.getCommands();
      assertThat( commands.size() ).isEqualTo( 1 );
   }

   @Test
   public void run() throws Exception {
      Environment environment = mock( Environment.class );
      fixture.run( new TestMeConfiguration(), environment );

      ArgumentCaptor<ServerLifecycleListener> captor = ArgumentCaptor.forClass( ServerLifecycleListener.class );

      verify( environment ).lifecycle().addServerLifecycleListener( captor.capture() );
      ServerLifecycleListener serverLifecycleListener = captor.getValue();
      assertThat( serverLifecycleListener ).isNotNull();
   }
}
