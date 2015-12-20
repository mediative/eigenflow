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

package com.mediative.eigenflow.domain.messages

/**
 * A generic message to be sent on stage complete.
 *
 * @param timestamp Time when the message was created.
 * @param runId Identification of the running process for which the message was created.
 * @param stage Stage name for which the message was created.
 * @param message A generic message.
 */
case class GenericMessage(timestamp: Long, runId: String, stage: String, message: String)
