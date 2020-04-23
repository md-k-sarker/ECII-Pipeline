# ECII Extended
Originally concept induction algorithom, where other functionality is added to provide:
* Similarity between natural language (sentence, paragraph, tweet etc).
* Provide insights of machine learning decisions.
* Identify new complex entities for knowledge graph.



## Program options:
1. Measure similarity between ontology entities
2. Perform concept induction
3. Strip down ontology or keeping entities of interest while discarding others
4. Create ontology from CSV file
5. Combine multiple ontology


### Download Jar
Link: https://github.com/md-k-sarker/ECII-Pipeline/releases/download/v0.0.1/ecii.jar


### How to run the program
<pre>
To measure similarity between ontology entities..... and 
To perform concept induction.....
Program runs in two mode. 
	Batch mode and 
	single mode. 
In single mode it will take a config file as input parameter and run the program as mentioned by the parameters in config file.
In Batch mode it take directory as parameter and will run all the config files within that directory.
Command:
	For single mode: [options] [config_file_path]
	For Batch mode:  [options] [-b directory_path]
	For Help: [-h]
	Options:
		-m : Measure similarity between ontology entity
		-e : Concept Induction by ecii algorithm
		-c : Combine ontology
		-s : Strip down ontology
		-o : Ontology Create from CSV

Parameters for different options:
	-c [inputOntologiesDirectory, outputOntologyIRI]
	-s [-obj/type] [inputOntoPath, entityCsvFilePath, indivColumnName, objPropColumnName/typeColumnName, outputOntoIRI] 
	-o [entityCsvFilePath, objPropColumnName, indivColumnName, typeColumnName, outputOntoIRI]
	Example of Concept Induction command:
	For single mode:
			java -jar ecii.jar config_file
	For Batch mode:
			java -jar ecii.jar -b directory

</pre>


## How to write Config file:
    Config file is a text file with user defined parameters and must end with .config
    Parameters are written as key, value pair. 

        # default namespace
        namespace : required 

        # K1/negExprTypeLimit, limit of number of concepts in a negative expression of a hornClause
        conceptLimitInNegExpr : integer, optional, default 3

        # K2/hornClauseLimit
        hornClauseLimit : integer, optional, default 3

        # K3/permutate/combination untill this number of objectproperties
        objPropsCombinationLimit: integer, optional, default 3

        # K5 select upto k5 hornClauses to make combination
        hornClausesListMaxSize: integer, optional, default 50

        # K6 select upto k6 candidate classes to make combination
        candidateClassesListMaxSize: integer, optional, default 50

        # k7/remove common atomic types. Those types appeared in both positive and negative individuals 
        removeCommonTypes: boolean, optional, default true.

        # k8, validate solutions upto this number of top solutions
        validateByReasonerSize: integer, optional, default 0

        # k9/ maximum posclasses (top scoring) to do the combination. size would be nCr or posClassListMaxSize--C--conceptLimitInPosExpr
        posClassListMaxSize: integer, optional, default 10

        # k10/ maximum negclasses (top scoring) to do the combination. size would be nCr or negClassListMaxSize--C--conceptLimitInNegExpr
        negClassListMaxSize: integer, optional, default 10

        # score type
        scoreType=coverage

        # declare some prefixes to use as abbreviations
        prefixes : map/dictionary, required 

        # knowledge source definition/owl_file_name
        ks.fileName : string, required

        # reasoner name. possible is hermit and pellet. default is pellet
        reasoner.reasonerImplementation : string, optional, default pellet

        # object properties to consider
        objectProperties : map/dictinary, optional

        # positive examples
        lp.positiveExamples : array, required
        # negative examples
        lp.negativeExamples : array, required 

        # measure similarity
        runPairwiseSimilarity : boolean, default false




## Source code:
```bash
├── README.md
├── java
│   └── ecii-run
├── python
└── kotlin
    └── annotate
```


Original ECII Algorithm source code is added as a submodel to this repository. How to access the submodule source code see here: 
https://stackoverflow.com/questions/7813030/how-can-i-have-linked-dependencies-in-a-git-repo