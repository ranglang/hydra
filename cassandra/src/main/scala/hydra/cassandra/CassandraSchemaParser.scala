package hydra.cassandra

import hydra.core.protocol.MissingMetadataException
import org.apache.avro.Schema

object CassandraSchemaParser {
  def clusteringColumns(schema: Schema): Seq[String] = {
    Option(schema.getProp("hydra.clustering.column")).map(_.split(",")) match {
      case Some(clusteringColumns) => clusteringColumns
      case None => Seq.empty
    }
  }

  def clusteringOrder(schema: Schema): String = {
    Option(schema.getProp("hydra.clustering.order")) match {
      case Some(clusteringOrder) => clusteringOrder
      case None => "ASC"
    }
  }

  def isCompactStorage(schema: Schema): Boolean = {
    Option(schema.getProp("hydra.enable.compact.storage")) match {
      case Some(isCompactStorage) => isCompactStorage.toBoolean
      case None => false
    }
  }

  def consistencyLevel(schema: Schema): String = {
    Option(schema.getProp("hydra.consistency.level")) match {
      case Some(consistencyLevel) => consistencyLevel
      case None => CassandraWriteOptions.DEFAULT_CONSISTENCY_LEVEL
    }
  }

  @throws(classOf[MissingMetadataException])
  def keySpace(schema: Schema): String = {
    Option(schema.getProp("hydra.keyspace")) match {
      case Some(keyspace) => keyspace
      case None => throw MissingMetadataException(
        CassandraRecordFactory.KEYSPACE_PARAM,
        s"A keyspace is required ${CassandraRecordFactory.KEYSPACE_PARAM}."
      )
    }
  }

  def partitionKeys(schema: Schema): Seq[String] = {
    Option(schema.getProp("hydra.partition.keys")).map(_.split(",")) match {
      case Some(partitionKeys) => partitionKeys
      case None => Seq.empty
    }
  }

}
