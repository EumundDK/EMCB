package com.example.emcb.NFCTag;

import com.st.st25sdk.STLog;
import com.st.st25sdk.ftmprotocol.FtmProtocol;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

public class MyTagProtocol {

    public MyTagProtocol(ST25DVTag st25DVTag) {

    }

//    private void createThread() {
//        (new Thread(() -> {
//            STLog.i("FtmProtocol thread created");
//            this.mFtmTheadRunning = true;
//
//            while(this.mFtmTheadRunning) {
//                try {
//                    if (this.mCancelCurrentTransfer && this.mState != FtmProtocol.State.IDLE) {
//                        STLog.w("Transmission cancelled");
//                        this.mCancelCurrentTransfer = false;
//                        this.finalizeTransfer(FtmProtocol.TransferStatus.TRANSFER_CANCELLED, (byte[])null);
//                    }
//
//                    if (this.mPauseCurrentTransfer) {
//                        this.sleepInMs(this.mSleepTimeInMsWhenPaused);
//                    } else {
//                        switch(this.mState) {
//                            case IDLE:
//                            default:
//                                this.ftmSemaphore.acquire();
//                                STLog.i("FTM Semaphore received");
//                                break;
//                            case PREPARE_PACKET:
//                                this.sendNextPacket();
//                                this.sleepBetweenConsecutiveCommands();
//                                break;
//                            case SEND_PACKET:
//                                this.resendPacket();
//                                this.sleepBetweenConsecutiveCommands();
//                                break;
//                            case WAIT_FOR_ACK:
//                                this.waitForAcknowledge();
//                                if (this.mState == FtmProtocol.State.WAIT_FOR_ACK) {
//                                    this.sleepWhenWaitingForAck();
//                                }
//                                break;
//                            case RECEIVE_PACKET:
//                                if (this.isMailboxAvailableForReading()) {
//                                    this.resetTimeOfFirstError();
//                                    this.receiveNextPacket();
//                                } else {
//                                    long timeSinceFirstError = this.getTimeSinceFirstError();
//                                    if (timeSinceFirstError > (long)this.mTimeOutInMs) {
//                                        this.handleFatalError("Error! Timeout when waiting data from MCU");
//                                    }
//                                }
//
//                                this.sleepBetweenConsecutiveCommands();
//                                break;
//                            case SEND_ACK:
//                                this.sendAcknowledge();
//                        }
//                    }
//                } catch (InterruptedException var3) {
//                    var3.printStackTrace();
//                    this.handleFatalError("Thread sleep interrupted");
//                }
//            }
//
//            STLog.i("FtmProtocol thread stopped");
//        })).start();
//    }
}
