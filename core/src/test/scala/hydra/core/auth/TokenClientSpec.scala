package hydra.core.auth

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import hydra.core.http.IHttpRequestor
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TokenClientSpec extends TestKit(ActorSystem("token-client-spec"))
  with Matchers
  with MockFactory
  with FlatSpecLike
  with ScalaFutures {

  val tokenConfig = ConfigFactory.parseString("""{"token-service-url": "da-url"}""")
  val requestor = mock[IHttpRequestor]
  implicit val mat = ActorMaterializer()

  "The token client" should "generate a token" in {
    val client = new TokenClient(tokenConfig, requestor)
    val groupId = "testGroup"
    val testUUID = UUID.randomUUID()

    (requestor.makeRequest _)
      .expects(*)
      .returning(Future.successful(HttpResponse(entity = HttpEntity(ContentType(MediaTypes.`application/json`), s"""{"token": "$testUUID", "group": "test-group"}"""))))
      .once()

    whenReady(client.generate(groupId)) { tokenResp =>
      tokenResp.token shouldBe testUUID

      tokenResp.group shouldBe "test-group"
    }

  }

  it should "validate a token" in {
    fail()
  }

  it should "return a cached token if available" in {

  }

  it should "call the token service if a token isn't present in the cache" in {

  }

}