package org.maochen.nlp.ml.classifier.hmm;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Maochen on 8/5/15.
 */
public class WordUtils {

    private static final Map<String, String> TAG_LIST = new HashMap<String, String>() {{
        put("(", "-LSB-");
        put(")", "-RSB-");
        put("``", "''");
        put("COMMA", ",");
    }};

    private static final Map<String, String> WORD_LIST = new HashMap<String, String>() {{
        put("COMMA", ",");
    }};


    public static String normalizeTag(String tag) {
        return TAG_LIST.containsKey(tag) ? TAG_LIST.get(tag) : tag;
    }

    public static String normalizeWord(String word) {
        return WORD_LIST.containsKey(word) ? WORD_LIST.get(word) : word;
    }
}
