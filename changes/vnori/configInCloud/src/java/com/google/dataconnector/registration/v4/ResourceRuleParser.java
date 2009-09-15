package com.google.dataconnector.registration.v4;

import com.google.dataconnector.util.FileUtil;
import com.google.dataconnector.util.RegistrationException;
import com.google.inject.Inject;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class ResourceRuleParser {
  private final FileUtil fileUtil;

  @Inject
  public ResourceRuleParser(FileUtil fileUtil) {
    this.fileUtil = fileUtil;
  }
  
  public List<String> parseResourcesFile(String resourcesFileName, String thisAgentId)
      throws RegistrationException, FileNotFoundException, 
      XMLStreamException, FactoryConfigurationError {
    FileInputStream fileInputStream = fileUtil.getFileInputStream(resourcesFileName);
    XMLStreamReader xmlStreamReader = 
      XMLInputFactory.newInstance().createXMLStreamReader(fileInputStream);
    String url = null, agentId = null;
    List<String> urlList = new ArrayList<String>();
    while (xmlStreamReader.hasNext()) {
      switch (xmlStreamReader.next()) {
        case XMLStreamConstants.START_ELEMENT:
          // look for url and agentId elements.
          boolean urlFound = xmlStreamReader.getLocalName().equalsIgnoreCase("url");
          boolean agentIdFound = xmlStreamReader.getLocalName().equalsIgnoreCase("agentId");
          if (urlFound || agentIdFound) {
            int event = xmlStreamReader.next();
            if (event == XMLStreamConstants.CHARACTERS) {
              // can't tolerate anything other than CHARACTERS element 
              if (urlFound) {
                url = xmlStreamReader.getText();
              } else {
                agentId = xmlStreamReader.getText();
              }
            }
          }
          break;
          
        case XMLStreamConstants.END_ELEMENT:
          if (xmlStreamReader.getLocalName().equals("rule")) {
            // do a sanity check
            if (url == null || agentId == null) {
              throw new RegistrationException("resources.xml file is mising url / agentId " +
                    "elements in one of the resource rules");
            }
            if (agentId.equals(thisAgentId) || agentId.equalsIgnoreCase("all")) {
              // this url is a resource served by this agent.
              urlList.add(url);
            }
            url = null;
            agentId = null;
          }
          break;
      }
    }
    xmlStreamReader.close();
    return urlList;
    }
  }