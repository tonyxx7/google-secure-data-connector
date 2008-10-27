/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.google.dataconnector.client;

import com.google.dataconnector.registration.v2.BeanCliHelper;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.util.ConfigurationException;
import com.google.dataconnector.util.ConnectionException;
import com.google.dataconnector.util.LocalConf;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Entry point class for starting the Secure Data Connector.  There are three components to the
 * Secure Data Connector: 
 * 
 * 1) The "secure data connection" to the server which provides the transport from the server
 * side back to the client.  see {@link SecureDataConnection}
 * 2) The Socks 5 proxy which provides the network firewall to incoming network connections through
 * the secure data transport. see {@link JsocksStarter}
 * 3) The HTTP(S) proxy which provides the http firewall filtering for incoming http requests.
 * see {@link WpgProxyStarter}
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class Client {
  
  // Logging instance
  private static final Logger log = Logger.getLogger(Client.class);

  /** Secure Data Connector Configuration */
  private LocalConf localConfiguration;
  private List<ResourceRule> resourceRules;

  /** Jsocks Starter Object */
  private JsocksStarter jsocksStarter;
  
  /** WPG Proxy Starter Object */
  private WpgProxyStarter wpgProxyStarter;
  
  /**
   * Creates a new client from the populated client configuration object.
   * 
   * @param localConfiguration the local configuration object.
   * @param resourceRules runtime configured resource rules.
   */
  public Client(LocalConf localConfiguration, List<ResourceRule> resourceRules) {
    this.localConfiguration = localConfiguration;
    this.resourceRules = resourceRules;
  }
  
  /**
   * Starts three components in separate threads.
   * 
   * @throws IOException if any socket communication issues occur.
   * @throws ConnectionException if login is incorrect or other Woodstock connection errors.
   * @throws ConfigurationException 
   */
  public void startUp() throws IOException, ConnectionException, ConfigurationException {
    
    // Check SSL flag and leave sslSocketFactory set to null if SSL is disabled.
    SSLSocketFactory sslSocketFactory = null;
    if (localConfiguration.getUseSsl()) {
      sslSocketFactory = getSslSocketFactory(localConfiguration);
    }
      
    wpgProxyStarter = new WpgProxyStarter(localConfiguration, resourceRules);
    wpgProxyStarter.startHttpProxy();
    jsocksStarter = new JsocksStarter(localConfiguration, resourceRules);
    jsocksStarter.start(); 
    SecureDataConnection secureDataConnection = new SecureDataConnection(localConfiguration, 
        resourceRules, sslSocketFactory);
    secureDataConnection.connect();
  }
  
  /**
   * Entry point for the Secure Data Connector binary.  Sets up logging, parses flags and
   * creates ClientConf.
   * 
   * @param args
   */
  public static void main(String[] args) throws ConfigurationException {
    // Bootstrap logging system
    PropertyConfigurator.configure(getBootstrapLoggingProperties());
    
    // Create configuration beans
    LocalConf localConfiguration = new LocalConf();
    
    // Load configuration file and command line flags into beans
    BeanCliHelper beanCliHelper = new BeanCliHelper();
    beanCliHelper.register(localConfiguration);
    beanCliHelper.parse(args);
    
    try {
      
      // Create resource rules based on rules config
      ResourceRuleUtil resourceRuleUtil = new ResourceRuleUtil();
      List<ResourceRule> resourceRules = resourceRuleUtil.getResourceRules(
          loadFileIntoString(localConfiguration.getRulesFile()));
      resourceRuleUtil.cleanUpBeans(resourceRules);
      resourceRuleUtil.setSocksServerPort(resourceRules,
          localConfiguration.getSocksServerPort());
      resourceRuleUtil.setHttpProxyPorts(resourceRules, 
          localConfiguration.getStartingHttpProxyPort());
      resourceRuleUtil.setSecretKeys(resourceRules);
      
      // Load logging properties file.
      Properties properties = new Properties();
      properties.load(new ByteArrayInputStream(
          localConfiguration.getLogProperties().trim().getBytes()));
      PropertyConfigurator.configure(properties);

      // Create the client instance and start services
      Client client = new Client(localConfiguration, resourceRules);
      client.startUp();
    } catch (ConnectionException e) {
     log.fatal("Connection error: " + e.getMessage());
     System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      log.fatal("Client connection failure: ", e);
    }
  }
  
  /**
   * Returns a base set of logging properties so we can log fatal errors before config parsing is 
   * done.
   * 
   * @return Properties a basic console logging setup.
   */
  public static Properties getBootstrapLoggingProperties() {
    Properties props = new Properties();
    props.setProperty("log4j.rootLogger","debug, A");
    props.setProperty("log4j.appender.A", "org.apache.log4j.ConsoleAppender");
    props.setProperty("log4j.appender.A.layout", "org.apache.log4j.PatternLayout");
    props.setProperty("log4j.appender.A.layout.ConversionPattern", "%-4r [%t] %-5p %c %x - %m%n");
    return props;
  }
  
  /**
   * Helper that loads a file into a new string.
   * 
   * @param fileName properties file with rules configuration.
   * @returns a String representing the file contents.
   * @throws IOException if any errors are encountered reading the file
   */
  public static String loadFileIntoString(final String fileName) throws IOException {
    File file = new File(fileName);
    byte[] buffer = new byte[(int) file.length()];
    InputStream fin = new FileInputStream(fileName);
    fin.read(buffer);
    return new String(buffer);
  }
  
  /**
   * Sets up our own local SSL context and returns a SSLSocketFactory with keystore and password
   * set by our flags.
   * 
   * @param localConfiguration the configuration object for the client.
   * @return SSLSocketFactory configured for use.
   */
  public static SSLSocketFactory getSslSocketFactory(LocalConf localConfiguration) {
    char[] password = localConfiguration.getSslKeyStorePassword().toCharArray();
    try {
      // Get a new "Java Key Store"
      KeyStore keyStore = KeyStore.getInstance("JKS");
      // Load with our trusted certs and setup the trust manager(the server).
      keyStore.load(new FileInputStream(localConfiguration.getSslKeyStoreFile()), password);
      TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
      tmf.init(keyStore);
      // Create the SSL context with our private store.
      SSLContext context = SSLContext.getInstance("TLSv1");
      context.init(null, tmf.getTrustManagers(), null); 
      return context.getSocketFactory();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("SSL setup error: " + e);
    } catch (IOException e) {
      throw new RuntimeException("Could read Keystore file: " + e);
    }
  }
  
}
