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
 */
package com.google.dataconnector.client;

import com.google.dataconnector.protocol.Dispatchable;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.HealthCheckInfo;
import com.google.dataconnector.util.LocalConf;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

/**
 * Handles both sending of health check requests and processing responses.  Health checks are sent
 * as frames to the server.  We implement a {@link Dispatchable} to handle the server responses.
 * if we do not receive a response within the specified timeout we execute the 
 * {@link RemoteFailSwitch} specified at runtime.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class HealthCheckHandler extends Thread implements Dispatchable {

  /**
   * Call back interface for when health check fails.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  public interface RemoteFailSwitch {
    public void handleFailure();
  }
  
  private static final Logger LOG = Logger.getLogger(HealthCheckHandler.class);
      
  // Injected Dependencies 
  private LocalConf localConf;
  
  // Runtime Dependencies
  private FrameSender frameSender;
  private RemoteFailSwitch remoteFailSwitch;
  
  // Class fields.
  private long lastHealthCheckReceivedStamp = 0;
  
  @Inject
  public HealthCheckHandler(LocalConf localConf) {
    this.localConf = localConf;
  }
  
  /**
   * Receives the {@link HealthCheckInfo} frame, parses and updates last received timestamp.
   */
  @Override
  public void dispatch(FrameInfo frameInfo) throws FramingException {
    
    try {
      HealthCheckInfo healthCheckInfo = HealthCheckInfo.parseFrom(frameInfo.getPayload());
      // Assignment is thread safe.
      lastHealthCheckReceivedStamp = System.currentTimeMillis();
    } catch (InvalidProtocolBufferException e) {
      throw new FramingException(e);
    }
  }

  @Override
  public void run() {
      Preconditions.checkNotNull(frameSender, "Must define frameSender before starting.");
      Preconditions.checkNotNull(remoteFailSwitch, "Must define remoteFailSwitch before starting.");
      
      // We send a healthcheck request every X seconds (configurable in localconf)
      while (true) {
        HealthCheckInfo hci = HealthCheckInfo.newBuilder()
            .setSource(HealthCheckInfo.Source.CLIENT)
            .setTimeStamp(System.currentTimeMillis())
            .setType(HealthCheckInfo.Type.REQUEST)
            .build();
        LOG.debug("Sending health check request");
        frameSender.sendFrame(SdcFrame.FrameInfo.Type.HEALTH_CHECK, hci.toByteString());
        
        try {
          sleep(localConf.getHealthCheckInterval() * 1000);
        } catch (InterruptedException e) {
          LOG.warn("Health check sender interrupted. Exiting.");
          break;
        }
        
        // Every send interval we check to see if we have timed out.  Sending is reliable as
        // it uses a large blocking queue to send frames.  Therefore we can re-use the send
        // thread to verify health check responses.   Java primitives have atomic assignment,
        // and only the dispatcher thread will actually assign the lastHealthCheckReceivedStamp.
        // We call the FailHandler when there is a failure.  
        if (System.currentTimeMillis() - (localConf.getHealthCheckTimeout() * 1000) > 
            lastHealthCheckReceivedStamp) {
          LOG.warn("Health check response not received in " + 
              (System.currentTimeMillis() - lastHealthCheckReceivedStamp) + "ms.");
          remoteFailSwitch.handleFailure(); 
          break;
        } else {
          LOG.trace("Health check ok, last received " + 
              (System.currentTimeMillis() - lastHealthCheckReceivedStamp) + "ms ago.");
        }
     }
  }
  
  public void setFrameSender(FrameSender frameSender) {
    this.frameSender = frameSender;
  }

  public void setFailHandler(RemoteFailSwitch failHandler) {
    this.remoteFailSwitch = failHandler;
  }
}
