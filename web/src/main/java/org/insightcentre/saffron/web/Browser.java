package org.insightcentre.saffron.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.util.Map;
import static java.lang.Integer.min;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.jena.rdf.model.Model;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.insightcentre.nlp.saffron.data.Author;
import org.insightcentre.nlp.saffron.data.Document;
import org.insightcentre.nlp.saffron.data.Taxonomy;
import org.insightcentre.nlp.saffron.data.TaxonomyWithSize;
import org.insightcentre.nlp.saffron.data.Term;
import org.insightcentre.nlp.saffron.data.connections.AuthorTerm;
import org.insightcentre.nlp.saffron.data.connections.DocumentTerm;
import org.insightcentre.nlp.saffron.data.connections.TermTerm;
import org.insightcentre.nlp.saffron.taxonomy.supervised.AddSizesToTaxonomy;
import org.insightcentre.saffron.web.rdf.RDFConversion;

/**
 * Handles the interface if there is a corpus loaded
 *
 * @author John McCrae &lt;john@mccr.ae&gt;
 */
public class Browser extends AbstractHandler {

    protected final SaffronInMemoryDataSource saffronHandler;

    public Browser(File dir, SaffronInMemoryDataSource saffron) throws IOException {

        this.saffronHandler = saffron;
        if (dir.exists()) {
            for (File subdir : dir.listFiles()) {
                if (subdir.exists() && subdir.isDirectory() && new File(subdir, "taxonomy.json").exists()) {
                    try {
                        SaffronData s2;
                        //s2 = SaffronData.fromDirectory(subdir);
//                        saffron.addRun(subdir.getName(), s2);
                        this.saffronHandler.fromDirectory(subdir, subdir.getName());
                    } catch (Exception x) {
                        x.printStackTrace();
                        System.err.println("Failed to load Saffron from the existing data, this may be because a previous run failed");
                    }
                }
            }
        }
    }

    @Override
    public void handle(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {
        if (target != null && target.startsWith("/") && target.lastIndexOf("/") != 0) {
            String name = target.substring(1, target.indexOf("/", 1));
            if (saffronHandler.containsKey(name)) {
                handle2(target.substring(target.indexOf("/", 1)),
                        baseRequest, request, response, name);
            }
        }
    }

    public void handle2(String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response,
            String saffronDatasetName)
            throws IOException, ServletException {
        try {
            // Exposing an existing directory
            if (saffronHandler != null && saffronHandler.isLoaded(saffronDatasetName)) {
                final ObjectMapper mapper = new ObjectMapper();
                if (target.equals("/taxonomy")) {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    mapper.writeValue(response.getWriter(), saffronHandler.getTaxonomy(saffronDatasetName));
                } else if (target.equals("/taxonomy_with_size")) {
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    Taxonomy taxonomy = saffronHandler.getTaxonomy(saffronDatasetName);
                    TaxonomyWithSize tws = AddSizesToTaxonomy.addSizes(taxonomy, saffronHandler.getDocTerms(saffronDatasetName));
                    mapper.writeValue(response.getWriter(), tws);
                } else if ("/parents".equals(target)) {
                    final String term = request.getParameter("term");
                    if (term != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), saffronHandler.getTaxoParents(saffronDatasetName, term));
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if ("/children".equals(target)) {
                    final String term = request.getParameter("term");
                    if (term != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), saffronHandler.getTaxoChildrenScored(saffronDatasetName, term));
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/author-sim")) {
                    final String author1 = request.getParameter("author1");
                    final String author2 = request.getParameter("author2");
                    final List<Author> aas;
                    if (author1 != null) {
                        aas = saffronHandler.authorAuthorToAuthor2(saffronDatasetName, saffronHandler.getAuthorSimByAuthor1(saffronDatasetName, author1));
                    } else if (author2 != null) {
                        aas = saffronHandler.authorAuthorToAuthor1(saffronDatasetName, saffronHandler.getAuthorSimByAuthor2(saffronDatasetName, author2));
                    } else {
                        aas = null;
                    }
                    if (aas != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), aas);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/term-sim")) {
                    final String term1 = request.getParameter("term1");
                    final String term2 = request.getParameter("term2");
                    final int n = request.getParameter("n") == null ? 20 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    final List<TermTerm> tts;
                    if (term1 != null) {
                        tts = saffronHandler.getTermByTerm1(saffronDatasetName, term1, saffronHandler.getTaxoChildren(saffronDatasetName, term1));
                    } else if (term2 != null) {
                        tts = saffronHandler.getTermByTerm2(saffronDatasetName, term2);
                    } else {
                        tts = null;
                    }
                    if (tts != null) {
                        List<TermTerm> tts2 = getTopN(tts, n, offset);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), tts2);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/author-terms")) {
                    final String author = request.getParameter("author");
                    final String term = request.getParameter("term");
                    final int n = request.getParameter("n") == null ? 20 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    if (author != null) {
                        final List<AuthorTerm> ats = saffronHandler.getTermByAuthor(saffronDatasetName, author);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), getTopNAuthorTerms(ats, n, offset));
                    } else if (term != null) {
                        final List<Author> as = saffronHandler.authorTermsToAuthors(saffronDatasetName, getTopNAuthorTerms(saffronHandler.getAuthorByTerm(saffronDatasetName, term), n, offset));
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), as);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/doc-terms")) {
                    final String doc = request.getParameter("doc");
                    final String term = request.getParameter("term");
                    final int n = request.getParameter("n") == null ? 20 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    if (doc != null) {
                        final List<DocumentTerm> dts = saffronHandler.getTermByDoc(saffronDatasetName, doc);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), getTopNDocTerms(dts, n, offset));

                    } else if (term != null) {
                        final List<Document> _docs = saffronHandler.getDocsByTerm(saffronDatasetName, term);
                        final List<Document> docs = new ArrayList<>();
                        int i = 0;
                        for (Document d : _docs) {
                            if (i >= offset && i < offset + n) {
                                docs.add(d.reduceContext(term, 20));
                            }
                            i++;
                        }
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), docs);

                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), Collections.EMPTY_LIST);
                    }
                } else if (target.equals("/terms")) {
                    final String id = request.getParameter("id");
                    final Term t;
                    if (id != null) {
                        t = saffronHandler.getTerm(saffronDatasetName, id);
                    } else {
                        t = null;
                    }
                    if (t != null) {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), t);
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), null);
                    }
                } else if (target.equals("/author-docs")) {
                    final String authorId = request.getParameter("author");
                    final int n = request.getParameter("n") == null ? 1000 : Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 0 : Integer.parseInt(request.getParameter("offset"));
                    if (authorId != null) {
                        List<Document> docs = saffronHandler.getDocsByAuthor(saffronDatasetName, authorId);
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        if (offset < docs.size()) {
                            mapper.writeValue(response.getWriter(), docs.subList(offset, min(docs.size(), n + offset)));
                        } else {
                            try (Writer w = response.getWriter()) {
                                w.write("[]");
                            }
                        }
                    } else {
                        response.setContentType("application/json;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        baseRequest.setHandled(true);
                        mapper.writeValue(response.getWriter(), null);
                    }
                } else if (target.equals("/top-terms")) {
                    final int n = Integer.parseInt(request.getParameter("n"));
                    final int offset = request.getParameter("offset") == null ? 20
                            : Integer.parseInt(request.getParameter("offset"));
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    mapper.writeValue(response.getWriter(), saffronHandler.getTopTerms(saffronDatasetName, n, offset + n));
                } else if (target.startsWith("/term/")) {
                    final String termString = decode(target.substring(6));
                    final Term term = saffronHandler.getTerm(saffronDatasetName, termString);

                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get(System.getProperty("saffron.home") + "/web/static/term.html")));
                    if (term != null) {
                        data = data.replaceAll("\\{\\{term\\}\\}", mapper.writeValueAsString(term));
                    } else {
                        data = data.replaceAll("\\{\\{term\\}\\}", "{}");
                    }
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                } else if (target.startsWith("/edit/terms")) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get(System.getProperty("saffron.home") + "/web/static/edit-terms-page.html")));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                } else if (target.startsWith("/edit/parents")) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get(System.getProperty("saffron.home") + "/web/static/edit-parents-page.html")));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                } else if (target.startsWith("/edit")) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get(System.getProperty("saffron.home") + "/web/static/edit-page.html")));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                }else if (target.startsWith("/author/")) {
                    final String authorString = decode(target.substring(8));
                    final Author author = saffronHandler.getAuthor(saffronDatasetName, authorString);
                    if (author != null) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get(System.getProperty("saffron.home") + "/web/static/author.html")));
                        data = data.replaceAll("\\{\\{author\\}\\}", mapper.writeValueAsString(author));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                    }
                } else if (target.startsWith("/doc/")) {
                    final String docId = decode(target.substring(5));
                    final Document doc = saffronHandler.getDoc(saffronDatasetName, docId);
                    if (doc != null) {
                        response.setContentType("text/html;charset=utf-8");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        String data = new String(Files.readAllBytes(Paths.get(System.getProperty("saffron.home") + "/web/static/doc.html")));
                        data = data.replace("{{doc}}", mapper.writeValueAsString(doc));
                        data = data.replace("{{name}}", saffronDatasetName);
                        response.getWriter().write(data);
                    }
                } else if (target.startsWith("/doc_content/")) {
                    final String docId = decode(target.substring(13));
                    final Document doc = saffronHandler.getDoc(saffronDatasetName, docId);
                    if (doc != null && doc.file != null) {
                        File f = doc.file.toFile();
                        response.setContentType(Files.probeContentType(f.toPath()));
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        response.getWriter().write(doc.contents());
                    } else if (doc != null) {
                        String contents = doc.contents();
                        response.setContentType("text/plain");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        try (Writer writer = response.getWriter()) {
                            writer.write(contents);
                        }
                    }
                } else if (target.equals("/status")) {
                    final Executor.Status status = new Executor.Status( new PrintWriter(new FileWriter("log.txt", true)), saffronHandler);
                    status.completed = true;
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getWriter().print("{\"completed\":true}");
                } else if ("/".equals(target)) {

                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File(System.getProperty("saffron.home") + "/web/static/index.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));

                } else if ("/treemap.html".equals(target)) {

                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File(System.getProperty("saffron.home") + "/web/static/treemap.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
                } else if ("/graph.html".equals(target)) {

                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File(System.getProperty("saffron.home") + "/web/static/graph.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
                } else if ("/search/".equals(target)) {
                    response.setContentType("text/html");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    FileReader reader = new FileReader(new File(System.getProperty("saffron.home") + "/web/static/search.html"));
                    Writer writer = new StringWriter();
                    char[] buf = new char[4096];
                    int i = 0;
                    while ((i = reader.read(buf)) >= 0) {
                        writer.write(buf, 0, i);
                    }
                    response.getWriter().write(writer.toString().replace("{{name}}", saffronDatasetName));
                } else if (target.startsWith("/ttl/doc/")) {
                    final String docId = decode(target.substring(9));
                    final Document doc = saffronHandler.getDoc(saffronDatasetName, docId);
                    if (doc != null) {
                        response.setContentType("text/turtle");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.documentToRDF(doc, saffronHandler, saffronDatasetName);
                        model.write(response.getWriter(), "TURTLE");
                    }
                } else if (target.startsWith("/ttl/author/")) {
                    final String authorId = decode(target.substring(12));
                    final Author author = saffronHandler.getAuthor(saffronDatasetName, authorId);
                    if (author != null) {
                        response.setContentType("text/turtle");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.authorToRdf(author, saffronHandler, saffronDatasetName);
                        model.write(response.getWriter(), "TURTLE");
                    }
                } else if (target.startsWith("/ttl/term/")) {
                    final String termId = decode(target.substring(11));
                    final Term term = saffronHandler.getTerm(saffronDatasetName, termId);
                    if (term != null) {
                        response.setContentType("text/turtle");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.termToRDF(term, saffronHandler, saffronDatasetName);
                        model.write(response.getWriter(), "TURTLE");
                    }
                } else if (target.startsWith("/rdf/doc/")) {
                    final String docId = decode(target.substring(9));
                    final Document doc = saffronHandler.getDoc(saffronDatasetName, docId);
                    if (doc != null) {
                        response.setContentType("application/rdf+xml");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.documentToRDF(doc, saffronHandler, saffronDatasetName);
                        model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                    }
                } else if (target.startsWith("/rdf/author/")) {
                    final String authorId = decode(target.substring(12));
                    final Author author = saffronHandler.getAuthor(saffronDatasetName, authorId);
                    if (author != null) {
                        response.setContentType("application/rdf+xml");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.authorToRdf(author, saffronHandler, saffronDatasetName);
                        model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                    }
                } else if (target.startsWith("/rdf/term/")) {
                    final String termId = decode(target.substring(11));
                    final Term term = saffronHandler.getTerm(saffronDatasetName, termId);
                    if (term != null) {
                        response.setContentType("application/rdf+xml");
                        response.setStatus(HttpServletResponse.SC_OK);
                        baseRequest.setHandled(true);
                        Model model = RDFConversion.termToRDF(term, saffronHandler, saffronDatasetName);
                        model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                    }
                } else if (target.equals("/download/rdf")) {
                    response.setContentType("application/rdf+xml");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    String base = getBase(request, "/download/rdf");
                    Model model = RDFConversion.allToRdf(base, saffronHandler, saffronDatasetName);
                    model.write(response.getWriter(), "RDF/XML", request.getRequestURI());
                } else if (target.equals("/download/ttl")) {
                    response.setContentType("text/turtle");
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    String base = getBase(request, "/download/ttl");
                    Model model = RDFConversion.allToRdf(base, saffronHandler, saffronDatasetName);
                    model.write(response.getWriter(), "TURTLE");
                }

            }
        } catch (Exception x) {
            x.printStackTrace();
            throw new ServletException(x);
        }
    }

    private String getBase(HttpServletRequest req, String path) {
        StringBuffer sb = req.getRequestURL();
        sb.delete(sb.length() - path.length(), sb.length());
        return sb.toString();
    }

    private String decode(String id) {
        try {
            return URLDecoder.decode(id, "UTF-8");
        } catch(UnsupportedEncodingException x) {
            // silly Java, you could have used an enum here and not had to throw an exception....
            return id;
        }
    }

    private List<TermTerm> getTopN(final List<TermTerm> tts, final int n, final int offset) {
        tts.sort(new Comparator<TermTerm>() {
            @Override
            public int compare(TermTerm o1, TermTerm o2) {
                if (o1.getSimilarity() > o2.getSimilarity()) {
                    return -1;
                } else if (o1.getSimilarity() < o2.getSimilarity()) {
                    return +1;
                } else {
                    int i = o1.getTerm1().compareTo(o2.getTerm1());
                    int j = o1.getTerm2().compareTo(o2.getTerm2());
                    return i != 0 ? i : j;
                }
            }
        });
        if (tts.size() < offset) {
            return Collections.EMPTY_LIST;
        } else if (tts.size() < offset + n) {
            return tts.subList(offset, tts.size());
        } else {
            return tts.subList(offset, offset + n);
        }
    }

    private List<DocumentTerm> getTopNDocTerms(final List<DocumentTerm> tts, final int n, final int offset) {
        tts.sort(new Comparator<DocumentTerm>() {
            @Override
            public int compare(DocumentTerm o1, DocumentTerm o2) {
                if (o1.getOccurrences() > o2.getOccurrences()) {
                    return -1;
                } else if (o1.getOccurrences() < o2.getOccurrences()) {
                    return +1;
                } else {
                    int i = o1.getDocumentId().compareTo(o2.getDocumentId());
                    int j = o1.getTermString().compareTo(o2.getTermString());
                    return i != 0 ? i : j;
                }
            }
        });
        if (tts.size() < offset) {
            return Collections.EMPTY_LIST;
        } else if (tts.size() < offset + n) {
            return tts.subList(offset, tts.size());
        } else {
            return tts.subList(offset, offset + n);
        }
    }

    private List<AuthorTerm> getTopNAuthorTerms(final List<AuthorTerm> tts, final int n, final int offset) {
        tts.sort(new Comparator<AuthorTerm>() {
            @Override
            public int compare(AuthorTerm o1, AuthorTerm o2) {
                if (o1.getScore() > o2.getScore()) {
                    return -1;
                } else if (o1.getScore() < o2.getScore()) {
                    return +1;
                } else {
                    int k = Integer.compare(o1.getOccurrences(), o2.getOccurrences());
                    if(k != 0) return -k;
                    int i = o1.getAuthorId().compareTo(o2.getAuthorId());
                    int j = o1.getTermId().compareTo(o2.getTermId());
                    return i != 0 ? i : j;
                }
            }
        });
        if (tts.size() < offset) {
            return Collections.EMPTY_LIST;
        } else if (tts.size() < offset + n) {
            return tts.subList(offset, tts.size());
        } else {
            return tts.subList(offset, offset + n);
        }
    }
}
