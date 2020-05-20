package com.r9.spjall

import cats.syntax.either._
import io.circe.Decoder.Result
import io.circe._

import scala.reflect.ClassTag

abstract class TypeCheckingDecoder[T: ClassTag] extends Decoder[T] {
  override def apply(c: HCursor): Result[T] = {
    val className = implicitly[ClassTag[T]].runtimeClass.getSimpleName

    val cp = canParse(c)

    cp.flatMap(if (_) parse(c) else Left(DecodingFailure(s"JSON is not a $className", c.history)))
  }

  def canParse(c: HCursor): Result[Boolean]

  def parse(c: HCursor): Result[T]
}
