package com.lectra.kapoeira.glue

import com.lectra.kapoeira.domain.BackgroundContext
import com.lectra.kapoeira.domain.functions.FunctionRepository

object FunctionManager {
  case class ParamsResolutionError(unresolvedVariable : String) {
    override def toString = s"Unable to resolve $unresolvedVariable variable."
  }
}

class FunctionManager(functionRepository: FunctionRepository) {

  import FunctionManager._

  def resolveVariables(params: String)(implicit backgroundContext: BackgroundContext): Either[ParamsResolutionError, Seq[String]] = {
    val Variable = """\$\{(.*)\}""".r
    val resolvedParams = params.split(" ")

    resolvedParams.foldLeft[Either[ParamsResolutionError, Seq[String]]](Right(Seq.empty[String])) {
      case (Right(values), Variable(varId)) =>
        backgroundContext.getVariable(varId)
          .map(v => values :+ v)
          .toRight(ParamsResolutionError(varId))
      case (Right(values), value) => Right(values :+ value)
      case (left, _) => left
    }
  }

  def apply(targetVariable: String, functionDefinition: String, params: String)(implicit backgroundContext: BackgroundContext): Either[String, Unit] = {
    functionDefinition match {
      case functionRepository(function)=>
        val resolvedParams = if(params.isEmpty) Right(Seq.empty) else resolveVariables(params)
        resolvedParams.map { p =>
          backgroundContext.addVariable(targetVariable, function(p.toArray).toString)
        }.left.map(_.toString)

      case unknownIdentifier => Left(s"Function $unknownIdentifier isn't supported.")
    }
  }

}
