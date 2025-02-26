package com.linkedin.davinci.kafka.consumer;

import static com.linkedin.venice.ConfigConstants.DEFAULT_KAFKA_BATCH_SIZE;
import static com.linkedin.venice.ConfigConstants.DEFAULT_KAFKA_LINGER_MS;
import static com.linkedin.venice.ConfigConstants.DEFAULT_KAFKA_SSL_CONTEXT_PROVIDER_CLASS_NAME;
import static com.linkedin.venice.ConfigKeys.KAFKA_BATCH_SIZE;
import static com.linkedin.venice.ConfigKeys.KAFKA_LINGER_MS;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static org.apache.kafka.common.config.SslConfigs.SSL_CONTEXT_PROVIDER_CLASS_CONFIG;

import com.linkedin.davinci.compression.StorageEngineBackedCompressorFactory;
import com.linkedin.davinci.config.VeniceConfigLoader;
import com.linkedin.davinci.config.VeniceServerConfig;
import com.linkedin.davinci.config.VeniceStoreVersionConfig;
import com.linkedin.davinci.helix.LeaderFollowerPartitionStateModel;
import com.linkedin.davinci.listener.response.AdminResponse;
import com.linkedin.davinci.notifier.LogNotifier;
import com.linkedin.davinci.notifier.MetaSystemStoreReplicaStatusNotifier;
import com.linkedin.davinci.notifier.VeniceNotifier;
import com.linkedin.davinci.stats.AggHostLevelIngestionStats;
import com.linkedin.davinci.stats.AggLagStats;
import com.linkedin.davinci.stats.AggVersionedDIVStats;
import com.linkedin.davinci.stats.AggVersionedIngestionStats;
import com.linkedin.davinci.stats.ParticipantStoreConsumptionStats;
import com.linkedin.davinci.stats.StoreBufferServiceStats;
import com.linkedin.davinci.storage.StorageEngineRepository;
import com.linkedin.davinci.storage.StorageMetadataService;
import com.linkedin.davinci.store.cache.backend.ObjectCacheBackend;
import com.linkedin.venice.client.store.ClientConfig;
import com.linkedin.venice.common.VeniceSystemStoreType;
import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.exceptions.VeniceNoStoreException;
import com.linkedin.venice.helix.HelixReadOnlyZKSharedSchemaRepository;
import com.linkedin.venice.kafka.KafkaClientFactory.MetricsParameters;
import com.linkedin.venice.kafka.TopicManagerRepository;
import com.linkedin.venice.kafka.consumer.ApacheKafkaConsumer;
import com.linkedin.venice.kafka.protocol.state.PartitionState;
import com.linkedin.venice.meta.ClusterInfoProvider;
import com.linkedin.venice.meta.Instance;
import com.linkedin.venice.meta.ReadOnlyLiveClusterConfigRepository;
import com.linkedin.venice.meta.ReadOnlySchemaRepository;
import com.linkedin.venice.meta.ReadOnlyStoreRepository;
import com.linkedin.venice.meta.ServerAdminAction;
import com.linkedin.venice.meta.Store;
import com.linkedin.venice.meta.StoreDataChangedListener;
import com.linkedin.venice.meta.Version;
import com.linkedin.venice.meta.VersionStatus;
import com.linkedin.venice.offsets.OffsetRecord;
import com.linkedin.venice.schema.SchemaReader;
import com.linkedin.venice.serialization.KafkaKeySerializer;
import com.linkedin.venice.serialization.avro.InternalAvroSpecificSerializer;
import com.linkedin.venice.serialization.avro.OptimizedKafkaValueSerializer;
import com.linkedin.venice.service.AbstractVeniceService;
import com.linkedin.venice.service.ICProvider;
import com.linkedin.venice.stats.KafkaClientStats;
import com.linkedin.venice.system.store.MetaStoreWriter;
import com.linkedin.venice.throttle.EventThrottler;
import com.linkedin.venice.utils.ComplementSet;
import com.linkedin.venice.utils.DaemonThreadFactory;
import com.linkedin.venice.utils.DiskUsage;
import com.linkedin.venice.utils.LatencyUtils;
import com.linkedin.venice.utils.Pair;
import com.linkedin.venice.utils.PartitionUtils;
import com.linkedin.venice.utils.Time;
import com.linkedin.venice.utils.Utils;
import com.linkedin.venice.utils.VeniceProperties;
import com.linkedin.venice.utils.locks.AutoCloseableLock;
import com.linkedin.venice.utils.locks.ResourceAutoClosableLockManager;
import com.linkedin.venice.writer.ApacheKafkaProducer;
import com.linkedin.venice.writer.KafkaProducerWrapper;
import com.linkedin.venice.writer.SharedKafkaProducerService;
import com.linkedin.venice.writer.VeniceWriterFactory;
import io.tehuti.metrics.MetricsRepository;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Assumes: One to One mapping between a Venice Store and Kafka Topic.
 * Manages Kafka topics and partitions that need to be consumed for the stores on this node.
 *
 * Launches {@link StoreIngestionTask} for each store version to consume and process messages.
 *
 * Uses the "new" Kafka Consumer.
 */
public class KafkaStoreIngestionService extends AbstractVeniceService implements StoreIngestionService {
  private static final String GROUP_ID_FORMAT = "%s_%s";

  private static final Logger LOGGER = LogManager.getLogger(KafkaStoreIngestionService.class);

  private final VeniceConfigLoader veniceConfigLoader;

  private final Queue<VeniceNotifier> leaderFollowerNotifiers = new ConcurrentLinkedQueue<>();

  private final StorageMetadataService storageMetadataService;

  private final ReadOnlyStoreRepository metadataRepo;

  private final AggHostLevelIngestionStats hostLevelIngestionStats;

  private final AggVersionedIngestionStats versionedIngestionStats;

  private final AggLagStats aggLagStats;

  /**
   * Store buffer service to persist data into local bdb for all the stores.
   */
  private final AbstractStoreBufferService storeBufferService;

  private final AggKafkaConsumerService aggKafkaConsumerService;

  /**
   * A repository mapping each Kafka Topic to it corresponding Ingestion task responsible
   * for consuming messages and making changes to the local store accordingly.
   */
  private final NavigableMap<String, StoreIngestionTask> topicNameToIngestionTaskMap;

  private final Optional<SchemaReader> kafkaMessageEnvelopeSchemaReader;

  private final ExecutorService cacheWarmingExecutorService;

  private final MetaStoreWriter metaStoreWriter;

  private final MetaSystemStoreReplicaStatusNotifier metaSystemStoreReplicaStatusNotifier;

  private final StoreIngestionTaskFactory ingestionTaskFactory;

  private final boolean isIsolatedIngestion;

  private final TopicManagerRepository topicManagerRepository;
  private final TopicManagerRepository topicManagerRepositoryJavaBased;

  private ExecutorService participantStoreConsumerExecutorService;

  private ExecutorService ingestionExecutorService;

  private ParticipantStoreConsumptionTask participantStoreConsumptionTask;

  private boolean metaSystemStoreReplicaStatusNotifierQueued = false;
  // TODO: This could be a composite storage engine which keeps secondary storage engines updated in lockstep with a
  // primary
  // source. This could be a view of the data, or in our case a cache, or both potentially.
  private final Optional<ObjectCacheBackend> cacheBackend;

  private final SharedKafkaProducerService sharedKafkaProducerService;

  private final InternalAvroSpecificSerializer<PartitionState> partitionStateSerializer;

  private final StorageEngineBackedCompressorFactory compressorFactory;

  private final ResourceAutoClosableLockManager<String> topicLockManager;

  public KafkaStoreIngestionService(
      StorageEngineRepository storageEngineRepository,
      VeniceConfigLoader veniceConfigLoader,
      StorageMetadataService storageMetadataService,
      ClusterInfoProvider clusterInfoProvider,
      ReadOnlyStoreRepository metadataRepo,
      ReadOnlySchemaRepository schemaRepo,
      ReadOnlyLiveClusterConfigRepository liveClusterConfigRepository,
      MetricsRepository metricsRepository,
      Optional<SchemaReader> kafkaMessageEnvelopeSchemaReader,
      Optional<ClientConfig> clientConfig,
      InternalAvroSpecificSerializer<PartitionState> partitionStateSerializer,
      Optional<HelixReadOnlyZKSharedSchemaRepository> zkSharedSchemaRepository,
      ICProvider icProvider,
      boolean isIsolatedIngestion,
      StorageEngineBackedCompressorFactory compressorFactory,
      Optional<ObjectCacheBackend> cacheBackend,
      boolean isDaVinciClient,
      RemoteIngestionRepairService remoteIngestionRepairService) {
    this.cacheBackend = cacheBackend;
    this.storageMetadataService = storageMetadataService;
    this.metadataRepo = metadataRepo;
    this.topicNameToIngestionTaskMap = new ConcurrentSkipListMap<>();
    this.veniceConfigLoader = veniceConfigLoader;
    this.isIsolatedIngestion = isIsolatedIngestion;
    this.partitionStateSerializer = partitionStateSerializer;
    this.compressorFactory = compressorFactory;
    // Each topic that has any partition ingested by this class has its own lock.
    this.topicLockManager = new ResourceAutoClosableLockManager<>(ReentrantLock::new);

    VeniceServerConfig serverConfig = veniceConfigLoader.getVeniceServerConfig();
    ServerKafkaClientFactory veniceConsumerFactory = new ServerKafkaClientFactory(
        serverConfig,
        kafkaMessageEnvelopeSchemaReader,
        Optional.of(new MetricsParameters(ServerKafkaClientFactory.class.getSimpleName(), metricsRepository)));

    /**
     * This new veniceConsumerJavaBasedFactory (underneath it works with java based admin client only) is needed for leader_offset_lag metrics to work.
     * TODO: This should be removed once the VeniceServerConsumerFactory uses java based admin client in production reliably.
     */
    ServerJavaKafkaClientFactory veniceConsumerJavaBasedFactory = new ServerJavaKafkaClientFactory(
        serverConfig,
        kafkaMessageEnvelopeSchemaReader,
        Optional.of(new MetricsParameters(ServerJavaKafkaClientFactory.class.getSimpleName(), metricsRepository)));

    Properties veniceWriterProperties =
        veniceConfigLoader.getVeniceClusterConfig().getClusterProperties().toProperties();
    if (serverConfig.isKafkaOpenSSLEnabled()) {
      veniceWriterProperties
          .setProperty(SSL_CONTEXT_PROVIDER_CLASS_CONFIG, DEFAULT_KAFKA_SSL_CONTEXT_PROVIDER_CLASS_NAME);
    }

    /**
     * Setup default batch size and linger time for better producing performance during server new push ingestion.
     * These configs are set for large volume ingestion, not for integration test.
     */
    if (!veniceWriterProperties.containsKey(KAFKA_BATCH_SIZE)) {
      veniceWriterProperties.put(KAFKA_BATCH_SIZE, DEFAULT_KAFKA_BATCH_SIZE);
    }

    if (!veniceWriterProperties.containsKey(KAFKA_LINGER_MS)) {
      veniceWriterProperties.put(KAFKA_LINGER_MS, DEFAULT_KAFKA_LINGER_MS);
    }

    if (serverConfig.isSharedKafkaProducerEnabled()) {
      sharedKafkaProducerService = new SharedKafkaProducerService(
          veniceWriterProperties,
          serverConfig.getSharedProducerPoolSizePerKafkaCluster(),
          new SharedKafkaProducerService.KafkaProducerSupplier() {
            @Override
            public KafkaProducerWrapper getNewProducer(VeniceProperties props) {
              return new ApacheKafkaProducer(props);
            }
          },
          metricsRepository,
          serverConfig.getKafkaProducerMetrics());
    } else {
      sharedKafkaProducerService = null;
    }
    LOGGER.info(
        "Shared kafka producer service is {}",
        serverConfig.isSharedKafkaProducerEnabled() ? "enabled" : "disabled");

    VeniceWriterFactory veniceWriterFactory =
        new VeniceWriterFactory(veniceWriterProperties, Optional.ofNullable(sharedKafkaProducerService));
    VeniceWriterFactory veniceWriterFactoryForMetaStoreWriter = new VeniceWriterFactory(veniceWriterProperties);

    // Create Kafka client stats
    KafkaClientStats.registerKafkaClientStats(
        metricsRepository,
        "KafkaClientStats",
        Optional.ofNullable(sharedKafkaProducerService));

    EventThrottler bandwidthThrottler = new EventThrottler(
        serverConfig.getKafkaFetchQuotaBytesPerSecond(),
        serverConfig.getKafkaFetchQuotaTimeWindow(),
        "kafka_consumption_bandwidth",
        false,
        EventThrottler.BLOCK_STRATEGY);

    EventThrottler recordsThrottler = new EventThrottler(
        serverConfig.getKafkaFetchQuotaRecordPerSecond(),
        serverConfig.getKafkaFetchQuotaTimeWindow(),
        "kafka_consumption_records_count",
        false,
        EventThrottler.BLOCK_STRATEGY);

    EventThrottler unorderedBandwidthThrottler = new EventThrottler(
        serverConfig.getKafkaFetchQuotaUnorderedBytesPerSecond(),
        serverConfig.getKafkaFetchQuotaTimeWindow(),
        "kafka_consumption_unordered_bandwidth",
        false,
        EventThrottler.BLOCK_STRATEGY);

    EventThrottler unorderedRecordsThrottler = new EventThrottler(
        serverConfig.getKafkaFetchQuotaUnorderedRecordPerSecond(),
        serverConfig.getKafkaFetchQuotaTimeWindow(),
        "kafka_consumption_unordered_records_count",
        false,
        EventThrottler.BLOCK_STRATEGY);

    final Map<String, EventThrottler> kafkaUrlToRecordsThrottler;
    if (liveClusterConfigRepository != null) {
      Set<String> regionNames = serverConfig.getRegionNames();
      kafkaUrlToRecordsThrottler = new HashMap<>(regionNames.size());
      regionNames.forEach(region -> {
        kafkaUrlToRecordsThrottler.put(
            region,
            new EventThrottler(
                () -> (long) liveClusterConfigRepository.getConfigs()
                    .getServerKafkaFetchQuotaRecordsPerSecondForRegion(region),
                serverConfig.getKafkaFetchQuotaTimeWindow(),
                "kafka_consumption_records_count_" + region,
                true, // Check quota before recording since we buffer throttled records and don't send them to disk or
                      // kafka
                EventThrottler.REJECT_STRATEGY) // We want exceptions to be thrown when quota is exceeded
        );
      });
    } else {
      kafkaUrlToRecordsThrottler = Collections.emptyMap();
    }

    KafkaClusterBasedRecordThrottler kafkaClusterBasedRecordThrottler =
        new KafkaClusterBasedRecordThrottler(kafkaUrlToRecordsThrottler);

    this.topicManagerRepository = new TopicManagerRepository(
        veniceConsumerFactory.getKafkaBootstrapServers(),
        veniceConsumerFactory.getKafkaZkAddress(),
        veniceConsumerFactory,
        metricsRepository);

    this.topicManagerRepositoryJavaBased = new TopicManagerRepository(
        veniceConsumerFactory.getKafkaBootstrapServers(),
        veniceConsumerFactory.getKafkaZkAddress(),
        veniceConsumerJavaBasedFactory,
        metricsRepository);

    VeniceNotifier notifier = new LogNotifier();
    this.leaderFollowerNotifiers.add(notifier);

    /**
     * Only Venice SN will pass this param: {@param zkSharedSchemaRepository} since {@link metaStoreWriter} is
     * used to report the replica status from Venice SN.
     */
    if (zkSharedSchemaRepository.isPresent()) {
      this.metaStoreWriter = new MetaStoreWriter(
          topicManagerRepository.getTopicManager(),
          veniceWriterFactoryForMetaStoreWriter,
          zkSharedSchemaRepository.get());
      this.metaSystemStoreReplicaStatusNotifier = new MetaSystemStoreReplicaStatusNotifier(
          serverConfig.getClusterName(),
          metaStoreWriter,
          metadataRepo,
          Instance.fromHostAndPort(Utils.getHostName(), serverConfig.getListenerPort()));
      LOGGER.info("MetaSystemStoreReplicaStatusNotifier was initialized");
      metadataRepo.registerStoreDataChangedListener(new StoreDataChangedListener() {
        @Override
        public void handleStoreDeleted(Store store) {
          String storeName = store.getName();
          if (VeniceSystemStoreType.META_STORE.equals(VeniceSystemStoreType.getSystemStoreType(storeName))) {
            metaStoreWriter.removeMetaStoreWriter(storeName);
            LOGGER.info("MetaSystemWriter for meta store: {} got removed.", storeName);
          }
        }
      });
    } else {
      this.metaStoreWriter = null;
      this.metaSystemStoreReplicaStatusNotifier = null;
    }

    this.hostLevelIngestionStats = new AggHostLevelIngestionStats(
        metricsRepository,
        serverConfig,
        topicNameToIngestionTaskMap,
        metadataRepo,
        serverConfig.isUnregisterMetricForDeletedStoreEnabled());
    AggVersionedDIVStats versionedDIVStats = new AggVersionedDIVStats(
        metricsRepository,
        metadataRepo,
        serverConfig.isUnregisterMetricForDeletedStoreEnabled());
    this.versionedIngestionStats = new AggVersionedIngestionStats(metricsRepository, metadataRepo, serverConfig);
    if (serverConfig.isDedicatedDrainerQueueEnabled()) {
      this.storeBufferService = new SeparatedStoreBufferService(serverConfig);
    } else {
      this.storeBufferService = new StoreBufferService(
          serverConfig.getStoreWriterNumber(),
          serverConfig.getStoreWriterBufferMemoryCapacity(),
          serverConfig.getStoreWriterBufferNotifyDelta());
    }
    this.kafkaMessageEnvelopeSchemaReader = kafkaMessageEnvelopeSchemaReader;
    /**
     * Collect metrics for {@link #storeBufferService}.
     * Since all the metrics will be collected passively, there is no need to
     * keep the reference of this {@link StoreBufferServiceStats} variable.
     */
    new StoreBufferServiceStats(metricsRepository, this.storeBufferService);

    this.aggLagStats = new AggLagStats(this, metricsRepository);

    if (clientConfig.isPresent()) {
      String clusterName = veniceConfigLoader.getVeniceClusterConfig().getClusterName();
      participantStoreConsumptionTask = new ParticipantStoreConsumptionTask(
          this,
          clusterInfoProvider,
          new ParticipantStoreConsumptionStats(metricsRepository, clusterName),
          ClientConfig.cloneConfig(clientConfig.get()).setMetricsRepository(metricsRepository),
          serverConfig.getParticipantMessageConsumptionDelayMs(),
          icProvider);
    } else {
      LOGGER.info(
          "Unable to start participant store consumption task because client config is not provided, jobs "
              + "may not be killed if admin helix messaging channel is disabled");
    }

    aggKafkaConsumerService = new AggKafkaConsumerService(
        veniceConsumerFactory,
        serverConfig,
        bandwidthThrottler,
        recordsThrottler,
        kafkaClusterBasedRecordThrottler,
        metricsRepository,
        new MetadataRepoBasedTopicExistingCheckerImpl(this.getMetadataRepo()));
    /**
     * After initializing a {@link AggKafkaConsumerService} service, it doesn't contain KafkaConsumerService yet until
     * a new Kafka cluster is registered; here we explicitly create KafkaConsumerService for the local Kafka cluster.
     *
     * Pass through all the customized Kafka consumer configs into the consumer as long as the customized config key
     * starts with {@link ConfigKeys#SERVER_LOCAL_CONSUMER_CONFIG_PREFIX}; the clipping and filtering work is done
     * in {@link VeniceServerConfig} already.
     *
     * Here, we only pass through the customized Kafka consumer configs for local consumption, if an ingestion task
     * creates a dedicated consumer or a new consumer service for a remote Kafka URL, we will passthrough the configs
     * for remote consumption inside the ingestion task.
     */
    Properties commonKafkaConsumerConfigs = getCommonKafkaConsumerProperties(serverConfig);
    if (!serverConfig.getKafkaConsumerConfigsForLocalConsumption().isEmpty()) {
      commonKafkaConsumerConfigs.putAll(serverConfig.getKafkaConsumerConfigsForLocalConsumption().toProperties());
    }
    aggKafkaConsumerService.createKafkaConsumerService(commonKafkaConsumerConfigs);

    if (serverConfig.isCacheWarmingBeforeReadyToServeEnabled()) {
      cacheWarmingExecutorService = Executors
          .newFixedThreadPool(serverConfig.getCacheWarmingThreadPoolSize(), new DaemonThreadFactory("Cache_Warming"));
    } else {
      cacheWarmingExecutorService = null;
    }
    LOGGER.info("Cache warming is {}", serverConfig.isCacheWarmingBeforeReadyToServeEnabled() ? "enabled" : "disabled");

    /**
     * Use the same diskUsage instance for all ingestion tasks; so that all the ingestion tasks can update the same
     * remaining disk space state to provide a more accurate alert.
     */
    DiskUsage diskUsage = new DiskUsage(
        veniceConfigLoader.getVeniceServerConfig().getDataBasePath(),
        veniceConfigLoader.getVeniceServerConfig().getDiskFullThreshold());

    ingestionTaskFactory = StoreIngestionTaskFactory.builder()
        .setVeniceWriterFactory(veniceWriterFactory)
        .setKafkaClientFactory(veniceConsumerFactory)
        .setStorageEngineRepository(storageEngineRepository)
        .setStorageMetadataService(storageMetadataService)
        .setLeaderFollowerNotifiersQueue(leaderFollowerNotifiers)
        .setBandwidthThrottler(bandwidthThrottler)
        .setRecordsThrottler(recordsThrottler)
        .setUnorderedBandwidthThrottler(unorderedBandwidthThrottler)
        .setUnorderedRecordsThrottler(unorderedRecordsThrottler)
        .setSchemaRepository(schemaRepo)
        .setMetadataRepository(metadataRepo)
        .setTopicManagerRepository(topicManagerRepository)
        .setTopicManagerRepositoryJavaBased(topicManagerRepositoryJavaBased)
        .setHostLevelIngestionStats(hostLevelIngestionStats)
        .setVersionedDIVStats(versionedDIVStats)
        .setVersionedIngestionStats(versionedIngestionStats)
        .setStoreBufferService(storeBufferService)
        .setServerConfig(serverConfig)
        .setDiskUsage(diskUsage)
        .setAggKafkaConsumerService(aggKafkaConsumerService)
        .setCacheWarmingThreadPool(cacheWarmingExecutorService)
        .setStartReportingReadyToServeTimestamp(System.currentTimeMillis() + serverConfig.getDelayReadyToServeMS())
        .setPartitionStateSerializer(partitionStateSerializer)
        .setIsDaVinciClient(isDaVinciClient)
        .setRemoteIngestionRepairService(remoteIngestionRepairService)
        .setMetaStoreWriter(metaStoreWriter)
        .setCompressorFactory(compressorFactory)
        .build();
  }

  /**
   * This function should only be triggered in classical Venice since replica status reporting is only valid
   * in classical Venice for meta system store.
   */
  public synchronized void addMetaSystemStoreReplicaStatusNotifier() {
    if (metaSystemStoreReplicaStatusNotifierQueued) {
      throw new VeniceException("MetaSystemStoreReplicaStatusNotifier should NOT be added twice");
    }
    if (this.metaSystemStoreReplicaStatusNotifier == null) {
      throw new VeniceException("MetaSystemStoreReplicaStatusNotifier wasn't initialized properly");
    }
    addIngestionNotifier(this.metaSystemStoreReplicaStatusNotifier);
    metaSystemStoreReplicaStatusNotifierQueued = true;
  }

  @Override
  public synchronized Optional<MetaSystemStoreReplicaStatusNotifier> getMetaSystemStoreReplicaStatusNotifier() {
    if (metaSystemStoreReplicaStatusNotifierQueued) {
      return Optional.of(metaSystemStoreReplicaStatusNotifier);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Starts the Kafka consumption tasks for already subscribed partitions.
   */
  @Override
  public boolean startInner() {
    ingestionExecutorService = Executors.newCachedThreadPool();
    topicNameToIngestionTaskMap.values().forEach(ingestionExecutorService::submit);

    storeBufferService.start();
    if (aggKafkaConsumerService != null) {
      aggKafkaConsumerService.start();
    }
    if (sharedKafkaProducerService != null) {
      sharedKafkaProducerService.start();
    }
    if (participantStoreConsumptionTask != null) {
      participantStoreConsumerExecutorService =
          Executors.newSingleThreadExecutor(new DaemonThreadFactory("ParticipantStoreConsumptionTask"));
      participantStoreConsumerExecutorService.submit(participantStoreConsumptionTask);
    }
    // Although the StoreConsumptionTasks are now running in their own threads, there is no async
    // process that needs to finish before the KafkaStoreIngestionService can be considered
    // started, so we are done with the start up process.
    return true;
  }

  private StoreIngestionTask createConsumerTask(VeniceStoreVersionConfig veniceStoreVersionConfig, int partitionId) {
    String storeName = Version.parseStoreFromKafkaTopicName(veniceStoreVersionConfig.getStoreVersionName());
    int versionNumber = Version.parseVersionFromKafkaTopicName(veniceStoreVersionConfig.getStoreVersionName());

    Pair<Store, Version> storeVersionPair =
        Utils.waitStoreVersionOrThrow(veniceStoreVersionConfig.getStoreVersionName(), metadataRepo);
    Store store = storeVersionPair.getFirst();
    Version version = storeVersionPair.getSecond();

    BooleanSupplier isVersionCurrent = () -> {
      try {
        return versionNumber == metadataRepo.getStoreOrThrow(storeName).getCurrentVersion();
      } catch (VeniceNoStoreException e) {
        LOGGER.warn("Unable to find store meta-data for {}", veniceStoreVersionConfig.getStoreVersionName(), e);
        return false;
      }
    };

    return ingestionTaskFactory.getNewIngestionTask(
        store,
        version,
        getKafkaConsumerProperties(veniceStoreVersionConfig),
        isVersionCurrent,
        veniceStoreVersionConfig,
        partitionId,
        isIsolatedIngestion,
        cacheBackend);
  }

  private static void shutdownExecutorService(ExecutorService executor, String name, boolean force) {
    if (executor == null) {
      return;
    }

    long startTime = System.currentTimeMillis();
    try {
      executor.shutdown();
      if (force || !executor.awaitTermination(60, TimeUnit.SECONDS)) {
        if (!force) {
          LOGGER.warn("Failed to gracefully shutdown executor {}", name);
        }
        executor.shutdownNow();
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          LOGGER.error("Failed to shutdown executor {}", name);
        }
      }
    } catch (InterruptedException e) {
      LOGGER.warn("Executor shutdown is interrupted");
      executor.shutdownNow();
      currentThread().interrupt();
    } finally {
      LOGGER.info("{} shutdown took {} ms.", name, System.currentTimeMillis() - startTime);
    }
  }

  /**
   * Stops all the Kafka consumption tasks.
   * Closes all the Kafka clients.
   */
  @Override
  public void stopInner() {
    Utils.closeQuietlyWithErrorLogged(participantStoreConsumptionTask);
    shutdownExecutorService(participantStoreConsumerExecutorService, "participantStoreConsumerExecutorService", true);

    /*
     * We would like to gracefully shutdown {@link #ingestionExecutorService},
     * so that it will have an opportunity to checkpoint the processed offset.
     */
    topicNameToIngestionTaskMap.values().forEach(StoreIngestionTask::close);
    shutdownExecutorService(ingestionExecutorService, "ingestionExecutorService", false);
    shutdownExecutorService(cacheWarmingExecutorService, "cacheWarmingExecutorService", true);

    Utils.closeQuietlyWithErrorLogged(aggKafkaConsumerService);

    leaderFollowerNotifiers.forEach(VeniceNotifier::close);
    Utils.closeQuietlyWithErrorLogged(metaStoreWriter);

    kafkaMessageEnvelopeSchemaReader.ifPresent(Utils::closeQuietlyWithErrorLogged);

    // close it the very end to make sure all ingestion task have released the shared producers.
    Utils.closeQuietlyWithErrorLogged(sharedKafkaProducerService);

    // close drainer service at the very end as it does not depend on any other service.
    Utils.closeQuietlyWithErrorLogged(storeBufferService);
    Utils.closeQuietlyWithErrorLogged(topicManagerRepository);
    Utils.closeQuietlyWithErrorLogged(topicManagerRepositoryJavaBased);
    topicLockManager.removeAllLocks();
  }

  /**
   * Starts consuming messages from Kafka Partition corresponding to Venice Partition.
   * Subscribes to partition if required.
   * @param veniceStore Venice Store for the partition.
   * @param partitionId Venice partition's id.
   * @param leaderState
   */
  @Override
  public void startConsumption(
      VeniceStoreVersionConfig veniceStore,
      int partitionId,
      Optional<LeaderFollowerStateType> leaderState) {

    final String topic = veniceStore.getStoreVersionName();
    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topic)) {
      StoreIngestionTask consumerTask = topicNameToIngestionTaskMap.get(topic);
      if (consumerTask == null || !consumerTask.isRunning()) {
        consumerTask = createConsumerTask(veniceStore, partitionId);
        topicNameToIngestionTaskMap.put(topic, consumerTask);
        versionedIngestionStats.setIngestionTask(topic, consumerTask);
        if (!isRunning()) {
          LOGGER.info(
              "Ignoring Start consumption message as service is stopping. Topic {} Partition {}",
              topic,
              partitionId);
          return;
        }
        ingestionExecutorService.submit(consumerTask);
      }

      /**
       * Since Venice metric is store-level, and it would have multiply topics tasks exist in the same time.
       * Only the task with the largest version would emit it stats. That being said, relying on the {@link #metadataRepo}
       * to get the max version may be unreliable, since the information in this object is not guaranteed
       * to be up-to-date. As a sanity check, we will also look at the version in the topic name, and
       * pick whichever number is highest as the max version number.
       */
      String storeName = Version.parseStoreFromKafkaTopicName(topic);
      int maxVersionNumberFromTopicName = Version.parseVersionFromKafkaTopicName(topic);
      int maxVersionNumberFromMetadataRepo = getStoreMaximumVersionNumber(storeName);
      if (maxVersionNumberFromTopicName > maxVersionNumberFromMetadataRepo) {
        LOGGER.warn(
            "Got stale info from metadataRepo. maxVersionNumberFromTopicName: {}, maxVersionNumberFromMetadataRepo: {}. "
                + "Will rely on the topic name's version.",
            maxVersionNumberFromTopicName,
            maxVersionNumberFromMetadataRepo);
      }
      /**
       * Notice that the version push for maxVersionNumberFromMetadataRepo might be killed already (this code path will
       * also be triggered after server restarts).
       */
      int maxVersionNumber = Math.max(maxVersionNumberFromMetadataRepo, maxVersionNumberFromTopicName);
      updateStatsEmission(topicNameToIngestionTaskMap, storeName, maxVersionNumber);

      consumerTask.subscribePartition(topic, partitionId, leaderState);
    }
    LOGGER.info("Started Consuming - Kafka Partition: {}-{}.", topic, partitionId);
  }

  /**
   * This method closes the specified {@link StoreIngestionTask} and wait for up to 10 seconds for fully shutdown.
   * @param veniceStoreVersionConfig store version config that carries topic information.
   */
  protected void shutdownStoreIngestionTask(VeniceStoreVersionConfig veniceStoreVersionConfig) {
    String topicName = veniceStoreVersionConfig.getStoreVersionName();
    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topicName)) {
      if (topicNameToIngestionTaskMap.containsKey(topicName)) {
        StoreIngestionTask storeIngestionTask = topicNameToIngestionTaskMap.remove(topicName);
        storeIngestionTask.shutdown(10000);
      } else {
        LOGGER.info("Ignoring close request for not-existing consumption task {}", topicName);
      }
    }
    topicLockManager.removeLockForResource(topicName);
  }

  @Override
  public void promoteToLeader(
      VeniceStoreVersionConfig veniceStoreVersionConfig,
      int partitionId,
      LeaderFollowerPartitionStateModel.LeaderSessionIdChecker checker) {
    final String topic = veniceStoreVersionConfig.getStoreVersionName();

    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topic)) {
      StoreIngestionTask consumerTask = topicNameToIngestionTaskMap.get(topic);
      if (consumerTask != null && consumerTask.isRunning()) {
        consumerTask.promoteToLeader(topic, partitionId, checker);
      } else {
        LOGGER.warn("Ignoring standby to leader transition message for Topic {} Partition {}", topic, partitionId);
      }
    }
  }

  @Override
  public void demoteToStandby(
      VeniceStoreVersionConfig veniceStoreVersionConfig,
      int partitionId,
      LeaderFollowerPartitionStateModel.LeaderSessionIdChecker checker) {
    final String topic = veniceStoreVersionConfig.getStoreVersionName();

    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topic)) {
      StoreIngestionTask consumerTask = topicNameToIngestionTaskMap.get(topic);
      if (consumerTask != null && consumerTask.isRunning()) {
        consumerTask.demoteToStandby(topic, partitionId, checker);
      } else {
        LOGGER.warn("Ignoring leader to standby transition message for Topic {} Partition {}", topic, partitionId);
      }
    }
  }

  @Override
  public VeniceConfigLoader getVeniceConfigLoader() {
    return veniceConfigLoader;
  }

  /**
   * Find the task that matches both the storeName and maximumVersion number, enable metrics emission for this task and
   * update ingestion stats with this task; disable metric emission for all the task that doesn't max version.
   */
  protected void updateStatsEmission(
      NavigableMap<String, StoreIngestionTask> taskMap,
      String storeName,
      int maximumVersion) {
    String knownMaxVersionTopic = Version.composeKafkaTopic(storeName, maximumVersion);
    if (taskMap.containsKey(knownMaxVersionTopic)) {
      taskMap.forEach((topicName, task) -> {
        if (Version.parseStoreFromKafkaTopicName(topicName).equals(storeName)) {
          if (Version.parseVersionFromKafkaTopicName(topicName) < maximumVersion) {
            task.disableMetricsEmission();
          } else {
            task.enableMetricsEmission();
          }
        }
      });
    } else {
      /**
       * The version push doesn't exist in this server node at all; it's possible the push for largest version has
       * already been killed, so instead, emit metrics for the largest known batch push in this node.
       */
      updateStatsEmission(taskMap, storeName);
    }
  }

  /**
   * This function will go through all known ingestion task in this server node, find the task that matches the
   * storeName and has the largest version number; if the task doesn't enable metric emission, enable it and
   * update store ingestion stats.
   */
  protected void updateStatsEmission(NavigableMap<String, StoreIngestionTask> taskMap, String storeName) {
    int maxVersion = -1;
    StoreIngestionTask latestOngoingIngestionTask = null;
    for (Map.Entry<String, StoreIngestionTask> entry: taskMap.entrySet()) {
      String topic = entry.getKey();
      if (Version.parseStoreFromKafkaTopicName(topic).equals(storeName)) {
        int version = Version.parseVersionFromKafkaTopicName(topic);
        if (version > maxVersion) {
          maxVersion = version;
          latestOngoingIngestionTask = entry.getValue();
        }
      }
    }
    if (latestOngoingIngestionTask != null && !latestOngoingIngestionTask.isMetricsEmissionEnabled()) {
      latestOngoingIngestionTask.enableMetricsEmission();
      /**
       * Disable the metrics emission for all lower version pushes.
       */
      Map.Entry<String, StoreIngestionTask> lowerVersionPush =
          taskMap.lowerEntry(Version.composeKafkaTopic(storeName, maxVersion));
      while (lowerVersionPush != null
          && Version.parseStoreFromKafkaTopicName(lowerVersionPush.getKey()).equals(storeName)) {
        lowerVersionPush.getValue().disableMetricsEmission();
        lowerVersionPush = taskMap.lowerEntry(lowerVersionPush.getKey());
      }
    }
  }

  private int getStoreMaximumVersionNumber(String storeName) {
    int maxVersionNumber = metadataRepo.getStoreOrThrow(storeName).getLargestUsedVersionNumber();
    if (maxVersionNumber == Store.NON_EXISTING_VERSION) {
      throw new VeniceException("No version has been created yet for store " + storeName);
    }
    return maxVersionNumber;
  }

  /**
   * Stops consuming messages from Kafka Partition corresponding to Venice Partition.
   * @param veniceStore Venice Store for the partition.
   * @param partitionId Venice partition's id.
   */
  @Override
  public void stopConsumption(VeniceStoreVersionConfig veniceStore, int partitionId) {
    final String topic = veniceStore.getStoreVersionName();

    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topic)) {
      StoreIngestionTask ingestionTask = topicNameToIngestionTaskMap.get(topic);
      if (ingestionTask != null && ingestionTask.isRunning()) {
        ingestionTask.unSubscribePartition(topic, partitionId);
      } else {
        LOGGER.warn("Ignoring stop consumption message for Topic {} Partition {}", topic, partitionId);
      }
    }
  }

  /**
   * Stops consuming messages from Kafka Partition corresponding to Venice Partition and wait up to
   * (sleepSeconds * numRetires) to make sure partition consumption is stopped.
   * @param veniceStore Venice Store for the partition.
   * @param partitionId Venice partition's id.
   * @param sleepSeconds
   * @param numRetries
   */
  @Override
  public void stopConsumptionAndWait(
      VeniceStoreVersionConfig veniceStore,
      int partitionId,
      int sleepSeconds,
      int numRetries) {
    if (isPartitionConsuming(veniceStore, partitionId)) {
      stopConsumption(veniceStore, partitionId);
      try {
        long startTimeInMs = System.currentTimeMillis();
        for (int i = 0; i < numRetries; i++) {
          if (!isPartitionConsuming(veniceStore, partitionId)) {
            LOGGER.info(
                "Partition: {} of topic: {} has stopped consumption in {}ms.",
                partitionId,
                veniceStore.getStoreVersionName(),
                LatencyUtils.getElapsedTimeInMs(startTimeInMs));
            break;
          }
          sleep((long) sleepSeconds * Time.MS_PER_SECOND);
        }
        LOGGER.error(
            "Partition: {} of store: {} is still consuming after waiting for it to stop for {} seconds.",
            partitionId,
            veniceStore.getStoreVersionName(),
            numRetries * sleepSeconds);
      } catch (InterruptedException e) {
        LOGGER.warn("Waiting for partition to stop consumption was interrupted", e);
        currentThread().interrupt();
      }
    } else {
      LOGGER.warn(
          "Partition: {} of topic: {} is not consuming, skipped the stop consumption.",
          partitionId,
          veniceStore.getStoreVersionName());
    }
    resetConsumptionOffset(veniceStore, partitionId);
    if (!ingestionTaskHasAnySubscription(veniceStore.getStoreVersionName())) {
      shutdownStoreIngestionTask(veniceStore);
    }
  }

  /**
   * @param topicName Venice topic (store and version number) for the corresponding consumer task that needs to be killed.
   *                  No action is taken for invocations of killConsumptionTask on topics that are not in the map. This
   *                  includes logging.
   * @return true if a kill is needed and called, otherwise false
   */
  @Override
  public boolean killConsumptionTask(String topicName) {
    if (!isRunning()) {
      throw new VeniceException("KafkaStoreIngestionService is not running.");
    }

    boolean killed = false;
    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topicName)) {
      StoreIngestionTask consumerTask = topicNameToIngestionTaskMap.get(topicName);
      if (consumerTask == null) {
        LOGGER.info("Ignoring kill request for not-existing consumption task {}", topicName);
        return false;
      }

      if (consumerTask.isRunning()) {
        consumerTask.kill();
        compressorFactory.removeVersionSpecificCompressor(topicName);
        killed = true;
        LOGGER.info("Killed consumption task for topic {}", topicName);
      } else {
        LOGGER.warn("Ignoring kill request for stopped consumption task {}", topicName);
      }
      // cleanup the map regardless if the task was running or not to prevent mem leak when failed tasks lingers
      // in the map since isRunning is set to false already.
      topicNameToIngestionTaskMap.remove(topicName);
      if (aggKafkaConsumerService != null) {
        aggKafkaConsumerService.unsubscribeAll(consumerTask.getVersionTopic());
      }

      /**
       * For the same store, there will be only one task emitting metrics, if this is the only task that is emitting
       * metrics, it means the latest ongoing push job is killed. In such case, find the largest version in the task
       * map and enable metric emission.
       */
      if (consumerTask.isMetricsEmissionEnabled()) {
        consumerTask.disableMetricsEmission();
        updateStatsEmission(topicNameToIngestionTaskMap, Version.parseStoreFromKafkaTopicName(topicName));
      }
    }
    topicLockManager.removeLockForResource(topicName);
    return killed;
  }

  @Override
  public void addIngestionNotifier(VeniceNotifier notifier) {
    leaderFollowerNotifiers.add(notifier);
  }

  @Override
  public boolean containsRunningConsumption(VeniceStoreVersionConfig veniceStore) {
    return containsRunningConsumption(veniceStore.getStoreVersionName());
  }

  @Override
  public boolean containsRunningConsumption(String topic) {
    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topic)) {
      StoreIngestionTask consumerTask = topicNameToIngestionTaskMap.get(topic);
      return consumerTask != null && consumerTask.isRunning();
    }
  }

  @Override
  public boolean isPartitionConsuming(VeniceStoreVersionConfig veniceStore, int partitionId) {
    final String topic = veniceStore.getStoreVersionName();
    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topic)) {
      StoreIngestionTask ingestionTask = topicNameToIngestionTaskMap.get(topic);
      return ingestionTask != null && ingestionTask.isRunning() && ingestionTask.isPartitionConsuming(partitionId);
    }
  }

  @Override
  public Set<String> getIngestingTopicsWithVersionStatusNotOnline() {
    Set<String> result = new HashSet<>();
    for (String topic: topicNameToIngestionTaskMap.keySet()) {
      try {
        Store store = metadataRepo.getStoreOrThrow(Version.parseStoreFromKafkaTopicName(topic));
        int versionNumber = Version.parseVersionFromKafkaTopicName(topic);
        if (store == null || !store.getVersion(versionNumber).isPresent()
            || store.getVersion(versionNumber).get().getStatus() != VersionStatus.ONLINE) {
          result.add(topic);
        }
      } catch (VeniceNoStoreException e) {
        // Include topics that may have their corresponding store deleted already
        result.add(topic);
      } catch (Exception e) {
        LOGGER.error("Unexpected exception while fetching ongoing ingestion topics, topic: {}", topic, e);
      }
    }
    return result;
  }

  public void recordIngestionFailure(String storeName) {
    hostLevelIngestionStats.getStoreStats(storeName).recordIngestionFailure();
  }

  @Override
  public AggVersionedIngestionStats getAggVersionedIngestionStats() {
    return versionedIngestionStats;
  }

  /**
   * @return Group Id for kafka consumer.
   */
  private static String getGroupId(String topic) {
    return String.format(GROUP_ID_FORMAT, topic, Utils.getHostName());
  }

  /**
   * So far, this function is only targeted to be used by shared consumer.
   * @param serverConfig
   * @return
   */
  private Properties getCommonKafkaConsumerProperties(VeniceServerConfig serverConfig) {
    Properties kafkaConsumerProperties = new Properties();
    kafkaConsumerProperties
        .setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, serverConfig.getKafkaBootstrapServers());
    kafkaConsumerProperties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    // Venice is persisting offset in local offset db.
    kafkaConsumerProperties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    // This is a temporary fix for the issue described here
    // https://stackoverflow.com/questions/37363119/kafka-producer-org-apache-kafka-common-serialization-stringserializer-could-no
    // In our case "com.linkedin.venice.serialization.KafkaKeySerializer" and
    // "com.linkedin.venice.serialization.avro.OptimizedKafkaValueSerializer" classes can not be found
    // because class loader has no venice-common in class path. This can be only reproduced on JDK11
    // Trying to avoid class loading via Kafka's ConfigDef class
    kafkaConsumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaKeySerializer.class);
    kafkaConsumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OptimizedKafkaValueSerializer.class);
    kafkaConsumerProperties.setProperty(
        ConsumerConfig.FETCH_MIN_BYTES_CONFIG,
        String.valueOf(serverConfig.getKafkaFetchMinSizePerSecond()));
    kafkaConsumerProperties.setProperty(
        ConsumerConfig.FETCH_MAX_BYTES_CONFIG,
        String.valueOf(serverConfig.getKafkaFetchMaxSizePerSecond()));
    /**
     * The following setting is used to control the maximum number of records to returned in one poll request.
     */
    kafkaConsumerProperties
        .setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.toString(serverConfig.getKafkaMaxPollRecords()));
    kafkaConsumerProperties
        .setProperty(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, String.valueOf(serverConfig.getKafkaFetchMaxTimeMS()));
    kafkaConsumerProperties.setProperty(
        ConsumerConfig.MAX_PARTITION_FETCH_BYTES_CONFIG,
        String.valueOf(serverConfig.getKafkaFetchPartitionMaxSizePerSecond()));
    kafkaConsumerProperties.setProperty(
        ApacheKafkaConsumer.CONSUMER_POLL_RETRY_TIMES_CONFIG,
        String.valueOf(serverConfig.getKafkaPollRetryTimes()));
    kafkaConsumerProperties.setProperty(
        ApacheKafkaConsumer.CONSUMER_POLL_RETRY_BACKOFF_MS_CONFIG,
        String.valueOf(serverConfig.getKafkaPollRetryBackoffMs()));
    if (kafkaMessageEnvelopeSchemaReader.isPresent()) {
      kafkaConsumerProperties
          .put(InternalAvroSpecificSerializer.VENICE_SCHEMA_READER_CONFIG, kafkaMessageEnvelopeSchemaReader.get());
    }

    return kafkaConsumerProperties;
  }

  /**
   * @return Properties Kafka properties corresponding to the venice store.
   */
  private Properties getKafkaConsumerProperties(VeniceStoreVersionConfig storeConfig) {
    Properties kafkaConsumerProperties = getCommonKafkaConsumerProperties(storeConfig);
    String groupId = getGroupId(storeConfig.getStoreVersionName());
    kafkaConsumerProperties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    /**
     * Temporarily we are going to use group_id as client_id as well since it is unique in cluster level.
     * With unique client_id, it will be easier for us to check Kafka consumer related metrics through JMX.
     * TODO: Kafka is throttling based on client_id, need to investigate whether we should use Kafka throttling or not.
     */
    kafkaConsumerProperties.setProperty(ConsumerConfig.CLIENT_ID_CONFIG, groupId);
    return kafkaConsumerProperties;
  }

  @Override
  public ByteBuffer getStoreVersionCompressionDictionary(String topicName) {
    return storageMetadataService.getStoreVersionCompressionDictionary(topicName);
  }

  public StoreIngestionTask getStoreIngestionTask(String topicName) {
    return topicNameToIngestionTaskMap.get(topicName);
  }

  @Override
  public AdminResponse getConsumptionSnapshots(String topicName, ComplementSet<Integer> partitions) {
    AdminResponse response = new AdminResponse();
    StoreIngestionTask ingestionTask = getStoreIngestionTask(topicName);
    if (ingestionTask != null) {
      ingestionTask.dumpPartitionConsumptionStates(response, partitions);
      ingestionTask.dumpStoreVersionState(response);
    } else {
      String msg = "Ingestion task for " + topicName + " doesn't exist for " + ServerAdminAction.DUMP_INGESTION_STATE
          + " admin command";
      LOGGER.warn(msg);
      response.setMessage(msg);
    }
    return response;
  }

  @Override
  public void traverseAllIngestionTasksAndApply(Consumer<StoreIngestionTask> consumer) {
    topicNameToIngestionTaskMap.values().forEach(consumer);
  }

  public AggLagStats getAggLagStats() {
    return aggLagStats;
  }

  public LeaderFollowerStateType getLeaderStateFromPartitionConsumptionState(String topicName, int partitionId) {
    return getStoreIngestionTask(topicName).getLeaderState(partitionId);
  }

  /**
   * This method updates all sub-partitions' latest offset records fetched from isolated ingestion process
   * in main process, so main process's in-memory storage metadata service could be aware of the latest updates and will
   * not re-start the ingestion from scratch.
   */
  public void updatePartitionOffsetRecords(String topicName, int partition, List<ByteBuffer> offsetRecordArray) {
    int amplificationFactor = PartitionUtils.getAmplificationFactor(metadataRepo, topicName);
    int offset = amplificationFactor * partition;
    for (int subPartition: PartitionUtils.getSubPartitions(partition, amplificationFactor)) {
      OffsetRecord offsetRecord =
          new OffsetRecord(offsetRecordArray.get(subPartition - offset).array(), partitionStateSerializer);
      storageMetadataService.put(topicName, subPartition, offsetRecord);
    }
  }

  /**
   * This method should only be called when the forked ingestion process is handing over ingestion task to main process.
   * It collects the user partition's latest OffsetRecords from partition consumption states (PCS).
   * In theory, PCS should be available in this situation as we haven't unsubscribed from topic. If it is not available,
   * we will throw exception as this is not as expected.
   */
  public List<ByteBuffer> getPartitionOffsetRecords(String topicName, int partition) {
    List<ByteBuffer> offsetRecordArray = new ArrayList<>();
    StoreIngestionTask storeIngestionTask = getStoreIngestionTask(topicName);
    int amplificationFactor = storeIngestionTask.getAmplificationFactor();
    for (int i = 0; i < amplificationFactor; i++) {
      int subPartitionId = amplificationFactor * partition + i;
      PartitionConsumptionState pcs = storeIngestionTask.getPartitionConsumptionState(subPartitionId);
      offsetRecordArray.add(ByteBuffer.wrap(pcs.getOffsetRecord().toBytes()));
    }
    return offsetRecordArray;
  }

  public ReadOnlyStoreRepository getMetadataRepo() {
    return metadataRepo;
  }

  private boolean ingestionTaskHasAnySubscription(String topic) {
    try (AutoCloseableLock ignore = topicLockManager.getLockForResource(topic)) {
      StoreIngestionTask consumerTask = topicNameToIngestionTaskMap.get(topic);
      return consumerTask != null && consumerTask.hasAnySubscription();
    }
  }

  /**
   * Resets Offset to beginning for Kafka Partition corresponding to Venice Partition.
   * @param veniceStore Venice Store for the partition.
   * @param partitionId Venice partition's id.
   */
  private void resetConsumptionOffset(VeniceStoreVersionConfig veniceStore, int partitionId) {
    String topic = veniceStore.getStoreVersionName();
    StoreIngestionTask consumerTask = topicNameToIngestionTaskMap.get(topic);
    if (consumerTask != null && consumerTask.isRunning()) {
      consumerTask.resetPartitionConsumptionOffset(topic, partitionId);
    }
    LOGGER.info("Offset reset to beginning - Kafka Partition: {}-{}.", topic, partitionId);
  }
}
