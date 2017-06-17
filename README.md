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
The DCS web interface was inadequate to address the following needs.
  * Unblock NLP work: People enthusiastic about applying machine learning tools to Sanskrit language data are hampered by lack of access to such data in a convenient format.
  * The valuable analysis results could be exploited and presented to end users in many more creative ways.

## License and origin acknowledgement
This builds on the foundational work by the DCS team headed by Oliver. See [LICENSE](LICENSE.md) file.

# Resultant Database
## API-s
* Get analysis for a sentence: [api](http://vvasuki.hopto.org:5984/dcs_sentences/sentence_354341), with [this](https://pastebin.com/d3td7qge) output.
*

# Databse Deployment
## Database repilicas
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
    - [Regarding **maven targets** in intellij](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#regarding-**maven-targets**-in-intellij)
    - [Releasing to maven.](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#releasing-to-maven.)
    - [Building a jar.](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#building-a-jar.)
  - [Technical choices](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#technical-choices)
    - [Scala](https://github.com/sanskrit-coders/indic-transliteration/blob/master/README.md#scala)
