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

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class HealthzRequestHandler extends Thread {
  public static Logger LOG = Logger.getLogger(HealthzRequestHandler.class.getName());

  // MAX amount of time to wait before giving up on the service starting up
  public static final int MAX_WAIT_TIME_FOR_SERVICE_TO_STATUP = 1; // in sec
  
  private ServerSocket serverSocket;

  /**
   * Returns the port this service is listening on. This is used when system rules such as
   * healthz service rule are added to the user-defined list of resource rules.
   *  
   * @return the port the healthz service is listening on.
   */
  public int getPort() {
    if (serverSocket == null) {
      return -1;
    }
    return serverSocket.getLocalPort();
  }
  
  /**
   * Returns true if the healthz service is successfully started up. false otherwise.
   * Waits on the serverSocket port to become -1. 
   * 
   * @return true if the healthz service started up. false otherwise.
   */
  public boolean isStarted() {
    
    int port;
    // sleep for 100 ms between each check
    int numSleepCycles = MAX_WAIT_TIME_FOR_SERVICE_TO_STATUP * 10;
    for (int k = 0; ((port = getPort()) == -1) && k < numSleepCycles; k++) { 
      LOG.info("healthz service NOT ready yet. sleep for 100ms and check again. attempt #" + k);
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        LOG.fatal("sleep interrupted");
        return false;
      }
    }
    if (port == -1) {
      // this should be a rarity
      LOG.warn("healthz service couldn't be started");
      return false;
    }
    LOG.info("healthz service started on port " + port);
    return true;
  }

  public HealthzRequestHandler() {
    this.start();
    // wait for the service to startup
    isStarted();
  }
  
  @Override
  public void run() {
    setName("HealthzRequestHandler");
    try {
      serverSocket = new ServerSocket(0);
      while (true) {
        Socket incomingSocket = serverSocket.accept();

        // read incoming request
        BufferedReader in = new BufferedReader(new InputStreamReader(
            incomingSocket.getInputStream()));
        PrintWriter out = new PrintWriter(incomingSocket.getOutputStream(), true);
        String inputStr;
        while ((inputStr = in.readLine()) != null) {
          if (inputStr.contains("/healthz")) {
            LOG.info("healthz req found");
            break;
          }
        }
        out.println("ok");
        out.close();
        LOG.info("processed healthz request");
      }
    } catch (IOException e) {
      LOG.warn("Healthz service IOException", e);
    }
  }
}
