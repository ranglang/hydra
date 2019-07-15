package hydra.s3.ingestors

import configs.syntax._
import hydra.core.ingest.{Ingestor, RequestParams, TransportOps}
import hydra.core.protocol.{Ignore, Ingest, Join, Publish}
import hydra.core.transport.RecordFactory
import hydra.s3.producer.S3RecordFactories

import scala.concurrent.duration._

class S3Ingestor extends Ingestor with TransportOps {

  override def recordFactory: RecordFactory[_, _] = new S3RecordFactories()

  override def initTimeout: FiniteDuration = applicationConfig
    .get[FiniteDuration]("s3-ingestor-timeout")
    .valueOrElse(2.seconds)

  ingest {
    case Publish(request) =>
      sender ! (if (request.hasMetadata(RequestParams.HYDRA_S3_BUCKET_NAME) && request.hasMetadata(RequestParams.HYDRA_S3_FILE_NAME)) Join else Ignore)
    case Ingest(record, ack) =>
      transport(record, ack)
  }

  override def transportName: String = "s3"

}
