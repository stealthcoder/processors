package edu.arizona.sista.odin.domains.bigmechanism.summer2015

import edu.arizona.sista.processors.bionlp.BioNLPProcessor
import edu.arizona.sista.odin._
import edu.arizona.sista.odin.domains.bigmechanism.dryrun2015.Ruler.readRules
import edu.arizona.sista.odin.domains.bigmechanism.dryrun2015.DarpaActions
import edu.arizona.sista.odin.domains.bigmechanism.dryrun2015.displayMention

object DarpaDoItAll extends App {
  // read file from command line
  val text = io.Source.fromFile(args.head).mkString

  // annotate text
  val proc = new BioNLPProcessor
  val doc = proc.annotate(text)

  // initialize extractor engine
  val rules = readRules()
  val actions = new DarpaActions
  val grounder = new Grounder
  val coref = new Coref
  val flow = new DarpaFlow(grounder, coref)
  val ee = new ExtractorEngine(rules, actions, flow.apply)

  // extract mentions from document
  val mentions = ee.extractFrom(doc)

  // print mentions found
  mentions foreach displayMention
}
