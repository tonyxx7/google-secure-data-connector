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
package com.google.dataconnector.protocol;

import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;

import org.apache.log4j.Logger;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sender for SDC Frame protocol.  The SDC Frame protocol uses the {@link FrameInfo} protocol 
 * buffer to encapsulate many different connections over the top of one stream.  The FrameSender
 * writes {@link FrameInfo} protos to the underlying output stream.  It is implemented as a
 * {@link Thread} which watches a queue.  {@link FrameSender#sendFrame(FrameInfo)} is designed
 * to be called from your thread.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class FrameSender extends Thread {

  private static final Logger LOG = Logger.getLogger(FrameReceiver.class);

  private BlockingQueue<FrameInfo> sendQueue;
  
  // Runtime dependencies
  private OutputStream outputStream;
  private AtomicLong byteCounter;
  
  // Local fields.
  private DataOutputStream dataOutputStream;
  private long sequence = 0;

  @Inject
  public FrameSender(BlockingQueue<FrameInfo> sendQueue) {
    this.sendQueue = sendQueue;
  }
  
  /**
   * Wraps the supplied Type and Payload in a FrameInfo and sends it over the output stream.
   * 
   * @param type the FrameInfo type. 
   * @param payload the payload of the Payload.
   */
  public void sendFrame(FrameInfo.Type type, ByteString payload) {
    sendFrame(FrameInfo.newBuilder()
        .setType(type)
        .setPayload(payload)
        .build());
  }
  
  /**
   * Sends an already constructed FrameInfo over the output stream.
   * 
   * @param frame the frame to send.
   */
  public void sendFrame(FrameInfo frame) {
    if (!frame.hasType()) {
      throw new RuntimeException("Frame missing type info");
    }
    try {
      sendQueue.put(frame);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Used by the queue watching loop to write a a single frame to the output stream.   We leave
   * this package-private to support testing.
   * 
   * @param frameInfo the frame to send.
   * @throws IOException if any IOerrors while writing.
   */
  // visible for testing.
  void writeOneFrame(FrameInfo frameInfo) throws IOException {
    Preconditions.checkNotNull(outputStream, "Must specify outputStream before writing frames.");

    byte[] frameInfoBytes = frameInfo.toByteArray();

    // Add frame start.
    outputStream.write(FrameReceiver.FRAME_START);
    // Add magic.
    outputStream.write(FrameReceiver.MAGIC);
    // Add sequence number.
    dataOutputStream.writeLong(sequence);
    // Add length value
    dataOutputStream.writeInt(frameInfoBytes.length);
    // Add frame info pb raw bytes.
    outputStream.write(frameInfoBytes);
    // Update bytes sent counter if one has been supplied.
    if (byteCounter != null) {
      byteCounter.addAndGet(FrameReceiver.HEADER_SIZE + frameInfoBytes.length);
    }
    // Increment sequence number.
    sequence++;
  }
  
  /** 
   * Reads the send queue, assigns a sequence number and puts on the wire.
   */
  @Override
  public void run() {
    
    try {
      while (true) {
        // Wait for a frame to become available.
        FrameInfo frameInfo = FrameInfo.newBuilder(sendQueue.take()).setSequence(sequence).build();
        writeOneFrame(frameInfo);
      }
    } catch (InterruptedException e) {
      LOG.info("Sending frames interrupted", e);
    } catch (IOException e) {
      LOG.info("IO error while sending frame", e);
    }
  }
  
  public void setOutputStream(OutputStream outputStream) {
    this.outputStream = outputStream;
    dataOutputStream = new DataOutputStream(outputStream);
  }
  
  public void setByteCounter(AtomicLong byteCounter) {
    this.byteCounter = byteCounter;
  }
}