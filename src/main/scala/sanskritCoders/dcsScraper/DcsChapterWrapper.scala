package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsChapter, DcsSentence}
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import sanskritCoders.dcsScraper.dcsScraper.{browser, log}
import sanskrit_coders.db.couchbaseLite.DcsCouchbaseLiteDB
import net.ruippeixotog.scalascraper.dsl.DSL._

class DcsChapterWrapper(chapter: DcsChapter) {
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

  def storeChapter(dcsDb: DcsCouchbaseLiteDB): Boolean = {
    if (chapter.sentenceIds == None) {
      scrapeChapter()
    }
    dcsDb.updateBooksDb(chapter)
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

  def storeSentences(dcsDb: DcsCouchbaseLiteDB): Int = {
    var numSentenceFailures = 0
    log.info(s"Processing chapter ${chapter.dcsName}")
    if (chapter.sentenceIds == None) {
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
      dcsDb.updateSentenceDb(s)
    })
    return numSentenceFailures
  }
}
