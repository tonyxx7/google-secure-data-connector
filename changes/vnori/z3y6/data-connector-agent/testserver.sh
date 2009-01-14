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
# Runs testserver and client to test various failure scenarios

. ./env.sh

export CLASSPATH=${CLASSPATH}:$WOODSTOCK_HOME/build/testClasses/
CMD="java ${JVM_ARGS} com.google.dataconnector.testserver.TestServerFailures \
 -localConfigFile ./config/localConfig.xml \
 -rulesFile ./config/resourceRules.xml \
 -sshPrivateKeyFile ./config/id_rsa_no_password \
 -testServerListenPort 9009"

# uncomment this line to get more debugging output
#CMD="${CMD} -verbose"

${CMD} -exitpoint AFTER_CLIENT_CONNECTS 
${CMD} -exitpoint AFTER_AUTHZ_REQ_RECVD
${CMD} -exitpoint AFTER_AUTHZ_RESPONSE_SENT
${CMD} -exitpoint AFTER_REG_REQ_RECVD
${CMD} -exitpoint AFTER_REG_RESPONSE_SENT
${CMD} -exitpoint AFTER_SSHD_START
${CMD} -exitpoint NORMAL_EXIT
