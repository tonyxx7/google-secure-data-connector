// Copyright 2008 Google Inc. All Rights Reserved.

package com.google.dataconnector.client;

import java.lang.Thread.UncaughtExceptionHandler;

import com.google.pinky.ServiceHandler;

/**
 *
 * @author mtp@google.com (Matt T. Proud)
 */
public class ClientHealth  implements ServiceHandler {

  private static final int BYTE_TRANSFERRED_CHECK_INTERVAL = 2 * 60 * 1000;
  private boolean lastCheckSufficient = false;
  private long currentBytesTransferredCount;
  private long lastBytesTransferredCount;
  private long nextBytesTransferredCheck;

  private final SecureDataConnection secureDataConnection;

  public ClientHealth(final SecureDataConnection secureDataConnection) {
    this.secureDataConnection = secureDataConnection;
  }

  private boolean sufficientDataTransferred() {
    boolean sufficient;
    long currentTime = System.currentTimeMillis();
    currentBytesTransferredCount = secureDataConnection.getCurrentTransferCount();
    sufficient = (currentBytesTransferredCount > lastBytesTransferredCount);
    if (currentTime > nextBytesTransferredCheck) {
      /* If the current time has surpassed the next transfer check, ensure that current the data 
       * transfer count has exceeded the past count. The fulfillment of this condition is later
       * stored, and the next check time will be pushed ahead.
       */
      nextBytesTransferredCheck = currentTime + BYTE_TRANSFERRED_CHECK_INTERVAL;
      lastBytesTransferredCount = currentBytesTransferredCount;
      lastCheckSufficient = sufficient;
      return sufficient;
    } else if (!lastCheckSufficient) {
      /* If the last check condition is insufficient and too little time has passed, re-run the
       * check in case something is flapping.
       */
      nextBytesTransferredCheck = 0;
      return sufficientDataTransferred();
    } else {
      return true;
    }
  }

  public void shutdown() {
    throw new RuntimeException("Not implemented.");
  }

  public String getStatus() {
    String message = (sufficientDataTransferred() ? "OK" : "NOT OK");
    return message + " " + currentBytesTransferredCount + " transferred";
  }

  public long getErrno() {
    return (sufficientDataTransferred() ? 0 : 1);
  }

  public void setDrainMode(boolean state) {
    throw new RuntimeException("Not implemented.");
  }

  public String getName() {
    return "Secure Link Client";
  }

  public UncaughtExceptionHandler getUncaughtExceptionHandler() {
    return null;
  }
}
