package hydra.s3.producer

import hydra.core.transport.{AckStrategy, HydraRecord}

final case class S3Record(
                           destination: String,
                           fileName: String,
                           payload: String,
                           ackStrategy: AckStrategy
                         ) extends HydraRecord[String, String] {
  def key: Option[String] = Some(fileName)
}
