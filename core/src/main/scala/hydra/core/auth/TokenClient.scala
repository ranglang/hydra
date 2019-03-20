package hydra.core.auth

import java.util.UUID

import akka.http.scaladsl.model._
import com.typesafe.config.Config
import hydra.core.auth.TokenClient.TokenServiceResponse
import hydra.core.http.IHttpRequestor
import akka.http.scaladsl.unmarshalling.Unmarshal
import scala.concurrent.Future

class TokenClient(tokenConfig: Config, httpRequestor: IHttpRequestor) {

  val tokenServiceUrl = tokenConfig.getString("token-service-url")

  def generate(groupId: String): Future[TokenServiceResponse] = {
    val fullyQualifiedUrl = Uri(tokenServiceUrl).withPath(Uri.Path(s"/api/$groupId/token"))
    val request = HttpRequest(uri = fullyQualifiedUrl, method = HttpMethods.POST)
    httpRequestor.makeRequest(request).flatMap { response =>
      Unmarshal(response.entity).to[TokenServiceResponse]
    }
  }

  def validate(): Unit = {}
}

object TokenClient {
  case class TokenServiceResponse(token: UUID, group: String)
}
