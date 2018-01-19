package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsBook, DcsSentence}
import net.ruippeixotog.scalascraper.browser.{Browser, JsoupBrowser}
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.slf4j.{Logger, LoggerFactory}
import sanskrit_coders.db.couchbaseLite.DcsCouchbaseLiteDB
import sanskrit_coders.dcs.DcsDb

import scala.collection.mutable


object dcsScraper {
  implicit def dcsBookWrap(s: DcsBook): DcsBookWrapper = new DcsBookWrapper(s)

  implicit def dcsSentenceWrap(s: DcsSentence): DcsSentenceWrapper = new DcsSentenceWrapper(s)

  private val log: Logger = LoggerFactory.getLogger(getClass.getName)
  val browser: Browser = JsoupBrowser()
//  val dcsDb: Either[DcsCouchbaseLiteDB, DcsDb] = Left(new DcsCouchbaseLiteDB())
  val dcsDb: Either[DcsCouchbaseLiteDB, DcsDb] = Right(new DcsDb(serverLocation = "localhost:5984", userName = "vvasuki"))


  def scrapeBookList: Seq[DcsBook] = {
    val doc = browser.get("http://kjc-sv013.kjc.uni-heidelberg.de/dcs/index.php?contents=texte")
    val bookTags = (doc >> elementList("select")).head.children
    val books = bookTags.map(bookTag => new DcsBook(title = bookTag.text, dcsId = bookTag.attr("value").toInt)).toList
    books
  }

  def scrapeAll(incompleteBookTitle: Option[String] = None, chapterToStartFromInIncompleteBook: Option[String] = None,
                bookTitleToEndWith: Option[String] = None): Unit = {
    var books = scrapeBookList
    var bookFailureMap = mutable.HashMap[String, Int]()
    if (incompleteBookTitle.isDefined) {
      // Finish up the incomplete book.
      val incompleteBook = books.filter(_.title.startsWith(incompleteBookTitle.get)).head
      bookFailureMap += Tuple2(incompleteBook.title, incompleteBook.storeChapters(dcsDb = dcsDb, chaptersToStartFrom = chapterToStartFromInIncompleteBook))
      books = books.dropWhile(!_.title.startsWith(incompleteBookTitle.get)).drop(1)
      // Now continue with the next book.
    }
    if (bookTitleToEndWith.isDefined) {
      val endBookSeq = books.filter(_.title.equals(bookTitleToEndWith.get))
      books = books.takeWhile(!_.title.equals(bookTitleToEndWith.get)) ++ endBookSeq
    }
    log.info(s"${incompleteBookTitle}-${chapterToStartFromInIncompleteBook} to $bookTitleToEndWith")
    log.info(s"Books count, minus the first if incompleteBookTitle was supplied: ${books.length}")
    books.foreach(book => {
      bookFailureMap += Tuple2(book.title, book.storeChapters(dcsDb = dcsDb))
    })
    log.error(s"Failures: ${bookFailureMap.mkString("\n")}")
  }

  def fillSentenceAnalyses(): Unit = {
    var numNoAnalysis = 0

    dcsDb.foreach(db => db.getSentences.foreach(sentence => {
      //      log debug s"before: $sentence"
      if (!sentence.scrapeAnalysis()) {
        numNoAnalysis += 1
      }
      db.updateSentenceDb(sentence)
      //      log debug s"after: $sentence"
    }))
    log info s"No analysis for $numNoAnalysis sentences"
  }

  def main(args: Array[String]): Unit = {
    if (dcsDb.isLeft) {
      dcsDb.left.get.openDatabasesLaptop()
      dcsDb.left.get.replicateAll()
    } else {
      dcsDb.right.get.initialize()
    }
    // Start points:
//    scrapeAll(incompleteBookTitle = Some("Agnipurāṇa"), chapterToStartFromInIncompleteBook = Some("12"), bookTitleToEndWith = Some("Bhāgavatapurāṇa"))
    // scrapeAll(incompleteBookTitle = Some("Bhāratamañjarī"), chapterToStartFromInIncompleteBook = Some("7"), bookTitleToEndWith = Some("Ekākṣarakoṣa"))
    // scrapeAll(incompleteBookTitle = Some("Garuḍapurāṇa"), chapterToStartFromInIncompleteBook = Some("15"), bookTitleToEndWith = Some("Liṅgapurāṇa"))
//     scrapeAll(incompleteBookTitle = Some("Madanapālanighaṇṭu"), chapterToStartFromInIncompleteBook = None, bookTitleToEndWith = Some("Mṛgendraṭīkā"))
//     scrapeAll(incompleteBookTitle = Some("Narmamālā"), chapterToStartFromInIncompleteBook = None, bookTitleToEndWith = Some("Pāśupatasūtra"))
//    fillSentenceAnalyses
    if (dcsDb.isLeft) {
      dcsDb.left.get.closeDatabases
    }
  }
}
