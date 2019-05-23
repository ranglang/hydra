/*
 * Copyright (C) 2016 Pluralsight, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package hydra.kafka.ingestors

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props, Stash, Timers}
import com.typesafe.config.Config
import hydra.common.config.ConfigSupport
import hydra.common.logging.LoggingAdapter
import hydra.kafka.ingestors.KafkaTopicActor.{GetTopicRequest, GetTopicResponse, RefreshTopicList, TopicsTimer}
import org.apache.kafka.clients.admin.AdminClient
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.Try

class KafkaTopicsActor(cfg: Config, checkInterval: FiniteDuration) extends Actor
  with Timers
  with LoggingAdapter
  with Stash {

  self ! RefreshTopicList

  timers.startPeriodicTimer(TopicsTimer, RefreshTopicList, checkInterval)

  private def fetchTopics(): Seq[String] = {
    Try(AdminClient.create(ConfigSupport.toMap(cfg).asJava)).map { c =>
      val t = c.listTopics().names.get(checkInterval.toSeconds.longValue / 2, TimeUnit.SECONDS).asScala.toSeq
      Try(c.close(checkInterval.toSeconds.longValue / 2, TimeUnit.SECONDS))
      t
    }
  }.recover { case e => log.error("Unable to load Kafka topics.", e); Seq.empty }.get

  override def receive: Receive = {
    case RefreshTopicList =>
      context.become(withTopics(fetchTopics()))
      unstashAll()
    case GetTopicRequest(_) => stash()
  }

  private def withTopics(topicList: Seq[String]): Receive = {
    case GetTopicRequest(topic) =>
      val topicR = topicList.find(_ == topic)
      sender ! GetTopicResponse(topic, DateTime.now, topicR.isDefined)

    case RefreshTopicList =>
      context.become(withTopics(fetchTopics()))
  }

  override def postStop(): Unit = {
    timers.cancel(TopicsTimer)
  }
}

object KafkaTopicActor {

  case object TopicsTimer

  case object RefreshTopicList

  case class GetTopicRequest(topic: String)

  case class GetTopicResponse(topic: String, lookupDate: DateTime, exists: Boolean)

  def props(cfg: Config, interval: FiniteDuration = 10 seconds) = Props(classOf[KafkaTopicsActor], cfg, interval)

}


