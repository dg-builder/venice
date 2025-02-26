package com.linkedin.venice.endToEnd;

import static com.linkedin.venice.utils.IntegrationTestPushUtils.createStoreForJob;
import static com.linkedin.venice.utils.TestWriteUtils.getTempDataDirectory;
import static com.linkedin.venice.utils.TestWriteUtils.writeSimpleAvroFileWithUserSchema;

import com.linkedin.venice.ConfigKeys;
import com.linkedin.venice.common.VeniceSystemStoreType;
import com.linkedin.venice.controllerapi.ClusterStaleDataAuditResponse;
import com.linkedin.venice.controllerapi.ControllerClient;
import com.linkedin.venice.controllerapi.StoreHealthAuditResponse;
import com.linkedin.venice.hadoop.VenicePushJob;
import com.linkedin.venice.integration.utils.ServiceFactory;
import com.linkedin.venice.integration.utils.VeniceControllerWrapper;
import com.linkedin.venice.integration.utils.VeniceMultiClusterWrapper;
import com.linkedin.venice.integration.utils.VeniceTwoLayerMultiColoMultiClusterWrapper;
import com.linkedin.venice.meta.StoreInfo;
import com.linkedin.venice.meta.Version;
import com.linkedin.venice.utils.TestUtils;
import com.linkedin.venice.utils.TestWriteUtils;
import com.linkedin.venice.utils.Time;
import com.linkedin.venice.utils.Utils;
import com.linkedin.venice.utils.VeniceProperties;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.avro.Schema;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class TestStaleDataVisibility {
  private static final Logger LOGGER = LogManager.getLogger(TestStaleDataVisibility.class);
  private static final int TEST_TIMEOUT = 360 * Time.MS_PER_SECOND;
  private static final int NUMBER_OF_CHILD_DATACENTERS = 2;
  private static final int NUMBER_OF_CLUSTERS = 1;
  private static final String[] CLUSTER_NAMES =
      IntStream.range(0, NUMBER_OF_CLUSTERS).mapToObj(i -> "venice-cluster" + i).toArray(String[]::new);

  private List<VeniceMultiClusterWrapper> childClusters;
  private List<List<VeniceControllerWrapper>> childControllers;
  private List<VeniceControllerWrapper> parentControllers;
  private VeniceTwoLayerMultiColoMultiClusterWrapper multiColoMultiClusterWrapper;

  @BeforeClass
  public void setUp() {
    Properties serverProperties = new Properties();
    serverProperties.setProperty(ConfigKeys.SERVER_PROMOTION_TO_LEADER_REPLICA_DELAY_SECONDS, Long.toString(1));
    Properties childControllerProperties = new Properties();
    childControllerProperties.setProperty(ConfigKeys.CONTROLLER_ENABLE_BATCH_PUSH_FROM_ADMIN_IN_CHILD, "true");
    multiColoMultiClusterWrapper = ServiceFactory.getVeniceTwoLayerMultiColoMultiClusterWrapper(
        NUMBER_OF_CHILD_DATACENTERS,
        NUMBER_OF_CLUSTERS,
        1,
        1,
        1,
        1,
        1,
        Optional.empty(),
        Optional.of(childControllerProperties),
        Optional.of(new VeniceProperties(serverProperties)),
        false);

    childClusters = multiColoMultiClusterWrapper.getClusters();
    childControllers = childClusters.stream()
        .map(veniceClusterWrapper -> new ArrayList<>(veniceClusterWrapper.getControllers().values()))
        .collect(Collectors.toList());
    parentControllers = multiColoMultiClusterWrapper.getParentControllers();

    LOGGER.info(
        "parentControllers: {}",
        parentControllers.stream().map(VeniceControllerWrapper::getControllerUrl).collect(Collectors.joining(", ")));

    int i = 0;
    for (VeniceMultiClusterWrapper multiClusterWrapper: childClusters) {
      LOGGER.info(
          "childCluster{} controllers: {}",
          i++,
          multiClusterWrapper.getControllers()
              .values()
              .stream()
              .map(VeniceControllerWrapper::getControllerUrl)
              .collect(Collectors.joining(", ")));
    }
  }

  @AfterClass
  public void cleanUp() {
    multiColoMultiClusterWrapper.close();
  }

  @Test(timeOut = TEST_TIMEOUT)
  public void testGetClusterStaleStores() throws Exception {
    String clusterName = CLUSTER_NAMES[0];
    File inputDir = getTempDataDirectory();
    Schema recordSchema = writeSimpleAvroFileWithUserSchema(inputDir);
    String inputDirPath = "file:" + inputDir.getAbsolutePath();
    String storeName = Utils.getUniqueString("store");
    String parentControllerUrls =
        parentControllers.stream().map(VeniceControllerWrapper::getControllerUrl).collect(Collectors.joining(","));

    // create a store via parent controller url
    Properties props = TestWriteUtils.defaultVPJProps(parentControllerUrls, inputDirPath, storeName);
    createStoreForJob(clusterName, recordSchema, props).close();
    try (ControllerClient controllerClient = new ControllerClient(clusterName, parentControllerUrls)) {
      String pushStatusStoreVersionName =
          Version.composeKafkaTopic(VeniceSystemStoreType.DAVINCI_PUSH_STATUS_STORE.getSystemStoreName(storeName), 1);
      String metaStoreVersionName =
          Version.composeKafkaTopic(VeniceSystemStoreType.META_STORE.getSystemStoreName(storeName), 1);
      TestUtils
          .waitForNonDeterministicPushCompletion(pushStatusStoreVersionName, controllerClient, 1, TimeUnit.MINUTES);
      TestUtils.waitForNonDeterministicPushCompletion(metaStoreVersionName, controllerClient, 1, TimeUnit.MINUTES);
    }

    try (VenicePushJob job = new VenicePushJob("Test push job", props)) {
      job.run();
    }

    try (ControllerClient controllerClient = new ControllerClient(clusterName, parentControllerUrls)) {

      // the store should not be appearing in the stale data audit
      ClusterStaleDataAuditResponse emptyResponse =
          controllerClient.getClusterStaleStores(clusterName, parentControllerUrls);
      Assert.assertFalse(emptyResponse.isError());
      Assert.assertFalse(emptyResponse.getAuditMap().containsKey(storeName));

      // get single child controller, empty push to it
      VeniceControllerWrapper childController = childControllers.get(0).get(0);
      Properties props2 = TestWriteUtils.defaultVPJProps(childController.getControllerUrl(), inputDirPath, storeName);
      try (VenicePushJob job = new VenicePushJob("Test push job", props2)) {
        job.run();
      }

      // store should now appear as stale
      ClusterStaleDataAuditResponse response =
          controllerClient.getClusterStaleStores(clusterName, parentControllerUrls);
      Assert.assertFalse(response.isError());
      Assert.assertEquals(response.getAuditMap().get(storeName).getStaleRegions().size(), 1);
      Assert.assertEquals(response.getAuditMap().get(storeName).getHealthyRegions().size(), 1);

      // test store health check
      StoreHealthAuditResponse healthResponse =
          controllerClient.listStorePushInfo(clusterName, parentControllerUrls, storeName);
      Assert.assertTrue(response.getAuditMap().containsKey(healthResponse.getStoreName()));
      Map<String, StoreInfo> auditMapEntry =
          response.getAuditMap().get(healthResponse.getStoreName()).getStaleRegions();
      for (Map.Entry<String, StoreInfo> entry: auditMapEntry.entrySet())
        if (Objects.equals(entry.getValue().getName(), storeName))
          Assert.assertTrue(healthResponse.getRegionsWithStaleData().contains(entry.getKey())); // verify that the same
                                                                                                // regions are stale
                                                                                                // across both responses
                                                                                                // for the same store
    }
  }
}
