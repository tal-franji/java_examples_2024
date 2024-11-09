package il.ac.tau.cs.experiment;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Float.NaN;

public class ArithCompile {
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


    /**
     * ExecutableCode holds a "program" - a list of floats that represent operations
     * Some of the operations require a parameter - which is the float right after them
     * NO-OP= 0.0
     * PUSH(number)= 1.0,number  # pushes the value of X to stack
     * PLUS= 2.0 # takes top two floats, adds them and push back
     * MINUS= 3.0 # take the top and the next and push(next-top)
     * MUL= 4.0  # takes top two floats, multiply them and push back
     * DIV= 5.0  # take the top and the next and push(next/top)
     * SWAP= 6.0  # swap the top and next on the stack
     * DUP= 7.0  # Duplicates the top of stack
     * PARAM(i) = 8.0,i  # push the value of param whose name is the i-th in the list paramIndex
     * ZERO=9.0  # push 0.0
     * ONE=10.0  # push 1.0
     *
     * @param opCodes a list of opcodes and constant to executre
     * @param paramIndex a list of parmeter referenced by the source code
     */
    public record ExecutableCode(float[] opCodes, String[] paramIndex) {}

    static final int NOOP = 0;
    static final int PUSH = 1;
    static final int PLUS = 2;
    static final int MINUS = 3;
    static final int MUL = 4;
    static final int DIV = 5;
    static final int SWAP = 6;
    static final int DUP = 7;
    static final int PARAM = 8;
    static final int ZERO = 9;
    static final int ONE = 10;

    static public float run(ExecutableCode exec, Map<String, Float> params) {
      var stack = new Stack<Float>();
      var prevOp = 0.0F;
      for (var op: exec.opCodes) {
          float a, b;
          switch((int)prevOp) {
              case PUSH:  // PUSH(const_num)
                  stack.push(op /* the number */);
                  // op is the number to push
                  prevOp = 0.0F;
                  continue;
              case PARAM:  // push param value
                  var paramVal = params.get(exec.paramIndex[(int)op]);
                  stack.push(paramVal);
                  prevOp = 0.0F;
                  continue;
          }
          switch ((int) op) {
              case NOOP:
                  break;
              case PLUS:
                  a = stack.pop();
                  b = stack.pop();
                  stack.push(b + a);
                  break;
              case MINUS:
                  a = stack.pop();
                  b = stack.pop();
                  stack.push(b - a);
                  break;
              case MUL:
                  a = stack.pop();
                  b = stack.pop();
                  stack.push(b * a);
                  break;
              case DIV:
                  a = stack.pop();
                  b = stack.pop();
                  stack.push(b / a);
                  break;
              case SWAP:
                  a = stack.pop();
                  b = stack.pop();
                  stack.push(a);
                  stack.push(b);
                  break;
              case DUP:
                  a = stack.pop();
                  stack.push(a);
                  stack.push(a);
                  break;
              case ZERO:
                  stack.push(0.0F);
                  break;
              case ONE:
                  stack.push(1.0F);
                  break;
              case PUSH:
              case PARAM:
                  prevOp = op;
                  break;
              default:
                  throw new IllegalStateException("Unexpected value: " + (int) op);
          }
      }  // end for
        // result is the top of the stack
      var result = stack.pop();
      if (!stack.empty()) {
          throw new RuntimeException("INTERNAL ERROR - stack should be empty");
      }
      return result;
    }

    static class CompileLocation {
        protected String sourceCode;
        protected ArrayList<Float> opCodes = new ArrayList<>();
        protected ArrayList<String> paramIndex;  // reference to the global param indices
        protected int parseStart;
        protected int parseEnd;
        protected String lastMatch;

        public CompileLocation(String sourceCode) {
            this.sourceCode = sourceCode.replaceAll("\\s+",""); // no white-space
            this.paramIndex = new ArrayList<>();
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

        protected CompileLocation innerParser() {
            CompileLocation inner = new CompileLocation(sourceCode);
            inner.paramIndex = paramIndex;  // keep a reference to the same param array
            inner.parseStart = parseEnd;
            inner.parseEnd = -1;
            return inner;
        }

        public static float calcOperator(String operator) {
            return switch (operator) {
                case "+" -> 2.0F;
                case "-" -> 3.0F;
                case "*" -> 4.0F;
                case "/" -> 5.0F;
                default -> throw new RuntimeException("Unknown operator " + operator);
            };
        }

        protected int getOrAppendParamIndex(String paramName) {
            int index = paramIndex.indexOf(paramName);
            if (index >= 0) {
                return index;
            }
            paramIndex.add(paramName);
            return paramIndex.size() - 1;
        }

        public void compileRecursive() {
            var hasValue = false;
            float leftVal = 0.0F;
            if (advanceIfMatch("^\\d+")) {
                leftVal =  Float.parseFloat(lastMatch);
                opCodes.add(1.0F);
                opCodes.add(leftVal);
                hasValue = true;
                next();
            }
            if (!hasValue && advanceIfMatch("^[a-zA-Z_]\\w*")) {
                int index = getOrAppendParamIndex(lastMatch);
                opCodes.add((float)PARAM);
                opCodes.add((float)index);
                hasValue = true;
                next();
            }
            if (!hasValue && advanceIfMatch("^\\(")) {
                var inner = innerParser();
                inner.compileRecursive();
                opCodes.addAll(inner.opCodes);
                parseEnd = inner.parseEnd;
                next();
                mustMatch("^\\)");
                next();
            }
            // check for end of text or end of inner expression
            if (matchAhead("^(\\)|$)")) {
                return;
            }
            // Try to see if there is an operator
            if (advanceIfMatch("^[\\+\\-\\*/]")) {
                var operator = lastMatch;
                var inner = innerParser();
                inner.compileRecursive();
                opCodes.addAll(inner.opCodes);
                parseEnd = inner.parseEnd;
                next();
                opCodes.add(calcOperator(operator));
                return;
            }

            throw new RuntimeException(String.format(
                    "Missing ')' of EOF in location %d in code %s", parseStart, sourceCode));
        }
    }

    public static ExecutableCode compile(String source_code) {
        var location = new CompileLocation(source_code);
        location.compileRecursive();
        float[] opCodes = new float[location.opCodes.size()];
        for(int i = 0; i < opCodes.length; i++) {
            opCodes[i] = location.opCodes.get(i);
        }
        String[] paramIndex = new String[location.paramIndex.size()];
        for(int i = 0; i < paramIndex.length; i++) {
            paramIndex[i] = location.paramIndex.get(i);
        }
        return new ExecutableCode(opCodes, paramIndex);
    }

    public static float interpret(String source_code, Map<String, Float> params) {
        var exec = compile(source_code);
        return run(exec, params);
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
    }

    public static void profileInterpreter() {
        ArithInterpreter.timeIt(() ->interpret("1 + x + (x*x/2) + (x*x*x/6) + (x*x*x*x/24) + (x*x*x*x*x/120)",
                Map.of("x", 1.0F)));
    }

    protected static boolean repeatsPattern(List<Float> pattern, ArrayList<Float> opCodes, int offset) {
        int patternSize = pattern.size();
        if (offset + 2 * patternSize > opCodes.size()) {
            return false;  // cannot fit a repeat of the pattern
        }
        for (int i = 0; i < patternSize; i++) {
            float patternVal = pattern.get(i);
            float opCodeVal1 = opCodes.get(offset + i);
            float opCodeVal2 = opCodes.get(offset + patternSize + i);
            if (opCodeVal1 != opCodeVal2) {  // check that the code repeats twice
                return false;
            }
            if (!Float.isNaN(patternVal) && patternVal != opCodeVal1) {
                // Check that the code matches the patten. NaN is used as a wildcard
                return false;
            }
        }
        return true;
    }

    public static ExecutableCode optimize(ExecutableCode originalCode) {
        var opCodes = new ArrayList<Float>();
        for( var f : originalCode.opCodes) {
            opCodes.add(f);
        }
        int prevLen = opCodes.size();
        for(;;) {
            for (int i = 0; i < opCodes.size(); i++) {
                if (repeatsPattern(List.of((float)PARAM, NaN), opCodes, i)) {
                    // covert PARAM(X),PARAM(X) -> PARAM(X),DUP
                    opCodes.remove(i + 2);
                    opCodes.set(i + 2, (float)DUP);  // replace the next identical PARAM(i) with DUP
                    break;
                }
                if (repeatsPattern(List.of((float)PARAM, NaN, (float)DUP, (float)MUL), opCodes, i)) {
                    // convert PARAM(X),DUP.MUL,PARAM(X),DUP,MUL -> PARAM(X),DUP,MUL,DUP
                    opCodes.remove(i + 4);
                    opCodes.remove(i + 4);
                    opCodes.remove(i + 4);
                    opCodes.set(i + 4, (float)DUP);  // replace the next identical PARAM(i) with DUP
                    break;
                }
            }
            if (opCodes.size() == prevLen) {
                break;
            }
            prevLen = opCodes.size();
        }
        float[] newOpcodes = new float[opCodes.size()];
        for (int i = 0; i < opCodes.size(); i++) {
            newOpcodes[i] = opCodes.get(i);
        }
        return new ExecutableCode(newOpcodes, originalCode.paramIndex);
    }

    public static void profileCompiler() {
        var exec = compile("1 + x + (x*x/2) + (x*x*x/6) + (x*x*x*x/24) + (x*x*x*x*x/120)");
        ArithInterpreter.timeIt(() -> run(exec, Map.of("x", 1.0F)));
    }

    public static void profileCompilerOptimizer() {
        var originalExec = compile("1 + x + (x*x/2) + (x*x*x/6) + (x*x*x*x/24) + (x*x*x*x*x/120)");
        var exec = optimize(originalExec);
        ArithInterpreter.timeIt(() -> run(exec, Map.of("x", 1.0F)));
    }

    public static void main(String[] args) {
        testInterpreter();
        profileInterpreter();  // on my laptop 20198
        profileCompiler();  // on my laptop 511
        profileCompilerOptimizer(); // on my laptop 487 (5% ? that's it?)
    }
}