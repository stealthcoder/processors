package edu.arizona.sista.matcher

import scala.util.parsing.combinator._
import edu.arizona.sista.processors.Sentence


/*
name: phosphorylation
priority: 1
type: sista
pattern: {{
  trigger: token="phosphorylates"
  theme: "dobj"
  cause: "nsubj"
}}
*/

case class TriggerMatcher(token: String) {
  def findAllIn(sentence: Sentence): Seq[Int] = {
    sentence.words.zipWithIndex filter (_._1 == token) map (_._2)
  }
}


trait Matcher {
  def findIn(sentence: Sentence, from: Int): Option[Int]
}


case class DepMatcher(dep: String) extends Matcher {
  def findIn(sentence: Sentence, from: Int): Option[Int] = {
    val matches = sentence.dependencies.get.outgoingEdges(from) filter (_._2 == dep) map (_._1)
    if (matches.size == 1) Some(matches.head)
    else None
  }
}


case class PathMatcher(lhs: Matcher, rhs: Matcher) extends Matcher {
  def findIn(sentence: Sentence, from: Int): Option[Int] = {
    lhs.findIn(sentence, from) match {
      case None => None
      case Some(i) => rhs.findIn(sentence, i)
    }
  }
}


class DependencyMatcher(val pattern: String) {
  private var _trigger: Option[TriggerMatcher] = None
  private var _arguments: Option[Map[String, Matcher]] = None

  def trigger = getFieldValue(_trigger)

  def arguments = getFieldValue(_arguments)

  parse(pattern)

  private def parse(pattern: String) {
    val fieldPat = """(\w+)\s*:\s*(.+)""".r
    val it = fieldPat findAllIn pattern map {
      case fieldPat(name, value) => (name -> value)
    }
    val fields = Map(it.toSeq: _*)
    _trigger = Some(TriggerMatcher(fields("trigger")))
    _arguments = Some(fields filterNot {
      case (k, v) => k == "trigger"
    } mapValues Parser.parse)
  }

  private def getFieldValue[T](field: Option[T]) = field match {
    case None => throw new Error("object not initialized")
    case Some(value) => value
  }

  def findAllIn(sentence: Sentence): Seq[Map[String, Int]] = {
    trigger findAllIn sentence flatMap (i => applyRules(sentence, i))
  }

  def applyRules(sentence: Sentence, i: Int): Option[Map[String, Int]] = {
    val matches = arguments.keySet flatMap { name =>
      arguments(name).findIn(sentence, i) match {
        case None => None
        case Some(i) => Some(name -> i)
      }
    }
    if (matches.isEmpty) None
    else Some(matches.toMap)
  }

}


object Parser extends RegexParsers {
  def parse(input: String): Matcher = parseAll(matcher, input).get

  def token: Parser[String] = """(\w+)""".r

  def matcher: Parser[Matcher] = pathMatcher

  def depMatcher: Parser[Matcher] = token ^^ {
    DepMatcher(_)
  }

  def pathMatcher: Parser[Matcher] = depMatcher ~ rep(">" ~ depMatcher) ^^ {
    case m ~ rest => (m /: rest) {
      case (lhs, ">" ~ rhs) => PathMatcher(lhs, rhs)
    }
  }
}
