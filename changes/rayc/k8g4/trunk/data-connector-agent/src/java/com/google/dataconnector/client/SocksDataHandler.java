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

import com.google.dataconnector.protocol.ConnectorStateCallback;
import com.google.dataconnector.protocol.Dispatchable;
import com.google.dataconnector.protocol.FrameSender;
import com.google.dataconnector.protocol.FramingException;
import com.google.dataconnector.protocol.InputStreamConnector;
import com.google.dataconnector.protocol.OutputStreamConnector;
import com.google.dataconnector.protocol.proto.SdcFrame.FrameInfo;
import com.google.dataconnector.protocol.proto.SdcFrame.SocketDataInfo;
import com.google.dataconnector.util.LocalConf;

import com.google.gdata.util.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;

import com.google.protobuf.InvalidProtocolBufferException;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.SocketFactory;

public class SocksDataHandler implements Dispatchable {
  
  public static Logger LOG = Logger.getLogger(SocksDataHandler.class);

  // Injected dependencies
  private SocketFactory socketFactory;
  private LocalConf localConf;
  private InetAddress localHostAddress;
  private ThreadPoolExecutor threadPoolExecutor;
  private Injector injector; 
  
  // Runtime dependencies
  private FrameSender frameSender;
  
  // Local fields
  private ConcurrentMap<Integer, BlockingQueue<SocketDataInfo>> queueMap;

  public interface ConnectionStateUpdatable {
     public void removeConnection(int connectionId);
  }
     
  @Inject
  public SocksDataHandler(LocalConf localConf, SocketFactory socketFactory, 
      @Named("localhost") InetAddress localHostAddress, ThreadPoolExecutor threadPoolExecutor, 
      Injector injector) {
    
    queueMap = new ConcurrentHashMap<Integer, BlockingQueue<SocketDataInfo>>();
    this.localConf = localConf;
    this.socketFactory = socketFactory; 
    this.localHostAddress = localHostAddress;
    this.threadPoolExecutor = threadPoolExecutor;
    this.injector = injector;
  }
  
  @Override
  public void dispatch(FrameInfo frameInfo) throws FramingException {
    Preconditions.checkNotNull(frameSender, "Must define frameSender before calling dispatch");
    try {
      SocketDataInfo socketDataInfo = SocketDataInfo.parseFrom(frameInfo.getPayload());
      int connectionId = (int) socketDataInfo.getConnectionId();
      
      // Handle incoming start request.
      if (socketDataInfo.getState() == SocketDataInfo.State.START) {
        LOG.info("Starting new connection");
        Socket socket = socketFactory.createSocket();
        socket.connect(new InetSocketAddress(localHostAddress, localConf.getSocksServerPort()));

        ConnectionRemover connectionRemoverCallback = new ConnectionRemover();
        
        InputStreamConnector inputStreamConnector = 
            injector.getInstance(InputStreamConnector.class);
        inputStreamConnector.setConnectionId(connectionId);
        inputStreamConnector.setInputStream(socket.getInputStream());
        inputStreamConnector.setFrameSender(frameSender);
        inputStreamConnector.setConnectorStateCallback(connectionRemoverCallback);
        inputStreamConnector.setName("Inputconnector-" + connectionId);

        OutputStreamConnector outputStreamConnector = 
            injector.getInstance(OutputStreamConnector.class);
        outputStreamConnector.setConnectionId(connectionId);
        outputStreamConnector.setOutputStream(socket.getOutputStream());
        outputStreamConnector.setConnectorStateCallback(connectionRemoverCallback);
        outputStreamConnector.setName("Outputconnector-" + connectionId);
        queueMap.put(connectionId, outputStreamConnector.getQueue());

        // Start threads 
        threadPoolExecutor.execute(inputStreamConnector);
        threadPoolExecutor.execute(outputStreamConnector);
        LOG.debug("active thread count = " + Thread.activeCount());
      // Deal with continuing connections.
      } else {
        if (queueMap.containsKey((int) socketDataInfo.getConnectionId())) {
          queueMap.get(connectionId).put(socketDataInfo);
        }
      }
    } catch (InvalidProtocolBufferException e) {
      throw new FramingException(e);
    } catch (IOException e) {
      // TODO(rayc) Later on do something more intelligent such as reject this request not kill
      // the tunnel.
      throw new FramingException(e);
    } catch (InterruptedException e) {
      throw new FramingException(e);
    } catch (RejectedExecutionException e){
      while (true) {
        LOG.warn("Out of threads, waiting for some to free up.  Total active " + 
            threadPoolExecutor.getActiveCount());
        if (threadPoolExecutor.getMaximumPoolSize() - threadPoolExecutor.getActiveCount() > 10) {
          break;
        }
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          LOG.warn("Interrupted", e);
          return;
        }
      }
    }
  }

  public void setFrameSender(FrameSender frameSender) {
    this.frameSender = frameSender;
  }
  
  /**
   * Provides callback for InputStreamConnector and OutputStreamConnector for when connection state
   * changes on input or output streams.
   * 
   * @author rayc@google.com (Ray Colline)
   */
  public class ConnectionRemover implements ConnectorStateCallback {
    
    @Override
    public void close(int connectionId) {
      // We never know if the input or output side will detect closure first.
      // We defensively call from both sides.  In the event we are called twice we check to see
      // if we have already cleaned up.
      if (queueMap.containsKey(connectionId)) {
        // We tell the output thread to give up by placing a final CLOSE SocketData.
        queueMap.get(connectionId).add(SocketDataInfo.newBuilder()
            .setState(SocketDataInfo.State.CLOSE)
            .setConnectionId(connectionId).build());
        queueMap.remove(connectionId);
      }
    }
  }
} 