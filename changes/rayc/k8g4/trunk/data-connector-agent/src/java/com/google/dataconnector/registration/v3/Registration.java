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
package com.google.dataconnector.registration.v3;

import com.google.dataconnector.protocol.FrameReceiver;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.RegistrationInfo;
import com.google.dataconnector.util.RegistrationException;

import com.google.inject.Inject;
import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;
import org.json.JSONException;

import java.util.List;

public class Registration {
  
  private static final Logger LOG = Logger.getLogger(Registration.class);
  
  private RegistrationRequest registrationRequest;
  private List<ResourceRule> resourceRules;

  @Inject
  public Registration(RegistrationRequest registrationRequest, 
      List<ResourceRule> resourceRules) {
    this.registrationRequest = registrationRequest;
    this.resourceRules = resourceRules;
  }
   
  public void register(FrameReceiver frameReceiver, FrameSender frameSender) 
      throws RegistrationException {
    
    try {
      // Prepare reg request.
      registrationRequest.populateFromResources(resourceRules);
      RegistrationInfo registrationFrame = RegistrationInfo.newBuilder()
          .setXml(registrationRequest.toJson().toString()).build();
      
      // Send frame.
      frameSender.sendFrame(FrameInfo.Type.REGISTRATION, registrationFrame.toByteString());
      
      // Wait for response.
      RegistrationInfo responseRegistrationFrame = 
          RegistrationInfo.parseFrom(frameReceiver.readOneFrame().getPayload());
      
      if (responseRegistrationFrame.getResult() != RegistrationInfo.ResultCode.OK) {
        throw new RegistrationException("Registration failed: " + 
            responseRegistrationFrame.getStatusMessage());
      }
      LOG.info("registration successful");
      return;
    } catch (JSONException e) {
      throw new RegistrationException(e);
    } catch (ResourceException e) {
      throw new RegistrationException(e);
    } catch (FramingException e) {
      throw new RegistrationException(e);
    } catch (InvalidProtocolBufferException e) {
      throw new RegistrationException(e);
    }
  }
}
    
  