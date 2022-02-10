package com.linkedin.venice.common;

import com.linkedin.venice.pushstatus.PushStatusKey;
import java.util.Arrays;
import java.util.Optional;


/**
 * Helper class for PushStatusStore related logic.
 */
public class PushStatusStoreUtils {

  public static final String SERVER_INCREMENTAL_PUSH_PREFIX = "SERVER_SIDE_INCREMENTAL_PUSH_STATUS";

  public enum PushStatusKeyType {
    HEARTBEAT, FULL_PUSH, INCREMENTAL_PUSH, SERVER_INCREMENTAL_PUSH;
  }

  public static PushStatusKey getHeartbeatKey(String instanceName) {
    PushStatusKey pushStatusKey = new PushStatusKey();
    pushStatusKey.keyStrings = Arrays.asList(instanceName);
    pushStatusKey.messageType = PushStatusKeyType.HEARTBEAT.ordinal();
    return pushStatusKey;
  }

  public static PushStatusKey getPushKey(int version, int partitionId, Optional<String> incrementalPushVersion) {
    return getPushKey(version, partitionId, incrementalPushVersion, Optional.empty());
  }

  public static PushStatusKey getPushKey(int version, int partitionId,
      Optional<String> incrementalPushVersion, Optional<String> incrementalPushPrefix) {
    if (incrementalPushVersion.isPresent()) {
      if (incrementalPushPrefix.isPresent()) {
        return getServerIncrementalPushKey(version, partitionId, incrementalPushVersion.get(), incrementalPushPrefix.get());
      }
      return getIncrementalPushKey(version, partitionId, incrementalPushVersion.get());
    }
    return getFullPushKey(version, partitionId);
  }

  public static PushStatusKey getFullPushKey(int version, int partitionId) {
    PushStatusKey pushStatusKey = new PushStatusKey();
    pushStatusKey.keyStrings = Arrays.asList(version, partitionId);
    pushStatusKey.messageType = PushStatusKeyType.FULL_PUSH.ordinal();
    return pushStatusKey;
  }

  public static PushStatusKey getIncrementalPushKey(int version, int partitionId, String incrementalPushVersion) {
    PushStatusKey pushStatusKey = new PushStatusKey();
    pushStatusKey.keyStrings = Arrays.asList(version, partitionId, incrementalPushVersion);
    pushStatusKey.messageType = PushStatusKeyType.INCREMENTAL_PUSH.ordinal();
    return pushStatusKey;
  }

  public static PushStatusKey getServerIncrementalPushKey(int version, int partitionId,
      String incrementalPushVersion, String incrementalPushPrefix) {
    PushStatusKey pushStatusKey = new PushStatusKey();
    pushStatusKey.keyStrings = Arrays.asList(version, partitionId, incrementalPushVersion, incrementalPushPrefix);
    pushStatusKey.messageType = PushStatusKeyType.SERVER_INCREMENTAL_PUSH.ordinal();
    return pushStatusKey;
  }
}
