package BlueCrab.com.example.dto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Precomputed token lookup result for batch notification flows.
 */
public class FcmTokenCollectionResult {

    private final List<FcmTokenLookupResponse> lookupResponses;
    private final Map<String, List<String>> tokensByUser;
    private final List<String> flattenedTokens;

    public FcmTokenCollectionResult(List<FcmTokenLookupResponse> lookupResponses,
                                    Map<String, List<String>> tokensByUser,
                                    List<String> flattenedTokens) {
        this.lookupResponses = lookupResponses != null ? Collections.unmodifiableList(lookupResponses) : Collections.emptyList();
        this.tokensByUser = tokensByUser != null ? Collections.unmodifiableMap(new LinkedHashMap<>(tokensByUser)) : Collections.emptyMap();
        this.flattenedTokens = flattenedTokens != null ? Collections.unmodifiableList(flattenedTokens) : Collections.emptyList();
    }

    public List<FcmTokenLookupResponse> getLookupResponses() {
        return lookupResponses;
    }

    public Map<String, List<String>> getTokensByUser() {
        return tokensByUser;
    }

    public List<String> getFlattenedTokens() {
        return flattenedTokens;
    }

    public int getTotalTokenCount() {
        return flattenedTokens.size();
    }
}
