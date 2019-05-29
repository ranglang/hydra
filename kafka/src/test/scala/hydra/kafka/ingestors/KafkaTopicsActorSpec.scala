package hydra.kafka.ingestors

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import hydra.common.config.ConfigSupport
import hydra.kafka.ingestors.KafkaTopicActor._
import net.manub.embeddedkafka.EmbeddedKafka
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import scala.concurrent.duration._

class KafkaTopicsActorSpec
  extends TestKit(ActorSystem("kafka-topics-spec", config = ConfigFactory.parseString("akka.actor.provider=cluster")))
    with Matchers
    with FlatSpecLike
    with ImplicitSender
    with ConfigSupport
    with Eventually
    with EmbeddedKafka
    with BeforeAndAfterAll {
  val config = ConfigFactory.parseString(
    """
      |  bootstrap.servers = "localhost:6001"
      |  zookeeper = "localhost:6000"
    """.stripMargin)
  implicit val patience = PatienceConfig(timeout = 5 seconds, interval = 1 second)

  override def afterAll = TestKit.shutdownActorSystem(system)

  "A KafkaTopicsActor" should "return topics that exist" in {
    withRunningKafka {
      createCustomTopic("topic-actor")
      val actor = system.actorOf(KafkaTopicActor.props(config))
      actor ! GetTopicRequest("topic-actor")
      expectMsgPF() {
        case GetTopicResponse(t, _, e) =>
          t shouldBe "topic-actor"
          e shouldBe true
      }
    }
  }

  it should "not return topics that doesn't exist" in {
    withRunningKafka {
      val actor = system.actorOf(KafkaTopicActor.props(config))
      actor ! GetTopicRequest("test-topic")
      expectMsgPF() {
        case GetTopicResponse(t, _, e) =>
          t shouldBe "test-topic"
          e shouldBe false
      }
    }
  }

  it should "update its local cache" in {
    withRunningKafka {
      val actor = system.actorOf(KafkaTopicActor.props(config))
      actor ! GetTopicRequest("new-topic")
      expectMsgPF() {
        case GetTopicResponse(t, _, e) =>
          t shouldBe "new-topic"
          e shouldBe false
      }
      createCustomTopic("new-topic")
      eventually {
        actor ! GetTopicRequest("new-topic")
        expectMsgPF() {
          case GetTopicResponse(t, _, e) =>
            t shouldBe "new-topic"
            e shouldBe true
        }
      }
    }
  }

  it should "publish an error if the first attempt to fetch topics fails" in {
    val actor = system.actorOf(KafkaTopicActor.props(config))
    val probinho = TestProbe()
    system.eventStream.subscribe(probinho.ref, classOf[GetTopicsFailure])
    probinho.expectMsgType[GetTopicsFailure]
  }

  it should "publish an error if subsequent attempts to fetch topics fail" in {
    val probinho = TestProbe()
    val actor: ActorRef = system.actorOf(KafkaTopicActor.props(config))
    system.eventStream.subscribe(probinho.ref, classOf[GetTopicsFailure])
    probinho.expectMsgType[GetTopicsFailure]
    actor ! GetTopicsResponse(Seq("test-topic"))
    actor ! RefreshTopicList
    probinho.expectMsgType[GetTopicsFailure]
  }
}