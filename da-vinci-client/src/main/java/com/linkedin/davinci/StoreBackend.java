package com.linkedin.davinci;

import com.linkedin.venice.exceptions.VeniceException;
import com.linkedin.venice.meta.Store;
import com.linkedin.venice.meta.Version;
import com.linkedin.venice.utils.ComplementSet;
import com.linkedin.venice.utils.ConcurrentRef;
import com.linkedin.venice.utils.ReferenceCounted;

import com.linkedin.davinci.client.ClientStats;

import org.apache.log4j.Logger;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class StoreBackend {
  private static final Logger logger = Logger.getLogger(StoreBackend.class);

  private final DaVinciBackend backend;
  private final String storeName;
  private final ClientStats stats;
  private final ComplementSet<Integer> subscription = ComplementSet.emptySet();
  private final ConcurrentRef<VersionBackend> currentVersionRef = new ConcurrentRef<>(this::deleteVersion);
  private VersionBackend currentVersion;
  private VersionBackend futureVersion;

  StoreBackend(DaVinciBackend backend, String storeName) {
    this.backend = backend;
    this.storeName = storeName;
    this.stats = new ClientStats(backend.getMetricsRepository(), storeName);
    try {
      backend.getStoreRepository().subscribe(storeName);
    } catch (InterruptedException e) {
      logger.info("Subscribe method is interrupted " + e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

  synchronized void close() {
    currentVersionRef.clear();
    subscription.clear();

    if (futureVersion != null) {
      futureVersion.close();
      futureVersion = null;
    }

    if (currentVersion != null) {
      currentVersion.close();
      currentVersion = null;
    }
    backend.getStoreRepository().unsubscribe(storeName);
  }

  synchronized void delete() {
    logger.info("Deleting local store " + storeName);
    currentVersionRef.clear();
    subscription.clear();

    if (futureVersion != null) {
      futureVersion.delete();
      futureVersion = null;
    }

    if (currentVersion != null) {
      currentVersion.delete();
      currentVersion = null;
    }
    backend.getStoreRepository().unsubscribe(storeName);
  }

  public ClientStats getStats() {
    return stats;
  }

  public ReferenceCounted<VersionBackend> getCurrentVersion() {
    return currentVersionRef.get();
  }

  private void setCurrentVersion(VersionBackend version) {
    logger.info("Switching to a new version " + version);
    currentVersion = version;
    currentVersionRef.set(version);
  }

  public CompletableFuture subscribe(ComplementSet<Integer> partitions) {
    return subscribe(partitions, Optional.empty());
  }

  synchronized CompletableFuture subscribe(ComplementSet<Integer> partitions, Optional<Version> bootstrapVersion) {
    if (currentVersion == null) {
      setCurrentVersion(new VersionBackend(
          backend,
          bootstrapVersion.orElseGet(
              () -> backend.getLatestVersion(storeName).orElseThrow(
                  () -> new VeniceException("Cannot subscribe to an empty store, storeName=" + storeName)))));

    } else if (bootstrapVersion.isPresent()) {
      throw new VeniceException("Bootstrap version is already selected, storeName=" + storeName +
                                    ", currentVersion=" + currentVersion +
                                    ", desiredVersion=" + bootstrapVersion.get().kafkaTopicName());
    }

    logger.info("Subscribing to partitions, storeName=" + storeName + ", partitions=" + partitions);
    subscription.addAll(partitions);

    if (futureVersion != null) {
      futureVersion.subscribe(partitions).whenComplete((v, t) -> trySwapCurrentVersion());
    } else if (bootstrapVersion.isPresent()) {
      trySubscribeFutureVersion();
    }

    ReferenceCounted<VersionBackend> ref = getCurrentVersion();
    return currentVersion.subscribe(partitions).whenComplete((v, t) -> ref.release());
  }

  public synchronized void unsubscribe(ComplementSet<Integer> partitions) {
    logger.info("Unsubscribing from partitions, storeName=" + storeName + ", partitions=" + subscription);
    subscription.removeAll(partitions);

    if (currentVersion != null) {
      currentVersion.unsubscribe(partitions);
    }

    if (futureVersion != null) {
      futureVersion.unsubscribe(partitions);
    }
  }

  synchronized void trySubscribeFutureVersion() {
    if (currentVersion == null || futureVersion != null) {
      return;
    }

    Version version = backend.getLatestVersion(storeName).orElse(null);
    if (version == null || version.getNumber() <= currentVersion.getVersion().getNumber()) {
      return;
    }

    logger.info("Subscribing to a future version " + version.kafkaTopicName());
    futureVersion = new VersionBackend(backend, version);
    futureVersion.subscribe(subscription).whenComplete((v, t) -> trySwapCurrentVersion());
  }

  // May be called indirectly by readers, so cannot be blocking
  private void deleteVersion(VersionBackend version) {
    backend.getExecutor().submit(() -> version.delete());
  }

  synchronized void deleteOldVersions() {
    if (futureVersion != null) {
      Store store = backend.getStoreRepository().getStoreOrThrow(storeName);
      int versionNumber = futureVersion.getVersion().getNumber();
      if (!store.getVersion(versionNumber).isPresent()) {
        logger.info("Deleting obsolete future version " + futureVersion);
        futureVersion.delete();
        futureVersion = null;
      }
    }
  }

  // May be called several times even after version was swapped
  private synchronized void trySwapCurrentVersion() {
    if (futureVersion != null && futureVersion.isReadyToServe(subscription)) {
      logger.info(futureVersion.getVersion().kafkaTopicName() + " is ready to use with partitions: " + subscription.toString());
      setCurrentVersion(futureVersion);
      futureVersion = null;
      trySubscribeFutureVersion();
    }
  }
}
