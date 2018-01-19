[Table of contents generated using this app](https://tableofcontents.herokuapp.com)

- [Introduction](#introduction)
  - [Goals](#goals)
  - [Motivation](#motivation)
  - [License and origin acknowledgement](#license-and-origin-acknowledgement)
- [Resultant Database](#resultant-database)
  - [API-s](#api-s)
- [Databse Deployment](#databse-deployment)
  - [Database repilicas](#database-repilicas)
  - [UI deployments](#ui-deployments)
- [Database and packaging software developer instructions](#database-and-packaging-software-developer-instructions)
  - [Links to general comments](#links-to-general-comments)

# Introduction
## Goals
  * Reconstruct database behind [DCS](http://kjc-sv013.kjc.uni-heidelberg.de/dcs)
  * Present it in a format better suited for consumption by the broader Sanskrit programming community.

## Motivation
This database copy aims to augment DCS web interface and address the following shortcomings:
  * __Unblock NLP work__: People enthusiastic about applying machine learning tools to Sanskrit language data are hampered by lack of access to such data in a convenient format. Examples: [201706](https://groups.google.com/d/msg/sanskrit-programmers/2uwvGmrfI68/Pt8hMB3XAAAJ), [201607](https://groups.google.com/forum/#!searchin/sanskrit-programmers/DCS|sort:relevance/sanskrit-programmers/Zdj80IzI--U/G-zJEXgYCAAJ), [201605](https://groups.google.com/forum/#!searchin/sanskrit-programmers/DCS|sort:relevance/sanskrit-programmers/GMDUKF7zCaM/bOAAnNdkCQAJ)
  * __Unblock UI work__: The valuable analysis could be presented to end users in many more creative ways.

## License and origin acknowledgement
This builds on the foundational work by the DCS team headed by Oliver. See [LICENSE](LICENSE.md) file.

# Resultant Database
## API-s
* Get analysis for a sentence: [api](http://vedavaapi.org:5984/dcs_sentences/sentence_354341), with [this](https://pastebin.com/d3td7qge) output.
* Get chapter with a certain sentenceId: [api](http://vedavaapi.org:5984/dcs_books/_design/sentence_index/_view/sentence_index?limit=20&reduce=false&include_docs=true&keys=%5B1%5D).
* Look up the sentences where a word appears: [api](http://vedavaapi.org:5984/dcs_sentences/_design/index_words/_view/index_words?limit=100&reduce=false&keys=%5B%22hari%22%5D).
* Get book with a certain chapterId: [api](http://vedavaapi.org:5984/dcs_books/_design/chapter_index/_view/chapter_index?limit=100&reduce=false&include_docs=true&keys=%5B59%5D).
* Lookup book by name: [api](http://vedavaapi.org:5984/dcs_books/_design/book_index/_view/book_index?limit=100&reduce=false&include_docs=true&keys=%5B%22Abhidh%C4%81nacint%C4%81ma%E1%B9%87i%22%5D).


## Dump files
* As a couchbase lite database [here](https://archive.org/details/dcs-data.tar).

# Databse Deployment
* Periodic update announcements [here](groups.google.com/forum/#!forum/sanskrit-programmers).

## Database replicas
* You want to host a repilica and make things faster for folks in your geographical area? Just open an issue in this project and let us know.
* Ahmedabad, IN <http://vedavaapi.org:5984/dcs_sentences/_all_docs>
* Bay area, USA (dev machine, unstable) <http://vvasuki.hopto.org:5984/dcs_sentences/_all_docs>

## UI deployments
* Nothing yet.


# Database and packaging software developer instructions
## Links to general comments
See [indic-transliteration/README](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md) for the following info:


  - [Setup](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#setup)
  - [Deployment](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#deployment)
