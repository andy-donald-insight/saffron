package org.insightcentre.nlp.saffron.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A single document in a corpus
 * 
 * @author John McCrae <john@mccr.ae>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Document {
    public final File file;
    public final String id;
    public String name;
    @JsonProperty("mime_type")
    public String mimeType;
    public List<Author> authors;
    private Loader contents;
    public Map<String, String> metadata;
     
    @JsonCreator
    public Document(@JsonProperty(value="file") File file, 
                    @JsonProperty(value="id", required=true) String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("mime_type") String mimeType, 
                    @JsonProperty("authors") List<Author> authors,
                    @JsonProperty("metadata") Map<String, String> metadata,
                    @JsonProperty("contents") String contents) {
        if(file == null && contents == null)
            throw new IllegalArgumentException("Please give either document contents or link to file");
        this.file = file;
        this.id = id;
        this.name = name;
        this.mimeType = mimeType == null && contents != null ? "text/plain" : mimeType;
        this.authors = authors == null ? new ArrayList<Author>() : authors;
        this.metadata = metadata == null ? new HashMap<String, String>() : metadata;
        this.contents = new InMemory(contents);
    }
    
    /**
     * Set the contents of this file once they have been loaded (i.e., from the source file)
     * 
     * @param contents A (possibly lazy) loader for the contents
     */
    public Document withLoader(Loader contents) {
        this.contents = contents;
        return this;
    }

    /**
     * Get the *raw* text contents of the document. This method will not load
     * files to read contents, unlike contents()
     * @return The contents
     * @throw IllegalArgumentException If the document has not been loaded
     */
    public String getContents() {
        if(contents != null) {
            return contents.getContentsSerializable(this);
        } else {
            return null;
        }
    }
    
    public String contents() {
        if(contents == null) {
            throw new IllegalArgumentException("Cannot retrieve contents, deserialization method not set");
        } else {
            return contents.getContents(this);
        }
    }
        
    public String getId() {
        return id;
    }

    public File getFile() {
        return file;
    }



    public Map<String, String> getMetadata() {
        return metadata;
    }

    @JsonProperty("mime_type")
    public String getMimeType() {
        return mimeType;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + Objects.hashCode(this.id);
        hash = 67 * hash + Objects.hashCode(this.name);
        hash = 67 * hash + Objects.hashCode(this.authors);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Document other = (Document) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.authors, other.authors)) {
            return false;
        }
        return true;
    }
    
    /**
     * Enables on-demand loading of document contents
     */
    public static interface Loader {
        /**
         * The contents of the document
         * @return The contents, possibly loading from a disk or internal source
         */
        String getContents(Document d);
        /**
         * The contents or null if they are serialized elsewhere
         * @return The contenst, or null if they are serialized elsewhere
         */
        String getContentsSerializable(Document d);
    }
    
    /**
     * Document is a string in memory
     */
    public static class InMemory implements Loader {
        private final String contents;

        public InMemory(String contents) {
            this.contents = contents;
        }

        @Override
        public String getContents(Document d) {
            return contents;
        }

        @Override
        public String getContentsSerializable(Document d) {
            return contents;
        }
    } 
    
}