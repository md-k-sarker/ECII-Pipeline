# ECII Extended
Originally concept induction algorithom, where other functionality is added to provide:
* Similarity between natural language (sentence, paragraph, tweet etc).
* Provide insights of machine learning decisions.
* Identify new complex entities for knowledge graph.


## Measure similarity between text


Complete pipeline to run [ECII](https://github.com/md-k-sarker/ecii)


## Source code:
```bash
├── README.md
├── java
│   └── ecii-run
├── python
└── kotlin
    └── annotate
```

## Steps:
<ol>
<li>Annotate a text file with respect to a knowledge graph : <code>source :kotlin/annotate/</code>
<ol>
    <li>Find entities from DBpedia in the provided file using
    [DBpedia Spotlight](https://www.dbpedia-spotlight.org/).
    <li>Match DBpedia entities with entities in the provided ontology and find matched entities classes.
    <li>Save the result in CSV file.
</ol>
<br>
<li>Run ECII `:java/ecii-run directory`
<ol>
    <li>Provide positive and negative individuals and their types and ontology to ECII system
    <li>Strip down the ontology
    <li>Run ECII on stripped ontology
</ol>
</ol>



Original ECII Algorithm source code is added as a submodel to this repository. How to access the submodule source code see here: 
https://stackoverflow.com/questions/7813030/how-can-i-have-linked-dependencies-in-a-git-repo