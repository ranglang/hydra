package hydra.cassandra

import hydra.core.transport.{AckStrategy, HydraRecord, RecordMetadata}
import org.apache.avro.generic.GenericRecord

case class CassandraRecord(cassandraOptions: CassandraWriteOptions,
                           payload: GenericRecord,
                           key: Option[Seq[String]],
                           cassandraProfile: String,
                           ackStrategy: AckStrategy
                          )
  extends HydraRecord[Seq[String], GenericRecord] {
  lazy val primaryKeys: Seq[String] = key.getOrElse(Seq.empty)
  lazy val keyValues: Map[String, AnyRef] = primaryKeys.map(k => k -> payload.get(k)).toMap
  val destination: String = cassandraOptions.table
}


case class CassandraRecordMetadata(destination: String,
                                   timestamp: Long = System.currentTimeMillis,
                                   ackStrategy: AckStrategy
                                  ) extends RecordMetadata {}


case class ClusteringColumns(columns: Seq[String], order: String)

object ClusteringColumns {
  val DEFAULT_CLUSTERING_ORDER = "ASC"
}