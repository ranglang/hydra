package hydra.s3.transport

import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.Uri
import akka.stream.alpakka.s3.MultipartUploadResult
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import akka.util.ByteString
import hydra.core.transport.Transport.Deliver
import hydra.core.transport.{AckStrategy, RecordMetadata, TransportCallback}
import hydra.s3.producer.S3Record
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future

class S3TransportSpec extends TestKit(ActorSystem("s3-transport-spec")) with Matchers with WordSpecLike
with ImplicitSender
with BeforeAndAfterAll {

  "S3Transport" must {

    val successSink = (_: String, _: String) => TestSink.probe[ByteString]
      .mapMaterializedValue(_ => Future successful MultipartUploadResult(Uri(""), "", "", "", None))

    val failureException = new Exception("Something went wrong...")

    val failureSink = (_: String, _: String) => TestSink.probe[ByteString]
      .mapMaterializedValue(_ => Future failed failureException)

    val s3Record = S3Record("dest", "fileName", "payload", AckStrategy.Persisted)

    val probe = TestProbe()

    val callback: TransportCallback = (d: Long, md: Option[RecordMetadata], err: Option[Throwable]) => probe.ref ! (d, md, err)

    val s3TransportSuccess = system.actorOf(Props(new S3Transport(successSink)))

    "call callback function on success" in {
      s3TransportSuccess ! Deliver(s3Record, 100L, callback)
      probe.expectMsgPF() {
        case (d, md, err) =>
          d shouldBe 100L
          md shouldBe None
          err shouldBe None
      }
    }

    val s3TransportFailure = system.actorOf(Props(new S3Transport(failureSink)))

    "call callback function on failure" in {
      s3TransportFailure ! Deliver(s3Record, 100L, callback)
      probe.expectMsgPF() {
        case (d, md, err) =>
          d shouldBe 100L
          md shouldBe None
          err shouldBe Some(failureException)
      }
    }

  }

}
