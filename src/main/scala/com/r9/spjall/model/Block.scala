package com.r9.spjall.model

import cats.syntax.either._
import cats.syntax.functor._
import io.circe.Decoder.Result
import io.circe._

sealed trait Block

case class UnsupportedBlock(blockType: String, json: String) extends Block

object UnsupportedBlock {
  implicit val unsupportedBlockDecoder: Decoder[UnsupportedBlock] = new Decoder[UnsupportedBlock] {
    override def apply(c: HCursor): Result[UnsupportedBlock] =
      for {
        blockType <- c.downField("type").as[String]
      } yield UnsupportedBlock(blockType, c.value.spaces2)
  }
}

object Block {
  implicit val blockDecoder: Decoder[Block] = List[Decoder[Block]](
    UnsupportedBlock.unsupportedBlockDecoder.widen).reduce(_ or _)
}