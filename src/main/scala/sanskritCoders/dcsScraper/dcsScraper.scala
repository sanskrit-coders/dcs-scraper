package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsBook, DcsChapter, DcsSentence, DcsWord}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.slf4j.LoggerFactory
import sanskritCoders.dcsScraper.dcsScraper.{browser, log}

import scala.collection.mutable
import scala.util.matching.Regex

class DcsBookWrapper(book: DcsBook) {
  implicit def dcsChapterWrap(s: DcsChapter) = new DcsChapterWrapper(s)

  def scrapeChapterList() = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printchapters",
        "textid" -> book.dcsId.toString
      ))
    val chapterTags = doc >> elementList("option")
    val chapterIds = chapterTags.map(tag => tag.attr("value").toInt)
    book.chapterIds = Some(chapterIds)
  }

  def storeBook(dcsDb: DcsCouchbaseLiteDB) = {
    log.info(s"Starting on book ${book.title}")
    scrapeChapterList()
    log.info(s"Fetched book ${book.title}")
    dcsDb.updateBooksDb(book)
  }

  def storeChapters(dcsDb: DcsCouchbaseLiteDB, chaptersToStartFrom: String = null, updateChapterNotSentences: Boolean = false): Int = {
    var numSentenceFailures = 0
    require(!(chaptersToStartFrom != null && updateChapterNotSentences == true))
    var chapters = book.chapterIds.get.map(x => new DcsChapter(dcsId = x))
    chapters = chapters.dropWhile(x => chaptersToStartFrom != null && !x.dcsName.contains(chaptersToStartFrom))
    if (updateChapterNotSentences) {
      chapters.foreach(_.storeChapter(dcsDb))
    } else {
      chapters.foreach(chapter => {
        numSentenceFailures += chapter.storeSentences(dcsDb)
        log.info(s"Processing book ${book.title} - chapter ${chapter.dcsName}")
        chapter
      })
    }
    return numSentenceFailures
  }
}

class DcsChapterWrapper(chapter: DcsChapter) {
  implicit def dcsSentenceWrap(s: DcsSentence) = new DcsSentenceWrapper(s)

  def scrapeChapter() = {
    val doc = browser.post(s"http://kjc-sv013.kjc.uni-heidelberg.de/dcs/ajax-php/ajax-text-handler-wrapper.php",
      form = Map(
        "mode" -> "printsentences",
        "chapterid" -> chapter.dcsId.toString
      ))
    val sentenceTags = doc >> elementList(".sentence_div")
    val sentenceIds = sentenceTags.map(tag => tag.attr("sentence_id").toInt)
    chapter.sentenceIds = Some(sentenceIds)
  }

  def storeChapter(dcsDb: DcsCouchbaseLiteDB) = {
    scrapeChapter()
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

class DcsSentenceWrapper(sentence: DcsSentence) {
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
    val wordGroupHtmls = doc.toHtml.split("&nbsp;&nbsp;").map(_.split("(?=\\])-"))
    val analysis = wordGroupHtmls.map(wordGroupHtml => {
      wordGroupHtml.map(x => {
//        log debug (x)
        val y = browser.parseString(x) >> element("a")
        val grammarHint = new Regex("\\[(.+)\\]").findFirstIn(x)
        val word = new DcsWord(root = y.text,
          dcsId = y.attr("href").replaceAll(".*&IDWord=", "").toInt,
          dcsGrammarHint = grammarHint)
        word
      }).toSeq
    }).toSeq
    sentence.dcsAnalysisDecomposition = Some(analysis)
    //    sentence.dcsAnalysis = Some(doc.toString)
    //    log.debug(sentence.toString)
    return true
  }

}

object dcsScraper {
  implicit def dcsBookWrap(s: DcsBook) = new DcsBookWrapper(s)

  implicit def dcsSentenceWrap(s: DcsSentence) = new DcsSentenceWrapper(s)

  val log = LoggerFactory.getLogger(getClass.getName)
  val browser = JsoupBrowser()
  val dcsDb = new DcsCouchbaseLiteDB


  def scrapeBookList: Seq[DcsBook] = {
    val doc = browser.get("http://kjc-sv013.kjc.uni-heidelberg.de/dcs/index.php?contents=texte")
    val bookTags = (doc >> elementList("select")) (0).children
    val books = bookTags.map(bookTag => new DcsBook(title = bookTag.text, dcsId = bookTag.attr("value").toInt)).toList
    return books
  }

  def scrapeAll() = {
    var books = scrapeBookList
    var bookFailureMap = mutable.HashMap[String, Int]()

    val incompleteBookTitle = "Pañcaviṃśabrāhmaṇa"
    val incompleteBook = books.filter(_.title.startsWith(incompleteBookTitle)).head
    bookFailureMap += Tuple2(incompleteBook.title, (incompleteBook.storeChapters(dcsDb = dcsDb)))

    books.dropWhile(!_.title.startsWith(incompleteBookTitle)).drop(1).foreach(book => {
      bookFailureMap += Tuple2(book.title, (book.storeChapters(dcsDb = dcsDb)))
    })
    log.error(s"Failures: ${bookFailureMap.mkString("\n")}")
    dcsDb.closeDatabases
  }

  def fillSentenceAnalyses() = {
    dcsDb.openDatabasesLaptop()
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
    fillSentenceAnalyses()
    scrapeAll()
    dcsDb.closeDatabases
  }
}
