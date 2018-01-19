package sanskritCoders.dcsScraper

import dbSchema.dcs.{DcsBook, DcsChapter, DcsSentence, DcsWord}
import net.ruippeixotog.scalascraper.browser.JsoupBrowser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors.elementList
import org.slf4j.LoggerFactory
import sanskritCoders.dcsScraper.dcsScraper.{browser, log}
import sanskrit_coders.db.couchbaseLite.DcsCouchbaseLiteDB

import scala.collection.mutable
import scala.util.matching.Regex

class DcsBookWrapper(book: DcsBook) {
  implicit def dcsChapterWrap(s: DcsChapter) = new DcsChapterWrapper(s)
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

    val incompleteBookTitle = "Ānandakanda"
    val incompleteBook = books.filter(_.title.startsWith(incompleteBookTitle)).head
    bookFailureMap += Tuple2(incompleteBook.title, (incompleteBook.storeChapters(dcsDb = dcsDb, chaptersToStartFrom = "ĀK, 1, 17")))

    books.dropWhile(!_.title.startsWith(incompleteBookTitle)).drop(1).foreach(book => {
      bookFailureMap += Tuple2(book.title, (book.storeChapters(dcsDb = dcsDb)))
    })
    log.error(s"Failures: ${bookFailureMap.mkString("\n")}")
    dcsDb.closeDatabases
  }

  def fillSentenceAnalyses() = {
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
