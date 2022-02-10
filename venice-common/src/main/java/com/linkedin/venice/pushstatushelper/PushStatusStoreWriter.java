package com.linkedin.venice.pushstatushelper;

import com.linkedin.venice.common.PushStatusStoreUtils;
import com.linkedin.venice.pushmonitor.ExecutionStatus;
import com.linkedin.venice.pushstatus.NoOp;
import com.linkedin.venice.pushstatus.PushStatusKey;
import com.linkedin.venice.pushstatus.PushStatusValue;
import com.linkedin.venice.pushstatus.PushStatusValueWriteOpRecord;
import com.linkedin.venice.pushstatus.HighWatermarksMapOps;
import com.linkedin.venice.pushstatus.instancesMapOps;
import com.linkedin.venice.serialization.avro.AvroProtocolDefinition;
import com.linkedin.venice.writer.VeniceWriter;
import com.linkedin.venice.writer.VeniceWriterFactory;
import java.util.Collections;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * PushStatusStoreWriter is a helper class for Da Vinci to write PushStatus and heartbeat message into PushStatus store
 * real-time topic.
 * Heartbeat update is a normal Venice write.
 * PushStatus update is via a map-merge of Write-Compute.
 */
public class PushStatusStoreWriter implements AutoCloseable {
  private static final Logger logger = LogManager.getLogger(PushStatusStoreWriter.class);
  private final String instanceName;
  private final PushStatusStoreVeniceWriterCache veniceWriterCache;
  private final int derivedSchemaId;

  /**
   * @param writerFactory Used for instantiate veniceWriterCache
   * @param instanceName format = hostAddress,appName
   * @param derivedSchemaId writeCompute schema for updating push status
   */
  public PushStatusStoreWriter(VeniceWriterFactory writerFactory, String instanceName, int derivedSchemaId) {
    this.veniceWriterCache = new PushStatusStoreVeniceWriterCache(writerFactory);
    this.instanceName = instanceName;
    this.derivedSchemaId = derivedSchemaId;
  }

  public void writeHeartbeat(String storeName) {
    VeniceWriter writer = veniceWriterCache.prepareVeniceWriter(storeName);
    PushStatusKey pushStatusKey = PushStatusStoreUtils.getHeartbeatKey(instanceName);
    PushStatusValue pushStatusValue = new PushStatusValue();
    pushStatusValue.reportTimestamp = System.currentTimeMillis();
    pushStatusValue.instances = Collections.emptyMap();
    logger.info("Sending heartbeat of " + instanceName);
    writer.put(pushStatusKey, pushStatusValue, AvroProtocolDefinition.PUSH_STATUS_SYSTEM_SCHEMA_STORE.getCurrentProtocolVersion());
  }

  public void writePushStatus(String storeName, int version, int partitionId, ExecutionStatus status) {
    writePushStatus(storeName, version, partitionId, status, Optional.empty());
  }

  public void writePushStatus(String storeName, int version, int partitionId, ExecutionStatus status, Optional<String> incrementalPushVersion) {
    writePushStatus(storeName, version, partitionId, status, incrementalPushVersion, Optional.empty());
  }

  public void writePushStatus(String storeName, int version, int partitionId, ExecutionStatus status, Optional<String> incrementalPushVersion, Optional<String> incrementalPushPrefix) {
    VeniceWriter writer = veniceWriterCache.prepareVeniceWriter(storeName);
    PushStatusKey pushStatusKey = PushStatusStoreUtils.getPushKey(version, partitionId, incrementalPushVersion, incrementalPushPrefix);
    PushStatusValueWriteOpRecord writeComputeRecord = new PushStatusValueWriteOpRecord();
    instancesMapOps instances = new instancesMapOps();
    instances.mapUnion = Collections.singletonMap(instanceName, status.getValue());
    instances.mapDiff = Collections.emptyList();
    writeComputeRecord.instances = instances;
    writeComputeRecord.highWatermarks = new NoOp();
    writeComputeRecord.reportTimestamp = new NoOp();
    logger.info("Updating pushStatus of {} to {}. storeName: {} , version: {} , partition: {}",
        instanceName, status.name(), storeName, version, partitionId);
    writer.update(pushStatusKey, writeComputeRecord, AvroProtocolDefinition.PUSH_STATUS_SYSTEM_SCHEMA_STORE.getCurrentProtocolVersion(), derivedSchemaId, null);
  }

  /* This method is called only when END_OF_INCREMENTAL_PUSH_RECEIVED */
  public void writeHighWatermark(String storeName, int version, int partitionId, long highWatermark,
      Optional<String> incrementalPushVersion, Optional<String> incrementalPushPrefix) {
    VeniceWriter writer = veniceWriterCache.prepareVeniceWriter(storeName);
    PushStatusKey pushStatusKey = PushStatusStoreUtils.getPushKey(version, partitionId, incrementalPushVersion, incrementalPushPrefix);
    PushStatusValueWriteOpRecord writeComputeRecord = new PushStatusValueWriteOpRecord();
    // execution status
    instancesMapOps instances = new instancesMapOps();
    instances.mapUnion = Collections.singletonMap(instanceName, ExecutionStatus.END_OF_INCREMENTAL_PUSH_RECEIVED.getValue());
    instances.mapDiff = Collections.emptyList();
    writeComputeRecord.instances = instances;
    // high watermark
    HighWatermarksMapOps highWatermarks = new HighWatermarksMapOps();
    highWatermarks.mapUnion = Collections.singletonMap(instanceName, highWatermark);
    highWatermarks.mapDiff = Collections.emptyList();
    writeComputeRecord.highWatermarks = highWatermarks;
    // heartbeat
    writeComputeRecord.reportTimestamp = new NoOp();
    logger.info("Updating pushStatus of {} to {}. highWatermark: {} storeName: {} , version: {} , partition: {} ",
        instanceName,  ExecutionStatus.END_OF_INCREMENTAL_PUSH_RECEIVED.name(), highWatermark, storeName, version, partitionId);
    writer.update(pushStatusKey, writeComputeRecord, AvroProtocolDefinition.PUSH_STATUS_SYSTEM_SCHEMA_STORE.getCurrentProtocolVersion(), derivedSchemaId, null);
  }

  @Override
  public void close() {
    veniceWriterCache.close();
  }
}
