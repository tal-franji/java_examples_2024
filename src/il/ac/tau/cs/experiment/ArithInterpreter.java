package il.ac.tau.cs.experiment;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArithInterpreter {
    /*
     * We define a very simple language for calculations. It may contain:
     * Numbers - only positive integers - e.g. 123
     * parameters - like identifiers - alpha-numeric_ starting with letter - e.g. Xk_7
     * 4 binary operator + - * /
     * Note - no unary minus - if you want to write -X - write 0 - X
     *    No operator precedence  2*3+4 --> 14 (with precedence it is 10)
     *    and note the language is right associative 2*3+4+5 --> 2*(3+(4+5))
     * and parenthesis ( )
     * Whitespace is ignored.
     * The result of the calculation is a float.
     */
    
    static class InterpreterLocation {
        protected String sourceCode;
        protected Map<String, Float> params;
        protected int parseStart;
        protected int parseEnd;
        protected float interpretedValue;
        protected String lastMatch;

        public InterpreterLocation(String sourceCode, Map<String, Float> params) {
            this.sourceCode = sourceCode.replaceAll("\\s+",""); // no white-space
            this.params = params;
            parseStart = 0;
            parseEnd = -1;
        }

        protected boolean advanceIfMatch(String regex, boolean mustMatch, boolean advance) {
            Matcher matcher = Pattern.compile(regex).matcher(sourceCode.substring(parseStart));
            if (matcher.find()) {
                if (advance) {
                    lastMatch = matcher.group();
                    // we used sourceCode.substring for the match but we want
                    // parseEnd relative to the full sourceCode so we add parseStart
                    parseEnd = matcher.end() + parseStart;
                }
                return true;
            } else if (mustMatch) {
                throw new RuntimeException(String.format(
                        "Missing #%s# in location %d in code %s", regex, parseStart, sourceCode));
            }
            return false;
        }

        protected boolean advanceIfMatch(String regex) {
            return advanceIfMatch(regex, false, true);
        }

        protected void mustMatch(String regex) {
            advanceIfMatch(regex, true, true);
        }

        protected boolean matchAhead(String regex) {
            return advanceIfMatch(regex, false, false);
        }

        protected void next() {
            parseStart = parseEnd;
        }

        protected InterpreterLocation innerParser() {
            InterpreterLocation inner = new InterpreterLocation(sourceCode, params);
            inner.parseStart = parseEnd;
            inner.parseEnd = -1;
            return inner;
        }
        public static float calcOperator(float leftVal,
                                         String operator,
                                         float rightVal) {
            switch (operator) {
                case "+": return leftVal + rightVal;
                case "-": return leftVal - rightVal;
                case "*": return leftVal * rightVal;
                case "/": return leftVal / rightVal;
                default: throw new RuntimeException("Unknown operator " + operator);
            }
        }

        public float interpretRecursive() {
            var hasValue = false;
            float leftVal = 0.0F;
            if (advanceIfMatch("^\\d+")) {
                leftVal =  Float.parseFloat(lastMatch);
                hasValue = true;
                next();
            }
            if (!hasValue && advanceIfMatch("^[a-zA-Z_]\\w*")) {
                leftVal = params.get(lastMatch);
                hasValue = true;
                next();
            }
            if (!hasValue && advanceIfMatch("^\\(")) {
                var inner = innerParser();
                inner.interpretRecursive();
                leftVal = inner.interpretedValue;
                parseEnd = inner.parseEnd;
                next();
                mustMatch("^\\)");
                next();
            }
            // check for end of text or end of inner expression
            if (matchAhead("^(\\)|$)")) {
                interpretedValue = leftVal;
                return leftVal;
            }
            // Try to see if there is an operator
            if (advanceIfMatch("^[\\+\\-\\*/]")) {
                var operator = lastMatch;
                var inner = innerParser();
                inner.interpretRecursive();
                var rightVal = inner.interpretedValue;
                parseEnd = inner.parseEnd;
                next();
                var calcValue = calcOperator(leftVal, operator, rightVal);
                interpretedValue = calcValue;
                return calcValue;
            }

            throw new RuntimeException(String.format(
                    "Missing ')' of EOF in location %d in code %s", parseStart, sourceCode));
        }
    }

    public static float interpret(String source_code, Map<String, Float> params) {
        var location = new InterpreterLocation(source_code, params);
        return location.interpretRecursive();
    }

    public static void check(Float val, float expectedVal) {
        var epsilon = 1e-5;
        if (Math.abs(val-expectedVal) > epsilon) {
            throw new RuntimeException(String.format(
                    "ERROR expecting %f, got %f", expectedVal, val));
        }
    }

    public static void testInterpreter() {
        check(interpret("123", Map.of()), 123.0F);
        check(interpret("X", Map.of("X", 17.0F)), 17.0F);
        check(interpret("(123)", Map.of()), 123.0F);
        check(interpret("((123))", Map.of()), 123.0F);
        check(interpret("3+4", Map.of()), 7.0F);
        // Next two is an example of bad operator precedence in our language
        check(interpret("3*4+5", Map.of()), 27.0F);
        check(interpret("2*3+4+5", Map.of()), 24.0F);
        check(interpret("(2*3+4)+5", Map.of()), 19.0F);
        check(interpret("1 + x + (x*x/2) + (x*x*x/6) + (x*x*x*x/24) + (x*x*x*x*x/120)",
                Map.of("x", 1.0F)), 2.716667F);
        check(interpret("(x*x) + (2*x*y) + (y*y)",
                Map.of("x", 3.0F, "y", 4.0F)), 49.0F);
    }

    public static void profileInterpreter() {
        timeIt(() ->interpret("1 + x + (x*x/2) + (x*x*x/6) + (x*x*x*x/24) + (x*x*x*x*x/120)",
                Map.of("x", 1.0F)));
    }

    public static void timeIt(Runnable r) {
        // Function to measure execution time of the above solutions
        // to use in the above main call like this (for example)
        //    String output = Main.timeIt(Main::capitalize_by_word, input);
        long startTime = System.nanoTime();
        for (int i = 0; i < 1000000; i++) {
            r.run();
        }
        System.out.println("time msec = " + Long.toString((System.nanoTime() - startTime) / 1_000_000));
    }

    public static void main(String[] args) {
        testInterpreter();
        profileInterpreter();  // On my laptop: 18248 msec
    }
}