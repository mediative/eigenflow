package com.mediative.eigenflow.helpers

import scala.language.implicitConversions

object PrimitiveImplicits {
  implicit def int2String(value: Int): String = value.toString
  implicit def string2Int(value: String): Int = Integer.parseInt(value)
}
