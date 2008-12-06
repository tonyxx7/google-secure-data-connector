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
package com.google.dataconnector.util;

import com.google.dataconnector.registration.v2.ResourceException;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.registration.v2.ResourceRuleValidator;
import com.google.feedserver.util.BeanCliHelper;
import com.google.feedserver.util.ConfigurationBeanException;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Client Guice module.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ClientGuiceModule extends AbstractModule {
  
  private static final Logger log = Logger.getLogger(ClientGuiceModule.class);

  private String[] args; // command line args
  
  public ClientGuiceModule(String[] args) {
    this.args = args;
  }

  @Override
  protected void configure() {}
  
  @Provides
  public Runtime getRuntime() {
    return Runtime.getRuntime();
  }
  
  @Provides @Singleton
  public LocalConf getLocalConf() {
    LocalConf localConf = new LocalConf();
    
    // Load configuration file and command line flags into beans
    BeanCliHelper beanCliHelper = new BeanCliHelper();
    beanCliHelper.register(localConf);
    try {
      beanCliHelper.parse(args);
      
      // validate localconf
      LocalConfValidator localConfValidator = new LocalConfValidator();
      localConfValidator.validate(localConf);
    } catch (ConfigurationBeanException e) {
      throw new RuntimeException(e);
    } catch (LocalConfException e) {
      throw new RuntimeException(e);
    }
    return localConf;
  }
  
  @Provides @Singleton
  public List<ResourceRule> getResourceRules(LocalConf localConf,
      ResourceRuleUtil resourceRuleUtil) {
    
    try {
      List<ResourceRule> resourceRules = resourceRuleUtil.getResourceRules(
          loadFileIntoString(localConf.getRulesFile()));
      // Validate Resource Rules
      ResourceRuleValidator resourceRuleValidator = new ResourceRuleValidator();
      resourceRuleValidator.validate(resourceRules);
      
      // Set runtime configuration.
      resourceRuleUtil.getThisClientRulesAndSetId(resourceRules, localConf.getClientId());
      resourceRuleUtil.setSocksServerPort(resourceRules,
          localConf.getSocksServerPort());
      // REMOVE THE NEXT LINE DONT LET ME CHECK THIS IN IF YOU SEE THIS COMMENT HERE!
      resourceRuleUtil.setHttpProxyPorts(resourceRules, localConf.getHttpProxyPort());
      resourceRuleUtil.setSecretKeys(resourceRules);
      return resourceRules;
    } catch (ResourceException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Sets up our own local SSL context and returns a SSLSocketFactory with keystore and password
   * set by our flags.
   * 
   * @param localConfiguration the configuration object for the client.
   * @return SSLSocketFactory configured for use.
   */
  @Provides @Singleton
  public SSLSocketFactory getSslSocketFactory(LocalConf localConfiguration) {
    // Only if we have useSsl set.
    if (!localConfiguration.getUseSsl()) {
      return null;
    }
    
    log.info("Using SSL for client connections.");
    
    char[] password = localConfiguration.getSslKeyStorePassword().toCharArray();
    try {
      String keystorePath = localConfiguration.getSslKeyStoreFile();
      
      SSLContext context = SSLContext.getInstance("TLSv1");
      if (keystorePath != null) { // The customer specified their own keystore.
        // Get a new "Java Key Store"
        KeyStore keyStore = KeyStore.getInstance("JKS");
        // Load with our trusted certs and setup the trust manager.
        keyStore.load(new FileInputStream(keystorePath), password);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
        tmf.init(keyStore);
        // Create the SSL context with our private store.
        context.init(null, tmf.getTrustManagers(), null);
      } else {
        // Use the JVM default as trusted store. This would be located somewhere around
        // jdk.../jre/lib/security/cacerts, and will contain widely used CAs.
        context.init(null, null, null);  // Use JVM default.
      }
      return context.getSocketFactory();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException("SSL setup error: " + e);
    } catch (IOException e) {
      throw new RuntimeException("Could read Keystore file: " + e);
    }
  }
  
  /**
   * Gets the properly configured {@link LocalConf}
   */
  
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

}
