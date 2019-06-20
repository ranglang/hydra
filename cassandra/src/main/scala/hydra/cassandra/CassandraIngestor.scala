package hydra.cassandra

import com.typesafe.config.Config
import configs.syntax._
import hydra.core.akka.SchemaRegistryActor
import hydra.core.ingest.{HydraRequest, Ingestor, TransportOps}
import hydra.core.protocol._
import hydra.core.transport.RecordFactory

import scala.util.Try

class CassandraIngestor extends Ingestor with TransportOps {

  private val schemaRegistryActor = context.actorOf(SchemaRegistryActor.props(applicationConfig))

  override def recordFactory: RecordFactory[_, _] = CassandraRecordFactory(schemaRegistryActor)

  override def transportName: String = "cassandra"

  override def validateRequest(request: HydraRequest): Try[HydraRequest] = {
    Try {
      val profile = request.metadataValue(CassandraRecordFactory.CASSANDRA_PROFILE_PARAM).get
      applicationConfig.get[Config](s"transports.cassandra.profiles.$profile").map(_ => request)
        .valueOrThrow(_ => new IllegalArgumentException(s"No db profile named '$profile' found."))
    }
  }

  ingest {
    case Publish(request) =>
      sender ! (if (request.hasMetadata(CassandraRecordFactory.CASSANDRA_PROFILE_PARAM)) Join else Ignore)

    case Ingest(record, ackStrategy) => transport(record, ackStrategy)
  }
}
