/*
 * Copyright 2007-2014, Lloyd Hilaiel <me@lloyd.io> and YAJL contributors
 * Copyright 2014 Higher Frequency Trading
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.saxophone.json;

import net.openhft.lang.io.ByteBufferBytes;
import net.openhft.lang.io.Bytes;
import net.openhft.lang.model.constraints.Nullable;
import net.openhft.saxophone.ParseException;
import net.openhft.saxophone.json.handler.*;

import java.nio.ByteBuffer;
import java.util.EnumSet;

import static net.openhft.saxophone.json.JsonParserOption.*;
import static net.openhft.saxophone.json.JsonParserTopLevelStrategy.ALLOW_MULTIPLE_VALUES;
import static net.openhft.saxophone.json.JsonParserTopLevelStrategy.ALLOW_TRAILING_GARBAGE;
import static net.openhft.saxophone.json.ParserState.*;
import static net.openhft.saxophone.json.TokenType.*;

/**
 * A pull JSON parser, accepts chunks of JSON as {@link net.openhft.lang.io.Bytes}.
 *
 * <p>Construction: <pre>{@code
 * Parser.builder().applyAdapter(eventHandler).options(...).build();
 * }</pre>
 *
 * <p><a name="reuse"></a>
 * Try to reuse the parser, because it's allocation cost is pretty high. It's safe and valid to
 * use the parser just like newly created one after a {@link #reset()} call.
 * To reset handlers' state on parser reset, implement and provide
 * {@link net.openhft.saxophone.json.handler.ResetHook}.
 *
 * Example usage: <pre>{@code
 * class Foo {
 *     JsonParser parser = ...;
 *
 *     // return true if parsing succeed
 *     boolean parse(Iterator<Bytes> chunks) {
 *         try {
 *             while (chunks.hasNext()) {
 *                 if (!parser.parse(chunks.next())) {
 *                     parser.reset();
 *                     return false;
 *                 }
 *             }
 *             return parser.finish();
 *         } finally {
 *             parser.reset();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>If you want to recover {@link net.openhft.saxophone.ParseException}: <pre>{@code
 * ...
 * boolean parse(Iterator<Bytes> chunks) throws IOException {
 *     try {
 *         try {
 *             while (chunks.hasNext()) {
 *                 if (!parser.parse(chunks.next())) {
 *                     parser.reset();
 *                     return false;
 *                 }
 *             }
 *             return parser.finish();
 *         } catch (ParseException e) {
 *             Throwable cause = e.getCause();
 *             if (cause instanceof IOException) {
 *                 throw (IOException) cause;
 *             } else {
 *                 System.err.println("Json is malformed!");
 *                 return false;
 *             }
 *         }
 *     } finally {
 *         parser.reset();
 *     }
 * }
 * }</pre>
 *
 * @see #builder()
 */
public final class JsonParser {

    /**
     * Porting note: this class approximately corresponds to src/yajl_parser.c, src/yajl_parser.h
     * and src/yajl.c in YAJL.
     *
     * I tried to preserve method order and names to ease side-to-side comparison.
     */

    /**
     * Return a new parser builder.
     *
     * <p>There are no public constructors of {@code JsonParser} at the moment, and likely won't
     * be, so configuring a builder and then call {@link JsonParserBuilder#build()}
     * is the only way to construct the parser.
     *
     * @return a new parser builder
     */
    public static JsonParserBuilder builder() {
        return new JsonParserBuilder();
    }

    private final Lexer lexer;
    String parseError;
    /** temporary storage for decoded strings */
    private final StringBuilder decodeBuf = new StringBuilder();
    private final ParserState.Stack stateStack;
    private final EnumSet<JsonParserOption> flags;
    private final JsonParserTopLevelStrategy topLevelStrategy;
    private final boolean eachTokenMustBeHandled;
    private Bytes finishSpace;

    @Nullable private final ObjectStartHandler objectStartHandler;
    @Nullable private final ObjectEndHandler objectEndHandler;
    @Nullable private final ArrayStartHandler arrayStartHandler;
    @Nullable private final ArrayEndHandler arrayEndHandler;
    @Nullable private final BooleanHandler booleanHandler;
    @Nullable private final NullHandler nullHandler;
    @Nullable private final StringValueHandler stringValueHandler;
    @Nullable private final ObjectKeyHandler objectKeyHandler;
    @Nullable private final NumberHandler numberHandler;
    @Nullable private final IntegerHandler integerHandler;
    @Nullable private final FloatingHandler floatingHandler;
    @Nullable private final ResetHook resetHook;

    private class OnString {
        boolean on() throws Exception {
            Bytes buf = lexer.outBuf;
            long bufPos = lexer.outPos;
            long bufLen = lexer.outLen;
            boolean borrowBuf = buf.position() != bufPos ||
                    buf.remaining() != bufLen;
            long pos = 0, lim = 0;
            if (borrowBuf) {
                pos = buf.position();
                lim = buf.limit();
                buf.clear();
                buf.position(bufPos);
                buf.limit(bufPos + bufLen);
            }
            boolean go = apply(value(buf));
            if (borrowBuf) {
                buf.clear();
                buf.position(pos);
                buf.limit(lim);
            }
            return go;
        }

        CharSequence value(Bytes buf) {
            return buf;
        }

        boolean apply(CharSequence value) throws Exception {
            assert stringValueHandler != null;
            return stringValueHandler.onStringValue(value);
        }
    }

    private class OnEscapedString extends OnString {
        @Override
        CharSequence value(Bytes buf) {
            decodeBuf.setLength(0);
            Unescaper.decode(decodeBuf, buf);
            return decodeBuf;
        }
    }

    private boolean applyKey(CharSequence key) throws Exception {
        assert objectKeyHandler != null;
        return objectKeyHandler.onObjectKey(key);
    }

    private class OnKey extends OnString {
        @Override
        boolean apply(CharSequence key) throws Exception {
            return applyKey(key);
        }
    }

    private class OnEscapedKey extends OnEscapedString {
        @Override
        boolean apply(CharSequence key) throws Exception {
            return applyKey(key);
        }
    }

    private class OnNumber extends OnString {
        @Override
        boolean apply(CharSequence number) throws Exception {
            assert numberHandler != null;
            return numberHandler.onNumber(number);
        }
    }

    private class OnFloating extends OnString {
        @Override
        boolean apply(CharSequence number) throws Exception {
            assert floatingHandler != null;
            // TODO optimize, get rid of toString() conversion
            return floatingHandler.onFloating(Double.parseDouble(number.toString()));
        }
    }

    private final OnString onString = new OnString();
    private final OnEscapedString onEscapedString = new OnEscapedString();
    private final OnKey onKey = new OnKey();
    private final OnEscapedKey onEscapedKey = new OnEscapedKey();
    private final OnNumber onNumber = new OnNumber();
    private final OnFloating onFloating = new OnFloating();


    JsonParser(EnumSet<JsonParserOption> flags,
               JsonParserTopLevelStrategy topLevelStrategy,
               boolean eachTokenMustBeHandled,
               @Nullable ObjectStartHandler objectStartHandler,
               @Nullable ObjectEndHandler objectEndHandler,
               @Nullable ArrayStartHandler arrayStartHandler,
               @Nullable ArrayEndHandler arrayEndHandler,
               @Nullable BooleanHandler booleanHandler,
               @Nullable NullHandler nullHandler,
               @Nullable StringValueHandler stringValueHandler,
               @Nullable ObjectKeyHandler objectKeyHandler,
               @Nullable NumberHandler numberHandler,
               @Nullable IntegerHandler integerHandler,
               @Nullable FloatingHandler floatingHandler,
               @Nullable ResetHook resetHook) {
        this.flags = flags;
        this.topLevelStrategy = topLevelStrategy;
        this.eachTokenMustBeHandled = eachTokenMustBeHandled;
        this.objectStartHandler = objectStartHandler;
        this.objectEndHandler = objectEndHandler;
        this.arrayStartHandler = arrayStartHandler;
        this.arrayEndHandler = arrayEndHandler;
        this.booleanHandler = booleanHandler;
        this.nullHandler = nullHandler;
        this.stringValueHandler = stringValueHandler;
        this.objectKeyHandler = objectKeyHandler;
        this.numberHandler = numberHandler;
        this.integerHandler = integerHandler;
        this.floatingHandler = floatingHandler;
        this.resetHook = resetHook;

        lexer = new Lexer(flags.contains(ALLOW_COMMENTS), !flags.contains(DONT_VALIDATE_STRINGS));
        stateStack = new Stack();
        reset();
    }

    /**
     * Resets the parser to clear "just like after construction" state.
     *
     * <p>At the end {@link net.openhft.saxophone.json.handler.ResetHook}, if provided,
     * is notified.
     *
     * <p>Reusing parsers is encouraged because parser allocation / garbage collection
     * is pretty costly.
     */
    public void reset() {
        lexer.reset();
        stateStack.clear();
        stateStack.push(START);
        parseError = null;
        if (resetHook != null)
            resetHook.onReset();
    }

    private long parseInteger(Bytes s, long off, long len) {
        long lim = off + len;
        boolean neg = false;
        int cutLim = (int) ((Long.MAX_VALUE) % 10);
        int first;
        long ret = (first = s.readByte(off)) - '0';
        if (ret < 0) {
            assert first == '-';
            neg = true;
            cutLim += 1;
            off++;
            ret = s.readByte(off) - '0';
        } else if (ret == 0) {
            assert len == 1;
            return 0;
        } else {
            assert ret > 0 && ret <= 9;
        }
        off++;
        ret = -ret;
        long cutoff = (-Long.MAX_VALUE) / 10;
        while (off < lim) {
            int c = s.readByte(off++) - '0';
            assert(0 <= c && c <= 9);
            if (ret < cutoff || (ret == cutoff && c > cutLim)) {
                throw new NumberFormatException();
            }
            ret = 10 * ret - c;
        }
        return !neg ? -ret : ret;
    }

    /**
     * Processes the last token.
     *
     * <p>If the JSON data portions given to {@link #parse(net.openhft.lang.io.Bytes)} method
     * by now since the previous {@link #reset()} call, or call of this method,
     * or parser construction don't comprise one or several complete JSON entities, and
     * {@link net.openhft.saxophone.json.JsonParserOption#ALLOW_PARTIAL_VALUES} is not set,
     * {@link net.openhft.saxophone.ParseException} is thrown.
     *
     * @return {@code true} if parsing successfully finished, {@code false}, if the handler
     *         of the last token cancelled parsing
     * @throws net.openhft.saxophone.ParseException if the given JSON is malformed (exact meaning of
     *         "malformed" depends on parser's {@link JsonParserBuilder#options() options}),
     *         or if the handler of the last token, if it was actually processed during this call,
     *         have thrown a checked exception,
     *         or if {@link net.openhft.saxophone.json.handler.IntegerHandler} is provided,
     *         the last token is an integer and it is out of primitive {@code long} range:
     *         greater than {@code Long.MAX_VALUE} or lesser than {@code Long.MIN_VALUE}
     * @throws IllegalStateException if parsing was cancelled or any exception was thrown
     *         in {@link #parse(net.openhft.lang.io.Bytes)} call after
     *         the previous {@link #reset()} call or parser construction
     */
    public boolean finish() {
        if (!parse(finishSpace())) return false;

        switch(stateStack.current()) {
            case PARSE_ERROR:
            case LEXICAL_ERROR:
            case HANDLER_CANCEL:
            case HANDLER_EXCEPTION:
                throw new AssertionError("exception should be thrown directly from parse()");
            case GOT_VALUE:
            case PARSE_COMPLETE:
                return true;
            default:
                return flags.contains(ALLOW_PARTIAL_VALUES) || parseError("premature EOF");
        }
    }

    private Bytes finishSpace() {
        if (finishSpace == null) {
            return finishSpace = new ByteBufferBytes(ByteBuffer.wrap(new byte[] {' '}));
        }
        return finishSpace.clear();
    }

    /**
     * Parses a portion of JSON from the given {@code Bytes} from it's
     * {@link net.openhft.lang.io.Bytes#position() position} to
     * {@link net.openhft.lang.io.Bytes#limit() limit}. Position is incremented until there are
     * no {@link net.openhft.lang.io.Bytes#remaining() remaining} bytes or a single top-level
     * JSON object is parsed and {@link JsonParserTopLevelStrategy#ALLOW_TRAILING_GARBAGE} is set.
     *
     * <p>As this is a pull parser, the given JSON text may break at any character. If the
     * {@link net.openhft.saxophone.json.JsonParserOption#ALLOW_PARTIAL_VALUES} is not set,
     * {@link net.openhft.saxophone.ParseException} will be thrown only on {@link #finish()} call.
     *
     * <p>As JSON tokens are parsed, appropriate event handlers are notified. To ensure that the
     * last token is processed, call {@link #finish()} afterwards.
     *
     * <p>Returns {@code true} if the parsing succeed and token handlers haven't send a cancel
     * request (i. e. returned {@code true} all the way). If any handler returns {@code false},
     * parsing is terminated immediately and {@code false} is returned.
     *
     * <p>If one of the handlers throws a checked exception, it is wrapped with
     * {@link net.openhft.saxophone.ParseException} (and available through
     * {@link net.openhft.saxophone.ParseException#getCause()} later). If one of the handlers throws
     * an unchecked exception ({@link java.lang.RuntimeException}),
     * it is rethrown without any processing.
     *
     * @param jsonText a portion of JSON to parse
     * @return {@code true} if the parsing wasn't cancelled by handlers
     * @throws net.openhft.saxophone.ParseException if the given JSON is malformed (exact meaning of
     *         "malformed" depends on parser's {@link JsonParserBuilder#options() options}),
     *         or if one of the handlers throws a checked exception,
     *         or {@link net.openhft.saxophone.json.handler.IntegerHandler} is provided but parsed
     *         integer value is out of primitive {@code long} range:
     *         greater than {@code Long.MAX_VALUE} or lesser than {@code Long.MIN_VALUE}
     * @throws IllegalStateException if parsing was cancelled or any exception was thrown
     *         in this method after the previous {@link #reset()} call or parser construction,
     *         or if the input JSON contains a token without corresponding handler assigned,
     *         and {@link JsonParserBuilder#eachTokenMustBeHandled()} is set to {@code true}.
     */
    public boolean parse(Bytes jsonText) {
        TokenType tok;

        long startOffset = jsonText.position();

        around_again:
        while (true) {
            switch (stateStack.current()) {
                case PARSE_COMPLETE:
                    if (topLevelStrategy == ALLOW_MULTIPLE_VALUES) {
                        stateStack.set(GOT_VALUE);
                        continue around_again;
                    }
                    if (topLevelStrategy != ALLOW_TRAILING_GARBAGE) {
                        if (jsonText.remaining() > 0) {
                            tok = lexer.lex(jsonText);
                            if (tok != EOF) {
                                return parseError("trailing garbage");
                            }
                            continue around_again;
                        }
                    }
                    return true;
                case LEXICAL_ERROR:
                case PARSE_ERROR:
                    throw new IllegalStateException("parse exception occurred: " + parseError);
                case HANDLER_CANCEL:
                    throw new IllegalStateException("client cancelled parse via handler");
                case HANDLER_EXCEPTION:
                    throw new IllegalStateException("handler threw an exception");
                case START:
                case GOT_VALUE:
                case MAP_NEED_VAL:
                case ARRAY_NEED_VAL:
                case ARRAY_START: {
                    /* for arrays and maps, we advance the state for this
                     * depth, then push the state of the next depth.
                     * If an error occurs during the parsing of the nesting
                     * entity, the state at this level will not matter.
                     * a state that needs pushing will be anything other
                     * than state_start */

                    byte stateToPush = START;

                    tok = lexer.lex(jsonText);

                    switch (tok) {
                        case EOF:
                            return true;
                        case ERROR:
                            lexicalError();
                        case STRING:
                            if (stringValueHandler != null) {
                                try {
                                    if (!onString.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            break;
                        case STRING_WITH_ESCAPES:
                            if (stringValueHandler != null) {
                                try {
                                    if (!onEscapedString.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(STRING);
                            }
                            break;
                        case BOOL:
                            if (booleanHandler != null) {
                                boolean value = lexer.outBuf.readUnsignedByte(lexer.outPos) == 't';
                                try {
                                    if (!booleanHandler.onBoolean(value)) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            break;
                        case NULL:
                            if (nullHandler != null) {
                                try {
                                    if (!nullHandler.onNull()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            break;
                        case LEFT_BRACKET:
                            if (objectStartHandler != null) {
                                try {
                                    if (!objectStartHandler.onObjectStart()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            stateToPush = MAP_START;
                            break;
                        case LEFT_BRACE:
                            if (arrayStartHandler != null) {
                                try {
                                    if (!arrayStartHandler.onArrayStart()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            stateToPush = ARRAY_START;
                            break;
                        case INTEGER:
                            if (numberHandler != null) {
                                try {
                                    if (!onNumber.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else if (integerHandler != null) {
                                try {
                                    long i = parseInteger(lexer.outBuf, lexer.outPos, lexer.outLen);
                                    if (!integerHandler.onInteger(i)) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (NumberFormatException e) {
                                    tryRestoreErrorEffect(jsonText, startOffset);
                                    return parseError("integer overflow", e);
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            break;
                        case DOUBLE:
                            if (numberHandler != null) {
                                try {
                                    if (!onNumber.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else if (floatingHandler != null) {
                                try {
                                    if (!onFloating.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (NumberFormatException e) {
                                    tryRestoreErrorEffect(jsonText, startOffset);
                                    return parseError("numeric (floating point) overflow", e);
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            break;
                        case RIGHT_BRACE: {
                            if (stateStack.current() == ARRAY_START) {
                                if (arrayEndHandler != null) {
                                    try {
                                        if (!arrayEndHandler.onArrayEnd()) {
                                            stateStack.set(HANDLER_CANCEL);
                                            return false;
                                        }
                                    } catch (Exception e) {
                                        return handlerError(e);
                                    }
                                } else {
                                    checkTokenCouldBePassed(tok);
                                }
                                stateStack.pop();
                                continue around_again;
                            }
                            /* intentional fall-through */
                        }
                        case COLON:
                        case COMMA:
                        case RIGHT_BRACKET:
                            return parseError("unallowed token at this point in JSON text");
                        default:
                            return parseError("invalid token, internal error");
                    }
                    /* got a value.  transition depends on the state we're in. */
                    {
                        byte s = stateStack.current();
                        if (s == START || s == GOT_VALUE) {
                            stateStack.set(PARSE_COMPLETE);
                        } else if (s == MAP_NEED_VAL) {
                            stateStack.set(MAP_GOT_VAL);
                        } else {
                            stateStack.set(ARRAY_GOT_VAL);
                        }
                    }
                    if (stateToPush != START) {
                        stateStack.push(stateToPush);
                    }

                    continue around_again;
                }
                case MAP_START:
                case MAP_NEED_KEY: {
                    /* only difference between these two states is that in
                     * start '}' is valid, whereas in need_key, we've parsed
                     * a comma, and a string key _must_ follow */
                    tok = lexer.lex(jsonText);
                    OnString onKey = this.onKey;
                    switch (tok) {
                        case EOF:
                            return true;
                        case ERROR:
                            lexicalError();
                        case STRING_WITH_ESCAPES:
                            onKey = onEscapedKey;
                            /* intentional fall-through */
                        case STRING:
                            if (objectKeyHandler != null) {
                                try {
                                    if (!onKey.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(STRING);
                            }
                            stateStack.set(MAP_SEP);
                            continue around_again;
                        case RIGHT_BRACKET:
                            if (stateStack.current() == MAP_START) {
                                if (objectEndHandler != null) {
                                    try {
                                        if (!objectEndHandler.onObjectEnd()) {
                                            stateStack.set(HANDLER_CANCEL);
                                            return false;
                                        }
                                    } catch (Exception e) {
                                        return handlerError(e);
                                    }
                                } else {
                                    checkTokenCouldBePassed(tok);
                                }
                                stateStack.pop();
                                continue around_again;
                            }
                            /* intentional fall-through */
                        default:
                            return parseError("invalid object key (must be a string)");
                    }
                }
                case MAP_SEP: {
                    tok = lexer.lex(jsonText);
                    switch (tok) {
                        case COLON:
                            stateStack.set(MAP_NEED_VAL);
                            continue around_again;
                        case EOF:
                            return true;
                        case ERROR:
                            lexicalError();
                        default:
                            return parseError(
                                    "object key and value must be separated by a colon (':')");
                    }
                }
                case MAP_GOT_VAL: {
                    tok = lexer.lex(jsonText);
                    switch (tok) {
                        case RIGHT_BRACKET:
                            if (objectEndHandler != null) {
                                try {
                                    if (!objectEndHandler.onObjectEnd()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            stateStack.pop();
                            continue around_again;
                        case COMMA:
                            stateStack.set(MAP_NEED_KEY);
                            continue around_again;
                        case EOF:
                            return true;
                        case ERROR:
                            lexicalError();
                        default:
                            tryRestoreErrorEffect(jsonText, startOffset);
                            return parseError(
                                    "after key and value, inside map, I expect ',' or '}'");
                    }
                }
                case ARRAY_GOT_VAL: {
                    tok = lexer.lex(jsonText);
                    switch (tok) {
                        case RIGHT_BRACE:
                            if (arrayEndHandler != null) {
                                try {
                                    if (!arrayEndHandler.onArrayEnd()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (Exception e) {
                                    return handlerError(e);
                                }
                            } else {
                                checkTokenCouldBePassed(tok);
                            }
                            stateStack.pop();
                            continue around_again;
                        case COMMA:
                            stateStack.set(ARRAY_NEED_VAL);
                            continue around_again;
                        case EOF:
                            return true;
                        case ERROR:
                            return lexicalError();
                        default:
                            return parseError("after array element, I expect ',' or ']'");
                    }
                }
            }
        }
    }

    private boolean handlerError(Exception e) {
        stateStack.set(HANDLER_EXCEPTION);
        if (e instanceof RuntimeException) throw (RuntimeException) e;
        throw new ParseException("Exception in the handler", e);
    }

    private boolean parseError(String message) {
        stateStack.set(PARSE_ERROR);
        parseError = message;
        throw new ParseException(parseError);
    }

    private boolean parseError(String message, Throwable cause) {
        stateStack.set(PARSE_ERROR);
        parseError = message;
        throw new ParseException(parseError, cause);
    }

    private boolean lexicalError() {
        stateStack.set(LEXICAL_ERROR);
        parseError = "lexical error: " + lexer.error;
        throw new ParseException(parseError);
    }

    private void checkTokenCouldBePassed(TokenType token) {
        if (eachTokenMustBeHandled) {
            stateStack.set(PARSE_ERROR);
            parseError = token +
                    " occurred in the input JSON, but corresponding handler is not assigned";
            throw new IllegalStateException(parseError);
        }
    }

    private void tryRestoreErrorEffect(Bytes jsonText, long startOffset) {
        long pos = jsonText.position();
        if (pos - startOffset >= lexer.outLen) {
            jsonText.position(pos - lexer.outLen);
        } else {
            jsonText.position(startOffset);
        }
    }
}
