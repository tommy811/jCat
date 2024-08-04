package org.coderead.jcat.common;


import java.util.regex.Pattern;

/**
 * 来自于jacoco 源码
 */
public class WildcardMatcher {

    private final Pattern pattern;

    /**
     * Creates a new matcher with the given expression.
     *
     * @param expression wildcard expressions
     */
    public WildcardMatcher(final String expression) {
        final String[] parts = expression.split("&");
        final StringBuilder regex = new StringBuilder(expression.length() * 2);
        boolean next = false;
        for (final String part : parts) {
            if (next) {
                regex.append('|');
            }
            regex.append('(').append(toRegex(part)).append(')');
            next = true;
        }
        pattern = Pattern.compile(regex.toString());
    }

    private static CharSequence toRegex(final String expression) {
        final StringBuilder regex = new StringBuilder(expression.length() * 2);
        for (final char c : expression.toCharArray()) {
            switch (c) {
                case '?':
                    regex.append(".?");
                    break;
                case '*':
                    regex.append(".*");
                    break;
                default:
                    regex.append(Pattern.quote(String.valueOf(c)));
                    break;
            }
        }
        return regex;
    }

    /**
     * Matches the given string against the expressions of this matcher.
     *
     * @param s string to test
     * @return <code>true</code>, if the expression matches
     */
    public boolean matches(final String s) {
        return pattern.matcher(s).matches();
    }

//    public static Predicate<String> build(String expression) {
//        WildcardMatcher matcher = new WildcardMatcher(expression);
//        return a -> matcher.matches(a);
//    }
//
//    public static Predicate<String> build(String includeExpr, String excludeExpr) {
//        Assert.isTrue(includeExpr != null || excludeExpr != null, "参数 includeExpr 或 excludeExpr 至少要有一个不为空");
//
//        WildcardMatcher matcher = new WildcardMatcher(includeExpr == null ? "*" : includeExpr);
//        WildcardMatcher excludeMatcher = new WildcardMatcher(excludeExpr == null ? "" : excludeExpr);
//        return a -> matcher.matches(a) && !excludeMatcher.matches(a);
//    }
}