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
package com.google.dataconnector.registration.v2;

import com.google.dataconnector.util.ConfigFile;
import com.google.dataconnector.util.ConfigurationException;
import com.google.dataconnector.util.Flag;
import com.google.feedserver.client.FeedServerEntry;
import com.google.feedserver.util.ContentUtil;
import com.google.gdata.data.OtherContent;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Uses reflection to scan registered classes for any String or boolean ending in "_FLAG" or
 * "_HELP" and creates command line flags for these fields.  Use {@linke #register(Class)} to 
 * register a classes flag Fields and {@link #parse} to parse the command line args.
 * 
 * Fields must be static and only String and boolean are currently supported.   The field name
 * up to the "_" is used as the flag name and its value is the default.
 * 
 * example:
 * to setup --filename as a flag. in class Foo with flag help.
 * 
 * public class Foo { 
 *   public static String filename_FLAG = "/tmp/defaultfile";
 *   public static String filename_HELP = "filename to access";
 *   
 *   public static void main(String[] args) {
 *     CommonsCliHelper cliHelper = new CommonsCliHelper();
 *     cliHelper.register(Foo.class);
 *     cliHelper.parse(args);
 *     System.stdout.println("Filename is " + filename_FLAG);
 *   }
 * 
 * @author r@kuci.org (Ray Colline)
 */
public class BeanCliHelper {
  
  @SuppressWarnings("unchecked")
  private List<Object> beans;
  private CommandLine flags;
  private Options options;
  
  private boolean requireConfigFile;
  
  @SuppressWarnings("unchecked")
  public BeanCliHelper() {
    beans = new ArrayList<Object>();
  }
  
  @SuppressWarnings("unchecked")
  public void register(Object bean) {
    beans.add(bean); 
  }
  
  /**
   * With provided command line string, populates all registered classes _FLAG fields with their
   * command-line values.
   * 
   * @param args command-line.
   */
  public void parse(String[] args) throws ConfigurationException {
    options = createOptions();
    GnuParser parser = new GnuParser();
    try {
      flags = parser.parse(options, args);
    } catch (ParseException e) {
      usage();
      throw new RuntimeException(e);
    }
    
    /*
     * Print help text and exit.
     */
    if (flags.hasOption("help")) {
      usage();
      System.exit(0);
    }
    populateBeansFromConfigFile();
    populateBeansFromCommandLine();
  }
  
  /**
   * Prints usage information.
   */
  public void usage() {
    new HelpFormatter().printHelp("Usage", options);
  }
  
  /**
   * Must be called after {@link GnuParser#parse(Options, String[])} and if bean has 
   * {@link ConfigFile} decorator load that file and populate bean.  If no decorator is present 
   * we just no-op.
   */
  @SuppressWarnings("unchecked")
  private void populateBeansFromConfigFile() throws ConfigurationException {
    
    populateBeansFromCommandLine(); // we do an initial pass to pick up ConfigFile flag
    
    for (Object bean : beans) {
      for(Field field : bean.getClass().getDeclaredFields()) {
        ConfigFile configFileAnnotation = field.getAnnotation(ConfigFile.class);
        String configFileName = "";
        if (configFileAnnotation != null) {
          try {
            configFileName = (String) bean.getClass().getMethod("get" + field.getName()
                .substring(0,1).toUpperCase() + field.getName().substring(1), (Class[]) null)
                .invoke(bean, (Object[]) null);
            String configFileContents = readFileIntoString(configFileName);
            FeedServerEntry configEntry = new FeedServerEntry(configFileContents);
            ContentUtil contentUtil = new ContentUtil();
            contentUtil.fillBean((OtherContent) configEntry.getContent(), bean);
          } catch (RuntimeException e) { 
            throw new ConfigurationException(e);
          } catch (IllegalAccessException e) {
            throw new ConfigurationException(e);
          } catch (InvocationTargetException e) {
            throw new ConfigurationException(e);
          } catch (NoSuchMethodException e) {
            throw new ConfigurationException(e);
          } catch (IOException e) {
            throw new ConfigurationException("Error reading config file " + configFileName, e);
          } catch (IntrospectionException e) {
            throw new ConfigurationException(e);
          } catch (SAXException e) {
            throw new ConfigurationException(e);
          } catch (ParserConfigurationException e) {
            throw new ConfigurationException(e);
          }
        }
      }
    }
  }
  
  /**
   * Helper that loads a file into a string.
   * 
   * @param fileName properties file with rules configuration.
   * @returns a String representing the file.
   * @throws IOException if any errors are encountered reading the properties file
   */
  public static String readFileIntoString(final String fileName) throws IOException {
    File file = new File(fileName);
    byte[] fileContents = new byte[(int) file.length()];
    new BufferedInputStream(new FileInputStream(fileName)).read(fileContents);
    return new String(fileContents);
  }
  
  /**
   * Loop through each registered class and parse the command line for their flags.  If
   * option isnt specified we leave the default.
   */
  @SuppressWarnings("unchecked")
  private void populateBeansFromCommandLine() {
    for (Object bean : beans) {
      for (Field field : bean.getClass().getDeclaredFields()) {
        Flag flag = field.getAnnotation(Flag.class);
        if (flag == null) {
          continue;
        }
        String argName = field.getName();
        // Boolean Flags
        if (field.getType().getName().equals(Boolean.class.getName())) {
          boolean newValue;
          if (flags.hasOption(argName)) {
            setField(field, bean, new Boolean(true));
          } else if (flags.hasOption("no" + argName)) {
            setField(field, bean, new Boolean(false));
          }
          // Integer Flags
        } else if (field.getType().getName().equals(Integer.class.getName())) {
          String argValue = flags.getOptionValue(argName, null);
          if (argValue != null) {
            try {
              setField(field, bean, Integer.valueOf(argValue));
            } catch (NumberFormatException e) {
              throw new RuntimeException(e); 
            }
          }
          // String Flag
        } else if (field.getType().getName().equals(String.class.getName())) {
          String argValue = flags.getOptionValue(argName, null);
          if (argValue != null) {
            setField(field, bean, argValue);
          }
          // Repeated String Flag
        } else if (field.getType().getName().equals(String[].class.getName())) {
          String[] argValues = flags.getOptionValues(argName);
          if (argValues != null) {
            setField(field, bean, argValues);
          }
        }
      }
    }
  }

  /**
   * Sets value in the supplied field's setter to the given value.
   * 
   * @param field the flag field.
   * @param value the value, usually a Boolean or a String.
   * @throws RuntimeException if the field is mis-configured.
   */
  private void setField(Field field, Object bean, Object value) {
    try {
      String methodName = "set" + field.getName().substring(0,1).toUpperCase() + 
              field.getName().substring(1);  
      bean.getClass().getMethod("set" + field.getName().substring(0,1).toUpperCase() + 
              field.getName().substring(1),  
          field.getType()).invoke(bean, value);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Field " + field.getName() + " must be a String", e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Field " + field.getName() + " must be public", e);
    } catch (NullPointerException e) {
      throw new RuntimeException("Field " + field.getName() + " must be static");
    } catch (SecurityException e) {
      throw new RuntimeException("Field " + field.getName() + " must be static");
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Field " + field.getName() + " must be static");
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Field " + field.getName() + " must be static");
    }
  }
  
  public boolean isRequireConfigFile() {
    return requireConfigFile;
  }

  public void setRequireConfigFile(boolean requireConfigFile) {
    this.requireConfigFile = requireConfigFile;
  }

  /**
   * For each class registered, we extract options based on the flags set within the class.
   * 
   *  - Any class field ending in {prefix}_FLAG is turned into "--{prefix}" on the command line. 
   *   eg.  "public String adminEmail_FLAG" becomes "--adminEmail"
   *  - Any field defined with {prefix}_HELP will be used as the help text.
   * 
   * @return Options all commandline options registered for parsing.
   */
  @SuppressWarnings("unchecked")
  private Options createOptions() {
    
    Options options = new Options();
    options.addOption(new Option("help", false, "Print out usage."));
    for (Object bean : beans) {
      for (Field field : bean.getClass().getDeclaredFields()) {
        Flag flag = field.getAnnotation(Flag.class);
        if (flag != null) {
          // Check type we only support boolean, String and Integer.
	      if ((field.getType() != Integer.class) && 
	          (field.getType() != String.class) && 
	          (field.getType() != boolean.class)) {
	        throw new RuntimeException("Field: " + field.getName() + " flag type not supported");
	      }
          String argName = field.getName();
          if (field.getType().getName().equals(Boolean.class.getName())) {
            options.addOption(new Option(argName, false, flag.help()));
            options.addOption(new Option("no" + argName, true, flag.help()));
          } else {
            options.addOption(new Option(argName, true, flag.help()));
          }
        }
      }
    }
    return options;
  }
}
