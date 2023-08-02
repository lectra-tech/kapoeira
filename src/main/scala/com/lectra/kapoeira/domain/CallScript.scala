package com.lectra.kapoeira.domain

import ammonite.ops.ImplicitWd._
import ammonite.ops._

import scala.util._

sealed trait CallScript

object CallScript {
  implicit class Syntax(callScript: CallScript) {
    def run(backgroundContext: BackgroundContext): ScriptResult = (callScript match {
      case CallScriptPath(script) => callScript(scriptFullPath(backgroundContext.substituteVariablesIn(script)))
      case CallPlainScript(script) => callScript(backgroundContext.substituteVariablesIn(script))
    }).fold({ case e: ShelloutException => ScriptResult.from(e.result) }, ScriptResult.from)

    private def callScript(script: String): Try[CommandResult] = Try(
      %%(script.split(" ").toList)
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
    commandResult.out.string.trim,
    commandResult.err.string.trim
  )
}
