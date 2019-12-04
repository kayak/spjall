package org.kapunga.spjall.model

import cats.syntax.either._
import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._

import scala.util.matching.Regex

case class EventTs(ts: Long, id: Long) {
  override def toString: String = s"$ts.$id"
}

object EventTs {
  val eventTsPattern: Regex = "^([0-9]*)\\.([0-9]*)$".r("ts", "id")

  def apply(str: String): EventTs =
    eventTsPattern.findFirstMatchIn(str)
      .map(m => EventTs(m.group("ts").toLong, m.group("id").toLong))
      .getOrElse(throw new Exception(s"'$str' does not fit EventTs format."))

  implicit val eventTsDecoder: Decoder[EventTs] = new Decoder[EventTs] {
    override def apply(c: HCursor): Result[EventTs] = c.as[String].map(EventTs(_))
  }

  implicit val eventTsEncoder: Encoder[EventTs] = new Encoder[EventTs] {
    override def apply(a: EventTs): Json = a.toString.asJson
  }

  implicit val eventTsOrdering: Ordering[EventTs] = new Ordering[EventTs] {
    override def compare(x: EventTs, y: EventTs): Int = {
      val lc = implicitly[Ordering[Long]]

      if (x.ts != y.ts) lc.compare(x.ts, y.ts) else lc.compare(x.id, y.id)
    }
  }
}
