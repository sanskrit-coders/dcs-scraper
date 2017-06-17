# Introduction
* Goals
  * Reconstruct database behind [DCS](http://kjc-sv013.kjc.uni-heidelberg.de/dcs)
  * Present it in a format better suited for consumption by the broader Sanskrit programming community.
* License and origin acknowledgement: see [LICENSE](LICENSE.md) file.

# Resultant Database
## API-s
* Get analysis for a sentence: [api](http://vvasuki.hopto.org:5984/dcs_sentences/sentence_354341), with [this](https://pastebin.com/d3td7qge) output.
*

# Databse Deployment
## Database repilicas
* You want to host a repilica and make things faster for folks in your geographical area? Just open an issue in this project and let us know.
* Ahmedabad, IN <http://vedavaapi.org:5984/dict_entries/_all_docs>
* Bay area, USA (dev machine, unstable) <http://vvasuki.hopto.org:5984/dict_entries/_all_docs>

## UI deployments
* You can use it right off github!
* You want to host copies (or even develop a superior UI?) and make things faster for folks in your geographical area? Just open an issue in this project and let us know. We'd love to list it here.


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
