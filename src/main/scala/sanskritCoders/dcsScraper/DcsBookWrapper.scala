package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsBook, DcsChapter}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.slf4j.{Logger, LoggerFactory}
import sanskritCoders.dcsScraper.dcsScraper.browser
import sanskrit_coders.db.couchbaseLite.DcsCouchbaseLiteDB
import sanskrit_coders.dcs.DcsDb

class DcsBookWrapper(book: DcsBook) {
  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  implicit def dcsChapterWrap(s: DcsChapter): DcsChapterWrapper = new DcsChapterWrapper(s)
  var chapterNames : Seq[String] = null

  def scrapeChapterList() = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printchapters",
        "textid" -> book.dcsId.toString
      ))
    val chapterTags = doc >> elementList("option")
    val chapterIds = chapterTags.map(tag => tag.attr("value").toInt)
    book.chapterIds = Some(chapterIds)
    log info s"Added ${chapterIds.length} chapters."
    chapterNames = chapterTags.map(tag => tag.text)
  }

  def storeBook(dcsDb: DcsCouchbaseLiteDB) = {
    log.info(s"Starting on book ${book.title}")
    if (book.chapterIds.isEmpty ) {
      scrapeChapterList()
    }
    log.info(s"Fetched book ${book.title}")
    dcsDb.updateBooksDb(book)
  }

  def storeChapters(dcsDb: Either[DcsCouchbaseLiteDB, DcsDb], chapterTitleToStartFrom: Option[String] = None, updateChapterNotSentences: Boolean = false): Int = {
    var numSentenceFailures = 0
    if (book.chapterIds.isEmpty ) {
      scrapeChapterList()
    }
    log.info(s"Starting on book ${book.title} with chaptersToStartFrom ${chapterTitleToStartFrom}.")
    require(!(chapterTitleToStartFrom.isDefined && updateChapterNotSentences == true))
    require(chapterNames != null)
    var chapters = book.chapterIds.get.zip(chapterNames).map(x => new DcsChapter(dcsId = x._1, dcsName = Some(x._2)))
    chapters.foreach(_.scrapeChapter())
//    log.info(chapters.map(x => s"${x.dcsId} ${x.dcsName}").mkString("\n"))
    chapters = chapters.dropWhile(x => chapterTitleToStartFrom.isDefined && !x.dcsName.contains(chapterTitleToStartFrom.get))
    if (updateChapterNotSentences) {
      chapters.foreach(_.storeChapter(dcsDb))
    } else {
      chapters.foreach(chapter => {
        log.info(s"Processing chapter ${chapter.dcsName} of ${book.title}")
        numSentenceFailures += chapter.storeSentences(dcsDb)
        chapter
      })
    }
    return numSentenceFailures
  }
}
