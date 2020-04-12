# ECII Pipeline
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
<li>Annotate a text file with respect to an ontology: `:kotlin/annotate/`
<ol>
    <li>Find entities from DBpedia in the provided file using
    [DBpedia Spotlight](https://www.dbpedia-spotlight.org/).
    <li>Match DBpedia entities with entities in the provided ontology and find
    matched entities' classes.
    <li>Save the result in CSV file.
</ol>
<li>Run ECII `:java/ecii-run directory`
<ol>
    <li>Provide positive and negative individuals and their types and ontology to ECII system
    <li>Strip down the ontology
    <li>Run ECII on stripped ontology
</ol>
</ol>