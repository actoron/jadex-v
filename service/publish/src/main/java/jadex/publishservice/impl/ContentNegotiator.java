package jadex.publishservice.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContentNegotiator {

    private static final Pattern Q_PATTERN = Pattern.compile("q=([0-9\\.]+)");

    /**
     * Parses a raw Accept header string like:
     *   "text/plain;q=0.5, application/*;q=0.8"
     * into a sorted list of MediaTypeWithQ by descending preference.
     */
    public static List<MediaTypeWithQ> parseAcceptHeader(String rawHeader) {
        if (rawHeader == null || rawHeader.isBlank()) {
            return List.of(new MediaTypeWithQ("*/*", 1.0)); // no header means accept all
        }

        String[] parts = rawHeader.split(",");
        List<MediaTypeWithQ> parsed = new ArrayList<>();
        for (String part : parts) {
            String[] tokens = part.trim().split(";");
            String mediaType = tokens[0].trim().toLowerCase(Locale.ROOT);
            double q = 1.0;
            for (int i = 1; i < tokens.length; i++) {
                Matcher m = Q_PATTERN.matcher(tokens[i]);
                if (m.find()) {
                    try {
                        q = Double.parseDouble(m.group(1));
                    } catch (NumberFormatException ignored) {}
                }
            }
            if (q > 0.0) { // q=0 means explicitly not acceptable
                parsed.add(new MediaTypeWithQ(mediaType, q));
            }
        }

        parsed.sort(Comparator
                .comparingDouble(MediaTypeWithQ::getQ).reversed()
                .thenComparing(MediaTypeWithQ::specificity).reversed());
        return parsed;
    }

    /**
     * Negotiates the best media type given:
     *  - rawAcceptHeader: the raw Accept header string from client
     *  - mappingProduced: media types the endpoint declares it can produce
     *  - serverPreference: fallback/order preference among candidates (can be null)
     *
     * Returns the chosen media type, or empty if none is acceptable (i.e., should be 406).
     */
    public static Optional<String> negotiate(
            String rawAcceptHeader,
            Collection<String> mappingProduced,
            List<String> serverPreference
    ) {
        List<MediaTypeWithQ> acceptList = parseAcceptHeader(rawAcceptHeader);

        // normalize mapping produced types (lowercase + trimmed)
        List<String> produced = (mappingProduced == null)
                ? Collections.emptyList()
                : mappingProduced.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .map(s -> s.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toList());

        // predicate: does client accept this candidate based on Accept header (incl. wildcards)?
        Predicate<String> clientAccepts = mt -> {
            String lower = mt.toLowerCase(Locale.ROOT);
            for (MediaTypeWithQ entry : acceptList) {
                if (matches(entry.getMediaType(), lower)) {
                    return true;
                }
            }
            return false;
        };

        // 1. If there are declared produced types, try to match those first
        if (!produced.isEmpty()) {
            List<String> candidates = new ArrayList<>();

            // a) Direct matches respecting client preference order
            for (MediaTypeWithQ clientEntry : acceptList) {
                for (String prod : produced) {
                    if (matches(clientEntry.getMediaType(), prod) && !candidates.contains(prod)) {
                        candidates.add(prod);
                    }
                }
            }

            // b) If no direct match but wildcards allow, include produced types client would accept
            if (candidates.isEmpty()) {
                for (String prod : produced) {
                    if (clientAccepts.test(prod)) {
                        candidates.add(prod);
                    }
                }
            }

            // c) Order candidates by server preference if provided
            if (!candidates.isEmpty()) {
                if (serverPreference != null && !serverPreference.isEmpty()) {
                    for (String pref : serverPreference) {
                        String normPref = pref.toLowerCase(Locale.ROOT);
                        for (String candidate : candidates) {
                            if (candidate.equals(normPref)) {
                                return Optional.of(candidate);
                            }
                        }
                    }
                }
                // fallback: take first candidate (which is best by client preference)
                return Optional.of(candidates.get(0));
            }

            // d) No overlap between produced and Accept => no acceptable type
            return Optional.empty();
        } else {
            // 2. No declared produced types: follow client preference but use serverPreference as tiebreaker
        	 if (!acceptList.isEmpty()) {
                 List<String> clientTypes = acceptList.stream()
                         .map(MediaTypeWithQ::getMediaType)
                         .collect(Collectors.toList());

                 boolean clientHasWildcard = clientTypes.stream().anyMatch(t -> t.equals("*/*"));

                 // If client allows everything (wildcard) and we have server preference, use the first preferred
                 if (clientHasWildcard && serverPreference != null && !serverPreference.isEmpty()) {
                     return Optional.of(serverPreference.get(0).toLowerCase(Locale.ROOT));
                 }

                 // Otherwise, if serverPreference intersects with client types, pick according to serverPreference order
                 if (serverPreference != null && !serverPreference.isEmpty()) {
                     for (String pref : serverPreference) {
                         String normPref = pref.toLowerCase(Locale.ROOT);
                         for (String ct : clientTypes) {
                             if (matches(normPref, ct)) {
                                 return Optional.of(ct);
                             }
                         }
                     }
                 }

                 // fallback to top client preference
                 return Optional.of(clientTypes.get(0));
             }
        }	
        return Optional.of("*/*");
    }

    /** Matches media types with wildcard support (e.g., "application/*" or "* / *"). */
    private static boolean matches(String pattern, String actual) {
        if (pattern.equals("*/*")) return true;
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.indexOf('/'));
            return actual.startsWith(prefix + "/");
        }
        return pattern.equalsIgnoreCase(actual);
    }

    /** Simple holder for media type and its q-value. */
    public static class MediaTypeWithQ {
        private final String mediaType;
        private final double q;

        public MediaTypeWithQ(String mediaType, double q) {
            this.mediaType = mediaType;
            this.q = q;
        }

        public String getMediaType() {
            return mediaType;
        }

        public double getQ() {
            return q;
        }

        /** Specificity: exact > type/* > * / * */
        public int specificity() {
            if (mediaType.equals("*/*")) return 0;
            if (mediaType.endsWith("/*")) return 1;
            return 2;
        }
    }
}