# ATR4S

An open-source library for [Automatic Term Recognition](https://en.wikipedia.org/wiki/Terminology_extraction)
written in Scala.

To cite ATR4S:
TBD

[//]: # (N.Astrakhantsev. ATR4S: Toolkit with State-of-the-art Automatic Terms Recognition Methods in Scala)

## Implemented algorithms

1) AvgTermFreq
2)  ResidualIDF
3)  TotalTF-IDF
4)  CValue
5)  Basic
6)  ComboBasic
7)  PostRankDC
8)  Relevance
9)  Weirdness
10)  DomainPertinence
11)  NovelTopicModel
12)  LinkProbability
13)  KeyConceptRelatedness
14)  Voting
15)  PU-ATR


[//]: # (See details in the paper.)

## Requirements

### Libraries

Scala 2.11

Spark 1.5+ (for Voting and PU-ATR)

[Emory nlp4j](https://emorynlp.github.io/nlp4j/)

([Stanford CoreNLP](http://stanfordnlp.github.io/CoreNLP/) is also supported by
[this helper](), which is moved to a separate project due to GPL licensing of Stanford CoreNLP).

### Data

In order to use some algorithms you need to download auxiliary files and place them into
`WORKING_DIRECTORY/data` directory (note that working directory can be specified in `gradle.properties`)
or specify path in the corresponding configuration/builder class
(e.g. `Word2VecAdapterConfig` of `KeyConceptRelatedness`).

Namely,
- for **LinkProbability** download [COHA_term_occurrences.txt](https://at.ispras.ru/owncloud/index.php/s/0eUMJywO3AhXDHb);
- for **Relevance** download [info_measure.txt](https://at.ispras.ru/owncloud/index.php/s/MzVm6GVOQ4eTJyR);
- for **KeyConceptRelatedness** download [w2vConcepts.model](https://at.ispras.ru/owncloud/index.php/s/SWP1YiISQPQCqTj).

## Building from Sources

Build library with gradle:

```shell
./gradlew jar
```

## Usage

### Command line example

```shell
./gradlew recognize -Pdataset=acl2 -PtopCount=10 -Pconfig=Relevance.conf -Poutput=relterms.txt
```

Here we recognize top 10 terms from text files stored in `acl2` directory by Relevance measure
(stored in `Relevance.conf` file) and writes recognized terms with weights in `relterms.txt`.

### Program API

See `ATRConfig` class, which is a Configuration/builder for a facade class `AutomaticTermsRecognizer`.

See `AutomaticTermsRecognizer` object for example.

## License

Apache License Version 2.0.
