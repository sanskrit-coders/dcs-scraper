package sanskritCoders.dcsScraper

import net.ruippeixotog.scalascraper.dsl.DSL._
import dbSchema.dcs.{DcsBook, DcsChapter}
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import sanskritCoders.dcsScraper.dcsScraper.{browser, log}
import sanskrit_coders.db.couchbaseLite.DcsCouchbaseLiteDB

class DcsBookWrapper(book: DcsBook) {
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
    if (book.chapterIds == None ) {
      scrapeChapterList()
    }
    log.info(s"Fetched book ${book.title}")
    dcsDb.updateBooksDb(book)
  }

  def storeChapters(dcsDb: DcsCouchbaseLiteDB, chaptersToStartFrom: String = null, updateChapterNotSentences: Boolean = false): Int = {
    var numSentenceFailures = 0
    if (book.chapterIds == None ) {
      scrapeChapterList()
    }
    log.info(s"Starting on book ${book.title}")
    require(!(chaptersToStartFrom != null && updateChapterNotSentences == true))
    var chapters = book.chapterIds.get.zip(chapterNames).map(x => new DcsChapter(dcsId = x._1, dcsName = Some(x._2)))
    chapters.foreach(_.scrapeChapter())
//    log.info(chapters.map(x => s"${x.dcsId} ${x.dcsName}").mkString("\n"))
    chapters = chapters.dropWhile(x => chaptersToStartFrom != null && !x.dcsName.contains(chaptersToStartFrom))
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
