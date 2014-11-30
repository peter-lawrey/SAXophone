/*
 * Copyright 2014 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.saxophone.json;

import net.openhft.lang.model.constraints.NotNull;
import net.openhft.lang.model.constraints.Nullable;
import net.openhft.saxophone.json.handler.*;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import static net.openhft.saxophone.json.JsonParserTopLevelStrategy.ALLOW_JUST_A_SINGLE_OBJECT;

/**
 * A builder of {@link JsonParser}. To obtain a new builder, call {@link JsonParser#builder()} static method.
 *
 * Example usage: <pre>{@code
 * public final class PrettyPrinter
 *         implements ResetHook,
 *         ObjectStartHandler, ObjectEndHandler,
 *         ArrayStartHandler, ArrayEndHandler,
 *         ObjectKeyHandler, StringValueHandler,
 *         NumberHandler, BooleanHandler, NullHandler {
 *
 *     public static void main(String[] args) {
 *         PrettyPrinter prettyPrinter = new PrettyPrinter();
 *         JsonParser parser = JsonParser.builder()
 *                 .applyAdapter(prettyPrinter)
 *                 .options(JsonParserOption.ALLOW_COMMENTS)
 *                 .build();
 *         parser.parse(... /&#42; not formatted JSON as Bytes &#42;/);
 *         parser.finish();
 *         System.out.println(prettyPrinter);
 *     }
 *
 *     private int indent = 0;
 *     private int indentStep = 4;
 *     private StringBuilder sb = new StringBuilder();
 *
 *     &#64;Override
 *     public String toString() {
 *         return sb.toString();
 *     }
 *
 *     &#64;Override
 *     public void onReset() {
 *         indent = 0;
 *         sb.setLength(0);
 *     }
 *
 *     private PrettyPrinter printIndent() {
 *         for (int i = 0; i < indent; i++) {
 *             sb.append(' ');
 *         }
 *         return this;
 *     }
 *
 *     private PrettyPrinter newLine() {
 *         sb.append(System.lineSeparator());
 *         return this;
 *     }
 *
 *     private char last() {
 *         return sb.charAt(sb.length() - 1);
 *     }
 *
 *     private void beforeValue() {
 *         if (sb.length() == 0 || indent == 0)
 *             return;
 *         if (last() == ':') {
 *             sb.append(' ');
 *         } else {
 *             if (last() != '[')
 *                 sb.append(',');
 *             newLine().printIndent();
 *         }
 *     }
 *
 *     private boolean start(char open) {
 *         beforeValue();
 *         sb.append(open);
 *         indent += indentStep;
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public boolean onObjectStart() {
 *         return start('&#x7b;');
 *     }
 *
 *     &#64;Override
 *     public boolean onArrayStart() {
 *         return start('[');
 *     }
 *
 *     private boolean end(char open, char close) {
 *         indent -= indentStep;
 *         if (last() != open)
 *             newLine().printIndent();
 *         sb.append(close);
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public boolean onArrayEnd() {
 *         return end('[', ']');
 *     }
 *
 *     &#64;Override
 *     public boolean onObjectEnd() {
 *         return end('{', '}');
 *     }
 *
 *     &#64;Override
 *     public boolean onObjectKey(CharSequence key) {
 *         if (last() != '&#x7b;')
 *             sb.append(',');
 *         newLine().printIndent();
 *         sb.append('"').append(key).append('"').append(':');
 *         return true;
 *     }
 *
 *     private boolean onValue(CharSequence value, boolean quotes) {
 *         beforeValue();
 *         if (quotes) sb.append('"');
 *         sb.append(value);
 *         if (quotes) sb.append('"');
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public boolean onBoolean(boolean value) {
 *         return onValue(value ? "true" : "false", false);
 *     }
 *
 *     &#64;Override
 *     public boolean onNull() {
 *         return onValue("null", false);
 *     }
 *
 *     &#64;Override
 *     public boolean onNumber(CharSequence number) {
 *         return onValue(number, false);
 *     }
 *
 *     &#64;Override
 *     public boolean onStringValue(CharSequence value) {
 *         return onValue(value, true);
 *     }
 * }
 * }</pre>
 *
 * @see JsonParser#builder()
 */
public final class JsonParserBuilder {

    @NotNull
    private EnumSet<JsonParserOption> options = EnumSet.noneOf(JsonParserOption.class);
    @NotNull
    private JsonParserTopLevelStrategy topLevelStrategy = ALLOW_JUST_A_SINGLE_OBJECT;
    private boolean eachTokenMustBeHandled = true;
    @Nullable
    private ObjectStartHandler objectStartHandler = null;
    @Nullable
    private ObjectEndHandler objectEndHandler = null;
    @Nullable
    private ArrayStartHandler arrayStartHandler = null;
    @Nullable
    private ArrayEndHandler arrayEndHandler = null;
    @Nullable
    private BooleanHandler booleanHandler = null;
    @Nullable
    private NullHandler nullHandler = null;
    @Nullable
    private StringValueHandler stringValueHandler = null;
    @Nullable
    private ObjectKeyHandler objectKeyHandler = null;
    @Nullable
    private NumberHandler numberHandler = null;
    @Nullable
    private IntegerHandler integerHandler = null;
    @Nullable
    private FloatingHandler floatingHandler = null;
    @Nullable
    private ResetHook resetHook = null;

    JsonParserBuilder() {
    }

    /**
     * Builds and returns a new {@code JsonParser} with the configured options and handlers.
     *
     * <p>After construction a {@code JsonParser} don't depend on it's builder, so you can change the builder
     * state and construct a different parser.
     *
     * @return a newly built {@code JsonParser}
     */
    public JsonParser build() {
        checkAnyTokenHandlerNonNull();
        return new JsonParser(options, topLevelStrategy, eachTokenMustBeHandled,
                objectStartHandler, objectEndHandler, arrayStartHandler, arrayEndHandler,
                booleanHandler, nullHandler, stringValueHandler, objectKeyHandler, numberHandler,
                integerHandler, floatingHandler, resetHook);
    }

    private void checkAnyTokenHandlerNonNull() {
        if (objectStartHandler != null) return;
        if (objectEndHandler != null) return;
        if (arrayStartHandler != null) return;
        if (arrayEndHandler != null) return;
        if (booleanHandler != null) return;
        if (nullHandler != null) return;
        if (stringValueHandler != null) return;
        if (objectKeyHandler != null) return;
        if (numberHandler != null) return;
        if (integerHandler != null) return;
        if (floatingHandler != null) return;
        // intentionally without resetHook
        throw new IllegalStateException("Parser should have at least one JSON token handler");
    }

    /**
     * Returns the parser options as read-only set. Initially there are no options (the set is empty).
     *
     * @return the parser options as read-only set
     */
    public Set<JsonParserOption> options() {
        return Collections.unmodifiableSet(options);
    }

    /**
     * Sets the parser options. The previous options, if any, are discarded.
     *
     * @param options parser options
     * @return a reference to this builder
     */
    public JsonParserBuilder options(Collection<JsonParserOption> options) {
        this.options = EnumSet.copyOf(options);
        return this;
    }

    /**
     * Sets the parser options. The previous options, if any, are discarded.
     *
     * @param first parser option
     * @param rest  the rest parser options
     * @return a reference to this builder
     */
    public JsonParserBuilder options(JsonParserOption first, JsonParserOption... rest) {
        this.options = EnumSet.of(first, rest);
        return this;
    }

    /**
     * Clears an option set. After this call {@link #options()} returns an empty set.
     *
     * @return a reference to this builder
     */
    public JsonParserBuilder clearOptions() {
        options = EnumSet.noneOf(JsonParserOption.class);
        return this;
    }

    /**
     * Returns the parser's top-level strategy.
     *
     * @return the parser's top-level strategy
     */
    @NotNull
    public JsonParserTopLevelStrategy topLevelStrategy() {
        return topLevelStrategy;
    }

    /**
     * Sets the parser's top-level strategy
     *
     * @param topLevelStrategy a new top-level strategy
     * @return a reference to this builder
     */
    public JsonParserBuilder topLevelStrategy(
            @NotNull JsonParserTopLevelStrategy topLevelStrategy) {
        this.topLevelStrategy = topLevelStrategy;
        return this;
    }

    /**
     * Returns if each input JSON token must be handled, {@code true} by default.
     *
     * @return if each input JSON token must be handled
     */
    public boolean eachTokenMustBeHandled() {
        return eachTokenMustBeHandled;
    }

    /**
     * Sets if each input JSON token must be handled. If so, when the parser see the token, but the
     * corresponding handler is absent, {@code IllegalStateException} is thrown. Otherwise, this situation of
     * silently ignored. The profit of the latter behaviour - the parser can skip unneeded tokens to speed up.
     * But it is error-prone.
     *
     * @param eachTokenMustBeHandled if each input JSON token must be handled
     * @return a reference to this builder
     */
    public JsonParserBuilder eachTokenMustBeHandled(boolean eachTokenMustBeHandled) {
        this.eachTokenMustBeHandled = eachTokenMustBeHandled;
        return this;
    }

    /**
     * Convenient method to apply the adapter which implements several handler interfaces in one call.
     *
     * <p>Is equivalent to <pre>{@code
     * if (a instanceof ObjectStartHandler)
     *     objectStartHandler((ObjectStartHandler) a);
     * if (a instanceof ObjectEndHandler)
     *     objectEndHandler((ObjectEndHandler) a);
     * // ... the same job for the rest handler interfaces
     * }</pre>
     *
     * @param a the adapter - an object, implementing some of concrete handler interfaces
     * @return a reference to this builder
     * @throws java.lang.IllegalArgumentException if the adapter doesn't implement any of concrete handler
     *                                            interfaces
     */
    public JsonParserBuilder applyAdapter(JsonHandlerBase a) {
        boolean applied = false;
        if (a instanceof ObjectStartHandler) {
            objectStartHandler((ObjectStartHandler) a);
            applied = true;
        }
        if (a instanceof ObjectEndHandler) {
            objectEndHandler((ObjectEndHandler) a);
            applied = true;
        }
        if (a instanceof ArrayStartHandler) {
            arrayStartHandler((ArrayStartHandler) a);
            applied = true;
        }
        if (a instanceof ArrayEndHandler) {
            arrayEndHandler((ArrayEndHandler) a);
            applied = true;
        }
        if (a instanceof BooleanHandler) {
            booleanHandler((BooleanHandler) a);
            applied = true;
        }
        if (a instanceof NullHandler) {
            nullHandler((NullHandler) a);
            applied = true;
        }
        if (a instanceof StringValueHandler) {
            stringValueHandler((StringValueHandler) a);
            applied = true;
        }
        if (a instanceof ObjectKeyHandler) {
            objectKeyHandler((ObjectKeyHandler) a);
            applied = true;
        }
        if (a instanceof NumberHandler) {
            numberHandler((NumberHandler) a);
            applied = true;
        }
        if (a instanceof IntegerHandler) {
            integerHandler((IntegerHandler) a);
            applied = true;
        }
        if (a instanceof FloatingHandler) {
            floatingHandler((FloatingHandler) a);
            applied = true;
        }
        if (a instanceof ResetHook) {
            resetHook((ResetHook) a);
            applied = true;
        }
        if (!applied)
            throw new IllegalArgumentException(a + " isn't an instance of any handler interface");
        return this;
    }

    /**
     * Returns the parser's object start handler, or {@code null} if the handler is not set.
     *
     * @return the parser's object start handler, or {@code null} if the handler is not set
     */
    @Nullable
    public ObjectStartHandler objectStartHandler() {
        return objectStartHandler;
    }

    /**
     * Sets the parser's object start handler, or removes it if {@code null} is passed.
     *
     * @param objectStartHandler a new object start handler. {@code null} means there shouldn't be an object
     *                           start handler in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder objectStartHandler(@Nullable ObjectStartHandler objectStartHandler) {
        this.objectStartHandler = objectStartHandler;
        return this;
    }

    /**
     * Returns the parser's object end handler, or {@code null} if the handler is not set.
     *
     * @return the parser's object end handler, or {@code null} if the handler is not set
     */
    @Nullable
    public ObjectEndHandler objectEndHandler() {
        return objectEndHandler;
    }

    /**
     * Sets the parser's object end handler, or removes it if {@code null} is passed.
     *
     * @param objectEndHandler a new object end handler. {@code null} means there shouldn't be an object end
     *                         handler in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder objectEndHandler(@Nullable ObjectEndHandler objectEndHandler) {
        this.objectEndHandler = objectEndHandler;
        return this;
    }

    /**
     * Returns the parser's array start handler, or {@code null} if the handler is not set.
     *
     * @return the parser's array start handler, or {@code null} if the handler is not set
     */
    @Nullable
    public ArrayStartHandler arrayStartHandler() {
        return arrayStartHandler;
    }

    /**
     * Sets the parser's array start handler, or removes it if {@code null} is passed.
     *
     * @param arrayStartHandler a new array start handler. {@code null} means there shouldn't be an array
     *                          start handler in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder arrayStartHandler(@Nullable ArrayStartHandler arrayStartHandler) {
        this.arrayStartHandler = arrayStartHandler;
        return this;
    }

    /**
     * Returns the parser's array end handler, or {@code null} if the handler is not set.
     *
     * @return the parser's array end handler, or {@code null} if the handler is not set
     */
    @Nullable
    public ArrayEndHandler arrayEndHandler() {
        return arrayEndHandler;
    }

    /**
     * Sets the parser's array end handler, or removes it if {@code null} is passed.
     *
     * @param arrayEndHandler a new array end handler. {@code null} means there shouldn't be an array end
     *                        handler in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder arrayEndHandler(@Nullable ArrayEndHandler arrayEndHandler) {
        this.arrayEndHandler = arrayEndHandler;
        return this;
    }

    /**
     * Returns the parser's boolean value handler, or {@code null} if the handler is not set.
     *
     * @return the parser's boolean value handler, or {@code null} if the handler is not set
     */
    @Nullable
    public BooleanHandler booleanHandler() {
        return booleanHandler;
    }

    /**
     * Sets the parser's boolean value handler, or removes it if {@code null} is passed.
     *
     * @param booleanHandler a new boolean value handler. {@code null} means there shouldn't be a boolean
     *                       value handler in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder booleanHandler(@Nullable BooleanHandler booleanHandler) {
        this.booleanHandler = booleanHandler;
        return this;
    }

    /**
     * Returns the parser's null value handler, or {@code null} if the handler is not set.
     *
     * @return the parser's null value handler, or {@code null} if the handler is not set
     */
    @Nullable
    public NullHandler nullHandler() {
        return nullHandler;
    }

    /**
     * Sets the parser's null value handler, or removes it if {@code null} is passed.
     *
     * @param nullHandler a new null value handler. {@code null} means there shouldn't be a null value handler
     *                    in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder nullHandler(@Nullable NullHandler nullHandler) {
        this.nullHandler = nullHandler;
        return this;
    }

    /**
     * Returns the parser's string value handler, or {@code null} if the handler is not set.
     *
     * @return the parser's string value handler, or {@code null} if the handler is not set
     */
    @Nullable
    public StringValueHandler stringValueHandler() {
        return stringValueHandler;
    }

    /**
     * Sets the parser's string value handler, or removes it if {@code null} is passed.
     *
     * @param stringValueHandler a new string value handler. {@code null} means there shouldn't be a string
     *                           value handler in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder stringValueHandler(@Nullable StringValueHandler stringValueHandler) {
        this.stringValueHandler = stringValueHandler;
        return this;
    }

    /**
     * Returns the parser's object key handler, or {@code null} if the handler is not set.
     *
     * @return the parser's object key handler, or {@code null} if the handler is not set
     */
    @Nullable
    public ObjectKeyHandler objectKeyHandler() {
        return objectKeyHandler;
    }

    /**
     * Sets the parser's object key handler, or removes it if {@code null} is passed.
     *
     * @param objectKeyHandler a new object key handler. {@code null} means there shouldn't be an object key
     *                         handler in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder objectKeyHandler(@Nullable ObjectKeyHandler objectKeyHandler) {
        this.objectKeyHandler = objectKeyHandler;
        return this;
    }

    /**
     * Returns the parser's number value handler, or {@code null} if the handler is not set.
     *
     * @return the parser's number value handler, or {@code null} if the handler is not set
     */
    @Nullable
    public NumberHandler numberHandler() {
        return numberHandler;
    }

    /**
     * Sets the parser's number value handler, or removes it if {@code null} is passed.
     *
     * @param numberHandler a new number value handler. {@code null} means there shouldn't be a number value
     *                      handler in the built parser.
     * @return a reference to this builder
     * @throws java.lang.IllegalStateException if {@link #integerHandler()} or {@link #floatingHandler()} is
     *                                         already set
     */
    public JsonParserBuilder numberHandler(@Nullable NumberHandler numberHandler) {
        checkNoConflict(integerHandler, "integer", numberHandler, "number");
        checkNoConflict(floatingHandler, "floating", numberHandler, "number");
        this.numberHandler = numberHandler;
        return this;
    }

    /**
     * Returns the parser's integer value handler, or {@code null} if the handler is not set.
     *
     * @return the parser's integer value handler, or {@code null} if the handler is not set
     */
    @Nullable
    public IntegerHandler integerHandler() {
        return integerHandler;
    }

    /**
     * Sets the parser's integer value handler, or removes it if {@code null} is passed.
     *
     * @param integerHandler a new integer value handler. {@code null} means there shouldn't be a integer
     *                       value handler in the built parser.
     * @return a reference to this builder
     * @throws java.lang.IllegalStateException if {@link #numberHandler()} is already set
     */
    public JsonParserBuilder integerHandler(@Nullable IntegerHandler integerHandler) {
        checkNoConflict(integerHandler, "integer", numberHandler, "number");
        this.integerHandler = integerHandler;
        return this;
    }

    /**
     * Returns the parser's floating value handler, or {@code null} if the handler is not set.
     *
     * @return the parser's floating value handler, or {@code null} if the handler is not set
     */
    @Nullable
    public FloatingHandler floatingHandler() {
        return floatingHandler;
    }

    /**
     * Sets the parser's floating value handler, or removes it if {@code null} is passed.
     *
     * @param floatingHandler a new floating value handler. {@code null} means there shouldn't be a floating
     *                        value handler in the built parser.
     * @return a reference to this builder
     * @throws java.lang.IllegalStateException if {@link #numberHandler()} is already set
     */
    public JsonParserBuilder floatingHandler(@Nullable FloatingHandler floatingHandler) {
        checkNoConflict(floatingHandler, "floating", numberHandler, "number");
        this.floatingHandler = floatingHandler;
        return this;
    }

    private void checkNoConflict(Object h1, String name1, Object h2, String name2) {
        if (h1 != null && h2 != null)
            throw new IllegalStateException(
                    "Parser cannot have " + name1 + " and " + name2 + " handlers simultaneously");
    }

    /**
     * Returns the parser's {@link JsonParser#reset() reset} hook, or {@code null} if the hook is not set.
     *
     * @return the parser's {@link JsonParser#reset() reset} hook, or {@code null} if the hook is not set
     */
    @Nullable
    public ResetHook resetHook() {
        return resetHook;
    }

    /**
     * Sets the parser's {@link JsonParser#reset() reset} hook, or removes it if {@code null} is passed.
     *
     * @param resetHook a new {@link JsonParser#reset() reset} hook. {@code null} means there shouldn't be a
     *                  {@link JsonParser#reset() reset} hook in the built parser.
     * @return a reference to this builder
     */
    public JsonParserBuilder resetHook(@Nullable ResetHook resetHook) {
        this.resetHook = resetHook;
        return this;
    }
}
