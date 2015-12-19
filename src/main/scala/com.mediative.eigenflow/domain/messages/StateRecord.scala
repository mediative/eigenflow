package com.mediative.eigenflow.domain.messages

/**
 * Process and stage states registry.
 */
sealed trait StateRecord {
  def identifier = toString
}

case object Processing extends StateRecord

case object Retrying extends StateRecord

case object Failed extends StateRecord

case object Complete extends StateRecord