package hydra.core.auth

import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import com.typesafe.config.ConfigFactory
import hydra.core.http.IHttpRequestor
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.Future

class TokenClientSpec extends Matchers with MockFactory with FlatSpecLike {

  val tokenConfig = ConfigFactory.load()
  val requestor = mock[IHttpRequestor]

  "The token client" should "generate a token" in {
    val client = new TokenClient(tokenConfig, requestor)
    val groupId = "testGroup"

    (requestor.makeRequest _)
      .expects(_: HttpRequest)
      .once()

    val tokenStr = client.generate(groupId)
    tokenStr shouldBe a[String]
  }

  it should "validate a token" in {
    fail()
  }

}