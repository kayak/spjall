package org.kapunga.spjall.model

import cats.syntax.either._
import io.circe.Decoder.Result
import io.circe._

case class Item(itemType: String, channel: SlackId, ts: EventTs)

object Item {
  implicit val itemDecoder: Decoder[Item] = new Decoder[Item] {
    override def apply(c: HCursor): Result[Item] =
      for {
        it <- c.downField("type").as[String]
        channel <- c.downField("channel").as[SlackId]
        ts <- c.downField("ts").as[EventTs]
      } yield { Item(it, channel, ts) }
  }
}
