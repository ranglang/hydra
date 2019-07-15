package hydra.s3.producer

import hydra.core.ingest.{HydraRequest, RequestParams}
import hydra.core.transport.{HydraRecord, RecordFactory}

import scala.concurrent.{ExecutionContext, Future}

class S3RecordFactories extends RecordFactory[String, String] {

  override def build(request: HydraRequest)
                    (implicit ec: ExecutionContext): Future[S3Record] = request match {
    case r =>
      val dest = r.metadataValue(RequestParams.HYDRA_S3_BUCKET_NAME).get
      val name = r.metadataValue(RequestParams.HYDRA_S3_FILE_NAME).get
      Future.successful(S3Record(dest, name, r.payload, r.ackStrategy))
  }

}
