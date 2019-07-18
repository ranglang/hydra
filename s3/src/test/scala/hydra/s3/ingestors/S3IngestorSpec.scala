package hydra.s3.ingestors

import akka.actor.{ActorSystem, Props}
import akka.testkit.TestActors.ForwardActor
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import hydra.core.ingest.{HydraRequest, RequestParams}
import hydra.core.protocol.{Join, Publish}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class S3IngestorSpec extends TestKit(ActorSystem("s3-transport-spec")) with Matchers with WordSpecLike
  with ImplicitSender
  with BeforeAndAfterAll  {

  "S3Ingestor" must {

    val transportProbe = TestProbe()

    val transportForward = Props(new ForwardActor(transportProbe.ref))

    val ingestor = system.actorOf(Props(new TestS3Ingestor(transportForward)))

    "Join ingest when proper" in {
      val metadata = Map(RequestParams.HYDRA_S3_BUCKET_NAME -> "bucketName", RequestParams.HYDRA_S3_FILE_NAME -> "fileName")
      ingestor ! Publish(HydraRequest(metadata = metadata, payload = "payload"))
      expectMsg(Join)
    }
  }

}

private[ingestors] class TestS3Ingestor(transport: Props) extends S3Ingestor {

  /**
    * Prevent actor from booting itself
    */
  override def preStart(): Unit = {}

  override def transportProps: Option[Props] = Some(transport)

}
