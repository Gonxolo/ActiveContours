package org.example;

public class CamelCaseConverter {

    public static String toCapitalized(String camelCase) {
        // Replacing all occurrences of a lowercase letter followed by an uppercase letter with a space and the uppercase letter
        String withSpaces = camelCase.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"), " ");
        // Capitalizing the first character of each word
        String[] words = withSpaces.split("\\s");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return result.toString().trim();
    }

}
