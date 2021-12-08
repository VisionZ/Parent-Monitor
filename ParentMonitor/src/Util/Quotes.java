package Util;

//dedicated class for quotes manipulation
public final class Quotes {
    
    public static final String DOUBLE_QUOTE = "\"";
    public static final String QUOTE = "'";
    
    private Quotes() {
        
    }
    
    /**
     * Surrounds a given {@link String} with double quotes and returns a new {@code String} with the 
     * given {@link String} argument surrounded on both ends by the double quote (") character.
     * @param str A given {@code String} argument.
     * @return A new String consisting of the given argument surrounded by the double quote (") character 
     * on both sides.
     */
    public static String surroundWithDoubleQuotes(String str) {
        return DOUBLE_QUOTE + str + DOUBLE_QUOTE;
    }
    
    /**
     * Surrounds a given {@link String} with single quotes and returns a new {@code String} with the 
     * given {@link String} argument surrounded on both ends by the single (') character.
     * @param str A given {@code String} argument.
     * @return A new String consisting of the given argument surrounded by the single quote (') character 
     * on both sides.
     */
    public static String surroundWithQuotes(String str) {
        return QUOTE + str + QUOTE;
    }
}