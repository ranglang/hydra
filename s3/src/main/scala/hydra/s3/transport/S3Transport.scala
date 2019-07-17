package hydra.s3.transport

import akka.actor.{ActorLogging, Props}
import akka.stream.alpakka.s3.{MultipartUploadResult, S3Attributes}
import akka.stream.alpakka.s3.scaladsl.S3
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.ByteString
import hydra.core.transport.Transport
import hydra.core.transport.Transport.Deliver
import hydra.s3.producer.S3Record

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class S3Transport(s3Sink: (String, String) => Sink[ByteString, Future[MultipartUploadResult]]) extends Transport with ActorLogging {

  private implicit val executionContext: ExecutionContext = context.dispatcher
  private implicit val materializer: Materializer = ActorMaterializer()

  override def transport: Receive = {
    case Deliver(record: S3Record, deliveryId, callback) =>
      val result = Source.single(ByteString(record.payload))
        .withAttributes(S3Attributes.settingsPath("alpakka.s3"))
        .runWith(s3Sink(record.destination, record.fileName))

      result.onComplete {
        case Success(_) =>
          callback.onCompletion(deliveryId, None, None)
        case Failure(e) =>
          log.error("Failed to upload to S3, {}", e)
          callback.onCompletion(deliveryId, None, Some(e))
      }
  }

}

object S3Transport {

  private def getS3Sink(bucketName: String, fileName: String): Sink[ByteString, Future[MultipartUploadResult]] =
    S3.multipartUpload(bucketName, fileName)

  def props: Props = Props(new S3Transport(getS3Sink))

}
