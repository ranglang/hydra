package hydra.core

import hydra.core.ingest.HydraRequest
import hydra.core.protocol._
import org.joda.time.DateTime
import org.scalatest.{FlatSpecLike, Matchers}

import scala.concurrent.duration._

class TokenClientSpec extends Matchers with FlatSpecLike {

  "The token client" should "generate a token" in {
    val ex = new HydraException("error")
    ex.getCause shouldBe null
  }

  it should "validate a token" in {

  }

}
