package com.linkedin.venice.listener;

import static com.linkedin.venice.router.api.VenicePathParser.TYPE_STORAGE;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.linkedin.davinci.compression.StorageEngineBackedCompressorFactory;
import com.linkedin.davinci.config.VeniceServerConfig;
import com.linkedin.davinci.kafka.consumer.PartitionConsumptionState;
import com.linkedin.davinci.listener.response.AdminResponse;
import com.linkedin.davinci.storage.DiskHealthCheckService;
import com.linkedin.davinci.storage.MetadataRetriever;
import com.linkedin.davinci.storage.StorageEngineRepository;
import com.linkedin.davinci.store.AbstractStorageEngine;
import com.linkedin.davinci.store.record.ValueRecord;
import com.linkedin.davinci.store.rocksdb.RocksDBServerConfig;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.listener.request.AdminRequest;
import com.linkedin.venice.listener.request.GetRouterRequest;
import com.linkedin.venice.listener.request.HealthCheckRequest;
import com.linkedin.venice.listener.response.HttpShortcutResponse;
import com.linkedin.venice.listener.response.StorageResponseObject;
import com.linkedin.venice.meta.QueryAction;
import com.linkedin.venice.meta.ReadOnlySchemaRepository;
import com.linkedin.venice.meta.ReadOnlyStoreRepository;
import com.linkedin.venice.meta.ServerAdminAction;
import com.linkedin.venice.meta.Store;
import com.linkedin.venice.offsets.OffsetRecord;
import com.linkedin.venice.serialization.avro.AvroProtocolDefinition;
import com.linkedin.venice.utils.TestUtils;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.testng.Assert;
import org.testng.annotations.Test;


public class StorageReadRequestsHandlerTest {
  @Test
  public static void storageExecutionHandlerPassesRequestsAndGeneratesResponses() throws Exception {
    String topic = "temp-test-topic_v1";
    String keyString = "testkey";
    String valueString = "testvalue";
    int schemaId = 1;
    int partition = 3;
    long expectedOffset = 12345L;
    List<Object> outputArray = new ArrayList<Object>();
    byte[] valueBytes = ValueRecord.create(schemaId, valueString.getBytes()).serialize();
    // [0]""/[1]"action"/[2]"store"/[3]"partition"/[4]"key"
    String uri = "/" + TYPE_STORAGE + "/" + topic + "/" + partition + "/" + keyString;
    HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    GetRouterRequest testRequest = GetRouterRequest.parseGetHttpRequest(httpRequest);

    AbstractStorageEngine testStore = mock(AbstractStorageEngine.class);
    doReturn(valueBytes).when(testStore).get(partition, ByteBuffer.wrap(keyString.getBytes()));

    StorageEngineRepository testRepository = mock(StorageEngineRepository.class);
    doReturn(testStore).when(testRepository).getLocalStorageEngine(topic);

    MetadataRetriever mockMetadataRetriever = mock(MetadataRetriever.class);

    ReadOnlySchemaRepository schemaRepo = mock(ReadOnlySchemaRepository.class);
    VeniceServerConfig serverConfig = mock(VeniceServerConfig.class);
    RocksDBServerConfig dbServerConfig = mock(RocksDBServerConfig.class);
    doReturn(dbServerConfig).when(serverConfig).getRocksDBServerConfig();

    ReadOnlyStoreRepository metadataRepo = mock(ReadOnlyStoreRepository.class);
    Store store = mock(Store.class);
    when(store.getVersion(anyInt())).thenReturn(Optional.empty());
    when(metadataRepo.getStoreOrThrow(anyString())).thenReturn(store);

    ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
    doReturn(new UnpooledByteBufAllocator(true)).when(mockCtx).alloc();
    when(mockCtx.writeAndFlush(any())).then(i -> {
      outputArray.add(i.getArguments()[0]);
      return null;
    });

    ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2));

    try {
      StorageReadRequestsHandler testHandler = new StorageReadRequestsHandler(
          threadPoolExecutor,
          threadPoolExecutor,
          testRepository,
          metadataRepo,
          schemaRepo,
          mockMetadataRetriever,
          null,
          false,
          false,
          10,
          serverConfig,
          mock(StorageEngineBackedCompressorFactory.class),
          Optional.empty());
      testHandler.channelRead(mockCtx, testRequest);

      waitUntilStorageExecutionHandlerRespond(outputArray);

      // parsing of response
      Assert.assertEquals(outputArray.size(), 1);
      StorageResponseObject obj = (StorageResponseObject) outputArray.get(0);
      byte[] response = obj.getValueRecord().getDataInBytes();

      // Verification
      Assert.assertEquals(response, valueString.getBytes());
      Assert.assertEquals(obj.getValueRecord().getSchemaId(), schemaId);
    } finally {
      TestUtils.shutdownExecutor(threadPoolExecutor);
    }
  }

  @Test
  public void testDiskHealthCheckService() throws Exception {
    DiskHealthCheckService healthCheckService = mock(DiskHealthCheckService.class);
    doReturn(true).when(healthCheckService).isDiskHealthy();

    ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2));
    try {
      StorageEngineRepository testRepository = mock(StorageEngineRepository.class);
      ReadOnlyStoreRepository metadataRepo = mock(ReadOnlyStoreRepository.class);
      ReadOnlySchemaRepository schemaRepo = mock(ReadOnlySchemaRepository.class);
      MetadataRetriever mockMetadataRetriever = mock(MetadataRetriever.class);
      VeniceServerConfig serverConfig = mock(VeniceServerConfig.class);
      RocksDBServerConfig dbServerConfig = mock(RocksDBServerConfig.class);
      doReturn(dbServerConfig).when(serverConfig).getRocksDBServerConfig();

      StorageReadRequestsHandler testHandler = new StorageReadRequestsHandler(
          threadPoolExecutor,
          threadPoolExecutor,
          testRepository,
          metadataRepo,
          schemaRepo,
          mockMetadataRetriever,
          healthCheckService,
          false,
          false,
          10,
          serverConfig,
          mock(StorageEngineBackedCompressorFactory.class),
          Optional.empty());

      ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
      doReturn(new UnpooledByteBufAllocator(true)).when(mockCtx).alloc();
      List<Object> outputs = new ArrayList<Object>();
      when(mockCtx.writeAndFlush(any())).then(i -> {
        outputs.add(i.getArguments()[0]);
        return null;
      });
      HealthCheckRequest healthCheckRequest = new HealthCheckRequest();

      testHandler.channelRead(mockCtx, healthCheckRequest);
      waitUntilStorageExecutionHandlerRespond(outputs);

      Assert.assertTrue(outputs.get(0) instanceof HttpShortcutResponse);
      HttpShortcutResponse healthCheckResponse = (HttpShortcutResponse) outputs.get(0);
      Assert.assertEquals(healthCheckResponse.getStatus(), HttpResponseStatus.OK);
    } finally {
      TestUtils.shutdownExecutor(threadPoolExecutor);
    }
  }

  private static void waitUntilStorageExecutionHandlerRespond(List<Object> outputs) throws Exception {
    // Wait for async stuff to finish
    int count = 1;
    while (outputs.size() < 1) {
      Thread.sleep(10); // on my machine, consistently fails with only 10ms, intermittent at 15ms, success at 20ms
      count += 1;
      if (count > 200) { // two seconds
        throw new RuntimeException("Timeout waiting for StorageExecutionHandler output to appear");
      }
    }
  }

  @Test
  public static void storageExecutionHandlerLogsExceptions() throws Exception {
    String topic = "temp-test-topic_v1";
    String keyString = "testkey";
    String valueString = "testvalue";
    int schemaId = 1;
    int partition = 3;
    long expectedOffset = 12345L;
    List<Object> outputArray = new ArrayList<Object>();
    byte[] valueBytes = ValueRecord.create(schemaId, valueString.getBytes()).serialize();
    // [0]""/[1]"action"/[2]"store"/[3]"partition"/[4]"key"
    String uri = "/" + TYPE_STORAGE + "/" + topic + "/" + partition + "/" + keyString;
    HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    GetRouterRequest testRequest = GetRouterRequest.parseGetHttpRequest(httpRequest);

    AbstractStorageEngine testStore = mock(AbstractStorageEngine.class);
    doReturn(valueBytes).when(testStore).get(partition, ByteBuffer.wrap(keyString.getBytes()));

    StorageEngineRepository testRepository = mock(StorageEngineRepository.class);

    final String exceptionMessage = "Exception thrown in Mock";
    // Forcing an exception to be thrown.
    doThrow(new VeniceException(exceptionMessage)).when(testRepository).getLocalStorageEngine(topic);

    MetadataRetriever mockMetadataRetriever = mock(MetadataRetriever.class);

    ReadOnlySchemaRepository schemaRepo = mock(ReadOnlySchemaRepository.class);
    ReadOnlyStoreRepository metadataRepo = mock(ReadOnlyStoreRepository.class);
    Store store = mock(Store.class);
    when(store.getVersion(anyInt())).thenReturn(Optional.empty());
    when(metadataRepo.getStoreOrThrow(anyString())).thenReturn(store);
    VeniceServerConfig serverConfig = mock(VeniceServerConfig.class);
    RocksDBServerConfig dbServerConfig = mock(RocksDBServerConfig.class);
    doReturn(dbServerConfig).when(serverConfig).getRocksDBServerConfig();

    ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
    doReturn(new UnpooledByteBufAllocator(true)).when(mockCtx).alloc();
    when(mockCtx.writeAndFlush(any())).then(i -> {
      outputArray.add(i.getArguments()[0]);
      return null;
    });

    ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2));
    try {
      AtomicInteger errorLogCount = new AtomicInteger();
      // Adding a custom appender to track the count of error logs we are interested in
      ErrorCountAppender errorCountAppender = new ErrorCountAppender.Builder().setErrorMessageCounter(errorLogCount)
          .setExceptionMessage(exceptionMessage)
          .build();
      errorCountAppender.start();

      LoggerContext ctx = ((LoggerContext) LogManager.getContext(false));
      Configuration config = ctx.getConfiguration();
      config.addLoggerAppender(
          (org.apache.logging.log4j.core.Logger) LogManager.getLogger(StorageReadRequestsHandler.class),
          errorCountAppender);

      // Actual test
      StorageReadRequestsHandler testHandler = new StorageReadRequestsHandler(
          threadPoolExecutor,
          threadPoolExecutor,
          testRepository,
          metadataRepo,
          schemaRepo,
          mockMetadataRetriever,
          null,
          false,
          false,
          10,
          serverConfig,
          mock(StorageEngineBackedCompressorFactory.class),
          Optional.empty());
      testHandler.channelRead(mockCtx, testRequest);

      waitUntilStorageExecutionHandlerRespond(outputArray);

      // parsing of response
      Assert.assertEquals(outputArray.size(), 1);
      HttpShortcutResponse obj = (HttpShortcutResponse) outputArray.get(0);

      // Verification
      Assert.assertEquals(obj.getStatus(), HttpResponseStatus.INTERNAL_SERVER_ERROR);
      Assert.assertEquals(obj.getMessage(), exceptionMessage);

      // Asserting that the exception got logged
      Assert.assertTrue(errorLogCount.get() > 0);
    } finally {
      TestUtils.shutdownExecutor(threadPoolExecutor);
    }
  }

  @Test
  public static void testAdminRequestsPassInStorageExecutionHandler() throws Exception {
    String topic = "test_store_v1";
    int expectedPartitionId = 12345;
    List<Object> outputArray = new ArrayList<Object>();

    // [0]""/[1]"action"/[2]"store_version"/[3]"dump_ingestion_state"
    String uri = "/" + QueryAction.ADMIN.toString().toLowerCase() + "/" + topic + "/"
        + ServerAdminAction.DUMP_INGESTION_STATE.toString();
    HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
    AdminRequest testRequest = AdminRequest.parseAdminHttpRequest(httpRequest);

    // Mock the AdminResponse from ingestion task
    AdminResponse expectedAdminResponse = new AdminResponse();
    PartitionConsumptionState state = new PartitionConsumptionState(
        expectedPartitionId,
        1,
        new OffsetRecord(AvroProtocolDefinition.PARTITION_STATE.getSerializer()),
        false);
    expectedAdminResponse.addPartitionConsumptionState(state);

    MetadataRetriever mockMetadataRetriever = mock(MetadataRetriever.class);
    doReturn(expectedAdminResponse).when(mockMetadataRetriever).getConsumptionSnapshots(eq(topic), any());

    /**
     * Capture the output written by {@link StorageReadRequestsHandler}
     */
    ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
    doReturn(new UnpooledByteBufAllocator(true)).when(mockCtx).alloc();
    when(mockCtx.writeAndFlush(any())).then(i -> {
      outputArray.add(i.getArguments()[0]);
      return null;
    });

    ThreadPoolExecutor threadPoolExecutor =
        new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(2));
    try {
      VeniceServerConfig serverConfig = mock(VeniceServerConfig.class);
      RocksDBServerConfig dbServerConfig = mock(RocksDBServerConfig.class);
      doReturn(dbServerConfig).when(serverConfig).getRocksDBServerConfig();

      // Actual test
      StorageReadRequestsHandler testHandler = new StorageReadRequestsHandler(
          threadPoolExecutor,
          threadPoolExecutor,
          mock(StorageEngineRepository.class),
          mock(ReadOnlyStoreRepository.class),
          mock(ReadOnlySchemaRepository.class),
          mockMetadataRetriever,
          null,
          false,
          false,
          10,
          serverConfig,
          mock(StorageEngineBackedCompressorFactory.class),
          Optional.empty());
      testHandler.channelRead(mockCtx, testRequest);

      waitUntilStorageExecutionHandlerRespond(outputArray);

      // parsing of response
      Assert.assertEquals(outputArray.size(), 1);
      Assert.assertTrue(outputArray.get(0) instanceof AdminResponse);
      AdminResponse obj = (AdminResponse) outputArray.get(0);

      // Verification
      Assert.assertEquals(obj.getResponseRecord().partitionConsumptionStates.size(), 1);
      Assert.assertEquals(obj.getResponseRecord().partitionConsumptionStates.get(0).partitionId, expectedPartitionId);
      Assert.assertEquals(
          obj.getResponseSchemaIdHeader(),
          AvroProtocolDefinition.SERVER_ADMIN_RESPONSE_V1.getCurrentProtocolVersion());
    } finally {
      TestUtils.shutdownExecutor(threadPoolExecutor);
    }
  }
}
