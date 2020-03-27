# ECII Pipeline
Complete pipeline to run [ECII] (https://github.com/md-k-sarker/ecii)


## Source code:
```bash
├── README.md
├── java
│   └── ecii-run
├── python
└── scala
    └── annotate
```

## Steps:
<ol>
<li>Annotate text file: `:scala/annotate/ directory`
<ol>
    <li>Provide text files and reference-system (dbpedia-spotlight) to annotator
    <li>Find any reference of text on the reference-system
    <li>Save the result in excel file 
</ol>
<li>Run ECII `:java/ecii-run directory`
<ol>
    <li>Provide positive and negative individuals and their types and ontology to ECII system
    <li>Strip down the ontology
    <li>Run ECII on stripped ontology
</ol>
</ol>