package com.pennassurancesoftware.dropwizard.stop.tests;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pennassurancesoftware.dropwizard.stop.StopConfiguration;

/**
 * Contains common test step.
 */
public class AbstractStopTests {
   protected class TestMeService extends Application<TestMeConfiguration> {
      @Override
      public void initialize( Bootstrap<TestMeConfiguration> testMeConfigurationBootstrap ) {
         //To change body of implemented methods use File | Settings | File Templates.
      }

      @Override
      public void run( TestMeConfiguration configuration, Environment environment ) throws Exception {
         //To change body of implemented methods use File | Settings | File Templates.
      }
   }

   protected class TestMeConfiguration extends Configuration {
      @NotNull
      @JsonProperty
      private String myValue;

      @NotNull
      @JsonProperty("stop")
      private StopConfiguration stopConfiguration = new StopConfiguration();

      public String getMyValue() {
         return myValue;
      }

      public void setMyValue( String myValue ) {
         this.myValue = myValue;
      }

      public StopConfiguration getStopConfiguration() {
         return stopConfiguration;
      }

      public void setStopConfiguration( StopConfiguration stopConfiguration ) {
         this.stopConfiguration = stopConfiguration;
      }
   }

}
