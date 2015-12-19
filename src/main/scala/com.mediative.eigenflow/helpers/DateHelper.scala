package com.mediative.eigenflow.helpers

import java.text.SimpleDateFormat
import java.util.Date

import jawn.ParseException

object DateHelper {
  val SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
  val DateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val TimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  def parse(string: String): Option[Date] = {
    try {
      Some(
        if (string.contains("-")) {
          if (string.contains("T")) {
            TimeFormat.parse(string)
          } else {
            DateFormat.parse(string)
          }
        } else {
          SimpleDateFormat.parse(string)
        })
    } catch {
      case e: ParseException => None
    }
  }
}
