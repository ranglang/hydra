package hydra.cassandra

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util
import akka.util.Timeout
import com.pluralsight.hydra.avro.JsonConverter
import hydra.avro.resource.SchemaResource
import hydra.avro.util.{AvroUtils, SchemaWrapper}
import hydra.cassandra.CassandraRecordFactory._
import hydra.common.config.ConfigSupport
import hydra.core.akka.SchemaRegistryActor.{FetchSchemaRequest, FetchSchemaResponse}
import hydra.core.ingest.HydraRequest
import hydra.core.ingest.RequestParams.HYDRA_SCHEMA_PARAM
import hydra.core.protocol.MissingMetadataException
import hydra.core.transport.RecordFactory
import hydra.core.transport.ValidationStrategy.Strict
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class CassandraRecordFactory(schemaResourceLoader: ActorRef) extends RecordFactory[Seq[String], GenericRecord]
  with ConfigSupport {
  private implicit val timeout: Timeout = util.Timeout(3.seconds)

  override def build(request: HydraRequest)(implicit ec: ExecutionContext): Future[CassandraRecord] = {
    for {
      schemaName <- Future.fromTry(CassandraRecordFactory.schemaName(request))
      schema <- (schemaResourceLoader ? FetchSchemaRequest(schemaName)).mapTo[FetchSchemaResponse].map(_.schemaResource)
      genericRecord <- convertToGenericRecord(schema, request)
      cassRecord <- buildRecord(request, genericRecord, schema.schema)
    } yield cassRecord
  }

  private def convertToGenericRecord(schemaResource: SchemaResource, request: HydraRequest)(implicit ec: ExecutionContext): Future[GenericRecord] = {
    val converter = new JsonConverter[GenericRecord](
      schemaResource.schema,
      request.validationStrategy == Strict)
    Future(converter.convert(request.payload))
      .recover { case ex => throw AvroUtils.improveException(ex, schemaResource) }
  }

  private def buildRecord(request: HydraRequest, record: GenericRecord, schema: Schema)
                         (implicit ec: ExecutionContext): Future[CassandraRecord] = {
    Future {
      val cassandraProfile = request.metadataValue(CASSANDRA_PROFILE_PARAM)
        .getOrElse(
          throw MissingMetadataException(
            CASSANDRA_PROFILE_PARAM,
            s"A cassandra profile name is required $CASSANDRA_PROFILE_PARAM."
          )
        )
      val table = request.metadataValue(TABLE_PARAM).getOrElse(schema.getName)
      val keyspace = request.metadataValue(KEYSPACE_PARAM)
        .orElse(CassandraSchemaParser.keySpace(schema))
        .getOrElse(
          throw MissingMetadataException(
          CassandraRecordFactory.KEYSPACE_PARAM,
          s"A keyspace is required ${CassandraRecordFactory.KEYSPACE_PARAM}."
        ))
      val consistencyLevel = request.metadataValue(CONSISTENCY_LEVEL_PARAM).getOrElse(CassandraSchemaParser.consistencyLevel(schema))

      val isCompactStorage = request.metadataValue(ENABLE_COMPACT_STORAGE_PARAM) match {
        case Some(compactStorage) => compactStorage.toBoolean
        case None => CassandraSchemaParser.isCompactStorage(schema)
      }

      val clusteringColumnName = request.metadataValue(CLUSTERING_COLUMNS_PARAM) match {
        case Some(columnNames) => columnNames.split(",").toSeq
        case None => CassandraSchemaParser.clusteringColumns(schema)
      }

      val partitionKeys = request.metadataValue(PARTITION_KEYS_PARAM) match {
        case Some(pKeys) => pKeys.split(",").toSeq
        case None => CassandraSchemaParser.partitionKeys(schema)
      }

      val clusteringOrder = request.metadataValue(CLUSTERING_ORDER_PARAM).getOrElse(CassandraSchemaParser.clusteringOrder(schema))
      val clusteringColumn = ClusteringColumns(clusteringColumnName, clusteringOrder)
      val cassandraWriteOptions = CassandraWriteOptions(keyspace, table, consistencyLevel, isCompactStorage, partitionKeys, clusteringColumn)
      val primaryKeys = CassandraRecordFactory.primaryKeys(request, schema)

      CassandraRecord(cassandraWriteOptions, record, Some(primaryKeys), cassandraProfile, request.ackStrategy)
    }
  }
}

object CassandraRecordFactory {
  val CASSANDRA_PROFILE_PARAM = "hydra-cassandra-profile"
  val PRIMARY_KEYS_PARAM = "hydra-cassandra-primary-keys"
  val KEYSPACE_PARAM = "hydra-cassandra-keyspace"
  val TABLE_PARAM = "hydra-cassandra-table"
  val PARTITION_KEYS_PARAM = "hydra-cassandra-partition-keys"
  val CONSISTENCY_LEVEL_PARAM = "hydra-cassandra-consistency-level"
  val CLUSTERING_ORDER_PARAM = "hydra-cassandra-clustering-order"
  val CLUSTERING_COLUMNS_PARAM = "hydra-cassandra-clustering-column"
  val ENABLE_COMPACT_STORAGE_PARAM = "hydra-cassandra-enable-compact-storage"

  private[cassandra] def primaryKeys(request: HydraRequest, schema: Schema): Seq[String] = {
    request.metadataValue(PRIMARY_KEYS_PARAM).map(_.split(",")) match {
      case Some(keys) => keys
      case None => SchemaWrapper.from(schema).primaryKeys
    }
  }

  private[cassandra] def clusteringColumns(request: HydraRequest, schema: Schema): Seq[String] = {
    request.metadataValue(CLUSTERING_COLUMNS_PARAM).map(_.split(",")) match {
      case Some(clusteringColumn) => clusteringColumn
      case None => CassandraSchemaParser.clusteringColumns(schema)
    }
  }

  private[cassandra] def clusteringOrder(request: HydraRequest, schema: Schema): String = {
    request.metadataValue(CLUSTERING_ORDER_PARAM) match {
      case Some(clusteringOrder) => clusteringOrder
      case None => CassandraSchemaParser.clusteringOrder(schema)
    }
  }

  private[cassandra] def isCompactStorage(request: HydraRequest, schema: Schema): Boolean = {
    request.metadataValue(ENABLE_COMPACT_STORAGE_PARAM) match {
      case Some(isCompactStorage) => isCompactStorage.toBoolean
      case None => CassandraSchemaParser.isCompactStorage(schema)
    }
  }

  private[cassandra] def consistencyLevel(request: HydraRequest, schema: Schema): String = {
    request.metadataValue(CONSISTENCY_LEVEL_PARAM) match {
      case Some(consistencyLevel) => consistencyLevel
      case None => CassandraSchemaParser.consistencyLevel(schema)
    }
  }

  private[cassandra] def schemaName(request: HydraRequest): Try[String] = {
    request.metadataValue(HYDRA_SCHEMA_PARAM).map(Success(_))
      .getOrElse(Failure(MissingMetadataException(
        HYDRA_SCHEMA_PARAM,
        s"A schema name is required [$HYDRA_SCHEMA_PARAM].")))
  }

  def apply(schemaResourceLoader: ActorRef): CassandraRecordFactory = new CassandraRecordFactory(schemaResourceLoader)
}


case class CassandraWriteOptions(keySpace: String,
                                 table: String,
                                 consistencyLevel: String,
                                 isCompactStorage: Boolean,
                                 partitionKeys: Seq[String],
                                 clusteringColumn: ClusteringColumns
                                ) {}

object CassandraWriteOptions {
  val DEFAULT_CONSISTENCY_LEVEL = "QUORUM"
  val DEFAULT_IS_COMPACT_STORAGE = false
}
