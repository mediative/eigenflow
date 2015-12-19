package com.mediative.eigenflow.domain

import scala.concurrent.duration.FiniteDuration

/**
 * Retry strategy.
 *
 * @param interval Interval between retries.
 * @param attempts Number of retries.
 */
case class Retry(interval: FiniteDuration, attempts: Int)

object Retry {

  import scala.concurrent.duration._

  def NoRetry: PartialFunction[Throwable, Retry] = {
    case _ => Retry(interval = 0.seconds, attempts = 0)
  }
}