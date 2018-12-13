package hydra.auth.endpoints

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class TokenEndpointSpec extends FlatSpec
  with ScalatestRouteTest
  with Matchers
  with BeforeAndAfterAll {

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  val tokenEndpointRoutes = new TokenEndpoint.routes

  "The TokenEndpoint" should "generate a new token" in {
    val creds = BasicHttpCredentials("test-group", "test-pass")
    Post("/token") ~> addCredentials(creds) ~> tokenEndpoint ~> check {
      status shouldBe StatusCodes.OK

    }
  }

  it should "return a list of currently associated tokens for a group" in {
    fail()
  }

  it should "delete a token" in {
    fail()
  }
}
