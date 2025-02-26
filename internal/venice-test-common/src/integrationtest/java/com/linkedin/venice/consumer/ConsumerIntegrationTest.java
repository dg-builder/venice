package com.linkedin.venice.consumer;

import com.linkedin.avroutil1.compatibility.AvroCompatibilityHelper;
import com.linkedin.venice.client.store.AvroGenericStoreClient;
import com.linkedin.venice.client.store.ClientConfig;
import com.linkedin.venice.client.store.ClientFactory;
import com.linkedin.venice.controller.VeniceHelixAdmin;
import com.linkedin.venice.controllerapi.ControllerClient;
import com.linkedin.venice.controllerapi.MultiSchemaResponse;
import com.linkedin.venice.controllerapi.StoreResponse;
import com.linkedin.venice.controllerapi.UpdateStoreQueryParams;
import com.linkedin.venice.exceptions.VeniceMessageException;
import com.linkedin.venice.integration.utils.ServiceFactory;
import com.linkedin.venice.integration.utils.VeniceClusterWrapper;
import com.linkedin.venice.kafka.protocol.KafkaMessageEnvelope;
import com.linkedin.venice.kafka.protocol.enums.MessageType;
import com.linkedin.venice.meta.Version;
import com.linkedin.venice.partitioner.DefaultVenicePartitioner;
import com.linkedin.venice.partitioner.VenicePartitioner;
import com.linkedin.venice.schema.avro.DirectionalSchemaCompatibilityType;
import com.linkedin.venice.serialization.DefaultSerializer;
import com.linkedin.venice.serialization.VeniceKafkaSerializer;
import com.linkedin.venice.serialization.avro.AvroProtocolDefinition;
import com.linkedin.venice.serialization.avro.InternalAvroSpecificSerializer;
import com.linkedin.venice.serialization.avro.KafkaValueSerializer;
import com.linkedin.venice.serialization.avro.VeniceAvroKafkaSerializer;
import com.linkedin.venice.utils.SystemTime;
import com.linkedin.venice.utils.TestUtils;
import com.linkedin.venice.utils.Time;
import com.linkedin.venice.utils.Utils;
import com.linkedin.venice.utils.VeniceProperties;
import com.linkedin.venice.writer.ApacheKafkaProducer;
import com.linkedin.venice.writer.KafkaProducerWrapper;
import com.linkedin.venice.writer.LeaderMetadataWrapper;
import com.linkedin.venice.writer.VeniceWriter;
import com.linkedin.venice.writer.VeniceWriterOptions;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Encoder;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class ConsumerIntegrationTest {
  private static final String TEST_KEY = "key1";

  /**
   * There could be cases where there exists an unreleased schema which conflicts with this new protocol version,
   * but that should be supported.
   */
  public static final int NEW_PROTOCOL_VERSION =
      AvroProtocolDefinition.KAFKA_MESSAGE_ENVELOPE.currentProtocolVersion.get() + 1;

  public static final Schema NEW_PROTOCOL_SCHEMA;

  static {
    // TODO: Consider refactoring this so that it is not static, in order to test other evolution scenarios, such as:
    // - Adding a field in a nested record (should be fine...).
    // - Adding new types to a union (not supported gracefully at the moment...).

    // General info about the current protocol
    Set<Integer> knownProtocols = new KafkaValueSerializer().knownProtocols();
    AvroProtocolDefinition protocolDef = AvroProtocolDefinition.KAFKA_MESSAGE_ENVELOPE;
    Schema currentProtocolSchema = protocolDef.getCurrentProtocolVersionSchema();
    List<Schema.Field> protocolSchemaFields = currentProtocolSchema.getFields()
        .stream()
        .map(field -> AvroCompatibilityHelper.newField(field).build())
        .collect(Collectors.toList());

    // Generation of a new protocol version by adding an optional field to the current protocol version
    Schema newFieldSchema =
        Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT)));
    protocolSchemaFields.add(
        AvroCompatibilityHelper.newField(null)
            .setName("newField")
            .setSchema(newFieldSchema)
            .setOrder(Schema.Field.Order.ASCENDING)
            .setDefault(null)
            .build());
    NEW_PROTOCOL_SCHEMA = Schema.createRecord(
        currentProtocolSchema.getName(),
        currentProtocolSchema.getDoc(),
        currentProtocolSchema.getNamespace(),
        false);
    NEW_PROTOCOL_SCHEMA.setFields(protocolSchemaFields);
  }

  private VeniceClusterWrapper cluster;
  private ControllerClient controllerClient;
  private String store;
  private int version;
  private String topicName;
  private AvroGenericStoreClient client;

  @BeforeClass
  public void sharedSetUp() {
    cluster = ServiceFactory.getVeniceCluster();
    controllerClient =
        ControllerClient.constructClusterControllerClient(cluster.getClusterName(), cluster.getAllControllersURLs());

    String systemStoreName = AvroProtocolDefinition.KAFKA_MESSAGE_ENVELOPE.getSystemStoreName();
    TestUtils.waitForNonDeterministicAssertion(15, TimeUnit.SECONDS, () -> {
      MultiSchemaResponse response = controllerClient.getAllValueSchema(systemStoreName);
      Assert.assertFalse(response.isError());
      Assert.assertEquals(
          response.getSchemas().length,
          AvroProtocolDefinition.KAFKA_MESSAGE_ENVELOPE.getCurrentProtocolVersion());
    });

    /**
     * By doing this, we emulate having started a new controller version which knows about the new protocol...
     */
    ((VeniceHelixAdmin) cluster.getRandomVeniceController().getVeniceAdmin()).addValueSchema(
        cluster.getClusterName(),
        systemStoreName,
        NEW_PROTOCOL_SCHEMA.toString(),
        NEW_PROTOCOL_VERSION,
        DirectionalSchemaCompatibilityType.NONE,
        false);
  }

  @BeforeMethod
  public void testSetUp() {
    store = Utils.getUniqueString("consumer_integ_test");
    version = 1;
    topicName = Version.composeRealTimeTopic(store);
    cluster.getNewStore(store);
    long streamingRewindSeconds = 25L;
    long streamingMessageLag = 2L;
    controllerClient.updateStore(
        store,
        new UpdateStoreQueryParams().setHybridRewindSeconds(streamingRewindSeconds)
            .setHybridOffsetLagThreshold(streamingMessageLag));
    controllerClient.emptyPush(store, "test_push", 1);
    TestUtils.waitForNonDeterministicAssertion(15, TimeUnit.SECONDS, () -> {
      StoreResponse freshStoreResponse = controllerClient.getStore(store);
      Assert.assertFalse(freshStoreResponse.isError());
      Assert.assertEquals(
          freshStoreResponse.getStore().getCurrentVersion(),
          version,
          "The empty push has not activated the store.");
    });
    client = ClientFactory.getAndStartGenericAvroClient(
        ClientConfig.defaultGenericClientConfig(store).setVeniceURL(cluster.getRandomRouterURL()));
  }

  @AfterMethod
  public void testCleanUp() {
    Utils.closeQuietlyWithErrorLogged(client);
  }

  @AfterClass
  public void sharedCleanUp() {
    Utils.closeQuietlyWithErrorLogged(cluster);
    Utils.closeQuietlyWithErrorLogged(controllerClient);
  }

  @Test(timeOut = 60 * Time.MS_PER_SECOND)
  public void testForwardCompatibility() throws ExecutionException, InterruptedException {
    // Verify that the regular writer can update the store
    try (VeniceWriter<String, String, byte[]> regularVeniceWriter = cluster.getVeniceWriter(topicName)) {
      writeAndVerifyRecord(regularVeniceWriter, client, "value1");
    }

    Properties javaProps = new Properties();
    javaProps.put(
        ApacheKafkaProducer.PROPERTIES_KAFKA_PREFIX + ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
        KafkaValueSerializerWithNewerProtocol.class.getName());
    javaProps.put(
        ApacheKafkaProducer.PROPERTIES_KAFKA_PREFIX + ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
        cluster.getKafka().getAddress());
    VeniceProperties props = new VeniceProperties(javaProps);
    String stringSchema = "\"string\"";
    VeniceKafkaSerializer keySerializer = new VeniceAvroKafkaSerializer(stringSchema);
    VeniceKafkaSerializer valueSerializer = new VeniceAvroKafkaSerializer(stringSchema);
    VenicePartitioner partitioner = new DefaultVenicePartitioner(props);
    Time time = new SystemTime();
    Supplier<KafkaProducerWrapper> producerWrapperSupplier = () -> new ApacheKafkaProducerWithNewerProtocol(props);

    VeniceWriterOptions veniceWriterOptions = new VeniceWriterOptions.Builder(topicName).setKeySerializer(keySerializer)
        .setValueSerializer(valueSerializer)
        .setWriteComputeSerializer(new DefaultSerializer())
        .setTime(time)
        .setPartitioner(partitioner)
        .build();
    try (VeniceWriter veniceWriterWithNewerProtocol =
        new VeniceWriterWithNewerProtocol(veniceWriterOptions, props, producerWrapperSupplier)) {
      writeAndVerifyRecord(veniceWriterWithNewerProtocol, client, "value2");
    }
  }

  private void writeAndVerifyRecord(
      VeniceWriter<String, String, byte[]> veniceWriter,
      AvroGenericStoreClient client,
      String testValue) throws ExecutionException, InterruptedException {
    veniceWriter.put(TEST_KEY, testValue, 1).get();
    TestUtils.waitForNonDeterministicAssertion(30, TimeUnit.SECONDS, () -> {
      try {
        Object value = client.get(TEST_KEY).get();
        Assert.assertNotNull(
            value,
            "The key written by the " + veniceWriter.getClass().getSimpleName() + " is not in the store yet.");
        Assert.assertEquals(
            value.toString(),
            testValue,
            "The key written by the " + veniceWriter.getClass().getSimpleName() + " is not valid.");
      } catch (ExecutionException e) {
        Assert.fail("Caught exception: " + e.getMessage());
      }

    });
  }

  private static class VeniceWriterWithNewerProtocol extends VeniceWriter<String, String, byte[]> {
    protected VeniceWriterWithNewerProtocol(
        VeniceWriterOptions veniceWriterOptions,
        VeniceProperties props,
        Supplier<KafkaProducerWrapper> producerWrapperSupplier) {
      super(veniceWriterOptions, props, producerWrapperSupplier);
    }

    @Override
    protected KafkaMessageEnvelope getKafkaMessageEnvelope(
        MessageType messageType,
        boolean isEndOfSegment,
        int partition,
        boolean incrementSequenceNumber,
        LeaderMetadataWrapper leaderMetadataWrapper,
        long logicalTs) {
      KafkaMessageEnvelope normalKME =
          super.getKafkaMessageEnvelope(messageType, isEndOfSegment, partition, true, leaderMetadataWrapper, logicalTs);

      NewKafkaMessageEnvelopeWithExtraField newKME = new NewKafkaMessageEnvelopeWithExtraField();
      for (int index = 0; index < newKME.newFieldIndex; index++) {
        newKME.put(index, normalKME.get(index));
      }
      newKME.newField = 42;

      return newKME;
    }
  }

  /**
   * A class which looks like a {@link KafkaMessageEnvelope} but which is more than that...
   *
   * A bit tricky, but we need to do that in order to keep using specific records inside the {@link VeniceWriter}...
   */
  static class NewKafkaMessageEnvelopeWithExtraField extends KafkaMessageEnvelope {
    public static final org.apache.avro.Schema SCHEMA$ = NEW_PROTOCOL_SCHEMA;
    private final int newFieldIndex;
    public Integer newField;

    public NewKafkaMessageEnvelopeWithExtraField() {
      super();
      int index = 0;
      while (true) {
        try {
          super.get(index);
          index++;
        } catch (org.apache.avro.AvroRuntimeException e) {
          break;
        }
      }
      this.newFieldIndex = index;
    }

    @Override
    public org.apache.avro.Schema getSchema() {
      return SCHEMA$;
    }

    // Used by DatumWriter. Applications should not call.
    @Override
    public java.lang.Object get(int field$) {
      if (newFieldIndex == field$) {
        return newField;
      } else {
        return super.get(field$);
      }
    }

    // Used by DatumReader. Applications should not call.
    @SuppressWarnings(value = "unchecked")
    @Override
    public void put(int field$, java.lang.Object value$) {
      if (newFieldIndex == field$) {
        this.newField = (java.lang.Integer) value$;
      } else {
        super.put(field$, value$);
      }
    }
  }

  private static class ApacheKafkaProducerWithNewerProtocol extends ApacheKafkaProducer {
    public ApacheKafkaProducerWithNewerProtocol(VeniceProperties props) {
      super(props, false);
    }
  }

  public static class KafkaValueSerializerWithNewerProtocol extends KafkaValueSerializer {
    @Override
    public byte[] serialize(String topic, KafkaMessageEnvelope object) {
      return serializeNewProtocol(object);
    }
  }

  /**
   * Code partially copied from {@link InternalAvroSpecificSerializer#serialize(String, SpecificRecord)} because
   * that class has so many safeguards that it would be hard to mock it in such way that we can inject new schemas
   * that are not truly part of the project.
   */
  public static byte[] serializeNewProtocol(GenericRecord messageFromNewProtocol) {
    try {
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      Encoder encoder = AvroCompatibilityHelper.newBinaryEncoder(byteArrayOutputStream);

      byteArrayOutputStream.write(AvroProtocolDefinition.KAFKA_MESSAGE_ENVELOPE.getMagicByte().get());
      byteArrayOutputStream.write((byte) NEW_PROTOCOL_VERSION);
      GenericDatumWriter writer = new GenericDatumWriter(messageFromNewProtocol.getSchema());
      writer.write(messageFromNewProtocol, encoder);
      encoder.flush();
      return byteArrayOutputStream.toByteArray();
    } catch (IOException e) {
      throw new VeniceMessageException("Failed to encode message: " + messageFromNewProtocol.toString(), e);
    }
  }
}
