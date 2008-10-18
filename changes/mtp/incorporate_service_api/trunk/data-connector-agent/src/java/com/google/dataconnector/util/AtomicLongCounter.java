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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements a counter that holds an AtomicLong.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class AtomicLongCounter implements Counter {
  
  private AtomicLong counter;
  
  /**
   * Creates the counter with an initial value of 0.
   */
  public AtomicLongCounter() {
    counter = new AtomicLong();
  }
  
  /**
   * Creates a counter with the specified initial value.
   * 
   * @param initialValue the value to initialize the counter with.
   */
  public AtomicLongCounter(long initialValue) {
    counter = new AtomicLong(initialValue);
  }

  /**
   * {@inheritDoc}
   */
  public long get() {
    return counter.get();
  }

  /**
   * {@inheritDoc}
   */
  public void increment(int delta) {
    counter.addAndGet(delta);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return counter.toString();
  }
}
