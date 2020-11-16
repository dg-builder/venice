package com.linkedin.venice;

public class ConfigKeys {
  private ConfigKeys() {}

  // cluster specific properties
  public static final String CLUSTER_NAME = "cluster.name";
  public static final String ENABLE_KAFKA_CONSUMER_OFFSET_MANAGEMENT = "enable.kafka.consumers.offset.management";
  public static final String OFFSET_MANAGER_TYPE = "offset.manager.type";
  public static final String OFFSET_DATA_BASE_PATH = "offsets.data.base.path";
  public static final String OFFSET_MANAGER_FLUSH_INTERVAL_MS = "offset.manager.flush.interval.ms";
  public static final String OFFSET_MANAGER_LOG_FILE_MAX_BYTES = "offset.manager.log.file.max.bytes";
  public static final String ZOOKEEPER_ADDRESS = "zookeeper.address";


  public static final String ADMIN_PORT = "admin.port";
  public static final String ADMIN_SECURE_PORT = "admin.secure.port";

  /**
   * Whether controller should check "Read" method against Kafka wildcard ACL while users request
   * for a topic to write.
   *
   * By default, the config value should be true, but setting it to false would allow us to release
   * new version of controller when the "Read" method check is not working as expected.
   */
  public static final String ADMIN_CHECK_READ_METHOD_FOR_KAFKA = "admin.check.read.method.for.kafka";

  public static final String STATUS_MESSAGE_RETRY_COUNT = "status.message.retry.count";
  public static final String STATUS_MESSAGE_RETRY_DURATION_MS = "status.message.retry.duration.ms";

  // store specific properties
  public static final String PERSISTENCE_TYPE = "persistence.type";
  public static final String KAFKA_BROKERS = "kafka.brokers";
  public static final String KAFKA_BROKER_PORT = "kafka.broker.port";
  public static final String KAFKA_CONSUMER_FETCH_BUFFER_SIZE = "kafka.consumer.fetch.buffer.size";
  public static final String KAFKA_CONSUMER_SOCKET_TIMEOUT_MS = "kafka.consumer.socket.timeout.ms";
  public static final String KAFKA_CONSUMER_NUM_METADATA_REFRESH_RETRIES = "kafka.consumer.num.metadata.refresh.retries";
  public static final String KAFKA_CONSUMER_METADATA_REFRESH_BACKOFF_MS = "kafka.consumer.metadata.refresh.backoff.ms";

  public static final String KAFKA_BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";
  public static final String SSL_KAFKA_BOOTSTRAP_SERVERS = "ssl.kafka.bootstrap.servers";

  /**
   * The time window used by the consumption throttler. Throttler will sum the requests during the time window and
   * compare with the quota accumulated in the time window to see whether the usage exceeds quota or not.
   */
  public static final String KAFKA_FETCH_QUOTA_TIME_WINDOW_MS = "kafka.fetch.quota.time.window.ms";

  public static final String KAFKA_FETCH_QUOTA_BYTES_PER_SECOND = "kafka.fetch.quota.bytes.per.second";
  /**
   * How many records that one server could consume from Kafka at most in one second.
   * If the consume rate reached this quota, the consumption thread will be blocked until there is the available quota.
   */
  public static final String KAFKA_FETCH_QUOTA_RECORDS_PER_SECOND = "kafka.fetch.quota.records.per.second";

  // Unordered throttlers aren't compatible with Shared Kafka Consumer and have no effect when Shared Consumer is used.
  public static final String KAFKA_FETCH_QUOTA_UNORDERED_BYTES_PER_SECOND = "kafka.fetch.quota.unordered.bytes.per.second";
  public static final String KAFKA_FETCH_QUOTA_UNORDERED_RECORDS_PER_SECOND = "kafka.fetch.quota.unordered.records.per.second";

  // Kafka security protocol
  public static final String KAFKA_SECURITY_PROTOCOL = "security.protocol";

  // Cluster specific configs for controller
  public static final String CONTROLLER_NAME = "controller.name";

  /**
   * Whether to turn on Kafka's log compaction for the store-version topics of hybrid (and real-time only) stores.
   *
   * Will take effect at topic creation time, and when the hybrid config for the store is turned on.
   */
  public static final String KAFKA_LOG_COMPACTION_FOR_HYBRID_STORES = "kafka.log.compaction.for.hybrid.stores";

  /**
   * Whether to turn on Kafka's log compaction for the store-version topics of incremental push stores.
   *
   * Will take effect at topic creation time, and when the incremental push config for the store is turned on.
   */
  public static final String KAFKA_LOG_COMPACTION_FOR_INCREMENTAL_PUSH_STORES = "kafka.log.compaction.for.incremental.push.stores";

  /**
   * For log compaction enabled topics, this config will define the minimum time a message will remain uncompacted in the log.
   */
  public static final String KAFKA_MIN_LOG_COMPACTION_LAG_MS = "kafka.min.log.compaction.lag.ms";

  /**
   * The min.isr property to be set at topic creation time. Will not modify already-existing topics.
   *
   * If unset, will use the Kafka cluster's default.
   */
  public static final String KAFKA_MIN_ISR = "kafka.min.isr";

  /**
   * The replication factor to set for real-time buffer topics and store-version topics, at topic creation time.
   */
  public static final String KAFKA_REPLICATION_FACTOR = "kafka.replication.factor";

  /**
   * Sets the default for whether or not native replication is enabled or not for a venice store.
   */
  public static final String ENABLE_NATIVE_REPLICATION_AS_DEFAULT = "enable.native.replication.as.default";

  /**
   * Sets the default for whether or not leader follower is enabled or not for a hybrid store.
   */
  public static final String ENABLE_LEADER_FOLLOWER_AS_DEFAULT_FOR_HYBRID_STORES =
      "enable.leader.follower.as.default.for.hybrid.stores";

  /**
   * Sets the default for whether or not leader follower is enabled or not for an incremental push store.
   */
  public static final String ENABLE_LEADER_FOLLOWER_AS_DEFAULT_FOR_INCREMENTAL_PUSH_STORES =
      "enable.leader.follower.as.default.for.incremental.push.stores";

  /**
   * Sets the default for whether or not leader follower is enabled or not for a venice store.
   */
  public static final String ENABLE_LEADER_FOLLOWER_AS_DEFAULT_FOR_ALL_STORES =
      "enable.leader.follower.as.default.for.all.stores";

  /**
   * Fallback to remain compatible with the old config spelling.
   *
   * Ignored if {@value KAFKA_REPLICATION_FACTOR} is present.
   */
  @Deprecated
  public static final String KAFKA_REPLICATION_FACTOR_LEGACY_SPELLING = "kafka.replica.factor";
  public static final String KAFKA_ZK_ADDRESS = "kafka.zk.address";
  public static final String DEFAULT_READ_STRATEGY = "default.read.strategy";
  public static final String DEFAULT_OFFLINE_PUSH_STRATEGY = "default.offline.push.strategy";
  public static final String DEFAULT_ROUTING_STRATEGY = "default.routing.strategy";
  /**
   * replication_factor is moved to be a store level config
   * check out {@link com.linkedin.venice.meta.Store#DEFAULT_REPLICATION_FACTOR}
   */
  @Deprecated
  public static final String DEFAULT_REPLICA_FACTOR = "default.replica.factor";
  public static final String DEFAULT_NUMBER_OF_PARTITION = "default.partition.count";
  public static final String DEFAULT_MAX_NUMBER_OF_PARTITIONS = "default.partition.max.count";
  public static final String DEFAULT_PARTITION_SIZE = "default.partition.size";
  public static final String OFFLINE_JOB_START_TIMEOUT_MS = "offline.job.start.timeout.ms";
  public static final String DELAY_TO_REBALANCE_MS = "delay.to.rebalance.ms";
  public static final String MIN_ACTIVE_REPLICA = "min.active.replica";
  public static final String DEFAULT_STORAGE_QUOTA = "default.storage.quota";
  public static final String DEFAULT_READ_QUOTA = "default.read.quota";
  public static final String CLUSTER_TO_D2 = "cluster.to.d2";
  public static final String HELIX_SEND_MESSAGE_TIMEOUT_MS = "helix.send.message.timeout.ms";
  public static final String REFRESH_ATTEMPTS_FOR_ZK_RECONNECT = "refresh.attempts.for.zk.reconnect";
  public static final String REFRESH_INTERVAL_FOR_ZK_RECONNECT_MS = "refresh.interval.for.zk.reconnect.ms";
  public static final String KAFKA_READ_CYCLE_DELAY_MS = "kafka.read.cycle.delay.ms";
  public static final String KAFKA_EMPTY_POLL_SLEEP_MS = "kafka.empty.poll.sleep.ms";
  public static final String KAFKA_FETCH_MIN_SIZE_PER_SEC = "kafka.fetch.min.size.per.sec";
  public static final String KAFKA_FETCH_MAX_SIZE_PER_SEC = "kafka.fetch.max.size.per.sec";
  public static final String KAFKA_FETCH_MAX_WAIT_TIME_MS = "kafka.fetch.max.wait.time.ms";
  public static final String KAFKA_FETCH_PARTITION_MAX_SIZE_PER_SEC = "kafka.fetch.partition.max.size.per.sec";

  // Controller specific configs
  public static final String CONTROLLER_CLUSTER_ZK_ADDRESSS = "controller.cluster.zk.address";
  /** Cluster name for all parent controllers */
  public static final String CONTROLLER_CLUSTER = "controller.cluster.name";
  /**
   * The retention policy for deprecated topics, which includes topics for both failed jobs and retired store versions.
   */
  public static final String DEPRECATED_TOPIC_RETENTION_MS = "deprecated.topic.retention.ms";

  /**
   * This config is to indicate the max retention policy we have setup for deprecated jobs currently and in the past.
   * And this is used to decide whether the topic is deprecated or not during topic cleanup.
   *
   * The reason to have this config instead of using {@link #DEPRECATED_TOPIC_RETENTION_MS} since the retention
   * policy for deprecated jobs could change from time to time, and we need to use a max threshold to cover all the
   * historical deprecated job topics.
   */
  public static final String DEPRECATED_TOPIC_MAX_RETENTION_MS = "deprecated.topic.max.retention.ms";

  /**
   * Sleep interval between each topic list fetch from Kafka ZK in TopicCleanup service.
   * We don't want to hit Kafka Zookeeper too frequently.
   */
  public static final String TOPIC_CLEANUP_SLEEP_INTERVAL_BETWEEN_TOPIC_LIST_FETCH_MS = "topic.cleanup.sleep.interval.between.topic.list.fetch.ms";
  public static final String TOPIC_CLEANUP_DELAY_FACTOR = "topic.cleanup.delay.factor";
  public static final String TOPIC_CLEANUP_SEND_CONCURRENT_DELETES_REQUESTS = "topic.cleanup.send.concurrent.delete.requests.enabled";

  public static final String ENABLE_TOPIC_REPLICATOR = "controller.enable.topic.replicator";
  public static final String ENABLE_TOPIC_REPLICATOR_SSL = "controller.enable.topic.replicator.ssl";

  /**
   * Sleep interval for polling topic deletion status from ZK.
   */
  public static final String TOPIC_DELETION_STATUS_POLL_INTERVAL_MS = "topic.deletion.status.poll.interval.ms";

  /**
   * The following config is to control the default retention time in milliseconds if it is not specified in store level.
   */
  public static final String CONTROLLER_BACKUP_VERSION_DEFAULT_RETENTION_MS = "controller.backup.version.default.retention.ms";

  /**
   * The following config is to control whether to enable backup version cleanup based on retention policy or not.
   */
  public static final String CONTROLLER_BACKUP_VERSION_RETENTION_BASED_CLEANUP_ENABLED = "controller.backup.version.retention.based.cleanup.enabled";

  /**
   * Whether controller should enforce SSL.
   */
  public static final String CONTROLLER_ENFORCE_SSL = "controller.enforce.ssl";

  // Server specific configs
  public static final String LISTENER_PORT = "listener.port";
  public static final String DATA_BASE_PATH = "data.base.path";
  public static final String AUTOCREATE_DATA_PATH = "autocreate.data.path";
  public static final String ENABLE_SERVER_WHITE_LIST = "enable.server.whitelist";
  public static final String MAX_ONLINE_OFFLINE_STATE_TRANSITION_THREAD_NUMBER = "max.state.transition.thread.number";
  public static final String MAX_LEADER_FOLLOWER_STATE_TRANSITION_THREAD_NUMBER = "max.leader.follower.state.transition.thread.number";
  public static final String STORE_WRITER_NUMBER = "store.writer.number";
  public static final String STORE_WRITER_BUFFER_MEMORY_CAPACITY = "store.writer.buffer.memory.capacity";
  public static final String STORE_WRITER_BUFFER_NOTIFY_DELTA = "store.writer.buffer.notify.delta";
  public static final String OFFSET_DATABASE_CACHE_SIZE = "offset.database.cache.size";
  public static final String SERVER_REST_SERVICE_STORAGE_THREAD_NUM = "server.rest.service.storage.thread.num";
  public static final String SERVER_NETTY_IDLE_TIME_SECONDS = "server.netty.idle.time.seconds";
  public static final String SERVER_MAX_REQUEST_SIZE = "server.max.request.size";
  public static final String SERVER_SOURCE_TOPIC_OFFSET_CHECK_INTERVAL_MS = "server.source.topic.offset.check.interval.ms";
  public static final String SERVER_NETTY_GRACEFUL_SHUTDOWN_PERIOD_SECONDS = "server.netty.graceful.shutdown.period.seconds";
  public static final String SERVER_NETTY_WORKER_THREADS = "server.netty.worker.threads";
  public static final String SSL_TO_KAFKA = "ssl.to.kakfa";
  public static final String SERVER_COMPUTE_THREAD_NUM = "server.compute.thread.num";
  public static final String HYBRID_QUOTA_ENFORCEMENT_ENABLED = "server.hybrid.quota.enforcement.enabled";
  public static final String SERVER_DATABASE_MEMORY_STATS_ENABLED = "server.database.memory.stats.enabled";

  public static final String ROUTER_MAX_READ_CAPACITY = "router.max.read.capacity";
  public static final String ROUTER_QUOTA_CHECK_WINDOW = "router.quota.check.window";

  /**
   * Whether to enable epoll in rest service layer.
   * This will be a best-effort since epoll support is only available in Linux, not Mac.
   */
  public static final String SERVER_REST_SERVICE_EPOLL_ENABLED = "server.rest.service.epoll.enabled";
  /**
   * Database sync per bytes for transactional mode.
   * This parameter will impact the sync frequency of database after batch push.
   * For BDB-JE transactional mode, it won't matter since BDB-JE will persist every update in the database right away;
   * For RocksDB transactional mode, it will impact the flush frequency of memtable to SST file, and normally we would
   * like to have this config to be comparable to the memtable size;
   *
   * Negative value will disable this threshold.
   */
  public static final String SERVER_DATABASE_SYNC_BYTES_INTERNAL_FOR_TRANSACTIONAL_MODE = "server.database.sync.bytes.interval.for.transactional.mode";
  /**
   * Database sync per bytes for deferred-write mode.
   * This parameter will impact the sync frequency of database during batch push.
   * For BDB-JE deferred-write mode, it will impact the sync frequency, but BDB-JE will do auto-flush if the memory is full;
   * For RocksDB deferred-write mode, it will decide the file size of each SST file since every sync invocation will
   * generate a new SST file;
   *
   * Negative value will disable this threshold.
   */
  public static final String SERVER_DATABASE_SYNC_BYTES_INTERNAL_FOR_DEFERRED_WRITE_MODE = "server.database.sync.bytes.interval.for.deferred.write.mode";

  /**
   * When load balance happens, a replica could be moved to another storage node.
   * When dropping the existing replica through Helix state transition: 'ONLINE' -> 'OFFLINE' and 'OFFLINE' -> 'DROPPED',
   * a race condition could happen since Router in-memory partition assignment update through Zookeeper
   * is independent from database drop in storage node, so Router could possibly forward the request to the storage node,
   * which has just dropped the partition.
   *
   * To mitigate this issue, we will add a delay in state transition: 'OFFLINE' -> 'DROPPED' to drain all the incoming
   * requests to the to-drop partition, and we will enable error-retry on Router as well.
   */
  public static final String SERVER_PARTITION_GRACEFUL_DROP_DELAY_IN_SECONDS = "server.partition.graceful.drop.time.in.seconds";

  /**
   * When a BDB partition is dropped, the disk space is not released immediately; a checkpoint is needed to release the disk space;
   * so a cleaner thread is spawned for the entire storage service; the cleaner thread will wake up every few hours and check
   * whether it needs to do a checkpoint; if so, clean up each store sequentially.
   */
  public static final String SERVER_LEAKED_RESOURCE_CLEAN_UP_INTERVAL_IN_MINUTES = "server.leaked.resource.clean.up.interval.in.minutes";

  /**
   * For batch-only store, enabling read-only could improve the performance greatly.
   */
  public static final String SERVER_DB_READ_ONLY_FOR_BATCH_ONLY_STORE_ENABLED = "server.db.read.only.for.batch.only.store.enabled";

  /**
   * Set to true to enable enforcement of quota by the storage node
   */
  public static final String SERVER_QUOTA_ENFORCEMENT_ENABLED = "server.quota.enforcement.enabled";

  /**
   * Number of Read Capacity Units per second that the node can handle across all stores.
   */
  public static final String SERVER_NODE_CAPACITY_RCU = "server.node.capacity.rcu.per.second";

  /**
   * This config is used to control the maximum records returned by every poll request.
   * So far, Store Ingestion is throttling per poll, so if the configured value is too big,
   * the throttling could be inaccurate and it may impact GC as well.
   *
   * We should try to avoid too many long-lasting objects in JVM to minimize GC overhead.
   */
  public static final String SERVER_KAFKA_MAX_POLL_RECORDS = "server.kafka.max.poll.records";

  /**
   * This config is used to control how many times Kafka consumer would retry polling during ingestion
   * when hitting {@literal org.apache.kafka.common.errors.RetriableException}.
   */
  public static final String SERVER_KAFKA_POLL_RETRY_TIMES = "server.kafka.poll.retry.times";

  /**
   * This config is used to control the backoff time between Kafka consumer poll retries.
   */
  public static final String SERVER_KAFKA_POLL_RETRY_BACKOFF_MS = "server.kafka.poll.backoff.ms";

  /**
   * This config decides the frequency of the disk health check; the disk health check service writes
   * 64KB data to a temporary file in the database directory and read from the file for each health check.
   */
  public static final String SERVER_DISK_HEALTH_CHECK_INTERVAL_IN_SECONDS = "server.disk.health.check.interval.in.seconds";

  /**
   * When there is an actual disk failure, health check operation would hang, so this config decides how fast the
   * servers will start reporting unhealthy after the health check stop updating status; however, in order to
   * reduce the possibility of false alerts (for example, the health check updates can be delayed by GC), we couldn't
   * set the timeout too small. Currently by default, the timeout is 30 seconds.
   */
  public static final String SERVER_DISK_HEALTH_CHECK_TIMEOUT_IN_SECONDS = "server.disk.health.check.timeout.in.seconds";

  /**
   * This config is used to enable/disable the disk health check service.
   */
  public static final String SERVER_DISK_HEALTH_CHECK_SERVICE_ENABLED = "server.disk.health.check.service.enabled";

  /**
   * Whether to enable fast-avro in compute request path.
   */
  public static final String SERVER_COMPUTE_FAST_AVRO_ENABLED = "server.compute.fast.avro.enabled";

  /**
   * Whether to enable parallel lookup for batch-get.
   */
  public static final String SERVER_ENABLE_PARALLEL_BATCH_GET = "server.enable.parallel.batch.get";

  /**
   * Chunk size of each task for parallel lookup of batch-get.
   */
  public static final String SERVER_PARALLEL_BATCH_GET_CHUNK_SIZE = "server.parallel.batch.get.chunk.size";

  /**
   * The request early termination threshold map:
   * The key will be store name, and the value will be the actual threshold.
   * This config is temporary, and in the long run, we will ask Venice Client to pass the actual timeout to the backend.
   */
  public static final String SERVER_STORE_TO_EARLY_TERMINATION_THRESHOLD_MS_MAP = "server.store.to.early.termination.threshold.ms.map";

  /**
   * The following config is used to control the maximum database lookup requests queued, when the queue is full,
   * server will propagate the back pressure to the caller.
   */
  public static final String SERVER_DATABASE_LOOKUP_QUEUE_CAPACITY = "server.database.lookup.queue.capacity";

  /**
   * Check @{@link #SERVER_DATABASE_LOOKUP_QUEUE_CAPACITY} for the explanation.
   * The following config is specifically for compute request.
   */
  public static final String SERVER_COMPUTE_QUEUE_CAPACITY = "server.compute.queue.capacity";

  /**
   * Check the available types in {@literal com.linkedin.venice.config.BlockingQueueType}
   */
  public static final String SERVER_BLOCKING_QUEUE_TYPE = "server.blocking.queue.type";

  /**
   * This config is used to control whether openssl is enabled for Kafka consumers in server.
   */
  public static final String SERVER_ENABLE_KAFKA_OPENSSL = "server.enable.kafka.openssl";

  /**
   * This config is used to control how much time Server will wait for connection warming from Routers.
   * This is trying to avoid availability issue when router connection warming happens when Server restarts.
   * In theory, this config should be equal to or bigger than {@link #ROUTER_HTTPASYNCCLIENT_CONNECTION_WARMING_NEW_INSTANCE_DELAY_JOIN_MS}.
   */
  public static final String SERVER_ROUTER_CONNECTION_WARMING_DELAY_MS = "server.router.connection.warming.delay.ms";

  /**
   * Whether to enable shared consumer pool in storage node.
   */
  public static final String SERVER_SHARED_CONSUMER_POOL_ENABLED = "server.shared.consumer.pool.enabled";

  /**
   * Consumer pool size per Kafka cluster.
   */
  public static final String SERVER_CONSUMER_POOL_SIZE_PER_KAFKA_CLUSTER = "server.consumer.pool.size.per.kafka.cluster";

  /**
   * Whether to enable leaked resource cleanup in storage node.
   * Right now, it only covers leaked storage partitions on disk.
   */
  public static final String SERVER_LEAKED_RESOURCE_CLEANUP_ENABLED = "server.leaked.resource.cleanup.enabled";

  /**
   * Whether Server will try to warm up cache before reporting ready-to-serve or not.
   */
  public static final String SERVER_CACHE_WARMING_BEFORE_READY_TO_SERVE_ENABLED = "server.cache.warming.before.ready.to.serve.enabled";

  /**
   * Store list to enable cache warming, and it is comma separated list.
   * TODO: once this has bee proved to be useful, we will need to store it as a store-level config.
   */
  public static final String SERVER_CACHE_WARMING_STORE_LIST = "server.cache.warming.store.list";

  /**
   * Cache warming thread pool, and this is used to try to not overwhelm the storage node by cache warming.
   */
  public static final String SERVER_CACHE_WARMING_THREAD_POOL_SIZE = "server.cache.warming.thread.pool.size";

  /**
   * The delay serving of the newly started storage node.
   * The reason to have this config is that we noticed a high GC pause for some time because of connection warming or initializing the
   * internal components.
   * We will need to do some experiment to find the right value.
   */
  public static final String SERVER_DELAY_REPORT_READY_TO_SERVE_MS = "server.delay.report.ready.to.serve.ms";

  /**
   * Ingestion isolation mode in target storage instance.
   * This will be applied to Da Vinci and Storage Node.
   */
  public static final String SERVER_INGESTION_ISOLATION_MODE = "server.ingestion.isolation.mode";

  /**
   * Port number for ingestion listener. For Parent/Child mode, it will be used by child process. For SplitService mode,
   * it will be used by IngestionService.
   */
  public static final String SERVER_INGESTION_ISOLATION_SERVICE_PORT = "server.ingestion.isolation.service.port";

  /**
   * Port number for ingestion listener. For Parent/Child mode, it will be used by parent process. For SplitService mode,
   * it will be used by venice server that are using the ingestion service.
   */
  public static final String SERVER_INGESTION_ISOLATION_APPLICATION_PORT = "server.ingestion.isolation.application.port";

  /**
   * Whether to enable storage engine to restore existing data partitions during creation.
   */
  public static final String SERVER_RESTORE_DATA_PARTITIONS_ENABLED = "server.restore.data.partitions.enabled";

  /**
   * Whether to enable storage engine to restore existing metadata partition during creation.
   */
  public static final String SERVER_RESTORE_METADATA_PARTITION_ENABLED = "server.restore.data.partition.enabled";

  /**
   * whether to enable checksum verification in the ingestion path from kafka to database persistency. If enabled it will
   * keep a running checksum for all and only PUT kafka data message received in the ingestion task and periodically
   * verify it against the key/values saved in the database persistency layer.
   */
  public static final String SERVER_DATABASE_CHECKSUM_VERIFICATION_ENABLED = "server.database.checksum.verification.enabled";

  /**
   * Any server config that start with "server.local.consumer.config.prefix" will be used as a customized consumer config
   * for local consumer.
   */
  public static final String SERVER_LOCAL_CONSUMER_CONFIG_PREFIX = "server.local.consumer.config.prefix.";

  /**
   * Any server config that start with "server.remote.consumer.config.prefix" will be used as a customized consumer config
   * for remote consumer.
   */
  public static final String SERVER_REMOTE_CONSUMER_CONFIG_PREFIX = "server.remote.consumer.config.prefix.";

  /**
   * Whether to enable to check the RocksDB storage type used to open the RocksDB storage engine and how it was built.
   * Having different storage types (BlockBasedTable and PlainTable) in read ops and write ops may lead to corruption of
   * RocksDB storage and crash of servers.
   */
  public static final String SERVER_ROCKSDB_STORAGE_CONFIG_CHECK_ENABLED = "server.rocksdb.storage.config.check.enabled";

  // Router specific configs
  // TODO the config names are same as the names in application.src, some of them should be changed to keep consistent
  // TODO with controller and server.
  public static final String LISTENER_SSL_PORT = "listener.ssl.port";
  public static final String CLIENT_TIMEOUT = "client.timeout";
  public static final String HEARTBEAT_TIMEOUT =  "heartbeat.timeout";
  public static final String HEARTBEAT_CYCLE =  "heartbeat.cycle";
  public static final String MAX_READ_CAPCITY = "max.read.capacity";
  public static final String SSL_TO_STORAGE_NODES = "sslToStorageNodes";
  /**
   * After this amount of time, DDS Router will retry once for the slow storage node request.
   *
   * Practically, we need to manually select the threshold (e.g. P95) for retrying based on latency metrics.
   */
  public static final String ROUTER_LONG_TAIL_RETRY_FOR_SINGLE_GET_THRESHOLD_MS = "router.long.tail.retry.for.single.get.threshold.ms";

  /**
   * After this amount of time, DDS Router will retry once for the slow storage node request.
   *
   * The configured format will be like this way:
   * "1-10:20,11-50:50,51-200:80,201-:1000"
   *
   * Let me explain the config by taking one example:
   * If the request key count for a batch-get request is '32', and it fails into this key range: [11-50], so the retry
   * threshold for this batch-get request is 50ms.
   *
   * That is a limitation here:
   * The retry threshold is actually for each scatter-gather request, but this config is not strictly with the actual key count
   * inside each scatter-gather request, which means even if there is only one key in a scatter-gather request with
   * the above example, Router will wait for 50ms to retry this scatter-gather request.
   *
   * For now, it is not big issue since for now we mostly want to use this config to skip the storage node, which
   * is experiencing long GC pause.
   * So coarse-grained config should be good enough.
   *
   */
  public static final String ROUTER_LONG_TAIL_RETRY_FOR_BATCH_GET_THRESHOLD_MS = "router.long.tail.retry.for.batch.get.threshold.ms";

  /**
   * Whether to enable smart long tail retry logic, and this logic is only useful for batch-get retry currently.
   * This feature is used to avoid the unnecessary retries in the following scenarios:
   * 1. Router is suffering long GC pause, no matter whether Storage Node is fast or not;
   * 2. The retried Storage Node is slow according to the original request;
   *
   * For case 1, unnecessary retries will make Router GC behavior even worse;
   * For case 2, unnecessary retries to the slow Storage Node will make the slow Storage Node even slower, and the
   * overall latency won't be improved;
   *
   * For case 1, here is how smart retry works:
   * 1. When the delay between the retry request and the original request is over {@link #ROUTER_LONG_TAIL_RETRY_FOR_BATCH_GET_THRESHOLD_MS}
   *   + {@link #ROUTER_SMART_LONG_TAIL_RETRY_ABORT_THRESHOLD_MS}, smart retry logic will treat the current Router to be
   *   in bad state (long GC pause or too busy), the retry request will be aborted;
   * 2.{@link #ROUTER_SMART_LONG_TAIL_RETRY_ABORT_THRESHOLD_MS} is the way to measure whether Router is in good state or not,
   *   and need to be tuned in prod;
   *
   * For case 2, the retry request will be aborted if the original request to the same storage node hasn't returned,
   * and the slowness measurement is inside one request when scatter-gathering.
   */
  public static final String ROUTER_SMART_LONG_TAIL_RETRY_ENABLED = "router.smart.long.tail.retry.enabled";

  /**
   * This config is used to tune the smart long-tail retry logic to avoid unnecessary retries,
   * check more details: {@link #ROUTER_SMART_LONG_TAIL_RETRY_ENABLED}
   */
  public static final String ROUTER_SMART_LONG_TAIL_RETRY_ABORT_THRESHOLD_MS = "router.smart.long.tail.retry.abort.threshold.ms";

  /**
   * This config is used to limit the maximum retries in route unit.
   * In large batch-get/compute cluster, when enabling long-tail retry, in the worst scenarios, Router could trigger a
   * retry storm since each route could retry independently.
   * This config is used to specify the maximum retry in route unit.
   * If the configured value is 1, it means the current request will at most one route.
   * This could mitigate the latency issue in most of the case since the chance to have multiple slow storage nodes is low,
   * also even with unlimited retries, it won't help since multiple replicas for the same partition are in a degraded state.
   */
  public static final String ROUTER_LONG_TAIL_RETRY_MAX_ROUTE_FOR_MULTI_KEYS_REQ = "router.long.tail.retry.max.route.for.multi.keys.req";

  /**
   * The max key count allowed in one multi-get request.
   * For now, it is configured in host level, and we could consider to configure it in store level.
   */
  public static final String ROUTER_MAX_KEY_COUNT_IN_MULTIGET_REQ = "router.max.key_count.in.multiget.req";
  public static final String ROUTER_CONNECTION_LIMIT = "router.connection.limit";
  /**
   * The http client pool size being used in one Router;
   */
  public static final String ROUTER_HTTP_CLIENT_POOL_SIZE = "router.http.client.pool.size";
  /**
   * The max connection number per route (to one storage node);
   */
  public static final String ROUTER_MAX_OUTGOING_CONNECTION_PER_ROUTE = "router.max.outgoing.connection.per.route";
  /**
   * The max connection number in one Router to storage nodes;
   */
  public static final String ROUTER_MAX_OUTGOING_CONNECTION = "router.max.outgoing.connection";

  /**
   * This config is used to bound the pending request.
   * Without this config, the accumulated requests in Http Async Client could grow unlimitedly,
   * which would put Router in a non-recoverable state because of long GC pause introduced
   * by the increasing memory usage.
   *
   * If the incoming request exceeds this configured threshold, Router will return 503 (Service Unavailable).
   */
  public static final String ROUTER_MAX_PENDING_REQUEST = "router.max.pending.request";

  /**
   * Whether sticky routing for single-get is enabled in Router.
   * Basically, sticky routing will ensure that the requests belonging to the same partition will always go to
   * the same storage node if rebalance/deployment doesn't happen.
   * With this way, the cache efficiency will be improved a lot in storage node since each storage node only needs
   * to serve 1/3 of key space in the most scenarios.
   */
  public static final String ROUTER_ENABLE_STICKY_ROUTING_FOR_SINGLE_GET = "router.enable.sticky.routing.for.single.get";

  /**
   * This config is used to define the routing strategy for multi-key requests.
   * Please check {@literal VeniceMultiKeyRoutingStrategy} to find available routing strategy.
   */
  public static final String ROUTER_MULTI_KEY_ROUTING_STRATEGY = "router.multi.key.routing.strategy";

  /**
   * The Helix virtual group field name in domain, and the allowed values: {@link com.linkedin.venice.helix.HelixInstanceConfigRepository#GROUP_FIELD_NAME_IN_DOMAIN}
   * and {@link com.linkedin.venice.helix.HelixInstanceConfigRepository#ZONE_FIELD_NAME_IN_DOMAIN}.
   */
  public static final String ROUTER_HELIX_VIRTUAL_GROUP_FIELD_IN_DOMAIN = "router.helix.virtual.group.field.in.domain";

  /**
   * Helix group selection strategy when Helix assisted routing is enabled.
   * Available strategies listed here: {@literal HelixGroupSelectionStrategyEnum}.
   */
  public static final String ROUTER_HELIX_ASSISTED_ROUTING_GROUP_SELECTION_STRATEGY = "router.helix.assisted.routing.group.selection.strategy";

  /**
   * The buffer we will add to the per storage node read quota. E.g 0.5 means 50% extra quota.
   */
  public static final String ROUTER_PER_STORAGE_NODE_READ_QUOTA_BUFFER = "router.per.storage.node.read.quota.buffer";

  /**
   * Whether router cache is enabled or not.
   */
  public static final String ROUTER_CACHE_ENABLED = "router.cache.enabled";

  /**
   * Router cache size, and this cache is for all the stores, which enables cache feature.
   */
  public static final String ROUTER_CACHE_SIZE_IN_BYTES = "router.cache.size.in.bytes";

  /**
   * Concurrency setup for router cache, and this is must be power of 2 when using 'OFF_HEAP_CACHE'.
   */
  public static final String ROUTER_CACHE_CONCURRENCY = "router.cache.concurrency";

  /**
   * Valid cache types: 'ON_HEAP_CACHE', 'OFF_HEAP_CACHE'.
   */
  public static final String ROUTER_CACHE_TYPE = "router.cache.type";

  /**
   * Valid cache eviction algorithms: 'LRU', 'W_TINY_LFU'.
   *
   * For 'ON_HEAP_CACHE', 'LRU' is the only available cache eviction for now.
   */
  public static final String ROUTER_CACHE_EVICTION = "router.cache.eviction";

  /**
   * Max hash table size per cache segment, and it must be power of 2, and it is only useful when using 'OFF_HEAP_CACHE'.
   */
  public static final String ROUTER_CACHE_HASH_TABLE_SIZE = "router.cache.hash.table.size";

  /**
   * The TTL for each entry in router cache (millisecond)
   * If 0, TTL is not enabled; other, cache TTL is enabled
   */
  public static final String ROUTER_CACHE_TTL_MILLIS = "router.cache.ttl.millis";

  /**
   * The request is still being throttled even it is a cache hit, but just with smaller weight.
   */
  public static final String ROUTER_CACHE_HIT_REQUEST_THROTTLE_WEIGHT = "router.cache.hit.request.throttle.weight";

  /**
   * Whether to enable customized dns cache in router or not.
   * This is mostly to address slow DNS lookup issue.
   */
  public static final String ROUTER_DNS_CACHE_ENABLED = "router.dns.cache.enabled";

  /**
   * The host matching the configured host pattern will be cached if {@link #ROUTER_DNS_CACHE_ENABLED} is true.
   */
  public static final String ROUTE_DNS_CACHE_HOST_PATTERN = "router.dns.cache.host.pattern";

  /**
   * Refresh interval of cached dns entries if {@link #ROUTER_DNS_CACHE_ENABLED} is true.
   */
  public static final String ROUTER_DNS_CACHE_REFRESH_INTERVAL_MS = "router.dns.cache.refresh.interval.ms";

  /**
   * Whether the router use netty http client or apache http async client
   */
  public static final String ROUTER_STORAGE_NODE_CLIENT_TYPE = "router.storage.node.client.type";

  /**
   * Number of event loop; one thread for each event loop.
   */
  public static final String ROUTER_NETTY_CLIENT_EVENT_LOOP_THREADS = "router.netty.client.event.loop.threads";

  /**
   * Timeout for getting a channel from channel pool; if timeout, create a new channel
   */
  public static final String ROUTER_NETTY_CLIENT_CHANNEL_POOL_ACQUIRE_TIMEOUT_MS = "router.netty.client.channel.pool.acquire.timeout.ms";

  /**
   * Minimum connections for each host (a host is identified by InetSocketAddress)
   */
  public static final String ROUTER_NETTY_CLIENT_CHANNEL_POOL_MIN_CONNECTIONS = "router.netty.client.channel.pool.min.connections";

  /**
   * Maximum connections for each host/InetSocketAddress
   */
  public static final String ROUTER_NETTY_CLIENT_CHANNEL_POOL_MAX_CONNECTIONS = "router.netty.client.channel.pool.max.connections";

  /**
   * The maximum number of pending acquires for a channel in the channel pool.
   *
   * If the pending acquires exceed the threshold, netty client will fail the new requests without blocking and it won't
   * throw exception in the router thread.
   */
  public static final String ROUTER_NETTY_CLIENT_CHANNEL_POOL_MAX_PENDING_ACQUIRES = "router.netty.client.channel.pool.max.pending.acquires";

  /**
   * Interval between each channel health check
   */
  public static final String ROUTER_NETTY_CLIENT_CHANNEL_POOL_HEALTH_CHECK_INTERVAL_MS = "router.netty.client.channel.pool.health.check.interval.ms";

  /**
   * The maximum length of the aggregated content (response from SN to router) in bytes.
   *
   * If the length of the aggregated content exceeds this value, an exception will be thrown in router and the channel that
   * receives this response will be closed.
   */
  public static final String ROUTER_NETTY_CLIENT_MAX_AGGREGATED_OBJECT_LENGTH = "router.netty.client.max.aggregated.object.length";

  /**
   * Netty graceful shutdown period considering the following factors:
   * 1. D2 de-announcement could take some time;
   * 2. Client could take some  time to receive/apply the zk update event from D2 server about router shutdown;
   * 3. Router needs some time to handle already-received client requests;
   */
  public static final String ROUTER_NETTY_GRACEFUL_SHUTDOWN_PERIOD_SECONDS = "router.netty.graceful.shutdown.period.seconds";

  public static final String ROUTER_CLIENT_DECOMPRESSION_ENABLED = "router.client.decompression.enabled";

  public static final String ROUTER_STREAMING_ENABLED = "router.streaming.enabled";

  /**
   * Whether to enable fast-avro in router;
   */
  public static final String ROUTER_COMPUTE_FAST_AVRO_ENABLED = "router.compute.fast.avro.enabled";

  /**
   * Socket timeout config for the connection manager from router to server
   */
  public static final String ROUTER_SOCKET_TIMEOUT = "router.socket.timeout";

  /**
   * Timeout for building a new connection from router to server
   */
  public static final String ROUTER_CONNECTION_TIMEOUT = "router.connection.timeout";

  /**
   * Whether to enable the cleanup of the idle connections to storage node.
   * Recently, we are seeing latency spike because of new connection setup, and we hope the total available connections will be
   * more stable by disabling the idle connection cleanup.
   * The potential long-term solutions could be connection warm-up for HTTP/1.1 or adopting HTTP/2
   */
  public static final String ROUTER_IDLE_CONNECTION_TO_SERVER_CLEANUP_ENABLED = "router.idle.connection.to.server.cleanup.enabled";

  /**
   * The idle threshold for cleaning up the connections to storage node.
   */
  public static final String ROUTER_IDLE_CONNECTION_TO_SERVER_CLEANUP_THRESHOLD_MINS = "router.idle.connection.to.server.cleanup.threshold.mins";

  /**
   * The following config controls how long the server with full pending queue will be taken OOR.
   */
  public static final String ROUTER_FULL_PENDING_QUEUE_SERVER_OOR_MS = "router.full.pending.queue.server.oor.ms";

  /**
   * Connection warming feature for httpasynclient.
   * So far, it only works when Router starts and runs in http-client-per-route mode, and it will try to warm up {@link #ROUTER_MAX_OUTGOING_CONNECTION_PER_ROUTE}
   * connections per route.
   */
  public static final String ROUTER_HTTPASYNCCLIENT_CONNECTION_WARMING_ENABLED = "router.httpasyncclient.connection.warming.enabled";

  /**
   * When Router starts, for a given route, the following config controls the warming up speed to minimize the impact to storage nodes.
   */
  public static final String ROUTER_HTTPASYNCCLIENT_CONNECTION_WARMING_SLEEP_INTERVAL_MS = "router.httpasyncclient.connection.warming.sleep.interval.ms";

  /**
   * When the available connections in an httpasyncclient is below the low water mark, the connection warming service will try to
   * spin up a new client to replace it.
   * In theory, this config must be lower than  {@link #ROUTER_MAX_OUTGOING_CONNECTION_PER_ROUTE}.
   */
  public static final String ROUTER_HTTPASYNCCLIENT_CONNECTION_WARMING_LOW_WATER_MARK = "router.httpasyncclient.connection.warming.low.water.mark";

  /**
   * Connection warming executor thread num.
   */
  public static final String ROUTER_HTTPASYNCCLIENT_CONNECTION_WARMING_EXECUTOR_THREAD_NUM = "router.httpasyncclient.connection.warming.executor.thread.num";

  /**
   * For the new instance (Storage Node) detected by Router, the following config defines how much delay because of connection warming it could tolerate.
   * If the connection warming takes longer than it, Router will put it in to serve online traffic by creating a new client without connection warming.
   */
  public static final String ROUTER_HTTPASYNCCLIENT_CONNECTION_WARMING_NEW_INSTANCE_DELAY_JOIN_MS  = "router.httpasyncclient.connection.warming.new.instance.delay.join.ms";

  /**
   * This config is used to control the socket timeout for connection warming requests.
   * In some cases, we would like to have different(maybe longer timeout) than the regular requests considering the deployment procedure,
   * and the connection warming requests could be very instensive.
   */
  public static final String ROUTER_HTTPAYSNCCLIENT_CONNECTION_WARMING_SOCKET_TIMEOUT_MS = "router.httpasyncclient.connection.warming.socket.timeout.ms";

  /**
   * Whether to enable async start in Router startup procedure.
   * The reason to introduce this feature is that in some env, the dependent services could be started out of order.
   *
   * IMPORTANT: enabling this feature won't guarantee that a successful restarted Router will be in a healthy state,
   * since async start will exit later if it detects any errors.
   */
  public static final String ROUTER_ASYNC_START_ENABLED = "router.async.start.enabled";

  /**
   * Venice uses a helix cluster to assign controllers to each named venice cluster.  This is the number of controllers
   * assigned to each venice cluster.  Should normally be 3; one master controller and 2 standby controllers.
   * */
  public static final String CONTROLLER_CLUSTER_REPLICA = "controller.cluster.replica";

  /**
   * The time window in ms used to throttle the Kafka topic creation, during the time window, only 1 topic is allowed to
   * be created.
   */
  public static final String TOPIC_CREATION_THROTTLING_TIME_WINDOW_MS = "topic.creation.throttling.time.window.ms";

  /** Timeout for create topic and delete topic operations. */
  public static final String TOPIC_MANAGER_KAFKA_OPERATION_TIMEOUT_MS = "topic.manager.kafka.operation.timeout.ms";


  /**
   * This is the minimum number of Kafka topics that are guaranteed to be preserved by the leaky topic clean
   * up routine. The topics with the highest version numbers will be favored by this preservative behavior.
   * All other topics (i.e.: those with smaller version numbers) which Venice does not otherwise know about
   * from its metadata will be considered leaked resources and thus be eligible for clean up.
   *
   * A value greater than zero is recommended for Mirror Maker stability.
   *
   * N.B.: A known limitation of this preservation setting is that during store deletion, if a topic has been
   * leaked recently due to an aborted push, then there is an edge case where that topic may leak forever.
   * This leak does not happen if the latest store-versions are successful pushes, rather than failed ones.
   * Furthermore, if a store with the same name is ever re-created, then the clean up routine would resume
   * and clean up the older leaky topics successfully. This edge case is deemed a small enough concern for
   * now, though it could be addressed with a more significant redesign of the replication pipeline.
   *
   * @see
   */
  public static final String MIN_NUMBER_OF_UNUSED_KAFKA_TOPICS_TO_PRESERVE = "min.number.of.unused.kafka.topics.to.preserve";

  /**
   * This is the number of fully-functional store-versions we wish to maintain. All resources of these versions
   * will be preserved (Helix resource, Storage Node data, Kafka topic, replication streams, etc.), and a swap
   * to these versions should be possible at all times.
   *
   * This setting must be set to 1 or greater.
   */
  public static final String MIN_NUMBER_OF_STORE_VERSIONS_TO_PRESERVE = "min.number.of.store.versions.to.preserve";

  /** Whether current controller is parent or not */
  public static final String CONTROLLER_PARENT_MODE = "controller.parent.mode";

  /**
   * This config is used to control how many errored topics we are going to keep in parent cluster.
   * This is mostly used to investigate the Kafka missing message issue.
   * If the issue gets resolved, we could change this config to be '0'.
   */
  public static final String PARENT_CONTROLLER_MAX_ERRORED_TOPIC_NUM_TO_KEEP = "parent.controller.max.errored.topic.num.to.keep";

  /**
   * Only required when controller.parent.mode=true
   * This prefix specifies the location of every child cluster that is being fed by this parent cluster.
   * The format for key/value would be like "key=child.cluster.url.ei-ltx1, value=url1;url2;url3"
   * the cluster name should be human readable, ex: ei-ltx1
   * the url should be of the form http://host:port
   *
   * Note that every cluster name supplied must also be specified in the child.cluster.whitelist in order to be included
   * */
  public static final String CHILD_CLUSTER_URL_PREFIX = "child.cluster.url";

  /**
   * Similar to {@link ConfigKeys#CHILD_CLUSTER_URL_PREFIX} but with D2 url.
   */
  public static final String CHILD_CLUSTER_D2_PREFIX = "child.cluster.d2.zkHost";

  /**
   * Config prefix for Kafka bootstrap url in all child fabrics; parent controllers need to know the
   * Kafka url in all fabrics for native replication.
   */
  public static final String CHILD_DATA_CENTER_KAFKA_URL_PREFIX = "child.data.center.kafka.url";

  /**
   * Config prefix for Kafka zk address in all child fabrics; parent controllers need to know the
   * Kafka zk in all fabrics for native replication.
   */
  public static final String CHILD_DATA_CENTER_KAFKA_ZK_PREFIX = "child.data.center.kafka.zk";

  public static final String CHILD_CLUSTER_D2_SERVICE_NAME = "child.cluster.d2.service.name";

  /**
   * The default source fabric used for native replication
   */
  public static final String NATIVE_REPLICATION_SOURCE_FABRIC = "native.replication.source.fabric";

  /**
   * Only required when controller.parent.mode=true
   * This is a comma-separated whitelist of cluster names used in the keys with the child.cluster.url prefix.
   *
   * Example, if we have the following child.cluster.url keys:
   *
   * child.cluster.url.cluster1=...
   * child.cluster.url.cluster2=...
   * child.cluster.url.cluster3=...
   *
   * And we want to use all three cluster, then we set
   *
   * child.cluster.whitelist=cluster1,cluster2,cluster3
   *
   * If we only want to use clusters 1 and 3 we can set
   *
   * child.cluster.whitelist=cluster1,cluster3
   *
   */
  public static final String CHILD_CLUSTER_WHITELIST = "child.cluster.whitelist";

  /**
   * Previously {@link #CHILD_CLUSTER_WHITELIST} is used to also represent the white list of source fabrics
   * for native replication; however, the final migration plan decides that a Kafka cluster in parent fabric
   * can also be the source fabric, so the below config is introduced to represent all potential source
   * fabrics for native replication.
   */
  public static final String NATIVE_REPLICATION_FABRIC_WHITELIST = "native.replication.fabric.whitelist";

  /**
   * A list of potential parent fabrics. Logically, there is only one parent fabric; during native replication
   * migration, there will be two Kafka clusters in parent fabric, so we need two fabric names to represent
   * the two different Kafka cluster url.
   *
   * TODO: deprecate this config after native replication migration is complete.
   */
  public static final String PARENT_KAFKA_CLUSTER_FABRIC_LIST = "parent.kafka.cluster.fabric.list";

  /**
   * When the parent controller receives an admin write operation, it replicates that message to the admin kafka stream.
   * After replication the parent controller consumes the message from the stream and processes it there.  This is the
   * timeout for waiting until that consumption happens.
   * */
  public static final String PARENT_CONTROLLER_WAITING_TIME_FOR_CONSUMPTION_MS = "parent.controller.waiting.time.for.consumption.ms";

  /**
   * If there is a failure in consuming from the admin topic, skip the message after retrying for this many minutes
   * Default 5 days
   */
  public static final String ADMIN_CONSUMPTION_TIMEOUT_MINUTES = "admin.consumption.timeout.minute";

  /**
   * The maximum time allowed for worker threads to execute admin messages in one cycle. A cycle is the processing of
   * delegated admin messages by some number of worker thread(s) defined by {@code ADMIN_CONSUMPTION_MAX_WORKER_THREAD_POOL_SIZE}.
   * Each worker thread will try to empty the queue for a store before moving on to process admin messages for another
   * store. The cycle is completed either by finishing all delegated admin messages or timing out with this config.
   * TODO: Note that the timeout is for all stores in the cycle and not individual stores. Meaning that some stores may starve.
   */
  public static final String ADMIN_CONSUMPTION_CYCLE_TIMEOUT_MS = "admin.consumption.cycle.timeout.ms";

  /**
   * The maximum number of threads allowed in the pool for executing admin messages.
   */
  public static final String ADMIN_CONSUMPTION_MAX_WORKER_THREAD_POOL_SIZE = "admin.consumption.max.worker.thread.pool.size";

  /**
   * This factor is used to estimate potential push size. H2V reducer multiplies it
   * with total record size and compares it with store storage quota
   * TODO: it will be moved to Store metadata if we allow stores have various storage engine types.
   */
  public static final String STORAGE_ENGINE_OVERHEAD_RATIO = "storage.engine.overhead.ratio";

  /**
   * Env variable for setting keystore when running Venice with quickstart.
   */
  public static final String KEYSTORE_ENV = "VENICE_KEYSTORE";

  /**
   * The switcher to enable/disable the whitelist of ssl offline pushes. If we disable the whitelist here, depends on
   * the config "SSL_TO_KAFKA", all pushes will be secured by SSL or none of pushes will be secured by SSL.
   */
  public static final String ENABLE_OFFLINE_PUSH_SSL_WHITELIST = "enable.offline.push.ssl.whitelist";
  /**
   * The switcher to enable/disable the whitelist of ssl hybrid pushes including both batch and near-line pushes for
   * that store. If we disable the whitelist here, depends on the config "SSL_TO_KAFKA", all pushes will be secured by
   * SSL or none of pushes will be secured by SSL.
   */
  public static final String ENABLE_HYBRID_PUSH_SSL_WHITELIST = "enable.hybrid.push.ssl.whitelist";

  /**
   * Whitelist of stores which are allowed to push data with SSL.
   */
  public static final String PUSH_SSL_WHITELIST = "push.ssl.whitelist";

  /**
   * Whether to block storage requests on the non-ssl port.  Will still allow metadata requests on the non-ssl port
   * and will log storage requests on the non-ssl port even if set to false;
   */
  public static final String ENFORCE_SECURE_ROUTER = "router.enforce.ssl";

  public static final String HELIX_REBALANCE_ALG = "helix.rebalance.alg";

  /**
   * Whether to establish SSL connection to Brooklin.
   */
  public static final String BROOKLIN_SSL_ENABLED  = "brooklin.ssl.enabled";

  /**
   * What replication factor should the admin topics have, upon creation.
   */
  public static final String ADMIN_TOPIC_REPLICATION_FACTOR = "admin.topic.replication.factor";

  public static final String SERVER_DISK_FULL_THRESHOLD = "disk.full.threshold";

  /**
   * If a request is slower than this, it will be reported as tardy in the router metrics
   */
  public static final String ROUTER_SINGLEGET_TARDY_LATENCY_MS = "router.singleget.tardy.latency.ms";
  public static final String ROUTER_MULTIGET_TARDY_LATENCY_MS = "router.multiget.tardy.latency.ms";
  public static final String ROUTER_COMPUTE_TARDY_LATENCY_MS = "router.compute.tardy.latency.ms";

  public static final String ROUTER_ENABLE_READ_THROTTLING = "router.enable.read.throttling";

  /**
   * This config is for {@literal LeakedCompletableFutureCleanupService}.
   * Polling interval.
   */
  public static final String ROUTER_LEAKED_FUTURE_CLEANUP_POLL_INTERVAL_MS = "router.leaked.future.cleanup.poll.interval.ms";
  /**
   * This config is for {@literal LeakedCompletableFutureCleanupService}.
   * If the CompletableFuture stays in current service beyonds the configured threshold,
   * {@literal LeakedCompletableFutureCleanupService} will complete it exceptionally.
   */
  public static final String ROUTER_LEAKED_FUTURE_CLEANUP_THRESHOLD_MS = "router.leaked.future.cleanup.threshold.ms";

  /**
   * The name of the cluster that the internal store for storing push job details records belongs to.
   */
  public static final String PUSH_JOB_STATUS_STORE_CLUSTER_NAME = "controller.push.job.status.store.cluster.name";

  /**
   * The job tracker identifier as part of a map reduce job id.
   */
  public static final String PUSH_JOB_MAP_REDUCE_JT_ID = "push.job.map.reduce.jt.id";

  /**
   * The job identifier as part of a map reduce job id.
   */
  public static final String PUSH_JOB_MAP_REDUCE_JOB_ID = "push.job.map.reduce.job.id";

  /**
   * Flag to indicate whether to perform add version and start of ingestion via the admin protocol.
   */
  public static final String CONTROLLER_ADD_VERSION_VIA_ADMIN_PROTOCOL = "controller.add.version.via.admin.protocol";

  public static final String CONTROLLER_EARLY_DELETE_BACKUP_ENABLED = "controller.early.delete.backup.enabled";
  /**
   * Flag to skip buffer replay for hybrid store.
   * For some scenario, buffer replay might not be necessary since the store version topic has already included all the data.
   */
  public static final String CONTROLLER_SKIP_BUFFER_REPLAY_FOR_HYBRID = "controller.skip.buffer.replay.for.hybrid";

  /**
   * Flag to indicate which push monitor controller will pick up for an upcoming push
   */
  public static final String PUSH_MONITOR_TYPE = "push.monitor.type";

  /**
   * Flag to enable the participant message store setup and write operations to the store.
   */
  public static final String PARTICIPANT_MESSAGE_STORE_ENABLED = "participant.message.store.enabled";

  /**
   * The name of the cluster that should host the special stores used to serve system schemas.
   */
  public static final String CONTROLLER_SYSTEM_SCHEMA_CLUSTER_NAME = "controller.system.schema.cluster.name";

  /**
   * Flag to enable the controller to send kill push job helix messages to the storage node upon consuming kill push job
   * admin messages.
   */
  public static final String ADMIN_HELIX_MESSAGING_CHANNEL_ENABLED = "admin.helix.messaging.channel.enabled";

  /**
   * Minimum delay between each cycle where the storage node polls the participant message store to see if any of its
   * ongoing push job has been killed.
   */
  public static final String PARTICIPANT_MESSAGE_CONSUMPTION_DELAY_MS = "participant.message.consumption.delay.ms";

  public static final String ROUTER_STATEFUL_HEALTHCHECK_ENABLED = "router.stateful.healthcheck.enabled";
  /**
  * Maximum number of pending router request per storage node after which router concludes that host to be unhealthy
  * and stops sending further request to it..
  */
  public static final String ROUTER_UNHEALTHY_PENDING_CONNECTION_THRESHOLD_PER_ROUTE = "router.unhealthy.pending.connection.threshold.per.host";

  /**
   * This is the threshold for pending request queue depth per storage node after which router resumes sending requests once a storage node
   * which was previously marked unhealthy due to high ROUTER_UNHEALTHY_PENDING_CONNECTION_THRESHOLD_PER_ROUTE
   */
  public static final String ROUTER_PENDING_CONNECTION_RESUME_THRESHOLD_PER_ROUTE = "router.pending.connection.resume.threshold.per.host";

  /**
   * Enables HttpAsyncClient allocation per storage node.
   */
  public static final String ROUTER_PER_NODE_CLIENT_ENABLED = "router.per.node.client.enabled";

  public static final String ROUTER_PER_NODE_CLIENT_THREAD_COUNT = "router.per.node.client.thread.count";

  /**
   * In Leader/Follower state transition model, in order to avoid split brain problem (multiple leaders) as much as possible,
   * the newly promoted leader should keep checking whether there is any new messages from the old leader in the version
   * topic, and wait for some time (5 minutes by default) after the last message consumed before switching to leader role
   * and potential starts producing to the version topic. Basically, the wait time could help us avoid the scenario that
   * more than one replica is producing to the version topic.
   */
  public static final String SERVER_PROMOTION_TO_LEADER_REPLICA_DELAY_SECONDS = "server.promotion.to.leader.replica.delay.seconds";

  /**
   * This config defines whether SSL is enabled in controller.
   */
  public static final String CONTROLLER_SSL_ENABLED = "controller.ssl.enabled";

  /**
   * Flag to indicate if the controller cluster leader will be amongst one of the local Helix as a library controllers
   * or a Helix as a service controller running remotely.
   */
  public static final String CONTROLLER_CLUSTER_LEADER_HAAS = "controller.cluster.leader.haas.enabled";

  /**
   * This config defines the source directory for gobblin deltas publisher workflow.
   */
  public static final String GOBBLIN_DELTAS_SOURCE_DIR = "gobblin.deltas.source.dir";

  /**
   * This config defines the source directory for ETL publisher workflow.
   */
  public static final String ETL_SNAPSHOT_SOURCE_DIR = "etl.snapshot.source.dir";

  /**
   * This config defines the destination directory for ETL publisher workflow.
   */
  public static final String ETL_DESTINATION_DIR_PREFIX = "etl.destination.dir.prefix";

  /**
   * This config defines the store owner's proxy user name for each store.
   */
  public static final String ETL_STORE_TO_ACCOUNT_NAME = "etl.store.to.account.name";

  /**
   * This config defines the default store owner's proxy user name for ETL publisher workflow.
   */
  public static final String ETL_STORE_TO_ACCOUNT_NAME_DEFAULT = "etl.store.to.account.name.default";

  /**
   * This config defines how many snapshots we keep for one store.
   */
  public static final String ETL_MAX_SNAPSHOTS_TO_KEEP = "etl.max.snapshots.to.keep";

  /**
   * This config defines how many daily deltas we keep for one store.
   */
  public static final String GOBBLIN_MAX_DAILY_DELTAS_TO_KEEP = "gobblin.max.daily.deltas.to.keep";

  /**
   * This config defines a list of stores which enabled on-demand etl.
   */
  public static final String ON_DEMAND_ETL_ENABLED_STORES = "on.demand.etl.enabled.stores";

  /**
   * This config defines a list of stores which enabled future etl feature.
   */
  public static final String FUTURE_ETL_ENABLED_STORES = "future.etl.enabled.stores";

  /**
   * This config define ssl key store property name for ETL workflows.
   */
  public static final String SSL_KEY_STORE_PROPERTY_NAME = "ssl.key.store.property.name";

  /**
   * This config define ssl trust store property name for ETL workflows.
   */
  public static final String SSL_TRUST_STORE_PROPERTY_NAME = "ssl.trust.store.property.name";

  /**
   * This config define ssl key store password for ETL workflows.
   */
  public static final String SSL_KEY_STORE_PASSWORD_PROPERTY_NAME = "ssl.key.store.password.property.name";

  /**
   * This config define ssl key password for ETL workflows.
   */
  public static final String SSL_KEY_PASSWORD_PROPERTY_NAME= "ssl.key.password.property.name";

  /**
   * The super cluster name for HAAS. This config is required if HAAS is enabled for the creation of helix clusters.
   */
  public static final String CONTROLLER_HAAS_SUPER_CLUSTER_NAME = "controller.haas.super.cluster.name";

  /**
   * Whether to enable batch push (including GF job) from Admin in Child Controller.
   * In theory, we should disable batch push in Child Controller no matter what, but the fact is that today there are
   * many tests, which are doing batch pushes to an individual cluster setup (only Child Controller), so disabling batch push from Admin
   * in Child Controller will require a lot of refactoring.
   * So the current strategy is to enable it by default, but disable it in EI and PROD.
   */
  public static final String CONTROLLER_ENABLE_BATCH_PUSH_FROM_ADMIN_IN_CHILD = "controller.enable.batch.push.from.admin.in.child";

  /**
   * A config that turns the key/value profiling stats on and off. This config can be placed in both Router and SNs and it
   * is off by default. When switching it on, We will emit a fine grained histogram that reflects the distribution of
   * key and value size. Since this will be run in the critical read path and it will emit additional ~20 stats, please
   * be cautious when turning it on.
   */
  public static final String KEY_VALUE_PROFILING_ENABLED = "key.value.profiling.enabled";

  /*
   * Flag to indicate if venice clusters' leader will be amongst one of the local Helix as a library controllers
   * or a Helix as a service controller running remotely.
   */
  public static final String VENICE_STORAGE_CLUSTER_LEADER_HAAS = "venice.cluster.leader.haas.enabled";

  /**
   * A config specifies which partitioning scheme should be used by KafkaPushJob.
   */
  public static final String PARTITIONER_CLASS = "partitioner.class";
  /**
   * A configs of over-partitioning factor
   * number of Kafka partitions in each partition
   */
  public static final String AMPLIFICATION_FACTOR = "amplification.factor";

  /**
   * A unique id that can represent this instance
   */
  public static final String INSTANCE_ID = "instance.id";

  /**
   * Maximum time allowed for router to download dictionary from Storage nodes.
   */
  public static final String ROUTER_DICTIONARY_RETRIEVAL_TIME_MS = "router.dictionary.retrieval.time.ms";

  /**
   * Number of threads that the Router will use to wait for dictionary to download from storage nodes and process it.
   */
  public static final String ROUTER_DICTIONARY_PROCESSING_THREADS = "router.dictionary.processing.threads";

  /**
   * The class name to use for the {@link com.linkedin.venice.kafka.admin.KafkaAdminWrapper}.
   */
  public static final String KAFKA_ADMIN_CLASS = "kafka.admin.class";

  /**
   * A config that determines whether to use Helix customized view for offline push
   */
  public static final String HELIX_OFFLINE_PUSH_ENABLED = "helix.offline.push.enabled";
  /**
   * A config that determines whether to use Helix customized view for hybrid store quota
   */
  public static final String HELIX_HYBRID_STORE_QUOTA_ENABLED = "helix.hybrid.store.quota.enabled";

  /**
   * A time after which a bad SSD will trigger server shutdown.
   */
  public static final String SERVER_SHUTDOWN_DISK_UNHEALTHY_TIME_MS = "server.shutdown.ssd.unhealthy.time.ms";

  /**
   * Turns on early router throttling before allocating most of the router resources.
   */
  public static final String ROUTER_EARLY_THROTTLE_ENABLED = "router.early.throttle.enabled";

  /**
   * Number of IO threads used for AHAC client.
   */
  public static final String ROUTER_HTTPASYNCCLIENT_CLIENT_POOL_THREAD_COUNT = "router.httpasyncclient.client.pool.io.thread.count";

  /** Maximum number of times controller will automatically reset an error partition for the current/serving version
   * to mitigate impact of transient or long running issues during re-balance or restart.
   */
  public static final String ERROR_PARTITION_AUTO_RESET_LIMIT = "error.partition.auto.reset.limit";

  /**
   * The delay between each cycle where we iterate over all applicable resources and their partition to reset error
   * partitions and collect data on the effectiveness of previous resets.
   */
  public static final String ERROR_PARTITION_PROCESSING_CYCLE_DELAY = "error.partition.processing.cycle.delay";
  /**
   * Turns on  least loaded selection of server hosts.
   */
  public static final String ROUTER_LEAST_LOADED_HOST_ENABLED = "router.least.loaded.host.enabled";
  /**
   * Delay between each cycle where the checker will iterate over existing topics that are yet to be truncated and poll
   * their job status until they reach terminal state to ensure version topics in parent fabric are truncated in a
   * timely manner.
   */
  public static final String TERMINAL_STATE_TOPIC_CHECK_DELAY_MS = "controller.terminal.state.topic.check.delay.ms";

  /**
   * A config for Da-Vinci clients to use system store based repositories or Zk based repositories.
   */
  public static final String CLIENT_USE_SYSTEM_STORE_REPOSITORY = "client.use.system.store.repository";

  /**
   * The refresh interval for system store repositories that rely on periodic polling.
   */
  public static final String CLIENT_SYSTEM_STORE_REPOSITORY_REFRESH_INTERVAL_SECONDS = "client.system.store.repository.refresh.interval.seconds";

  /**
   * Test only config used to disable parent topic truncation upon job completion. This is needed because kafka cluster
   * in test environment is shared between parent and child controllers. Truncating topic upon completion will confuse
   * child controllers in certain scenarios.
   */
  public static final String CONTROLLER_DISABLE_PARENT_TOPIC_TRUNCATION_UPON_COMPLETION = "controller.disable.parent.topic.truncation.upon.completion";
}
