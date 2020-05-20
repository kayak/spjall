package com.r9.spjall

import akka.actor.ActorRef
import collection.JavaConverters._

package object rtm {
  val pingWarnSize: Int = config.getInt("slack.rtm_api.ping_warn_size")
  val ignoredMsgTypes: Set[String] = config.getStringList("slack.rtm_api.ignored_msg_types").asScala.toSet

  final case class RtmClosedException(subscribers: Set[ActorRef], message: String = "", cause: Throwable = None.orNull)
    extends Exception(message, cause)
}
