package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsSentence, DcsWord}
import org.slf4j.{Logger, LoggerFactory}
import sanskritCoders.dcsScraper.dcsScraper.browser

class DcsSentenceWrapper(sentence: DcsSentence) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  //  <a href="index.php?contents=lemma&IDWord=96104"target="_blank">tadā</a> [indecl.]-
  //    <a href="index.php?contents=lemma&IDWord=159581"target="_blank">majj</a> [3. sg. athem. s-Aor.]&nbsp;&nbsp;
  //  <a href="index.php?contents=lemma&IDWord=51708"target="_blank">cintā</a> [comp.]-
  //    <a href="index.php?contents=lemma&IDWord=104154"target="_blank">sarit</a> [l.s.f.]&nbsp;&nbsp;
  //  <a href="index.php?contents=lemma&IDWord=138107"target="_blank">virahin</a> [n.s.f.]
  def scrapeAnalysis(): Boolean = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printonesentence",
        "sentenceid" -> sentence.dcsId.toString
      ))
    if (doc.toHtml.contains("no analysis for this sentence")) {
      return false
    }
//    log.debug(doc.toHtml)
//    log.debug(doc.toHtml.split("&nbsp;&nbsp;").mkString("|||"))
    val wordGroupHtmls = doc.toHtml.split("&nbsp;&nbsp;")
    val wordAnalysisRegex = """<a href=".+?IDWord=(\d+?)".+?>(.+?)</a> \[(.+?)\]""".r
    val analysis = wordGroupHtmls.map(wordGroupHtml => {
      val wordAnalysisMatches = wordAnalysisRegex.findAllIn(wordGroupHtml).matchData.toSeq

//      log debug wordAnalysisMatches.mkString("|||")
      wordAnalysisMatches.toSeq.map(subwordAnalysisMatch => DcsWord(root = subwordAnalysisMatch.group(2), dcsId = subwordAnalysisMatch.group(1).toInt, dcsGrammarHint = Some(subwordAnalysisMatch.group(3))))
      }).toSeq

    sentence.dcsAnalysisDecomposition = Some(analysis)
    //    sentence.dcsAnalysis = Some(doc.toString)
    //    log.debug(sentence.toString)
    return true
  }

}
