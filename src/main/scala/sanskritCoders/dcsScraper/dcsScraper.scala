package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsBook, DcsChapter, DcsSentence, DcsWord}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.slf4j.{Logger, LoggerFactory}
import sanskritCoders.dcsScraper.dcsScraper.{browser, log}
import sanskrit_coders.db.couchbaseLite.DcsCouchbaseLiteDB

import scala.collection.mutable
import scala.util.matching.Regex







object dcsScraper {
  implicit def dcsBookWrap(s: DcsBook): DcsBookWrapper = new DcsBookWrapper(s)

  implicit def dcsSentenceWrap(s: DcsSentence): DcsSentenceWrapper = new DcsSentenceWrapper(s)

  val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val browser: Browser = JsoupBrowser()
  val dcsDb: DcsCouchbaseLiteDB = new DcsCouchbaseLiteDB


  def scrapeBookList: Seq[DcsBook] = {
    val doc = browser.get("http://kjc-sv013.kjc.uni-heidelberg.de/dcs/index.php?contents=texte")
    val bookTags = (doc >> elementList("select")).head.children
    val books = bookTags.map(bookTag => new DcsBook(title = bookTag.text, dcsId = bookTag.attr("value").toInt)).toList
    books
  }

  def scrapeAll(): Unit = {
    var books = scrapeBookList
    var bookFailureMap = mutable.HashMap[String, Int]()

    val incompleteBookTitle = "Ānandakanda"
    val incompleteBook = books.filter(_.title.startsWith(incompleteBookTitle)).head
    bookFailureMap += Tuple2(incompleteBook.title, incompleteBook.storeChapters(dcsDb = dcsDb, chaptersToStartFrom = "ĀK, 1, 17"))

    books.dropWhile(!_.title.startsWith(incompleteBookTitle)).drop(1).foreach(book => {
      bookFailureMap += Tuple2(book.title, book.storeChapters(dcsDb = dcsDb))
    })
    log.error(s"Failures: ${bookFailureMap.mkString("\n")}")
  }

  def fillSentenceAnalyses(): Unit = {
    var numNoAnalysis = 0
    dcsDb.getSentencesWithoutAnalysis().foreach(sentence => {
      //      log debug s"before: $sentence"
      if (!sentence.scrapeAnalysis()) {
        numNoAnalysis += 1
      }
      dcsDb.updateSentenceDb(sentence)
      //      log debug s"after: $sentence"
    })
    log info s"No analysis for $numNoAnalysis sentences"
  }

  def main(args: Array[String]): Unit = {
    dcsDb.openDatabasesLaptop()
    dcsDb.replicateAll()
    scrapeAll()
    dcsDb.closeDatabases
  }
}
