package hydra.core.ingest

import akka.actor.{ActorRef, Props}
import configs.syntax._
import hydra.common.config.ConfigSupport
import hydra.common.logging.LoggingAdapter
import hydra.core.akka.InitializingActor.{InitializationError, Initialized}
import hydra.core.protocol._
import hydra.core.transport.{AckStrategy, HydraRecord}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
  * Encapsulates basic transport operations: Look up an existing transport and
  * transports a HydraRequest using the looked up transport.
  *
  * Also has logic for dealing with errors.
  *
  * Created by alexsilva on 5/27/17.
  */
trait TransportOps extends ConfigSupport with LoggingAdapter {
  this: Ingestor =>

  implicit val ec: ExecutionContext = context.dispatcher

  override def initTimeout: FiniteDuration = applicationConfig
    .getOrElse[FiniteDuration](s"transports.$transportName.resolve-timeout", 30.seconds).value

  /**
    * Always override this with a def due to how Scala initializes val in subtraits.
    */
  def transportName: String

  def transportProps: Option[Props] = None

  private val transportPath = applicationConfig.get[String](s"transports.$transportName.path")
    .valueOrElse(s"/user/service/transport_registrar/${transportName}_transport")

  lazy val transportActorFuture: Future[ActorRef] =
    transportProps match {
      case Some(props) =>
        Future.successful(context.actorOf(props, transportName))
      case None =>
        context.actorSelection(transportPath).resolveOne()(initTimeout)
    }

  /**
    * Overrides the init method to look up a transport
    */
  override def init: Future[HydraMessage] = {
    transportActorFuture
      .map { _ =>
        log.info("{}[{}] initialized", Seq(thisActorName, self.path): _*); Initialized
      }
      .recover {
        case e => InitializationError(new IllegalArgumentException(s"[$thisActorName]: No transport found " +
          s" at $transportPath", e))
      }
  }

  def transport[K, V](record: HydraRecord[K, V], ack: AckStrategy): Unit = {
    val supervisor = sender()
    transportActorFuture.foreach(_ ! Produce(record, supervisor, ack))
  }

}
