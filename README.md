# ATR4S

An open-source library for [Automatic Term Recognition](https://en.wikipedia.org/wiki/Terminology_extraction)
written in Scala.

To cite ATR4S:

N.Astrakhantsev.
ATR4S: Toolkit with State-of-the-art Automatic Terms Recognition Methods in Scala.
arXiv preprint [arXiv:1611.07804](http://arxiv.org/abs/1611.07804), 2016.

## Implemented algorithms

1. AvgTermFreq
2.  ResidualIDF
3.  TotalTF-IDF
4.  CValue
5.  Basic
6.  ComboBasic
7.  PostRankDC
8.  Relevance
9.  Weirdness
10.  DomainPertinence
11.  NovelTopicModel
12.  LinkProbability
13.  KeyConceptRelatedness
14.  Voting
15.  PU-ATR


[//]: # (See details in the paper.)

## Requirements

### Libraries

Scala 2.11

Spark 1.5+ (for Voting and PU-ATR)

[Emory nlp4j](https://emorynlp.github.io/nlp4j/)

([Apache OpenNLP](http://opennlp.apache.org/) is also supported, but
preliminary experiments showed that its quality is not better than Emory nlp4j, while it is not thread-safe;
if you are going to use OpenNLP, download models from Apache OpenNLP and place them into `src/main/resources`)

([Stanford CoreNLP](http://stanfordnlp.github.io/CoreNLP/) is also supported by
[this helper](https://at.ispras.ru/owncloud/index.php/s/Or7je8dxk5xIotL),
which is moved to a separate module licensed by GPL, due to GPL licensing of Stanford CoreNLP).

### Data

In order to use some algorithms you need to download auxiliary files and place them into
`WORKING_DIRECTORY/data` directory (note that working directory can be specified in `gradle.properties` - by default, this is `experiments`)
or specify path in the corresponding configuration/builder class
(e.g. `Word2VecAdapterConfig` of `KeyConceptRelatedness`).

Namely,
- for **LinkProbability** download [info_measure.txt](https://at.ispras.ru/owncloud/index.php/s/MzVm6GVOQ4eTJyR); 
- for **Relevance** download [COHA_term_occurrences.txt](https://at.ispras.ru/owncloud/index.php/s/0eUMJywO3AhXDHb);
- for **KeyConceptRelatedness** download [w2vConcepts.model](https://at.ispras.ru/owncloud/index.php/s/SWP1YiISQPQCqTj).

Datasets used in the experiments and examples can be downloaded [here](https://at.ispras.ru/owncloud/index.php/s/kXqCBSryRswThTy).

## Linking

The library is published into Maven central and JCenter.
Add the following lines depending on your build system.

### Gradle

```gradle
compile 'ru.ispras:atr4s:1.1'
```

### Maven

```xml
<dependency>
    <groupId>ru.ispras</groupId>
    <artifactId>atr4s</artifactId>
    <version>1.1</version>
</dependency>
```

### SBT

```
libraryDependencies += "ru.ispras" % "atr4s" % "1.1"
```

## Building from Sources

Build library with gradle:

```shell
./gradlew jar
```

## Usage

### Command line example

```shell
./gradlew recognize -Pdataset=acl2 -PtopCount=10 -Pconfig=CValue.conf -Poutput=cvalueterms.txt
```

Here we recognize top 10 terms from text files stored in `acl2` directory 
(should be subdirectory of `WORKING_DIRECTORY`) by CValue measure
(stored in `CValue.conf` file) and writes recognized terms with weights in `cvalueterms.txt`.

Note that input text files should be encoded in UTF-8. (There are many [tools](http://stackoverflow.com/questions/64860/best-way-to-convert-text-files-between-character-sets) for converting text files).

### Program API

See `ATRConfig` class, which is a Configuration/builder for a facade class `AutomaticTermsRecognizer`.

See `AutomaticTermsRecognizer` object for example.

### Program API (Java)

Usage in Java does not differ significantly, so see the same classes for examples. 
However, since Java does not support parameters with default values, 
we provide helper static functions named `make()` 
for most classes containing parameters with default values or parameters with Scala collections, 
see example below.

Also note that there is a special method returning weighted terms as Java Iterable, 
so that you won't need to convert Scala collections to Java ones.

```java
class ATRExample {
    public static void main(String[] args) {
        String datasetDir = args[0];
        int topCount = args[1];
        ATRConfig atrConfig = new ATRConfig(EmoryNLPPreprocessorConfig.make(),
                TCCConfig.make(),
                new OneFeatureTCWeighterConfig(Weirdness.make()));
        Iterable<WeightedTerm> terms = atrConfig.build().recognizeAsJavaIterable(datasetDir, topCount);
        for (WeightedTerm termAndWeight: terms) {
            System.out.println(termAndWeight);
        }
    }
}
```

## License

Apache License Version 2.0.
