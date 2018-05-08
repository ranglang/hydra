package hydra.common.auth

import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCredentials}

import scala.concurrent.{ExecutionContext, Future}

trait HydraAuthenticator {

  type AuthenticationResult[+T] = Either[HttpChallenge, T]

  val challenge = HttpChallenge("Hydra", Some("Hydra"))

  def auth(credentials: Option[HttpCredentials]): Future[String]

  def authenticate(credentials: Option[HttpCredentials])
                  (implicit ec: ExecutionContext): Future[AuthenticationResult[String]] = {
    auth(credentials)
      .map(Right(_))
      .recover {
        case _: Throwable => Left(challenge)
      }
  }
}
