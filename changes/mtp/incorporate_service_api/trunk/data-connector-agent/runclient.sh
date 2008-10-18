#!/bin/bash
#
# * Copyright 2008 Google Inc.
# *
# * This program is free software; you can redistribute it and/or
# * modify it under the terms of the GNU General Public License
# * as published by the Free Software Foundation; either version 2
# * of the License, or (at your option) any later version.
# *
# * This program is distributed in the hope that it will be useful,
# * but WITHOUT ANY WARRANTY; without even the implied warranty of
# * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# * GNU General Public License for more details.
# *
# * You should have received a copy of the GNU General Public License
# * along with this program; if not, write to the Free Software
# * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#
# Runs client with appropriate class path.

WOODSTOCK_HOME=$(pwd)

export JAVA_HOME=/home/build/buildtools/java/jdk1.6.0
export PATH=${JAVA_HOME}/bin/:${PATH}
export CLASSPATH=\
${WOODSTOCK_HOME}/third-party/jsocks/bin/jsocks-server.jar:\
${WOODSTOCK_HOME}/third-party//wpg-proxy/bin/wpgproxy.jar:\
${WOODSTOCK_HOME}/third-party/jline/jline-0.9.91.jar:\
${WOODSTOCK_HOME}/third-party/commons-io/commons-io-1.3.2.jar:\
${WOODSTOCK_HOME}/third-party/jsch/jsch-20071024.jar:\
${WOODSTOCK_HOME}/third-party/apache-log4j/log4j-1.2.15.jar:\
${WOODSTOCK_HOME}/third-party/commons-logging/commons-logging-1.1.jar:\
${WOODSTOCK_HOME}/third-party/commons-cli/commons-cli-1.1.jar:\
${WOODSTOCK_HOME}/third-party/json/json.jar:\
${WOODSTOCK_HOME}/build/prod/classes/:\
${WOODSTOCK_HOME}/third-party/serviceapi/service_api.jar:\
${WOODSTOCK_HOME}/third-party/serviceapi/third_party/apache-abdera-0.4.0-incubating/abdera-0.4.0-incubating.jar:\
${WOODSTOCK_HOME}/third-party/serviceapi/third_party/servlet.jar:\
${WOODSTOCK_HOME}/third-party/serviceapi/third_party/apache-abdera-0.4.0-incubating/lib/jetty-6.1.5.jar:\
${WOODSTOCK_HOME}/third-party/serviceapi/third_party/apache-abdera-0.4.0-incubating/lib/jetty-util-6.1.5.jar:\
${WOODSTOCK_HOME}/third-party/serviceapi/third_party/apache-abdera-0.4.0-incubating/lib/axiom-impl-1.2.5.jar:\
${WOODSTOCK_HOME}/third-party/serviceapi/third_party/apache-abdera-0.4.0-incubating/lib/axiom-api-1.2.5.jar:\
${WOODSTOCK_HOME}/third-party/serviceapi/third_party/apache-abdera-0.4.0-incubating/lib/jaxen-1.1.1.jar
JVM_ARGS="-Djava.net.preferIPv4Stack=true"   

if (( "${#}" > 0 )) ; then
  java ${JVM_ARGS} com.google.securelink.client.Client "${@}"
  exit ${?}
else
  while /bin/true; do
    java com.google.securelink.client.Client \
      -configFile ./rules.properties \
      -secureLinkServerHost enterprise-apps-tls.sandbox.google.com \
      -secureLinkServerPort 443 \
      -logPropertiesFile ./logging.properties \
      -useSsl \
      -sslKeyStoreFile ./config/secureLinkClientTrustStore \
      -sslKeyStorePassword woodstock
    sleep 5
    echo "RECONNECTING..."
  done
fi
