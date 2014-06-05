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

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.EnumSet;

import static net.openhft.saxophone.json.JsonParserOption.*;
import static net.openhft.saxophone.json.ParserState.*;
import static net.openhft.saxophone.json.TokenType.*;

public final class JsonParser implements Closeable {

    /**
     * Porting note: this class approximately corresponds to src/yajl_parser.c, src/yajl_parser.h
     * and src/yajl.c in YAJL.
     *
     * I tried to preserve method order and names to ease side-to-side comparison.
     */

    public static JsonParserBuilder builder() {
        return new JsonParserBuilder();
    }

    private static ParseException handlerException(Exception e) {
        return new ParseException("Exception in the handler", e);
    }

    private final Lexer lexer;
    String parseError;
    /** temporary storage for decoded strings */
    private final StringBuilder decodeBuf = new StringBuilder();
    private final ParserState.Stack stateStack;
    private final EnumSet<JsonParserOption> flags;
    private Bytes finishSpace;

    @Nullable private final ObjectStartHandler objectStartHandler;
    @Nullable private final ObjectEndHandler objectEndHandler;
    @Nullable private final ArrayStartHandler arrayStartHandler;
    @Nullable private final ArrayEndHandler arrayEndHandler;
    @Nullable private final BooleanHandler booleanHandler;
    @Nullable private final NullHandler nullHandler;
    @Nullable private final StringHandler stringHandler;
    @Nullable private final ObjectKeyHandler objectKeyHandler;
    @Nullable private final NumberHandler numberHandler;
    @Nullable private final IntegerHandler integerHandler;
    @Nullable private final FloatingHandler floatingHandler;

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
            assert stringHandler != null;
            return stringHandler.onString(value);
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
               @Nullable ObjectStartHandler objectStartHandler,
               @Nullable ObjectEndHandler objectEndHandler,
               @Nullable ArrayStartHandler arrayStartHandler,
               @Nullable ArrayEndHandler arrayEndHandler,
               @Nullable BooleanHandler booleanHandler, @Nullable NullHandler nullHandler,
               @Nullable StringHandler stringHandler, @Nullable ObjectKeyHandler objectKeyHandler,
               @Nullable NumberHandler numberHandler, @Nullable IntegerHandler integerHandler,
               @Nullable FloatingHandler floatingHandler) {
        this.flags = flags;
        this.objectStartHandler = objectStartHandler;
        this.objectEndHandler = objectEndHandler;
        this.arrayStartHandler = arrayStartHandler;
        this.arrayEndHandler = arrayEndHandler;
        this.booleanHandler = booleanHandler;
        this.nullHandler = nullHandler;
        this.stringHandler = stringHandler;
        this.objectKeyHandler = objectKeyHandler;
        this.numberHandler = numberHandler;
        this.integerHandler = integerHandler;
        this.floatingHandler = floatingHandler;

        lexer = new Lexer(flags.contains(ALLOW_COMMENTS), !flags.contains(DONT_VALIDATE_STRINGS));
        stateStack = new Stack();
        reset();
    }

    public void reset() {
        lexer.reset();
        stateStack.clear();
        stateStack.push(START);
        parseError = null;
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

    public boolean finish() {
        if (!parse(finishSpace())) return false;

        switch(stateStack.current()) {
            case PARSE_ERROR:
            case LEXICAL_ERROR:
                throw new ParseException(parseError);
            case HANDLER_CANCEL:
                throw new IllegalStateException("client cancelled parse via handler");
            case GOT_VALUE:
            case PARSE_COMPLETE:
                return true;
            default:
                if (!flags.contains(ALLOW_PARTIAL_VALUES)) {
                    stateStack.set(PARSE_ERROR);
                    parseError = "premature EOF";
                    throw new ParseException(parseError);
                }
                return true;
        }
    }

    private Bytes finishSpace() {
        if (finishSpace == null) {
            return finishSpace = new ByteBufferBytes(ByteBuffer.wrap(new byte[] {' '}));
        }
        return finishSpace.clear();
    }

    @Override
    public void close() {
        finish();
    }

    public boolean parse(Bytes jsonText) {
        TokenType tok;

        long startOffset = jsonText.position();

        around_again:
        while (true) {
            switch (stateStack.current()) {
                case PARSE_COMPLETE:
                    if (flags.contains(ALLOW_MULTIPLE_VALUES)) {
                        stateStack.set(GOT_VALUE);
                        continue around_again;
                    }
                    if (!flags.contains(ALLOW_TRAILING_GARBAGE)) {
                        if (jsonText.remaining() > 0) {
                            tok = lexer.lex(jsonText);
                            if (tok != EOF) {
                                stateStack.set(PARSE_ERROR);
                                parseError = "trailing garbage";
                            }
                            continue around_again;
                        }
                    }
                    return true;
                case LEXICAL_ERROR:
                    throw new ParseException("lexical error: " + lexer.error);
                case PARSE_ERROR:
                    throw new ParseException(parseError);
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
                            stateStack.set(LEXICAL_ERROR);
                            continue around_again;
                        case STRING:
                            if (stringHandler != null) {
                                try {
                                    if (!onString.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
                            }
                            break;
                        case STRING_WITH_ESCAPES:
                            if (stringHandler != null) {
                                try {
                                    if (!onEscapedString.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
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
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
                            }
                            break;
                        case NULL:
                            if (nullHandler != null) {
                                try {
                                    if (!nullHandler.onNull()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
                            }
                            break;
                        case LEFT_BRACKET:
                            if (objectStartHandler != null) {
                                try {
                                    if (!objectStartHandler.onObjectStart()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
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
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
                            }
                            stateToPush = ARRAY_START;
                            break;
                        case INTEGER:
                            if (numberHandler != null) {
                                if (stringHandler != null) {
                                    try {
                                        if (!onNumber.on()) {
                                            stateStack.set(HANDLER_CANCEL);
                                            return false;
                                        }
                                    } catch (RuntimeException e) {
                                        throw e;
                                    } catch (Exception e) {
                                        stateStack.set(HANDLER_EXCEPTION);
                                        throw handlerException(e);
                                    }
                                }
                            } else if (integerHandler != null) {
                                try {
                                    long i = parseInteger(lexer.outBuf, lexer.outPos, lexer.outLen);
                                    if (!integerHandler.onInteger(i)) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (NumberFormatException e) {
                                    stateStack.set(PARSE_ERROR);
                                    parseError = "integer overflow";
                                    /* try to restore error offset */
                                    tryRestoreErrorEffect(jsonText, startOffset);
                                    continue around_again;
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
                            }
                            break;
                        case DOUBLE:
                            if (numberHandler != null) {
                                if (stringHandler != null) {
                                    try {
                                        if (!onNumber.on()) {
                                            stateStack.set(HANDLER_CANCEL);
                                            return false;
                                        }
                                    } catch (RuntimeException e) {
                                        throw e;
                                    } catch (Exception e) {
                                        stateStack.set(HANDLER_EXCEPTION);
                                        throw handlerException(e);
                                    }
                                }
                            } else if (floatingHandler != null) {
                                try {
                                    if (!onFloating.on()) {
                                        stateStack.set(HANDLER_CANCEL);
                                        return false;
                                    }
                                } catch (NumberFormatException e) {
                                    stateStack.set(PARSE_ERROR);
                                    parseError = "numeric (floating point) overflow";
                                    /* try to restore error offset */
                                    tryRestoreErrorEffect(jsonText, startOffset);
                                    continue around_again;
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
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
                                    } catch (RuntimeException e) {
                                        throw e;
                                    } catch (Exception e) {
                                        stateStack.set(HANDLER_EXCEPTION);
                                        throw handlerException(e);
                                    }
                                }
                                stateStack.pop();
                                continue around_again;
                            }
                            /* intentional fall-through */
                        }
                        case COLON:
                        case COMMA:
                        case RIGHT_BRACKET:
                            stateStack.set(PARSE_ERROR);
                            parseError = "unallowed token at this point in JSON text";
                            continue around_again;
                        default:
                            stateStack.set(PARSE_ERROR);
                            parseError = "invalid token, internal error";
                            continue around_again;
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
                            stateStack.set(LEXICAL_ERROR);
                            continue around_again;
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
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
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
                                    } catch (RuntimeException e) {
                                        throw e;
                                    } catch (Exception e) {
                                        stateStack.set(HANDLER_EXCEPTION);
                                        throw handlerException(e);
                                    }
                                }
                                stateStack.pop();
                                continue around_again;
                            }
                            /* intentional fall-through */
                        default:
                            stateStack.set(PARSE_ERROR);
                            parseError = "invalid object key (must be a string)";
                            continue around_again;
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
                            stateStack.set(LEXICAL_ERROR);
                            continue around_again;
                        default:
                            stateStack.set(PARSE_ERROR);
                            parseError = "object key and value must be separated by a colon (':')";
                            continue around_again;
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
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
                            }
                            stateStack.pop();
                            continue around_again;
                        case COMMA:
                            stateStack.set(MAP_NEED_KEY);
                            continue around_again;
                        case EOF:
                            return true;
                        case ERROR:
                            stateStack.set(LEXICAL_ERROR);
                            continue around_again;
                        default:
                            stateStack.set(PARSE_ERROR);
                            parseError = "after key and value, inside map, I expect ',' or '}'";
                            tryRestoreErrorEffect(jsonText, startOffset);

                            continue around_again;
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
                                } catch (RuntimeException e) {
                                    throw e;
                                } catch (Exception e) {
                                    stateStack.set(HANDLER_EXCEPTION);
                                    throw handlerException(e);
                                }
                            }
                            stateStack.pop();
                            continue around_again;
                        case COMMA:
                            stateStack.set(ARRAY_NEED_VAL);
                            continue around_again;
                        case EOF:
                            return true;
                        case ERROR:
                            stateStack.set(LEXICAL_ERROR);
                            continue around_again;
                        default:
                            stateStack.set(PARSE_ERROR);
                            parseError = "after array element, I expect ',' or ']'";
                            // continue around_again
                    }
                }
            }
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
