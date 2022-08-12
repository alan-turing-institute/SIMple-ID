package org.globalplatform;
import javacard.framework.Shareable;
public interface CVM extends Shareable {
boolean isActive();
boolean isSubmitted();
boolean isVerified();
boolean isBlocked();
byte getTriesRemaining();
boolean update(byte[] param1, short param2, byte param3, byte param4);
boolean resetState();
boolean blockState();
boolean resetAndUnblockState();
boolean setTryLimit(byte param1);
short verify(byte[] param1, short param2, byte param3, byte param4);
}
