package hydra.cassandra

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestKit
import com.pluralsight.hydra.avro.JsonConverter
import hydra.avro.registry.JsonToAvroConversionExceptionWithMetadata
import hydra.avro.resource.SchemaResource
import hydra.core.akka.SchemaRegistryActor.{FetchSchemaRequest, FetchSchemaResponse}
import hydra.core.ingest.HydraRequest
import hydra.core.ingest.RequestParams.HYDRA_SCHEMA_PARAM
import hydra.core.protocol.MissingMetadataException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FunSpecLike, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.io.Source

class CassandraRecordFactorySpec extends TestKit(ActorSystem("cassandra-record-factory-spec"))
  with Matchers
  with FunSpecLike
  with ScalaFutures
  with BeforeAndAfterAll {

  override def afterAll = TestKit.shutdownActorSystem(system, verifySystemShutdown = true)

  val schemaWithPrimaryKey: Schema = new Schema.Parser().parse(Source.fromResource("schema-with-primary-key.avsc").mkString)
  val schemaNoPrimaryKey: Schema = new Schema.Parser().parse(Source.fromResource("schema-with-no-primary-key.avsc").mkString)
  val schemaNoPrimaryKeyName = "schemaNoPrimaryKey"
  val schemaWithPrimaryKeyName = "schemaWithPrimaryKey"

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(2000.millis),
    interval = scaled(100.millis))

  val loader = system.actorOf(Props(new Actor() {
    override def receive: Receive = {
      case FetchSchemaRequest(schema) =>
        val schemaToUse = schema match {
          case `schemaWithPrimaryKeyName` => schemaWithPrimaryKey
          case `schemaNoPrimaryKeyName` => schemaNoPrimaryKey
        }
        sender ! FetchSchemaResponse(SchemaResource(1, 1, schemaToUse))
    }
  }))

  val factory = CassandraRecordFactory(loader)

  describe("The CassandraRecordFactory should") {
    it("extracts primary keys if present as hydra.key in schema") {
      val req = HydraRequest("1", "test")
      CassandraRecordFactory.primaryKeys(req, schemaWithPrimaryKey) shouldBe Seq("id", "name")
    }

    it("extracts no primary keys if no hydra.key in schema") {
      val req = HydraRequest("1", "test")
      CassandraRecordFactory.primaryKeys(req, schemaNoPrimaryKey) shouldBe Seq.empty
    }

    it("extracts primary keys when they are in the metadata") {
      val req = HydraRequest("1", "test")
        .withMetadata(CassandraRecordFactory.PRIMARY_KEYS_PARAM -> "id,name")
      CassandraRecordFactory.primaryKeys(req, schemaNoPrimaryKey) shouldBe Seq("id", "name")
    }

    it("throws an error if no schema is in the request metadata") {
      val req = HydraRequest("1", "test")
      whenReady(factory.build(req).failed)(_ shouldBe an[MissingMetadataException])
    }

    it("throws an error if the payload does not comply to the schema") {
      val req = HydraRequest("1", """{"name":"test"}""")
        .withMetadata(HYDRA_SCHEMA_PARAM -> schemaNoPrimaryKeyName)
      whenReady(factory.build(req).failed)(_ shouldBe a[JsonToAvroConversionExceptionWithMetadata])
    }

    it("throws an error if the payload has a bad field") {
      val req = HydraRequest("1", """{"id":1, "badField":2, "name":"test"}""")
        .withMetadata(HYDRA_SCHEMA_PARAM -> schemaNoPrimaryKeyName)
      whenReady(factory.build(req).failed)(_ shouldBe a[JsonToAvroConversionExceptionWithMetadata])
    }

    it("throws an error when no keyspace is specified") {
      val req = HydraRequest("123", """{"id":1, "name":"test", "rank" : 1}""")
        .withMetadata(HYDRA_SCHEMA_PARAM -> schemaNoPrimaryKeyName, CassandraRecordFactory.CASSANDRA_PROFILE_PARAM -> "table")

      whenReady(factory.build(req).failed)(_ shouldBe a[MissingMetadataException])
    }

    it("should use schema name for table name") {
      val req = HydraRequest("123", """{"id":1, "name":"test", "rank" : 1}""")
        .withMetadata(
          HYDRA_SCHEMA_PARAM -> schemaNoPrimaryKeyName,
          CassandraRecordFactory.CASSANDRA_PROFILE_PARAM -> "table",
          CassandraRecordFactory.KEYSPACE_PARAM -> "test_key_space"
        )

      whenReady(factory.build(req))(_.destination shouldBe schemaNoPrimaryKey.getName)
    }

    it("throws an error when no cassandra profile is provided in the request") {
      val req = HydraRequest("123", """{"id":1, "name":"test", "rank" : 1}""")
        .withMetadata(
          HYDRA_SCHEMA_PARAM -> schemaNoPrimaryKeyName,
          CassandraRecordFactory.KEYSPACE_PARAM -> "test_key_space"
        )
      whenReady(factory.build(req).failed)(_ shouldBe a[MissingMetadataException])
    }

    it("builds a correct record") {
      val payload = """{"id":1, "name":"test", "rank" : 1}"""
      val keySpace = "test_key_space"
      val req = HydraRequest("123", payload)
        .withMetadata(
          HYDRA_SCHEMA_PARAM -> schemaWithPrimaryKeyName,
          CassandraRecordFactory.CASSANDRA_PROFILE_PARAM -> "table",
          CassandraRecordFactory.KEYSPACE_PARAM -> keySpace,
          CassandraRecordFactory.CLUSTERING_ORDER_PARAM -> "DESC",
          CassandraRecordFactory.CLUSTERING_COLUMNS_PARAM -> "name",
          CassandraRecordFactory.PARTITION_KEYS_PARAM -> "id,name,rank",
          CassandraRecordFactory.CONSISTENCY_LEVEL_PARAM -> "ALL"
        )
      val clusteringColumn = ClusteringColumns(Seq("name"), "DESC")
      val cassandraWriteOptions = CassandraWriteOptions(
        keySpace,
        schemaWithPrimaryKey.getName,
        consistencyLevel = "ALL",
        isCompactStorage = false,
        partitionKeys = Seq("id", "name", "rank"),
        clusteringColumn
      )

      whenReady(factory.build(req)) { rec =>
        rec.cassandraOptions shouldBe cassandraWriteOptions
        rec.destination shouldBe schemaWithPrimaryKey.getName
        rec.primaryKeys shouldBe Seq("id", "name")
        rec.keyValues shouldBe Map("id" -> 1, "name" -> "test")
        rec.key shouldBe Some(Seq("id", "name"))
        rec.payload shouldBe new JsonConverter[GenericRecord](schemaWithPrimaryKey).convert(payload)
      }
    }

  }

}
