package utils;

import java.util.regex.Pattern;

public class Validator {
    private static final Pattern ID_PATTERN = Pattern.compile("^[1-9]\\d*$");
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$");

    private Validator() {
    }

    public static boolean isValidId(String rawId) {
        if (rawId == null) {
            return false;
        }

        String value = rawId.trim();
        if (!ID_PATTERN.matcher(value).matches()) {
            return false;
        }

        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    public static boolean isValidId(int id) {
        return isValidId(String.valueOf(id));
    }

    public static boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidFriendship(String rawId1, String rawId2) {
        if (!isValidId(rawId1) || !isValidId(rawId2)) {
            return false;
        }

        return Integer.parseInt(rawId1.trim()) != Integer.parseInt(rawId2.trim());
    }

    public static boolean isValidFriendship(int id1, int id2) {
        return isValidId(id1) && isValidId(id2) && id1 != id2;
    }
}
