package hydra.core.auth

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import com.typesafe.config.ConfigFactory
import hydra.core.http.IHttpRequestor
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.Future

class TokenClientSpec extends Matchers with FlatSpecLike {

  val tokenConfig = ConfigFactory.load()
  val requestor = TestRequestor()

  "The token client" should "generate a token" in {
    val client = new TokenClient(tokenConfig, requestor)
    val tokenStr = client.generateToken()
    tokenStr shouldBe a[String]
  }

  it should "validate a token" in {
    fail()
  }

}


case class TestRequestor() extends IHttpRequestor {
  override def makeRequest(request: HttpRequest): Future[HttpResponse] = {
    Future.successful(HttpResponse(StatusCodes.OK, entity="It's sicknasty bruh"))
  }
}
