package org.insightcentre.nlp.saffron.data;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A taxonomy of terms where the root node is a virtual node rather than an existing term
 *
 * @author Bianca Pereira
 */
public class VirtualRootTaxonomy extends Taxonomy{

    public static final String VIRTUAL_ROOT = "HEAD_TERM";

    public VirtualRootTaxonomy() {
        super();
        this.setRoot(VIRTUAL_ROOT);
    }

    public VirtualRootTaxonomy(Taxonomy taxonomy) {
        super();
        this.setRoot(VIRTUAL_ROOT);

        if(taxonomy.getRoot().equals(VIRTUAL_ROOT)) {
            for(Taxonomy child: taxonomy.getChildren()) {
                this.addChild(child);
            }
        } else {
            this.addChild(taxonomy);
        }
    }

    public VirtualRootTaxonomy(Collection<Taxonomy> taxonomies) {
        super();
        this.setRoot(VIRTUAL_ROOT);
        if (taxonomies != null) {
            for (Taxonomy taxonomy: taxonomies) {
                this.addChild(taxonomy);
            }
        }
    }

    @JsonCreator
    @JsonIgnoreProperties(ignoreUnknown = true)
    public VirtualRootTaxonomy(@JsonProperty("root") String root,
                               @JsonProperty("score") double score,
                               @JsonProperty("linkScore") double linkScore,
                               @JsonProperty("children") List<Taxonomy> children,
                               @JsonProperty("status") Status status) {

        super();
        this.setRoot(VIRTUAL_ROOT);

        if (root.equals(VIRTUAL_ROOT)) {
            for(Taxonomy child: children) {
                this.addChild(child);
            }
        } else {
            this.addChild(new Taxonomy(root, score, linkScore, children, status));
        }
    }

}
