/*
 * Copyright 2016 Mediative
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

package com.mediative.eigenflow.test.helpers

import java.text.SimpleDateFormat
import java.util.{ Calendar, Date }

import com.mediative.eigenflow.helpers.DateHelper
import org.scalatest.FreeSpec

class DateHelperTest extends FreeSpec {

  val isoDateFormat = "yyyy-MM-dd"
  val isoSimpleDateFormat = "yyyyMMdd"
  val isoTimeFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'"

  "DateHelper" - {
    "when a date is provided" - {
      s"must parse $isoDateFormat date properly but without time" in {
        testDateFormat(isoDateFormat)
      }

      s"must parse $isoSimpleDateFormat date properly but without time" in {
        testDateFormat(isoSimpleDateFormat)
      }

      "must return None if date format is incorrect" in {
        val dateFormat = new SimpleDateFormat("yyyy/MM/dd")

        val date = new Date

        val dateString = dateFormat.format(date)
        val result = DateHelper.parse(dateString)

        assert(result.isEmpty)
      }

      "must return None if date is invalid" in {
        val dates = Seq("2013-12-32", "-2013-12-21", "2013-13-01", "2013-02-30")

        dates.foreach { date =>
          assert(DateHelper.parse(date).isEmpty)
        }
      }
    }

    "when time is provided" - {
      s"must parse date and time in $isoTimeFormat format" in {
        val dateFormat = new SimpleDateFormat(isoTimeFormat)

        val date = new Date

        val dateString = dateFormat.format(date)
        val resultOption = DateHelper.parse(dateString)

        resultOption.fold(fail(s"failed to parse: ${dateString}")) { result =>
          assert(dateFormat.format(result) == dateFormat.format(date))
        }
      }
    }
  }

  private def testDateFormat(format: String): Unit = {
    val dateFormat = new SimpleDateFormat(format)
    val date = new Date

    val dateString = dateFormat.format(date)

    val resultOption = DateHelper.parse(dateString)

    resultOption.fold(fail(s"failed to parse: ${dateString}")) { result =>
      val calendar = Calendar.getInstance()
      calendar.setTime(result)
      assert(calendar.get(Calendar.HOUR) == 0)
      assert(calendar.get(Calendar.MINUTE) == 0)
      assert(dateFormat.format(result) == dateFormat.format(date))
    }
  }
}
