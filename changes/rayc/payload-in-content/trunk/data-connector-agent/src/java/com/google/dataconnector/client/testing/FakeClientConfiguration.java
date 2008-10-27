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
package com.google.dataconnector.client.testing;

import com.google.dataconnector.registration.v1.ResourceConfigException;
import com.google.dataconnector.registration.v2.ResourceRule;
import com.google.dataconnector.registration.v2.ResourceRuleUtil;
import com.google.dataconnector.util.LocalConf;
import com.google.feedserver.client.FeedServerEntry;
import com.google.feedserver.util.ContentUtil;
import com.google.gdata.data.OtherContent;

import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility that provides pre populated registration configuration for testing purposes.
 *
 * @author rayc@google.com (Ray Colline)
 */
public class FakeClientConfiguration {

  /** We do not generate random keys but instead start at 1 and increment 1 for each rule */
  public static final Long STARTING_VALID_KEY = 1L;

  /** The fake properties file we generate the config from */
  private LocalConf fakeLocalConf;
  private List<ResourceRule> resourceRules;

  /**
   * Creates a configuration beans from the fake hardcoded XML files.  
   */
  public FakeClientConfiguration() {

    FeedServerEntry configEntry = new FeedServerEntry(CONFIG_XML);
    ContentUtil contentUtil = new ContentUtil();
    ResourceRuleUtil resourceRuleUtil = new ResourceRuleUtil();
    try {
      contentUtil.fillBean((OtherContent) configEntry.getContent(), fakeLocalConf);
      resourceRules = resourceRuleUtil.getResourceRules(RESOURCE_RULES_XML);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (IntrospectionException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(e);
    } catch (ResourceConfigException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return localConf populated with a fake set of configuration from {@link #CONFIG_XML}
   */
  public LocalConf getFakeLocalConf() {
    return fakeLocalConf;
  }
  
  /**
   * @return list of ResourceRule populated with a fake set of configuration from 
   * {@link #RESOURCE_RULES_XML}
   */
  public List<ResourceRule> getFakeResourceRules() {
    return resourceRules;
  }

  public static final String CONFIG_XML =  "<entity>\n" +
    "<sdcServerHost>127.0.0.1</sdcServerHost>\n" +
    "<sdcServerPort>4444</sdcServerPort>\n" +
    "<domain>test.joonix.net</domain>\n" +
    "<user>rcolline</user>\n" +
    "<password>90ssuck</password>\n" +
    "<useSsl>true</useSsl>\n" +
    "<sslKeyStorePassword>woodstock</sslKeyStorePassword>\n" +
    "<sslKeyStoreFile>./config/secureLinkClientTrustStore</sslKeyStoreFile>\n" +
    "<clientId>test_joonix.net1</clientId>\n" +
    "<sshd>\n" +
    "/usr/local/securelink/secure-link-client/third-party/openssh/bin/start_sshd.sh\n" +
    "</sshd>\n" +
    "<startingHttpProxyPort>18000</startingHttpProxyPort>\n" +
    "<httpProxyBindHost>127.0.0.1</httpProxyBindHost>\n" +
    "<socksServerPort>1080</socksServerPort>\n" +
    "<socksdBindHost>127.0.0.1</socksdBindHost>\n" +
    "<logPropertiesFile>./log.properties</logPropertiesFile>\n" +
    "<socksProperties>\n" +
    "iddleTimeout    = 600000   # 10 minutes\n" +
    "acceptTimeout   = 60000    # 1 minute\n" +
    "udpTimeout      = 600000   # 10 minutes\n" +
    "log = -\n" +
    "</socksProperties>\n" +
    "<logProperties>\n" +
    "# ***** Set root logger level to DEBUG and its only appender to A.\n" +
    "log4j.rootLogger=debug, A\n" +
    "\n" +
    "# ***** A is set to be a ConsoleAppender.\n" +
    "log4j.appender.A=org.apache.log4j.ConsoleAppender\n" +
    "# ***** A uses PatternLayout.\n" +
    "log4j.appender.A.layout=org.apache.log4j.PatternLayout\n" +
    "log4j.appender.A.layout.ConversionPattern=%-4r [%t] %-5p %c %x - %m%n\n" +
    "</logProperties>\n" +
    "</entity>\n";
  
  public static final String RESOURCE_RULES_XML = "<feed>\n" +
    "<entity repeatable='true'>\n" +
    "<name>1</name>\n" +
    "<clientId>all</clientId>\n" +
    "<allowedEntities repeatable='true'>rcolline@test.joonix.net</allowedEntities>\n" +
    "<allowedEntities>admin@test.joonix.net</allowedEntities>\n" +
    "<pattern>http://www.example.com</pattern>\n" +
    "</entity>\n" +
    "<entity>\n" +
    "<name>2</name>\n" +
    "<clientId>all</clientId>\n" +
    "<allowedEntities repeatable='true'>rcolline@test.joonix.net</allowedEntities>\n" +
    "<allowedEntities>admin@test.joonix.net</allowedEntities>\n" +
    "<pattern>http://.*.google.com/.*</pattern>\n" +
    "</entity>\n" +
    "</feed>\n" ;

}
