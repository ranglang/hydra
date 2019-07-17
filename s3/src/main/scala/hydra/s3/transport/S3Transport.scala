package hydra.s3.transport

import akka.actor.ActorLogging
import akka.stream.alpakka.s3.{S3Attributes, S3Settings}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import hydra.common.config.ConfigSupport
import hydra.core.transport.Transport
import hydra.core.transport.Transport.Deliver
import hydra.s3.producer.S3Record

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class S3Transport extends Transport with ActorLogging {

  private implicit val executionContext: ExecutionContext = context.dispatcher
  private implicit val materializer: Materializer = ActorMaterializer()

  override def transport: Receive = {
    case Deliver(record: S3Record, deliveryId, callback) =>
      val b = ConfigFactory.load.getString("alpakka.s3.endpoint-url")
      val s3Sink = S3.multipartUpload(record.destination, record.fileName)
      val result = Source.single(ByteString(record.payload))
        .withAttributes(S3Attributes.settingsPath("alpakka.s3"))
        .runWith(s3Sink)

      result.onComplete {
        case Success(_) =>
          callback.onCompletion(deliveryId, None, None)
        case Failure(e) =>
          log.error("Failed to upload to S3, {}", e)
          callback.onCompletion(deliveryId, None, Some(e))
      }
  }

}
