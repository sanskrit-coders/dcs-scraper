package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsChapter, DcsSentence}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.slf4j.{Logger, LoggerFactory}
import sanskritCoders.dcsScraper.dcsScraper.browser
import sanskrit_coders.db.couchbaseLite.DcsCouchbaseLiteDB
import sanskrit_coders.dcs.DcsDb

class DcsChapterWrapper(chapter: DcsChapter) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  implicit def dcsSentenceWrap(s: DcsSentence) = new DcsSentenceWrapper(s)

  def scrapeChapter(): Unit = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printsentences",
        "chapterid" -> chapter.dcsId.toString
      ))
    val sentenceTags = doc >> elementList(".sentence_div")
    val sentenceIds = sentenceTags.map(tag => tag.attr("sentence_id").toInt)
    chapter.sentenceIds = Some(sentenceIds)
  }

  def storeChapter(dcsDb: Either[DcsCouchbaseLiteDB, DcsDb]): Boolean = {
    if (chapter.sentenceIds.isEmpty) {
      scrapeChapter()
    }
    dcsDb.foreach(_.updateBooksDb(chapter))
    return true
  }

  def scrapeSentences(): Seq[DcsSentence] = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printsentences",
        "chapterid" -> chapter.dcsId.toString
      ))
    val sentenceTags = doc >> elementList(".sentence_div")
    val sentences = sentenceTags.map(tag => new DcsSentence(text = tag.text, dcsId = tag.attr("sentence_id").toInt))
    return sentences
  }

  def storeSentences(dcsDb: Either[DcsCouchbaseLiteDB, DcsDb]): Int = {
    var numSentenceFailures = 0
    log.info(s"Processing chapter ${chapter.dcsName}")
    if (chapter.sentenceIds.isEmpty) {
      scrapeChapter()
    }
    val sentences = scrapeSentences()
    sentences.foreach(s => {
      try {
        if (!s.scrapeAnalysis()) {
          numSentenceFailures += 1
        }
      } catch {
        case e: NoSuchElementException => {
          log.error(s"Alas! can't analyze $s. Error: ${e.toString}")
          numSentenceFailures = numSentenceFailures + 1
        }
      }
      dcsDb.foreach(_.updateSentenceDb(s))
    })
    return numSentenceFailures
  }
}
