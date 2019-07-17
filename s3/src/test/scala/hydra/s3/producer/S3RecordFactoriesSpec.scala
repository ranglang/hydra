package hydra.s3.producer

import hydra.core.ingest.{HydraRequest, RequestParams}
import hydra.core.transport.AckStrategy
import org.scalatest.{AsyncFlatSpec, Matchers}

class S3RecordFactoriesSpec extends AsyncFlatSpec with Matchers {

  "S3RecordFactories" must "properly handle a HydraRequest" in {
    val metadata = Map(RequestParams.HYDRA_S3_BUCKET_NAME -> "bucketName", RequestParams.HYDRA_S3_FILE_NAME -> "fileName")
    new S3RecordFactories().build(HydraRequest(payload = "payload", metadata = metadata)).map {
      _ shouldBe S3Record(
        destination = "bucketName",
        fileName = "fileName",
        payload = "payload",
        ackStrategy = AckStrategy.NoAck
      )
    }
  }

}
