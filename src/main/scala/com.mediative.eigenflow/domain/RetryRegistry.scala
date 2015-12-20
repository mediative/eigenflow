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

package com.mediative.eigenflow.domain

/**
 * Registry for the stage retries.
 *
 * Keeps tracking of:
 * - when the registry was created (first exception happened)
 * - timeout indicating how long to try (in milliseconds)
 * - failures registry.
 */
case class RetryRegistry(timestamp: Long, timeout: Option[Long] = None, failuresRegistry: Seq[FailureRegistry])

case class FailureRegistry(failure: Throwable, attemptsMade: Int, maxAttempts: Int)

/**
 * Helper functions for retry registry.
 */
object RetryRegistry {
  /**
   * Create retry registry with the given timeout.
   *
   * @param timeout Global timeout of retries. None - for no timeout.
   */
  def create(timeout: Option[Long]): RetryRegistry = RetryRegistry(
    timestamp = System.currentTimeMillis(),
    timeout = timeout,
    failuresRegistry = Seq.empty
  )

  /**
   * Add a failure to the given registry.
   * If the registry does not exists it will create a new one with the new failure.
   *
   * @param registryOption Existing registry. None, if this is the first failure.
   * @param failure Failure to register.
   * @param retry Retry strategy applied for the given failure.
   * @param timeout The global timeout, is used only when creating a new registry.
   */
  def addFailure(registryOption: Option[RetryRegistry],
    failure: Throwable,
    retry: Retry,
    timeout: Option[Long]): RetryRegistry = {
    val registry = registryOption.getOrElse(create(timeout)) // get existing or create a new registry if not exist.
    val updatedFailuresRegistry = findFailure(Some(registry), failure).fold {
      // first time this failure happened, add it to the registry
      registry.failuresRegistry.:+(FailureRegistry(failure, 1, retry.attempts))
    } { failureRegistryEntry =>
      // this failure already happened, increase the number of attempts.
      registry.failuresRegistry.
        filterNot(_ == failureRegistryEntry).
        :+(failureRegistryEntry.
          copy(attemptsMade = failureRegistryEntry.attemptsMade + 1))
    }
    // update the registry
    registry.copy(failuresRegistry = updatedFailuresRegistry)
  }

  /**
   * Find a record of the given failure.
   *
   * @param registry Registry to search in.
   * @param failure Failure to search for.
   * @return Failure entry or None if nothing found.
   */
  def findFailure(registry: Option[RetryRegistry], failure: Throwable): Option[FailureRegistry] = {
    registry.flatMap(_.failuresRegistry.find(_.failure.getClass.equals(failure.getClass)))
  }

  /**
   * Check if there are attempts left for the given failure or the timeout is not reached.
   *
   */
  def isValidForNextRetry(registryOption: Option[RetryRegistry], failure: Throwable): Boolean = {
    registryOption.fold(true) { registry =>
      registry.timeout.fold {
        // no timeout applied, validate if the number of attempts is not reached
        !isNumberOfAttemptsReached(registryOption, failure)
      } { deadline =>
        // check if deadline and number of attempts are not reached
        (registry.timestamp + deadline < System.currentTimeMillis()) &&
          !isNumberOfAttemptsReached(registryOption, failure)
      }
    }
  }

  /**
   * Check if the number of attempts are not reached for the given failure.
   *
   */
  def isNumberOfAttemptsReached(registryOption: Option[RetryRegistry], failure: Throwable): Boolean = {
    registryOption.fold(false) { registry =>
      findFailure(Some(registry), failure).fold(false)(entry => entry.attemptsMade >= entry.maxAttempts)
    }
  }
}
