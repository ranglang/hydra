package hydra.core.http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}

import scala.concurrent.Future

trait IHttpRequestor {
  def makeRequest(request: HttpRequest): Future[HttpResponse]
}

class HttpRequestor(implicit sys: ActorSystem) extends IHttpRequestor {
  override def makeRequest(request: HttpRequest): Future[HttpResponse] =
    Http().singleRequest(request)
}