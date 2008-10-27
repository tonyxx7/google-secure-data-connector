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

import com.google.dataconnector.registration.v1.ResourceConfigException;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * First class object representing the Registration request for the Secure Link protocol.  This 
 * provides a toJson() utility to serialize information for over-the-wire communication and 
 * provides a constructor to create from a serialized JSONObject.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class RegistrationRequest {

  static final String RESOURCES_KEY = "resources";
  
  // Dependencies
  private BeanUtil beanUtil;
  private XmlUtil xmlUtil;
  
  private JSONArray resourcesJsonArray;
  private List<ResourceRule> resourceRules;
  
  
  public RegistrationRequest() {
    this(new BeanUtil(), new XmlUtil());
  }
  
  public RegistrationRequest(BeanUtil beanUtil, XmlUtil xmlUtil) {
    this.beanUtil = beanUtil;
    this.xmlUtil = xmlUtil;
    resourceRules = new ArrayList<ResourceRule>();
  }
  /**
   * Creates the request from JSONObject.
   * 
   * @param json The JSON representing the object.
   * @throws ResourceConfigException if the JSON is malformed.
   */
  public void populateFromJSON(final JSONObject json) throws ResourceConfigException {
    try {
      this.resourcesJsonArray = json.getJSONArray(RESOURCES_KEY);
      for (int index=0; index < resourcesJsonArray.length(); index++) {
        String resourceXml = resourcesJsonArray.getString(index);
        ResourceRule resourceRule = new ResourceRule();
        beanUtil.convertPropertiesToBean(xmlUtil.convertXmlToProperties(resourceXml), resourceRule);
      }
    } catch (JSONException e) {
      throw new ResourceConfigException(e);
    } catch (IllegalArgumentException e) {
      throw new ResourceConfigException(e);
    } catch (IntrospectionException e) {
      throw new ResourceConfigException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceConfigException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceConfigException(e);
    } catch (SAXException e) {
      throw new ResourceConfigException(e);
    } catch (IOException e) {
      throw new ResourceConfigException(e);
    } catch (ParserConfigurationException e) {
      throw new ResourceConfigException(e);
    }
  }
  
  /**
   * Creates empty object that must be populated before calling toJSON().
   */
  public void populateFromResources(List<ResourceRule> resourceRules) throws 
      ResourceConfigException {
    
    resourcesJsonArray = new JSONArray();
    for (ResourceRule resourceRule : resourceRules) {
      try {
        resourcesJsonArray.put(xmlUtil.convertPropertiesToXml(
            beanUtil.convertBeanToProperties(resourceRule)));
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

  public List<ResourceRule> getResources() {
    return resourceRules;
  }
  
  /**
   * Returns JSON object representing data.
   * 
   * @return populated JSON object with class data.
   * @throws JSONException if any fields are null.
   */
  public JSONObject toJson() throws JSONException {
    JSONObject json = new JSONObject();
    json.put(RESOURCES_KEY, resourcesJsonArray);
    return json;
  }
}
  