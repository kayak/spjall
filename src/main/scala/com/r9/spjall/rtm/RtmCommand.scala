package com.r9.spjall.rtm

import io.circe._
import io.circe.syntax._
import com.r9.spjall._

sealed trait RtmCommand {
  val id: Long
}

case class Ping(id: Long = RtmCommand.nextId, time: Long = System.currentTimeMillis) extends RtmCommand

object Ping {
  implicit val pingEncoder: Encoder[Ping] = new Encoder[Ping] {
    override def apply(p: Ping): Json =
      Json.obj(("type", "ping".asJson), ("id", p.id.asJson), ("time", p.time.asJson))
  }
}

case class Typing(channel: SlackId, id: Long = RtmCommand.nextId) extends RtmCommand

object Typing {
  implicit val typingEncoder: Encoder[Typing] = new Encoder[Typing] {
    override def apply(t: Typing): Json =
      Json.obj(("type", "typing".asJson), ("id", t.id.asJson), ("channel", t.channel.asJson))
  }
}

case class BotMessage(channel: SlackId, text: String, id: Long = RtmCommand.nextId) extends RtmCommand

object BotMessage {
  implicit val messageEncoder: Encoder[BotMessage] = new Encoder[BotMessage] {
    override def apply(m: BotMessage): Json =
      Json.obj(("type", "message".asJson), ("id", m.id.asJson), ("channel", m.channel.asJson), ("text", m.text.asJson))
  }
}

object RtmCommand {
  val counter: Iterator[Long] = new Iterator[Long] {
    var i: Long = 0
    def hasNext = true
    def next(): Long = { i += 1; i }
  }

  def nextId: Long = counter.next()

  implicit val rtmCommandEncoder: Encoder[RtmCommand] = Encoder.instance {
    case p: Ping => p.asJson
    case t: Typing => t.asJson
    case m: BotMessage => m.asJson
  }
}