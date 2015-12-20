/*
 * Copyright 2015 Mediative
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
