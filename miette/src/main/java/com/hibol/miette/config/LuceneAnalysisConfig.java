package com.hibol.miette.config;

import org.apache.lucene.analysis.core.LowerCaseFilterFactory;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurationContext;
import org.hibernate.search.backend.lucene.analysis.LuceneAnalysisConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LuceneAnalysisConfig {

    @Bean
    public LuceneAnalysisConfigurer luceneAnalysisConfigurer() {
        return (LuceneAnalysisConfigurationContext context) -> {

            // Analyzer d'indexation : tokenise par mots, normalise les accents, puis crée les n-grams
            context.analyzer("ngram_index").custom()
                .tokenizer(WhitespaceTokenizerFactory.class)
                .tokenFilter(LowerCaseFilterFactory.class)
                .tokenFilter(ASCIIFoldingFilterFactory.class)
                .tokenFilter(NGramFilterFactory.class)
                    .param("minGramSize", "3")
                    .param("maxGramSize", "15");

            // Analyzer de recherche : tokenise par mots et normalise seulement (pas de n-grams)
            context.analyzer("ngram_search").custom()
                .tokenizer(StandardTokenizerFactory.class)
                .tokenFilter(LowerCaseFilterFactory.class)
                .tokenFilter(ASCIIFoldingFilterFactory.class);
        };
    }
}
