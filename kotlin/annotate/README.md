## Annotation

The file `EntityExtraction.kt` contains a program (written in Kotlin) for
finding entities in text files, matching them with entities in a local OWL
ontology, and finding types declared for matching entities in the same.

The file `pom.xml` contains the requisite Maven dependencies.

Entities are highlighted using the
[DBpedia Spotlight](https://www.dbpedia-spotlight.org/) web service. As such,
you may not get many matches if your ontology's entity names aren't similar to
DBpedia's names. However, there is some provision for programmatic differences
between DBpedia entities and local ontology entities, as discussed below.

The remote web service, used by default, is not always reliable, so you should
probably set up a local web service by following the instructions from the
[Spotlight
repository](https://github.com/dbpedia-spotlight/dbpedia-spotlight-model):
download
https://sourceforge.net/projects/dbpedia-spotlight/files/2016-10/en/model/en.tar.gz/
and
https://sourceforge.net/projects/dbpedia-spotlight/files/spotlight/dbpedia-spotlight-1.0.0.jar
and execute the following from a shell.
```
tar xzf en.tar.gz
java -jar dbpedia-spotlight-1.0.jar en http://localhost:2222/rest
```

Concretely, there exists a function in `EntityExtraction.kt` with signature
```
fun annotateAndMatch(
    textPaths: List<String>,
    ontology: OWLOntology,
    replacements: (String) -> String = replDefault,
    encoding: Charset = StandardCharsets.UTF_8,
    confidence: Double = .5,
    outPath: String = "out",
    service: String = DBP_SERVICE
)
```
so that if you have

- a list `L` of strings representing paths to text files;
- a local OWL ontology `O`;
- a function `T`—mapping strings to strings—specifying how to transform a
  DBpedia IRI into an IRI in `O`;
- an encoding `E` from Java's `StandardCharsets` for the files at the paths in
  `L`;
- a double-precision floating point value `C` representing the confidence at
  which you want Spotlight to make annotations;
- a string `U` specifying the path to the directory to which to write the
  results; and
- a string `S` specifying the location of the web service to use,

then the invocation `annotateAndMatch(L, O, T, E, C, U, S)` will write a CSV
file with columns for underlying text, DBpedia IRI, local ontology IRI, and
local ontology class IRIs (separated by semicolons) to the specified output
directory. A single line of such a CSV might look like the following.
```
Moscow,http://dbpedia.org/resource/Moscow,http://example.com/Moscow,http://example.com/Cities_in_Russia;http://example.com/National_capitals;
```

You only have to provide the first two arguments; by default, IRIs are not
transformed (which is only useful if you're working with a DBpedia ontology),
`StandardCharsets.UTF_8` is assumed as the encoding, annotations are made with a
confidence of .5, output is written to a (possibly new) directory "out" in the
working directory, and the
[DBpedia Spotlight remote web
service](http://model.dbpedia-spotlight.org/en/annotate)
is used.

If you're still in the stone age, then you may find the Kotlin
documentation's instructions for
[Calling Kotlin from
Java](https://kotlinlang.org/docs/reference/java-to-kotlin-interop.html)
useful.