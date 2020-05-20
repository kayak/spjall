package com.r9.spjall.model

import cats.syntax.either._
import io.circe._
import io.circe.Decoder.Result
import io.circe.syntax._

case class DndStatus(enabled: Boolean, nextStartTs: Long, nextEndTs: Long)

object DndStatus {
  implicit val dndStatusDecoder: Decoder[DndStatus] = new Decoder[DndStatus] {
    override def apply(c: HCursor): Result[DndStatus] =
      for {
        enabled <- c.downField("dnd_enabled").as[Boolean]
        nextStartTs <- c.downField("next_dnd_start_ts").as[Long]
        nextEndTs <- c.downField("next_dnd_end_ts").as[Long]
      } yield { DndStatus(enabled, nextStartTs, nextEndTs) }
  }

  implicit val dndStatusEncoder: Encoder[DndStatus] = new Encoder[DndStatus] {
    override def apply(d: DndStatus): Json = Json.obj(
      ("dnd_enabled", d.enabled.asJson),
      ("next_dnd_start_ts", d.nextStartTs.asJson),
      ("next_dnd_end_ts", d.nextEndTs.asJson))
  }
}
