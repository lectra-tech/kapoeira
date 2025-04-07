/*
 * Copyright (C) 2025 Lectra
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */
package com.lectra.kapoeira.domain

import os.{CommandResult, SubprocessException}

import scala.util._

sealed trait CallScript

object CallScript {
  implicit class Syntax(callScript: CallScript) {
    def run(backgroundContext: BackgroundContext): ScriptResult = (callScript match {
      case CallScriptPath(script) => callScript(scriptFullPath(backgroundContext.substituteVariablesIn(script)))
      case CallPlainScript(script) => callScript(backgroundContext.substituteVariablesIn(script))
    }).fold({ case e: SubprocessException => ScriptResult.from(e.result) }, ScriptResult.from)

    private def callScript(script: String): Try[CommandResult] = Try(
      os.proc(script.split(" ").toList).call()
    )

    private def scriptFullPath(script: String): String = {
      Try(System.getProperty("user.dir"))
        .map {
          case null => ""
          case s => s.trim
        }
        .fold(
          _ => script,
          { baseDir =>
            if (script.startsWith("/")) script else s"$baseDir/$script"
          }
        )
    }
  }

  final case class CallScriptPath(script: String) extends CallScript

  final case class CallPlainScript(script: String) extends CallScript
}

final case class ScriptResult(exitCode: Int, stdOut: String, stdErr: String)

object ScriptResult {
  def from(commandResult: CommandResult): ScriptResult = ScriptResult(
    commandResult.exitCode,
    commandResult.out.trim(),
    commandResult.err.trim()
  )
}
