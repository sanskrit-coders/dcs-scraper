package dcsScraper

import dbSchema.dcs.DcsSentence
import org.json4s.DefaultFormats
import org.scalatest.FlatSpec
import org.slf4j.LoggerFactory
import sanskritCoders.dcsScraper.DcsSentenceWrapper

class DcsSentenceWrapperTest extends FlatSpec {
  private val log = LoggerFactory.getLogger(this.getClass)
  implicit val formats: DefaultFormats.type = DefaultFormats
  implicit def dcsSentenceWrap(s: DcsSentence) = new DcsSentenceWrapper(s)

  "DcsSentenceWrapper" should "parse analysis correctly" in {

    // The below is from https://github.com/sanskrit-coders/dcs-scraper/issues/2
    val sentence = new DcsSentence(text = "rūpamārogyamaiśvaryaṃ teṣvanantaṃ bhaviṣyati // (17.2)", dcsId = 100003)
    assert(sentence.scrapeAnalysis())
    log.debug(sentence.toString)
    assert(sentence.dcsAnalysisDecomposition.get.length == 3)
    assert(sentence.dcsAnalysisDecomposition.get.flatten.length == 6)

  }

}
