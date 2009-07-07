/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.google.dataconnector.client;

import com.google.dataconnector.registration.v3.ResourceException;
import com.google.dataconnector.util.ClientGuiceModule;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.HealthCheckRequestHandler;
import com.google.dataconnector.util.LocalConf;
import com.google.dataconnector.util.LocalConfException;
import com.google.dataconnector.util.LocalConfValidator;
import com.google.feedserver.util.BeanCliHelper;
import com.google.feedserver.util.ConfigurationBeanException;
import com.google.inject.Inject;
import com.google.inject.Injector;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.IOException;
import java.util.Properties;

/**
 * Entry point class for starting the Secure Data Connector.  There are three components to the
 * Secure Data Connector: 
 * 
 * 1) The "secure data connection" to the server which provides the transport from the server
 * side back to the client.  see {@link SdcConnection}
 * 2) The Socks 5 proxy which provides the network firewall to incoming network connections through
 * the secure data transport. see {@link JsocksStarter}
 * 3) The HTTP(S) proxy which provides the http firewall filtering for incoming http requests.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Client {
  
  // Logging instance
  private static final Logger log = Logger.getLogger(Client.class);

  /* Dependencies */
  private LocalConf localConfiguration;
  private SdcConnection secureDataConnection;
  private JsocksStarter jsocksStarter;
  private HealthCheckRequestHandler healthCheckRequestHandler;
  
  /**
   * Creates a new client from the populated client configuration object.
   * 
   * @param localConfiguration the local configuration object.
   * @param secureDataConnection
   */
  @Inject
  public Client(LocalConf localConfiguration, SdcConnection secureDataConnection,
      JsocksStarter jsocksStarter, HealthCheckRequestHandler healthCheckRequestHandler) {
    this.localConfiguration = localConfiguration;
    this.secureDataConnection = secureDataConnection;
    this.jsocksStarter = jsocksStarter;
    this.healthCheckRequestHandler = healthCheckRequestHandler;
  }
  
  /**
   * Starts 2 components in separate threads.
   * @throws IOException 
   * @throws ConnectionException 
   * @throws ResourceException 
   */
  public void startUp() throws IOException, ConnectionException, ResourceException {
    // Set log4j properties and watch for changes every min (default)
    PropertyConfigurator.configureAndWatch(localConfiguration.getLog4jPropertiesFile());
    if (localConfiguration.getDebug()) {
      Logger.getRootLogger().setLevel(Level.DEBUG);
    }
    
    // start the healthcheck service
    healthCheckRequestHandler.init();
    
    /* 
     * process resource rules to make sure they are good.
     * but why did we start healthcheck handler before validating the resources?
     * because the following method - incorrectly named validate() -
     * does more than validation of resource rules. 
     * 
     * thats terrible - but don't worry. the entire class ProcessResourceRules is going to 
     * be gone when config-in-cloud is done. so I left this ugly code in there, for now.
     * 
     * since this code will go away soon, I didn't inject ProcessResourceRules into this class
     */
    ClientGuiceModule.getInjector().getInstance(ProcessResourceRules.class).validate();
    jsocksStarter.startJsocksProxy();
    secureDataConnection.connect();
  }

  /**
   * validate the localConf.xml file and the input args
   * @throws ConfigurationBeanException 
   * @throws LocalConfException 
   */
  private static void validateLocalConf(Injector injector, String[] args) 
      throws ConfigurationBeanException, LocalConfException {        
    // Load configuration file and command line flags into beans
    LocalConf localConf = injector.getInstance(LocalConf.class);
    BeanCliHelper beanCliHelper = new BeanCliHelper();
    beanCliHelper.register(localConf);
    beanCliHelper.parse(args);
    new LocalConfValidator().validate(localConf);
  }
  
  /**
   * Returns a base set of logging properties so we can log fatal errors before config parsing is 
   * done.
   * 
   * @return Properties a basic console logging setup.
   */
  public static Properties getBootstrapLoggingProperties() {
    Properties props = new Properties();
    props.setProperty("log4j.rootLogger","info, A");
    props.setProperty("log4j.appender.A", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.A.layout", "org.apache.log4j.PatternLayout");
    props.setProperty("log4j.appender.A.layout.ConversionPattern", "%d [%t] %-5p %c %x - %m%n");
    return props;
  }
  
  
  /**
   * Entry point for the Secure Data Connector binary.  Sets up logging, parses flags and
   * creates ClientConf.
   * 
   * @param args
   */
  public static void main(String[] args) {
    // Bootstrap logging system
    PropertyConfigurator.configure(getBootstrapLoggingProperties());
    
    // validate the localConf.xml file and the input args
    try {
      validateLocalConf(ClientGuiceModule.getInjector(), args);
            
      // Create the client instance and start services
      ClientGuiceModule.getInjector().getInstance(Client.class).startUp();
    } catch (LocalConfException e) {
      log.fatal("Configuration error", e);
    } catch (ConfigurationBeanException e) {
      log.fatal("Configuration error", e);
    } catch (IOException e) {
      log.fatal("Startup error", e);
    } catch (ConnectionException e) {
      log.fatal("Startup error", e);
    } catch (ResourceException e) {
      log.fatal("Error while parsing resource rules", e);
    }
  }
}
