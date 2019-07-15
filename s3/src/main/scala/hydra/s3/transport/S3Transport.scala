package hydra.s3.transport

import akka.actor.ActorLogging
import hydra.core.transport.Transport
import hydra.core.transport.Transport.Deliver
import hydra.s3.producer.S3Record

class S3Transport extends Transport with ActorLogging {

  override def transport: Receive = {
    case Deliver(record: S3Record, deliveryId, callback) =>
      log.info("SENDING TO S3: {}", record)
  }

}
