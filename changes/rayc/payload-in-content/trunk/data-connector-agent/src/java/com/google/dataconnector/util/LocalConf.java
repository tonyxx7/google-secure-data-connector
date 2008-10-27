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

/**
 * Bean representing all the startup configuration for the agent.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class LocalConf {
  
  private static final String DEFAULT_BIND_HOST = "127.0.0.1";
  
  private String name;

  @ConfigFile(required = true)
  @Flag(help = "Configuration File")
  private String localConfigFile = ""; //Configure flag to be of type "String"
  @Flag
  private String rulesFile;
  @Flag
  private String sdcServerHost;
  @Flag
  private Integer sdcServerPort;
  @Flag
  private String domain;
  @Flag
  private String user;
  @Flag
  private String password;  
  @Flag
  private boolean useSsl = true;
  @Flag
  private String sslKeyStorePassword = "./secureLinkClientTrustStore";
  @Flag
  private String sslKeyStoreFile = "";
  @Flag
  private String clientId;
  @Flag
  private String sshd;
  @Flag
  private Integer startingHttpProxyPort;
  @Flag
  private String httpProxyBindHost = DEFAULT_BIND_HOST;
  @Flag
  private Integer socksServerPort;
  @Flag
  private String socksdBindHost = DEFAULT_BIND_HOST;
  
  // Config Only
  private String logProperties;
  private String socksProperties;
  
  // getters and setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocalConfigFile() {
    return localConfigFile;
  }

  public void setLocalConfigFile(String configFile) {
    this.localConfigFile = configFile;
  }
  
  public void setRulesFile(String rulesFile) {
    this.rulesFile = rulesFile;
  }

  public String getRulesFile() {
    return rulesFile;
  }

  public String getSdcServerHost() {
    return sdcServerHost;
  }

  public void setSdcServerHost(String sdcServerHost) {
    this.sdcServerHost = sdcServerHost;
  }

  public Integer getSdcServerPort() {
    return sdcServerPort;
  }

  public void setSdcServerPort(Integer sdcServerPort) {
    this.sdcServerPort = sdcServerPort;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getSslKeyStorePassword() {
    return sslKeyStorePassword;
  }

  public void setSslKeyStorePassword(String sslKeyStorePassword) {
    this.sslKeyStorePassword = sslKeyStorePassword;
  }

  public String getSslKeyStoreFile() {
    return sslKeyStoreFile;
  }

  public void setSslKeyStoreFile(String sslKeyStoreFile) {
    this.sslKeyStoreFile = sslKeyStoreFile;
  }

  public Boolean getUseSsl() {
    return useSsl;
  }

  public void setUseSsl(boolean useSsl) {
    this.useSsl = useSsl;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getSshd() {
    return sshd;
  }

  public void setSshd(String sshd) {
    this.sshd = sshd;
  }

  public Integer getStartingHttpProxyPort() {
    return startingHttpProxyPort;
  }

  public void setStartingHttpProxyPort(Integer startingHttpProxyPort) {
    this.startingHttpProxyPort = startingHttpProxyPort;
  }

  public String getHttpProxyBindHost() {
    return httpProxyBindHost;
  }

  public void setHttpProxyBindHost(String httpProxyBindHost) {
    this.httpProxyBindHost = httpProxyBindHost;
  }

  public Integer getSocksServerPort() {
    return socksServerPort;
  }

  public void setSocksServerPort(Integer socksServerPort) {
    this.socksServerPort = socksServerPort;
  }

  public String getSocksdBindHost() {
    return socksdBindHost;
  }

  public void setSocksdBindHost(String socksdBindHost) {
    this.socksdBindHost = socksdBindHost;
  }

  public String getLogProperties() {
    return logProperties;
  }

  public void setLogProperties(String logProperties) {
    this.logProperties = logProperties;
  }

  public String getSocksProperties() {
    return socksProperties;
  }

  public void setSocksProperties(String socksProperties) {
    this.socksProperties = socksProperties;
  }
}
