/**
 * Entity extraction (there's probably a better name for this).
 * Functions to extract entities from a text file using DBpedia Spotlight
 * (https://www.dbpedia-spotlight.org/) and find matching entities and their
 * classes in an ontology.
 *
 * Joshua Schwartz (jschwartz427@ksu.edu)
 * 12 April 2020
 *
 * No rights reserved.
 */

import java.io.File
import java.io.BufferedReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.net.HttpURLConnection
import java.net.URL
import java.io.DataOutputStream
import java.io.InputStreamReader
import org.jsoup.Jsoup
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.search.EntitySearcher
import uk.ac.manchester.cs.owl.owlapi.OWLNamedIndividualImpl
import kotlin.streams.asSequence

// constants
/** 
 * The length of the prefix for DBpedia entities. 
 *
 * The prefix itself is "http://dbpedia.org/resource/", but that's unimportant;
 * we only need its length so we can remove it when translating entity names
 * into those of the local ontology.
 */
const val DBP_PREFIX_LENGTH = 28
/** The maximum string length we can feed to DBpedia at once. */
const val MAX_LENGTH_DBP = 7237   // approximate
/** Command line usage help message. */
const val USAGE_MESSAGE =
"""(I think this doesn't exactly adhere to usage message conventions: sorry.)
Pretend this program runs from a binary called "ee".
Usage: ee (<text>... | -a) -o ont [-r rep] [-e enc] [-c conf] [-u out]
where
text   is the path to a text file to annotate, with an arbitrary number of input
         files possible, and specifying "-a" instead of a sequence of file paths
         will run the program on all files in the working directory
         (nonrecursively);
ont    is the path to the ontology against which to match the extracted
         entities;
rep    is the path to a text file specifying how to transform IRIs from DBpedia
         to seek matches in the local ontology, the first line of which contains
         the prefix with which to replace DBpedia's prefix in modifying IRIs,
         the second line of which indicates whether (by containing the single
         character 'y' or not) to strip replaced characters from the beginning 
         and end of each IRI, and each subsequent line of which contains a
         character that might appear in DBpedia's entity names, followed by a 
         space, followed by a character with which to replace the foregoing 
         character when seeking matches in the local ontology (by default, no
         replacements are applied);
enc    is the encoding (from Java's StandardCharsets—if you want a different
         one, you're out of luck) for all given text files (default UTF_8);
conf   is the confidence level at which to make annotations (default .5);
out    is the path of the directory to which to write outputs (default ./out,
         creating the directory if necessary, with file names based on the
         given input paths with slashes replaced by underscores and ".csv"
         appended).
Basically, first provide a bunch of files to annotate (or specify that you want
the whole working directory annotated), ending the list with "-o" followed by
the ontology path, and then provide optional arguments."""

/** Default replacement function: identity. */
val replDefault = { s: String -> s}

/*
Exception handling is kept to a minimum, so if something goes wrong while you're
trying to use this program, you're toast, buster.

I assume that the input text files will adhere to standard English punctuation
conventions, in particular regularly using periods (or question marks, or
exclamation marks) to delimit sentences so that sentence divisions are all I
need to consider. As such, once the file has been niceified, we'll not worry
about whether a line might be too long for Spotlight.

I assume line breaks BETWEEN SENTENCES are not significant to Spotlight.
*/

/**
 * A quadruple with information about an entity, obtained from matching a pair
 * consisting of underlying [source] text and name of a DBpedia entity 
 * ([namedbp]) obtained from DBpedia Spotlight with a local ontology entity, 
 * including [name] and names of [classes] (concatenated into a single string
 * with delimiting semicolons) to which the local entity belongs.
 *
 * When used to represent a pair from DBpedia that has not (yet) been matched
 * with a local ontology entity, the last two components are null; the first
 * two components will never change after creation, but the last two might
 * if the DBpedia entity matches a local one.
 */
data class Entity(
    val source: String,
    val namedbp: String,
    var name: String?,
    var classes: String?
) {
    // for inclusion in a CSV file
    override fun toString() = "$source,$namedbp,$name,$classes"
}

/**
 * Return a string with the contents of the file at the given [path], assuming
 * the given [encoding] and trimming trailing (or leading) whitespace.
 *
 * Essentially copied from https://stackoverflow.com/a/326440.
 */
fun readFile(path: String, encoding: Charset) =
    String(Files.readAllBytes(Paths.get(path)), encoding).trim()

/**
 * Make the given [string] nice.
 *
 * In this context, "nice" means that a line break follows each sentence (so the
 * string may still be un-nice in other ways).
 * The above may not be strictly true because this function just replaces spaces
 * with newlines when they follow characters that usually end sentences, but 
 * it's a reasonable approximation and should be good enough.
 */
fun niceifyString(string: String) =
    StringBuilder(string)
        .replace(Regex("\\. "), ".\n")
        .replace(Regex("\\? "), "?\n")
        .replace(Regex("\\! "), "!\n")
        .toString()

/**
 * Get DBpedia spotlight's annotations at the given [confidence] level for the
 * given [text] (assumed to be niceified) as a set of entities
 * with null local ontology components.
 *
 * Requires an Internet connection (for the DBpedia Spotlight Web Service).
 */
fun extractEntities(text: String, confidence: Double): HashSet<Entity> {
    val entities = HashSet<Entity>()
    val sentences =
        text.replace("\"", "")
            .replace("%", "")
            .split("\n")
    val n = sentences.size
    var i = 0
    while (i < n) {
        var j = i
        val s = StringBuilder(MAX_LENGTH_DBP)
        while (j < n && s.length + sentences[j].length < MAX_LENGTH_DBP)
            s.append(sentences[j++])
        i = j
        entities.addAll(annotateChunk(s.toString().trim(), confidence))
    }

    return entities
}

/**
 * Get DBpedia Spotlight's annotations, at the given [confidence] level, for the
 * given [chunk] (i.e., long string not comprising an entire document) of text,
 * returning them in a set of entities with null local ontology components.
 *
 * Assisted by https://www.baeldung.com/java-http-request; as I had never 
 * previously needed to bother with Internet programming, the following may
 * be exceedingly clumsily written (I have no way of knowing).
 * And I suspect there is a much better way to iterate over the input stream.
 *
 * Presently hard-coded to use the web service DBpedia Spotlight provides,
 * which is sometimes down.
 *
 * See also
 * https://www.oracle.com/corporate/features/jsoup-html-parsing-library.html
 * and https://jsoup.org/cookbook/extracting-data/dom-navigation
 */
fun annotateChunk(chunk: String, confidence: Double): HashSet<Entity> {
    // send the chunk and confidence to Spotlight
    val con = URL("http://model.dbpedia-spotlight.org/en/annotate")
        .openConnection() as HttpURLConnection
    con.requestMethod = "POST"
    con.doOutput = true
    val out = DataOutputStream(con.outputStream)
    var status = 0
    while (status != 200) {
        out.writeBytes("text=$chunk&confidence=$confidence")
        out.flush()
        status = con.responseCode
    }
    out.close()   // probably inefficient to keep opening and closing

    // process the output from Spotlight
    val reader = BufferedReader(InputStreamReader(con.inputStream))
    val response = StringBuilder()
    var nextLine = reader.readLine()
    while (nextLine != null) {
        response.append(nextLine)
        nextLine = reader.readLine()
    }
    val doc = Jsoup.parse(response.toString())
    val content = doc.getElementsByTag("a")
    val entities = HashSet<Entity>()
    for (link in content)
        entities.add(Entity(link.text(), link.attr("href"), null, null))

    return entities
}

/**
 * Seek matches in a local [ontology] (applying the given function to
 * [transform] entity names from DBpedia to local ontology form) for each
 * element of the given set of [entities], adding the appropriate components for
 * matched entities and deleting unmatched ones.
 *
 * There may be an apter name for this function.
 */
fun crossIndex(
    entities: Set<Entity>,
    ontology: OWLOntology,
    transform: (String) -> String
): HashSet<Entity> {
    val outEntities = HashSet<Entity>()
    for (entity in entities) {
        val types = StringBuilder()
        val name = transform(entity.namedbp)
        for (type in
             EntitySearcher
                 .getTypes(OWLNamedIndividualImpl(IRI.create(name)),
                           ontology).asSequence())
            types.append("${type.asOWLClass().iri};")
        if (types.isNotEmpty()) {
            entity.name = name
            entity.classes = types.toString()
            outEntities.add(entity)
        }
    }

    return outEntities
}

/**
 * Write the contents of the given set of [entities] to a four-column CSV file 
 * [outfile] assuming the given [encoding].
 */
fun writeEntities(
    outfile: String,
    encoding: Charset,
    entities: Set<Entity>
) {
    val writer = Files.newBufferedWriter(Paths.get(outfile), encoding)
    for (entity in entities) {
        writer.write(entity.toString())
        writer.newLine()
    }
    writer.close()
}

/**
 * Turn the specification at the given [specificationPath] for a set of 
 * replacements into a function that applies the specified replacements to a 
 * string.
 *
 * This would be a lot simpler if I could just read the contents of the file
 * into a string and evaluate the contents of the string as a Kotlin expression,
 * but the Internet seems to be silent on that topic, which is annoying. I bet
 * it's easy in Lisp.
 *
 * Here we only consider replacing single characters by single characters.
 * And things might go wrong if no replacements are specified but the file is
 * provided anyway.
 */
fun makeReplacer(specificationPath: String, enc: Charset): (String) -> String {
    try {
        val spec = readFile(specificationPath, enc).split("\n")
        val prefixLocal = spec[0]
        val stripEnds = spec[1][0] == 'y'
        val repList = spec.subList(2, spec.size)

        val subs = HashMap<Char, Char>(repList.size)
        for (r in repList) {
            val sub = r.toCharArray()
            subs[sub[0]] = sub[2]
        }

        return { s: String ->
                 val sb = StringBuilder(s.length - DBP_PREFIX_LENGTH)
                 var s1 = s.substring(DBP_PREFIX_LENGTH)
                 if (stripEnds) s1 = s1.trim { it in subs }
                 for (c in s1) sb.append(subs[c] ?: c)
                 "$prefixLocal$sb" }
    }
    catch (e: Exception) {
        throw Exception("REP")
    }    
}

/**
 * Turn a string describing an encoding from java.nio.charset.StandardCharsets
 * into a Charset object.
 */
fun parseEncoding(x: String) =
    when (x) {
        "ISO_8859_1" -> StandardCharsets.ISO_8859_1
        "US_ASCII" -> StandardCharsets.US_ASCII
        "UTF_16" -> StandardCharsets.UTF_16
        "UTF_16BE" -> StandardCharsets.UTF_16BE
        "UTF_16LE" -> StandardCharsets.UTF_16LE
        else -> StandardCharsets.UTF_8
    }

/**
 * Compose the other elements of this file to produce a CSV for the file at
 * each of the given [textPaths].
 *
 * Specifically, for each file, extract entities from Spotlight's annotation 
 * thereof (with the given [confidence]), find matches (that may vary according 
 * to the given strategy for [replacements]) in a local ontology using the given
 * [searcher], and write a four-column CSV file to the directory at the given
 * [outPath].
 */
fun annotateAndMatch(
    textPaths: List<String>,
    ontology: OWLOntology,
    replacements: (String) -> String = replDefault,
    encoding: Charset = StandardCharsets.UTF_8,
    confidence: Double = .5,
    outPath: String = "out"
) {
    if (confidence < 0 || confidence > 1)
        throw RuntimeException("Confidence must be between 0 and 1.")

    // make sure output directory exists
    val outDir = File(outPath)
    outDir.mkdirs()

    // run the annotation pipeline for each file
    for (path in textPaths)
        writeEntities(
            "$outPath/${path.replace("/", "")}.csv",
            encoding,
            crossIndex(
                extractEntities(
                    niceifyString(readFile(path, encoding)),
                    confidence),
                ontology,
                replacements))
}

/**
 * Wrapper for annotateAndMatch.
 *
 * Load the ontology from [ontologyPath] and replacements from [repPath], get 
 * the right Charset object based on [encoding], and pass everything else 
 * through to annotateAndMatch.
 *
 * This may not be the best way to structure things, as it results in retyping
 * default parameter values.
 *
 * Yes, the expression that is the body of this function is the application of
 * a lambda to the result of calling a function. It would look cleaner in ML.
 */
fun amWrapper(
    textPaths: List<String>,
    ontologyPath: String,
    repPath: String? = null,
    encoding: String = "UTF_8",
    confidence: Double = .5,
    outPath: String = "out"
) = { enc: Charset ->
          annotateAndMatch(
              textPaths,
              OWLManager.createOWLOntologyManager()
                  .loadOntologyFromOntologyDocument(File(ontologyPath)),
              if (repPath != null) makeReplacer(repPath, enc)
              else replDefault,
              enc,
              confidence,
              outPath) }(parseEncoding(encoding))

/** 
 * Return a list containing the paths of all files in the working directory.
 *
 * A little inefficient: gets Path objects and turns them into strings, but
 * they'll need to be re-Pathified later. That suggests that there is a simpler
 * way to write this. Oh well.
 *
 * Adapted from 
 * https://stonesoupprogramming.com/2017/11/30/kotlin-files-attributes-and-list-files-folders-in-directory/
 *
 * I want to inline this because it's essentially a macro with no parameters,
 * but Kotlin tells me "expected performance impact from inlining is
 * insignificant", so I will appease it.
 */
fun listifyWD() =
    Files.list(Paths.get(""))
    .collect(Collectors.toList())
    .map { x: java.nio.file.Path -> x.toString() }

// "main" exists to interact with users via the command line and is a wrapper
// for amWrapper, which is itself a wrapper for annotateAndMatch, which other
// programs can call to do their dirty work.
// ee (<text>... | -a) -o ont [-r rep] [-e enc] [-c conf] [-o out]
fun main(args: Array<String>) {
    try {
        val endTexts = args.indexOf("-o")
        // you can't submit an ontology or text in a file called "help", which
        // is not a severe restriction IMO
        if ("--help" in args || "-h" in args || "help" in args || endTexts < 1)
            throw RuntimeException("HELP")

        val texts = if (args[0] == "-a") listifyWD() else args.take(endTexts)
        val ontPath = args[endTexts + 1]
        val indexR = args.indexOf("-r")
        val repPath = if (indexR > -1) args[indexR + 1] else null
        val indexE = args.indexOf("-e")
        val enc = if (indexE > -1) args[indexE + 1] else "UTF_8"
        val indexC = args.indexOf("-c")
        val conf = if (indexC > -1) args[indexC + 1].toDouble() else .5
        val indexU = args.indexOf("-u")
        val outPath = if (indexU > -1) args[indexU + 1] else "out"

        amWrapper(texts, ontPath, repPath, enc, conf, outPath)
    }
    catch (e: Exception) {
        println(
            when (e.message) {
                "HELP" -> USAGE_MESSAGE
                "REP" -> "Check the replacement specification."
                else -> "Error: $e"
            })
    }
}
