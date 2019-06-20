package hydra.cassandra

import org.apache.avro.Schema
import org.scalatest.{FlatSpec, Matchers}

import scala.io.Source

class CassandraSchemaParserSpec extends FlatSpec
  with Matchers {

  val schemaNoProperties: Schema = new Schema.Parser().parse(Source.fromResource("schema-with-no-primary-key.avsc").mkString)
  val schemaWithAllProperties: Schema = new Schema.Parser().parse(Source.fromResource("schema-with-all-schema-properties.avsc").mkString)

  "The CassandraSchemaParser" should
    s"default to ${CassandraWriteOptions.DEFAULT_IS_COMPACT_STORAGE} when there isn't a hydra.enable.compact.storage property on the schema" in {
      val isCompactStorage = CassandraSchemaParser.isCompactStorage(schemaNoProperties)
      isCompactStorage shouldBe CassandraWriteOptions.DEFAULT_IS_COMPACT_STORAGE
  }

  it should "get the schema value for the hydra.enable.compact.storage property" in {
    val isCompactStorage = CassandraSchemaParser.isCompactStorage(schemaWithAllProperties)
    isCompactStorage shouldBe true
  }

  it should "default to an empty Seq when there isn't a hydra.clustering.columns property on the schema" in {
    val clusteringColumns = CassandraSchemaParser.clusteringColumns(schemaNoProperties)
    clusteringColumns shouldBe Seq.empty
  }

  it should "get the schema value for the hydra.clustering.columns property" in {
    val clusteringColumns = CassandraSchemaParser.clusteringColumns(schemaWithAllProperties)
    clusteringColumns shouldBe Seq("name")
  }

  it should s"default to ${ClusteringColumns.DEFAULT_CLUSTERING_ORDER} when there isn't a hydra.clustering.order property on the schema" in {
    val clusteringOrder = CassandraSchemaParser.clusteringOrder(schemaNoProperties)
    clusteringOrder shouldBe ClusteringColumns.DEFAULT_CLUSTERING_ORDER
  }

  it should "get the schema value for the hydra.clustering.order property" in {
    val clusteringOrder = CassandraSchemaParser.clusteringOrder(schemaWithAllProperties)
    clusteringOrder shouldBe "DESC"
  }

  it should
    s"default to ${CassandraWriteOptions.DEFAULT_CONSISTENCY_LEVEL} when there isn't a hydra.consistency.level property on the schema"in {
    val consistencyLevel = CassandraSchemaParser.consistencyLevel(schemaNoProperties)
    consistencyLevel shouldBe CassandraWriteOptions.DEFAULT_CONSISTENCY_LEVEL
  }

  it should "get the schema value for the hydra.consistency.level property" in {
    val consistencyLevel = CassandraSchemaParser.consistencyLevel(schemaWithAllProperties)
    consistencyLevel shouldBe "ALL"
  }

  it should "default to None when there isn't a hydra.keyspace property on the schema" in {
    val keyspace = CassandraSchemaParser.keySpace(schemaNoProperties)
    keyspace shouldBe None
  }

  it should "get the schema value for the hydra.keyspace property" in {
    val keyspace = CassandraSchemaParser.keySpace(schemaWithAllProperties)
    keyspace shouldBe Some("test")
  }

  it should "default to an empty Seq when there isn't a hydra.partition.keys property on the schema" in {
    val partitionKeys = CassandraSchemaParser.partitionKeys(schemaNoProperties)
    partitionKeys shouldBe Seq.empty
  }

  it should "get the schema value for the hydra.partition.keys property" in {
    val partitionKeys = CassandraSchemaParser.partitionKeys(schemaWithAllProperties)
    partitionKeys shouldBe Seq("id", "name")
  }

}
