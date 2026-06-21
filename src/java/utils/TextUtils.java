package utils;

public class TextUtils {
    private TextUtils() {
    }

    public static String toCapital(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(value.length());
        boolean capitalizeNext = true;

        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isWhitespace(current) || current == '-' || current == '\'') {
                result.append(current);
                capitalizeNext = true;
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(current));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(current));
            }
        }

        return result.toString();
    }

    public static String normalizeName(String value) {
        if (value == null) {
            return "";
        }

        return toCapital(value.trim().replaceAll("\\s+", " "));
    }
}
