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

package com.google.dataconnector.registration.v2;

import com.google.dataconnector.registration.v1.ResourceConfigEntry;
import com.google.dataconnector.registration.v1.ResourceConfigException;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;

import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility methods to convert resource strings into appropriate {@link ResourceConfigEntry} list.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceRuleUtil {
  
  private static final String TOP_LEVEL_ELEMENT = "feed";
  private static final String ENTITY = "entity";
  private static final String HTTP = "http";
  
  // Dependencies
  private XmlUtil xmlUtil;
  private BeanUtil beanUtil;
  
  /**
   * Creates a new resource util and its dependencies
   */
  public ResourceRuleUtil() {
    this(new XmlUtil(), new BeanUtil());
    
  }
  
  /**
   * Creates a new resource util with provided dependcies
   * 
   * @param xmlUtil utility to convert XML into map properties
   * @param beanUtil utility to covert properties map into a bean.
   */
  public ResourceRuleUtil(XmlUtil xmlUtil, BeanUtil beanUtil) {
    this.xmlUtil = xmlUtil;
    this.beanUtil = beanUtil;
  }
  
  /**
   * Creates secret keys for each of the resources.
   * 
   * @param resourceRules a list of resourceRules.
   */
  public void setSecretKeys(List<ResourceRule> resourceRules) {
    for (ResourceRule resourceRule : resourceRules) {
      resourceRule.setSecretKey(new Random().nextLong());
    }
  }
  
  /**
   * Sets the httpProxyProt for wpg-proxy http thread handling this request.
   * 
   * @param resourceRules a list of resourceRules.
   * @param startingHttpProxyPort the first port number to use for each http rule.
   */
  public void setHttpProxyPorts(List<ResourceRule> resourceRules, int startingHttpProxyPort) {
    for (ResourceRule resourceRule : resourceRules) {
      if (resourceRule.getPattern().startsWith(HTTP)) {
	    resourceRule.setHttpProxyPort(startingHttpProxyPort);
	    startingHttpProxyPort++;
      }
    }
  }
  
  /**
   * Sets the socks server listen port for these resource rules
   * 
   * @param resourceRules a list of resourceRules.
   * @param socksServerPort the first port number to use for each http rule.
   */
  public void setSocksServerPort(List<ResourceRule> resourceRules, int socksServerPort) {
    for (ResourceRule resourceRule : resourceRules) {
      resourceRule.setSocksServerPort(socksServerPort);
    }
  }
  
  /**
   * Removes whitespace artifacts from XML parsing
   * TODO(rayc) remove when Abhinav has trimming built into XMLUtil
   * 
   * @param resourceRules
   */
  public void cleanUpBeans(List<ResourceRule> resourceRules) {
    for (ResourceRule resourceRule : resourceRules) {
      resourceRule.setName(resourceRule.getName().trim());
      resourceRule.setClientId(resourceRule.getClientId().trim());
      resourceRule.setPattern(resourceRule.getPattern().trim());
      String[] allowedEntities = resourceRule.getAllowedEntities();
      for (int index=0; index < allowedEntities.length; index++) {
        allowedEntities[index] = allowedEntities[index].trim();
      }
    }
  }
  
  @SuppressWarnings("unchecked")
  public List<ResourceRule> getResourceRules(String xmlText) throws ResourceConfigException {
    List<ResourceRule> resourceRules = new ArrayList<ResourceRule>();
    try {
      Map<String,Object> entities = xmlUtil.convertXmlToProperties(xmlText, TOP_LEVEL_ELEMENT);
      if (entities.containsKey(ENTITY)) {
        if (entities.get(ENTITY) instanceof Object[]) { 
          for (Object ruleProperties :  (Object[]) entities.get(ENTITY)) {
            if (ruleProperties instanceof HashMap) {
              ResourceRule resourceRule = new ResourceRule();  
              beanUtil.convertPropertiesToBean((HashMap) ruleProperties, resourceRule);
              resourceRules.add(resourceRule);
            }
          }
        }
      }
      return resourceRules;
    } catch (SAXException e) {
      throw new ResourceConfigException(e);
    } catch (IOException e) {
      throw new ResourceConfigException(e);
    } catch (ParserConfigurationException e) {
      throw new ResourceConfigException(e);
    } catch (IllegalArgumentException e) {
      throw new ResourceConfigException(e);
    } catch (IntrospectionException e) {
      throw new ResourceConfigException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceConfigException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceConfigException(e);
    }
  }
}
  