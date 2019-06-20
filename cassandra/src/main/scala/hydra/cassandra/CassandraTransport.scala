package hydra.cassandra

import hydra.core.transport.Transport
import hydra.common.config.ConfigSupport
import hydra.common.logging.LoggingAdapter

class CassandraTransport extends Transport with ConfigSupport with LoggingAdapter {
  override def transport: Receive = ???
}
