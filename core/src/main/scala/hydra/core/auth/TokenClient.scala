package hydra.core.auth

import com.typesafe.config.Config
import hydra.core.http.IHttpRequestor

class TokenClient(tokenConfig: Config, httpRequestor: IHttpRequestor) {

  def generate(): Unit = {}
  def validate(): Unit = {}
}

object TokenClient {

}
