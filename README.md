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
1. Annotate text file: `:scala/annotate/ directory`
    i. Provide text files and reference-system (dbpedia-spotlight) to annotator
    ii. Find any reference of text on the reference-system
    iii. Save the result in excel file 
2. Run ECII `:java/ecii-run directory`
    i. Provide positive and negative individuals and their types and ontology to ECII system
    ii. Strip down the ontology
    iii. Run ECII on stripped ontology