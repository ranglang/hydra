package hydra.core.auth

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.config.Config
import hydra.core.http.IHttpRequestor
import hydra.core.marshallers.HydraJsonSupport

import scala.concurrent.{ExecutionContext, Future}


class TokenClient(tokenConfig: Config, httpRequestor: IHttpRequestor)(implicit ec: ExecutionContext, mat: ActorMaterializer) {
  import TokenClient._

  implicit val tokenServiceResponseFmt = jsonFormat2(TokenServiceResponse)

  case class TokenServiceResponse(token: UUID, group: String)

  val tokenServiceUrl = tokenConfig.getString("token-service-url")

  def generate(groupId: String): Future[TokenServiceResponse] = {
    val fullyQualifiedUrl = Uri.from(scheme = "http", host = tokenServiceUrl, path = s"/api/$groupId/token")
    val request = HttpRequest(uri = fullyQualifiedUrl, method = HttpMethods.POST)
    httpRequestor.makeRequest(request).flatMap { response =>
      Unmarshal(response.entity).to[TokenServiceResponse]
    }
  }

  def validate(): Unit = {

  }
}

object TokenClient extends HydraJsonSupport {

}
