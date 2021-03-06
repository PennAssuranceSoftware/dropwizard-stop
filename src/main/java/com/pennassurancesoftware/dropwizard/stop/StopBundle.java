package com.pennassurancesoftware.dropwizard.stop;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Generics;

/**
 * Optional functionality to coordinate a graceful stop of the service.<p/>
 * To add this functionality, you should add this bundle during the Service initialization.
 * And make sure that you 'initialize' the bundle too.<p/>
 * Here is a code snippet:
 * <pre>
    public void initialize(Bootstrap{@code <YourAppConfiguration>} bootstrap) {
        ....
        StopBundle{@code <YourAppConfiguration>} stopBundle = new StopBundle{@code <YourAppConfiguration>}() {
          {@literal @}Override
          public StopConfiguration getStopConfiguration(YourAppConfiguration configuration) {
            return configuration.getStopConfiguration();
          }
        };
        bootstrap.addBundle(stopBundle);
        // This seems like a bug to me.  I.e. some bundles are initialized and others are not.
        stopBundle.initialize(bootstrap);
        ....
    }
 * }
 * </pre>
 * <strong>Don't forget to add a {@link StopConfiguration} to YourAppConfiguration.</strong>
 *
 */
public abstract class StopBundle<T extends Configuration> implements ConfiguredBundle<T>, ConfigurationStrategy<T> {
   @Override
   public final void initialize( Bootstrap<?> bootstrap ) {
      final Class<T> klass = Generics.getTypeParameter( getClass(), Configuration.class );
      bootstrap.addCommand( new StopCommand<T>( this, klass ) );
   }

   @Override
   public void run( T configuration, Environment environment ) throws Exception {
      final StopConfiguration stopConfig = getStopConfiguration( configuration );
      environment.lifecycle().addServerLifecycleListener( new StopMonitor( stopConfig ) );
   }
}
