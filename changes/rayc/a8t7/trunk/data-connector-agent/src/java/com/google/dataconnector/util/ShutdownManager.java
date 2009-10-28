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
import com.google.inject.Singleton;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Collects all threads and objects that need to be shutdown.  This may be for exiting but it 
 * could also be for other cleanups.  Register your {@link Stoppable} instances here and
 * call {@link #shutdownGroup(String)} or {@link #shutdownAll()} when you need to terminate
 * them.
 * 
 * @author rayc@google.com (Ray Colline)
 */
@Singleton
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
   * Loop through all registered stoppables and issue a shutdown.  If no stoppables are registered,
   * we do nothing and return.  This allows defensive calls to shutdown.
   */
  public void shutdownAll() {
    for (String group : stoppableGroups.keySet()) {
      shutdownGroup(group);
    }
  }
  
  /**
   * For a given group, loop through all registered stoppables and issue a shutdown. If group
   * does not exist in map, we do nothing and just return as this allows defensive cleanups.
   * 
   * @param groupName
   */
  public void shutdownGroup(String groupName) {
    if (!stoppableGroups.containsKey(groupName)) {
      return;
    }
    
    for (Pair<String, Stoppable> stoppablePair : stoppableGroups.get(groupName)) {
      try {
        stoppablePair.second().shutdown();
        LOG.info("Issued shutdown for " + stoppablePair.first());
      } catch (RuntimeException e) {
        LOG.warn("Stop failed for " + stoppablePair.first(), e);
      }
    }
    // We are a singleton therefore we must clean up any references we have attempted to shutdown
    // so they get GCed.
    stoppableGroups.remove(groupName);
  }
}
