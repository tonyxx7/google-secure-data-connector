/* Copyright 2009 Google Inc.
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
 *
 * $Id: HealthCheckRequestHandler.java 486 2009-10-12 21:00:48Z matt.proud $
 */
package com.google.dataconnector.util;

import com.google.common.collect.Maps;
import com.google.gdata.util.common.base.Preconditions;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShutdownManager {

  private static final Logger LOG = Logger.getLogger(ShutdownManager.class);

  private static final String DEFAULT = "__default__";
  
  private Map<String, List<Pair<String, Stoppable>>> stoppableGroups = Maps.newHashMap();
  
  /**
   * Adds the stoppable to the shutdown manager using the default group.
   * 
   * @param className The name of the class associated with this stoppable.
   * @param stoppable An instance that should be shutdown.
   */
  public void addStoppable(String className, Stoppable stoppable) {
    addStoppable(className, stoppable, DEFAULT);
  }
  
  /**
   * Adds the stoppable to the shutdown manager using the supplied group.
   * 
   * @param className The name of the class associated with this stoppable.
   * @param stoppable An instance that should be shutdown.
   * @param group A group identifier to support shutting down in separate phases.
   */
  public void addStoppable(String className, Stoppable stoppable, String group) {
    if (!stoppableGroups.containsKey(group)) {
      stoppableGroups.put(group, new ArrayList<Pair<String, Stoppable>>());
    }
    stoppableGroups.get(group).add(new Pair<String, Stoppable>(className, stoppable));
  }
  
  /**
   * Loop through all registered stoppables and issue a shutdown.
   */
  public void shutdownAll() {
    for (String group : stoppableGroups.keySet()) {
      shutdownGroup(group);
    }
  }
  
  /**
   * For a given group, loop through all registered stoppables and issue a shutdown.
   * 
   * @param groupName
   */
  public void shutdownGroup(String groupName) {
    Preconditions.checkArgument(stoppableGroups.containsKey(groupName), 
        groupName + " does not exist");
    for (Pair<String, Stoppable> stoppablePair : stoppableGroups.get(groupName)) {
      try {
        stoppablePair.second().shutdown();
        LOG.info("Issued shutdown for " + stoppablePair.first());
      } catch (RuntimeException e) {
        LOG.warn("Stop failed for " + stoppablePair.first(), e);
      }
    }
  }
}
