package org.insightcentre.nlp.saffron.crawler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.sangupta.murmur.Murmur3;
import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.insightcentre.nlp.saffron.data.Document;

/**
 *
 * @author John McCrae
 */
public class SaffronCrawler extends WebCrawler {

    private final String languageFilter;
    private final String urlFilter;
    private final int collectionLimit;
    private final File saveFolder;
    private final List<Document> corpus;

    public SaffronCrawler(String languageFilter, String urlFilter,
            int collectionLimit, File saveFolder, List<Document> corpus) {
        this.languageFilter = languageFilter;
        this.urlFilter = urlFilter;
        this.saveFolder = saveFolder;
        this.corpus = corpus;
        this.collectionLimit = collectionLimit;
    }

    private String toHexString(long[] d) {
        StringBuilder sb = new StringBuilder();
        for (long l : d) {
            sb.append(String.format("%x", l));
        }
        return sb.toString();
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return (urlFilter == null || url.getURL().matches(urlFilter));
    }

    private static final Set<String> SUPPORTED_TYPES = new HashSet<>();
    static{
        SUPPORTED_TYPES.add("application/x-ibooks+zip");
        SUPPORTED_TYPES.add("application/epub+zip");
        SUPPORTED_TYPES.add("application/x-mspublisher");
        SUPPORTED_TYPES.add("application/x-tika-msoffice");
        SUPPORTED_TYPES.add("application/vnd.ms-excel");
        SUPPORTED_TYPES.add("application/sldworks");
        SUPPORTED_TYPES.add("application/x-tika-msworks-spreadsheet");
        SUPPORTED_TYPES.add("application/vnd.ms-powerpoint");
        SUPPORTED_TYPES.add("application/x-tika-msoffice-embedded; format=ole10_native");
        SUPPORTED_TYPES.add("application/vnd.ms-project");
        SUPPORTED_TYPES.add("application/x-tika-ooxml-protected");
        SUPPORTED_TYPES.add("application/msword");
        SUPPORTED_TYPES.add("application/vnd.ms-outlook");
        SUPPORTED_TYPES.add("application/vnd.visio");
        SUPPORTED_TYPES.add("application/vnd.ms-excel.sheet.macroenabled.12");
        SUPPORTED_TYPES.add("application/vnd.ms-powerpoint.presentation.macroenabled.12");
        SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.presentationml.template");
        SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
        SUPPORTED_TYPES.add("application/vnd.ms-excel.addin.macroenabled.12");
        SUPPORTED_TYPES.add("application/vnd.ms-word.document.macroenabled.12");
        SUPPORTED_TYPES.add("application/vnd.ms-excel.template.macroenabled.12");
        SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        SUPPORTED_TYPES.add("application/vnd.ms-powerpoint.slideshow.macroenabled.12");
        SUPPORTED_TYPES.add("application/vnd.ms-powerpoint.addin.macroenabled.12");
        SUPPORTED_TYPES.add("application/vnd.ms-word.template.macroenabled.12");
        SUPPORTED_TYPES.add("application/x-tika-ooxml");
        SUPPORTED_TYPES.add("application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.graphics-template");
        SUPPORTED_TYPES.add("application/vnd.sun.xml.writer");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.text");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.text-web");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.spreadsheet-template");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.formula-template");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.presentation");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.image-template");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.graphics");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.chart-template");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.presentation-template");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.image-template");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.formula");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.image");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.spreadsheet-template");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.chart-template");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.formula");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.spreadsheet");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.text-web");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.text-template");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.text");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.formula-template");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.spreadsheet");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.chart");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.text-master");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.text-master");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.text-template");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.graphics");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.graphics-template");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.presentation");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.image");
        SUPPORTED_TYPES.add("application/x-vnd.oasis.opendocument.presentation-template");
        SUPPORTED_TYPES.add("application/vnd.oasis.opendocument.chart");
        SUPPORTED_TYPES.add("application/pdf");
        SUPPORTED_TYPES.add("application/rtf");
        SUPPORTED_TYPES.add("text/plain");
    };
    
    @Override
    public void visit(Page page) {
        if ((languageFilter == null || page.getLanguage().equals(languageFilter))
                && (urlFilter == null || page.getWebURL().getURL().matches(urlFilter))) {
                byte[] urlBytes = page.getWebURL().getURL().getBytes();
                String key = toHexString(Murmur3.hash_x64_128(urlBytes, urlBytes.length, 0));
            if (page.getParseData() instanceof HtmlParseData) {
                HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                String html = htmlParseData.getHtml();
                File file = new File(saveFolder, key + ".html");
                try (PrintWriter out = new PrintWriter(file)) {
                    out.println(html);
                } catch (IOException x) {
                    System.err.println("Could not write html for " + page.getWebURL().getURL());
                }
                corpus.add(new Document(file, key, htmlParseData.getTitle(), "text/html", Collections.EMPTY_LIST, htmlParseData.getMetaTags()));
                if (corpus.size() >= collectionLimit) {
                    getMyController().shutdown();
                }
            } else if(SUPPORTED_TYPES.contains(page.getContentType())) {
                String url = page.getWebURL().getURL();
                String extension = url.lastIndexOf('.') > 0 ?
                        url.substring(url.lastIndexOf('.')) : ".bin";
                File file = new File(saveFolder, key + extension);
                try(FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(page.getContentData());
                } catch(IOException x) {
                    System.err.println("Could not write binary for " + url);
                }
                corpus.add(new Document(file, key, url, "text/html", Collections.EMPTY_LIST, Collections.EMPTY_MAP));
                if (corpus.size() >= collectionLimit) {
                    getMyController().shutdown();
                }
            }
        }
    }

    private static void badOptions(OptionParser p, String message) throws IOException {
        System.err.println("Error: " + message);
        p.printHelpOn(System.err);
        System.exit(-1);
    }

    public static void main(String[] args) throws Exception {
        try {
            // Parse command line arguments
            final OptionParser p = new OptionParser() {
                {
                    accepts("l", "Only accept pages of a particular language (e.g., \"en\")").withRequiredArg().ofType(String.class);
                    accepts("u", "The form of URLs to accept (as Regex)").withRequiredArg().ofType(String.class);
                    accepts("n", "The maximal number of URLs to harvest").withRequiredArg().ofType(Integer.class);
                    accepts("s", "The seed URL to start with").withRequiredArg().ofType(URL.class);
                    accepts("t", "The directory to save temporary work to").withRequiredArg().ofType(String.class);
                    accepts("c", "The number of crawler threads").withRequiredArg().ofType(Integer.class);
                    accepts("o", "The output folder").withRequiredArg().ofType(File.class);
                }
            };
            final OptionSet os;

            try {
                os = p.parse(args);
            } catch (Exception x) {
                badOptions(p, x.getMessage());
                return;
            }
            final File saveFolder;
            if (os.has("o")) {
                saveFolder = (File) os.valueOf("o");
                if (!saveFolder.exists()) {
                    saveFolder.mkdirs();
                } else if (!saveFolder.isDirectory()) {
                    badOptions(p, "Save folder does not exist or is not a directory");
                }
            } else {
                badOptions(p, "The output folder is required");
                return;
            }
            SaffronCrawlerFactory factory = new SaffronCrawlerFactory(saveFolder);

            final String crawlStorageFolder;
            if (os.has("t")) {
                crawlStorageFolder = (String) os.valueOf("t");
            } else {
                File f = Files.createTempDir();
                crawlStorageFolder = f.getAbsolutePath();
            }
            int numberOfCrawlers = os.has("c") ? (Integer) os.valueOf("c") : 7;

            CrawlConfig config = new CrawlConfig();
            config.setCrawlStorageFolder(crawlStorageFolder);
            config.setIncludeBinaryContentInCrawling(true);

            factory.setLanguageFilter((String) os.valueOf("l"));
            factory.setCollectionLimit((Integer) os.valueOf("n"));
            factory.setUrlFilter((String) os.valueOf("u"));


            /*
             * Instantiate the controller for this crawl.
             */
            PageFetcher pageFetcher = new PageFetcher(config);
            RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
            RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
            CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

            if (!os.has("s")) {
                badOptions(p, "A seed URL is required");
            }
            controller.addSeed(((URL) os.valueOf("s")).toString());
            

            /*
             * Start the crawl. This is a blocking operation, meaning that your code
             * will reach the line after this only when crawling is finished.
             */
            System.err.println("Starting crawl");
            controller.start(factory, numberOfCrawlers);

            ObjectMapper mapper = new ObjectMapper();

            CrawledCorpus c = new CrawledCorpus(factory.getDocuments());
            mapper.writeValue(new File(saveFolder, "corpus.json"), c);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(-1);
        }
    }
}
