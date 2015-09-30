package com.pennassurancesoftware.dropwizard.stop;

import io.dropwizard.Configuration;

/**
 * Establish the configuration strategy for the Stop configuration.
 */
public interface ConfigurationStrategy<T extends Configuration> {
   StopConfiguration getStopConfiguration( T configuration );
}
