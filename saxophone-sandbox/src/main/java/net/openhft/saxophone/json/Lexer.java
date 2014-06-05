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

import net.openhft.lang.io.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.openhft.saxophone.json.LexError.*;
import static net.openhft.saxophone.json.TokenType.*;

/**
 * Porting note: this class approximately corresponds to src/yajl_lex.c and src/yajl_lex.h in YAJL.
 *
 * I tried to preserve method order and names to ease side-to-side comparison.
 */
final class Lexer {
    private static final Logger LOG = LoggerFactory.getLogger(Lexer.class);

    /** Impact of the stream parsing feature on the lexer:
     *
     * YAJL support stream parsing.  That is, the ability to parse the first
     * bits of a chunk of JSON before the last bits are available (still on
     * the network or disk).  This makes the lexer more complex.  The
     * responsibility of the lexer is to handle transparently the case where
     * a chunk boundary falls in the middle of a token.  This is
     * accomplished is via a buffer and a character reading abstraction.
     *
     * Overview of implementation
     *
     * When we lex to end of input string before end of token is hit, we
     * copy all of the input text composing the token into our lexBuf.
     *
     * Every time we read a character, we do so through the readChar function.
     * readChar's responsibility is to handle pulling all chars from the buffer
     * before pulling chars from input text
     */

    /* a lookup table which lets us quickly determine three things:
     * VEC - valid escaped control char
     * note.  the solidus '/' may be escaped or not.
     * IJC - invalid json char
     * VHC - valid hex char
     * NFP - needs further processing (from a string scanning perspective)
     * NUC - needs utf8 checking when enabled (from a string scanning perspective)
     */
    private static final byte VEC = 0x01;
    private static final byte IJC = 0x02;
    private static final byte VHC = 0x04;
    private static final byte NFP = 0x08;
    private static final byte NUC = 0x10;

    private static final byte[] CHAR_LOOKUP_TABLE = {
     /*00*/ IJC, IJC, IJC        , IJC, IJC        , IJC, IJC    , IJC,
     /*08*/ IJC, IJC, IJC        , IJC, IJC        , IJC, IJC    , IJC,
     /*10*/ IJC, IJC, IJC        , IJC, IJC        , IJC, IJC    , IJC,
     /*18*/ IJC, IJC, IJC        , IJC, IJC        , IJC, IJC    , IJC,

     /*20*/ 0  , 0  , NFP|VEC|IJC, 0  , 0          , 0  , 0      , 0  ,
     /*28*/ 0  , 0  , 0          , 0  , 0          , 0  , 0      , VEC,
     /*30*/ VHC, VHC, VHC        , VHC, VHC        , VHC, VHC    , VHC,
     /*38*/ VHC, VHC, 0          , 0  , 0          , 0  , 0      , 0  ,

     /*40*/ 0  , VHC, VHC        , VHC, VHC        , VHC, VHC    , 0  ,
     /*48*/ 0  , 0  , 0          , 0  , 0          , 0  , 0      , 0  ,
     /*50*/ 0  , 0  , 0          , 0  , 0          , 0  , 0      , 0  ,
     /*58*/ 0  , 0  , 0          , 0  , NFP|VEC|IJC, 0  , 0      , 0  ,

     /*60*/ 0  , VHC, VEC|VHC    , VHC, VHC        , VHC, VEC|VHC, 0  ,
     /*68*/ 0  , 0  , 0          , 0  , 0          , 0  , VEC    , 0  ,
     /*70*/ 0  , 0  , VEC        , 0  , VEC        , 0  , 0      , 0  ,
     /*78*/ 0  , 0  , 0          , 0  , 0          , 0  , 0      , 0  ,

            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,

            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,

            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,

            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC,
            NUC, NUC, NUC        , NUC, NUC        , NUC, NUC    , NUC
    };

    private static final char[] RUE_CHARS = new char[] {'r', 'u', 'e'};
    private static final char[] ALSE_CHARS = new char[] {'a', 'l', 's', 'e'};
    private static final char[] ULL_CHARS = new char[] {'u', 'l', 'l'};

    LexError error;

    /** a input buffer to handle the case where a token is spread over multiple chunks */
    private final Buf buf = new Buf();

    /** are we using the lex buf? */
    private boolean bufInUse;

    private final boolean allowComments;

    /** shall we validate utf8 inside strings? */
    private final boolean validateUTF8;

    /** Emulating out parameters of lex() method. */
    Bytes outBuf;
    long outPos;
    long outLen;

    Lexer(boolean allowComments, boolean validateUTF8) {
        this.allowComments = allowComments;
        this.validateUTF8 = validateUTF8;
    }

    void reset() {
        bufInUse = false;
        buf.clear();
        error = null;
        outBuf = null;
        outPos = outLen = 0;
    }

    private int readChar(Bytes txt) {
        if (bufInUse && buf.remaining() > 0) {
            return buf.readUnsignedByte();
        } else {
            return txt.readUnsignedByte();
        }
    }

    private void unreadChar(Bytes txt) {
        long pos = txt.position();
        if (pos > 0) {
            txt.position(pos - 1);
        } else {
            buf.position(buf.position() - 1);
        }
    }

    /** process a variable length utf8 encoded codepoint.
     *
     *  @return
     *    STRING - if valid utf8 char was parsed and offset was advanced
     *    EOF - if end of input was hit before validation could complete
     *    ERROR - if invalid utf8 was encountered
     *
     *  NOTE: on error the offset will point to the first char of the
     *  invalid utf8
     */
    private TokenType lexUtf8Char(Bytes jsonText, int curChar) {
        if (curChar <= 0x7f) {
            /* single byte */
            return STRING;
        } else if ((curChar >> 5) == 0x6) {
            /* two byte */
            if (jsonText.remaining() == 0) return EOF;
            curChar = readChar(jsonText);
            if ((curChar >> 6) == 0x2) return STRING;
        } else if ((curChar >> 4) == 0x0e) {
            /* three byte */
            if (jsonText.remaining() == 0) return EOF;
            curChar = readChar(jsonText);
            if ((curChar >> 6) == 0x2) {
                if (jsonText.remaining() == 0) return EOF;
                curChar = readChar(jsonText);
                if ((curChar >> 6) == 0x2) return STRING;
            }
        } else if ((curChar >> 3) == 0x1e) {
            /* four byte */
            if (jsonText.remaining() == 0) return EOF;
            curChar = readChar(jsonText);
            if ((curChar >> 6) == 0x2) {
                if (jsonText.remaining() == 0) return EOF;
                curChar = readChar(jsonText);
                if ((curChar >> 6) == 0x2) {
                    if (jsonText.remaining() == 0) return EOF;
                    curChar = readChar(jsonText);
                    if ((curChar >> 6) == 0x2) return STRING;
                }
            }
        }
        return ERROR;
    }

    /**
     * scan a string for interesting characters that might need further
     * review.  return the number of chars that are uninteresting and can
     * be skipped.
     * (lth) hi world, any thoughts on how to make this routine faster?
     */
    private void stringScan(Bytes bytes) {
        int mask = IJC | NFP | (validateUTF8 ? NUC : 0);
        long pos = bytes.position();
        long limit = bytes.limit();
        while (pos < limit && ((CHAR_LOOKUP_TABLE[bytes.readUnsignedByte(pos)] & mask) == 0)) {
            pos++;
        }
        bytes.position(pos);
    }

    /**
     * Lex a string.
     *
     *  @return
     *     STRING - lex of string was successful.  offset points to terminating '"'.
     *     EOF - end of text was encountered before we could complete the lex.
     *     ERROR - embedded in the string were unallowable chars.  offset
     *             points to the offending char
     */
    private TokenType lexString(Bytes jsonText) {
        TokenType tok = ERROR;
        boolean hasEscapes = false;

        finish_string_lex:
        for (;;) {
            int curChar;

            /* now jump into a faster scanning routine to skip as much
             * of the buffers as possible */
            {
                if (bufInUse && buf.remaining() > 0) {
                    stringScan(buf);
                } else if (jsonText.remaining() > 0) {
                    stringScan(jsonText);
                }
            }

            if (jsonText.remaining() == 0) {
                tok = EOF;
                break;
            }

            curChar = readChar(jsonText);

            /* quote terminates */
            if (curChar == '"') {
                tok = STRING;
                break;
            }
            /* backslash escapes a set of control chars, */
            else if (curChar == '\\') {
                hasEscapes = true;
                if (jsonText.remaining() == 0) {
                    tok = EOF;
                    break;
                }

                /* special case \\u */
                curChar = readChar(jsonText);
                if (curChar == 'u') {
                    for (int i = 0; i < 4; i++) {
                        if (jsonText.remaining() == 0) {
                            tok = EOF;
                            break finish_string_lex;
                        }
                        curChar = readChar(jsonText);
                        if ((CHAR_LOOKUP_TABLE[curChar] & VHC) == 0) {
                        /* back up to offending char */
                            unreadChar(jsonText);
                            error = STRING_INVALID_HEX_CHAR;
                            break finish_string_lex;
                        }
                    }
                } else if ((CHAR_LOOKUP_TABLE[curChar] & VEC) == 0) {
                /* back up to offending char */
                    unreadChar(jsonText);
                    error = STRING_INVALID_ESCAPED_CHAR;
                    break;
                }
            }
            /* when not validating UTF8 it's a simple table lookup to determine
             * if the present character is invalid */
            else if((CHAR_LOOKUP_TABLE[curChar] & IJC) != 0) {
                /* back up to offending char */
                unreadChar(jsonText);
                error = STRING_INVALID_JSON_CHAR;
                break;
            }
            /* when in validate UTF8 mode we need to do some extra work */
            else if (validateUTF8) {
                TokenType t = lexUtf8Char(jsonText, curChar);

                if (t == EOF) {
                    tok = EOF;
                    break;
                } else if (t == ERROR) {
                    error = STRING_INVALID_UTF8;
                    break;
                }
            }
            /* accept it, and move on */
        }
        /* tell our buddy, the parser, whether he needs to process this string again */
        if (hasEscapes && tok == STRING) {
            tok = STRING_WITH_ESCAPES;
        }

        return tok;
    }

    private TokenType lexNumber(Bytes jsonText) {
        /** XXX: numbers are the only entities in json that we must lex
         *       _beyond_ in order to know that they are complete.  There
         *       is an ambiguous case for integers at EOF. */

        int c;

        TokenType tok = INTEGER;

        if (jsonText.remaining() == 0) return EOF;
        c = readChar(jsonText);

        /* optional leading minus */
        if (c == '-') {
            if (jsonText.remaining() == 0) return EOF;
            c = readChar(jsonText);
        }

        /* a single zero, or a series of integers */
        if (c == '0') {
            if (jsonText.remaining() == 0) return EOF;
            c = readChar(jsonText);
        } else if (c >= '1' && c <= '9') {
            do {
                if (jsonText.remaining() == 0) return EOF;
                c = readChar(jsonText);
            } while (c >= '0' && c <= '9');
        } else {
            unreadChar(jsonText);
            error = MISSING_INTEGER_AFTER_MINUS;
            return ERROR;
        }

        /* optional fraction (indicates this is floating point) */
        if (c == '.') {
            boolean readSome = false;

            if (jsonText.remaining() == 0) return EOF;
            c = readChar(jsonText);

            while (c >= '0' && c <= '9') {
                readSome = true;
                if (jsonText.remaining() == 0) return EOF;
                c = readChar(jsonText);
            }

            if (!readSome) {
                unreadChar(jsonText);
                error = MISSING_INTEGER_AFTER_DECIMAL;
                return ERROR;
            }
            tok = DOUBLE;
        }

        /* optional exponent (indicates this is floating point) */
        if (c == 'e' || c == 'E') {
            if (jsonText.remaining() == 0) return EOF;
            c = readChar(jsonText);

            /* optional sign */
            if (c == '+' || c == '-') {
                if (jsonText.remaining() == 0) return EOF;
                c = readChar(jsonText);
            }

            if (c >= '0' && c <= '9') {
                do {
                    if (jsonText.remaining() == 0) return EOF;
                    c = readChar(jsonText);
                } while (c >= '0' && c <= '9');
            } else {
                unreadChar(jsonText);
                error = MISSING_INTEGER_AFTER_EXPONENT;
                return ERROR;
            }
            tok = DOUBLE;
        }

        /* we always go "one too far" */
        unreadChar(jsonText);

        return tok;
    }

    private TokenType lexComment(Bytes jsonText) {
        int c;

        TokenType tok = COMMENT;

        if (jsonText.remaining() == 0) return EOF;
        c = readChar(jsonText);

        /* either slash or star expected */
        if (c == '/') {
            /* now we throw away until end of line */
            do {
                if (jsonText.remaining() == 0) return EOF;
                c = readChar(jsonText);
            } while (c != '\n');
        } else if (c == '*') {
            /* now we throw away until end of comment */
            for (;;) {
                if (jsonText.remaining() == 0) return EOF;
                c = readChar(jsonText);
                if (c == '*') {
                    if (jsonText.remaining() == 0) return EOF;
                    c = readChar(jsonText);
                    if (c == '/') {
                        break;
                    } else {
                        unreadChar(jsonText);
                    }
                }
            }
        } else {
            error = INVALID_CHAR;
            tok = ERROR;
        }

        return tok;
    }

    TokenType lex(Bytes jsonText) {
        TokenType tok;
        int c;
        long startOffset = jsonText.position();

        outBuf = null;
        outPos = 0;
        outLen = 0;

        lexing:
        for (;;) {

            if (jsonText.remaining() == 0) {
                tok = EOF;
                break;
            }

            c = readChar(jsonText);

            switch (c) {
                case '{':
                    tok = LEFT_BRACKET;
                        break lexing;
                case '}':
                    tok = RIGHT_BRACKET;
                    break lexing;
                case '[':
                    tok = LEFT_BRACE;
                    break lexing;
                case ']':
                    tok = RIGHT_BRACE;
                    break lexing;
                case ',':
                    tok = COMMA;
                    break lexing;
                case ':':
                    tok = COLON;
                    break lexing;
                case '\t': case '\n': case '\u000B': case '\f': case '\r': case ' ':
                    startOffset++;
                    break;
                case 't': {
                    for (char want : RUE_CHARS) {
                        if (jsonText.remaining() == 0) {
                            tok = EOF;
                            break lexing;
                        }
                        c = readChar(jsonText);
                        if (c != want) {
                            unreadChar(jsonText);
                            error = INVALID_STRING;
                            tok = ERROR;
                            break lexing;
                        }
                    }
                    tok = BOOL;
                    break lexing;
                }
                case 'f': {
                    for (char want : ALSE_CHARS) {
                        if (jsonText.remaining() == 0) {
                            tok = EOF;
                            break lexing;
                        }
                        c = readChar(jsonText);
                        if (c != want) {
                            unreadChar(jsonText);
                            error = INVALID_STRING;
                            tok = ERROR;
                            break lexing;
                        }
                    }
                    tok = BOOL;
                    break lexing;
                }
                case 'n': {
                    for (char want : ULL_CHARS) {
                        if (jsonText.remaining() == 0) {
                            tok = EOF;
                            break lexing;
                        }
                        c = readChar(jsonText);
                        if (c != want) {
                            unreadChar(jsonText);
                            error = INVALID_STRING;
                            tok = ERROR;
                            break lexing;
                        }
                    }
                    tok = NULL;
                    break lexing;
                }
                case '"': {
                    tok = lexString(jsonText);
                    break lexing;
                }
                case '-':
                case '0': case '1': case '2': case '3': case '4':
                case '5': case '6': case '7': case '8': case '9': {
                    /* integer parsing wants to start from the beginning */
                    unreadChar(jsonText);
                    tok = lexNumber(jsonText);
                    break lexing;
                }
                case '/':
                    /* hey, look, a probable comment!  If comments are disabled
                     * it's an error. */
                    if (!allowComments) {
                        unreadChar(jsonText);
                        error = UNALLOWED_COMMENT;
                        tok = ERROR;
                        break lexing;
                    }
                    /* if comments are enabled, then we should try to lex
                     * the thing.  possible outcomes are
                     * - successful lex (tok_comment, which means continue),
                     * - malformed comment opening (slash not followed by
                     *   '*' or '/') (tok_error)
                     * - eof hit. (tok_eof) */
                    tok = lexComment(jsonText);
                    if (tok == COMMENT) {
                        buf.clear();
                        bufInUse = false;
                        startOffset = jsonText.position();
                        continue lexing;
                    }
                    /* hit error or eof, bail */
                    break lexing;
                default:
                    error = INVALID_CHAR;
                    tok = ERROR;
                    break lexing;
            }
        }

        
        /* need to append to buffer if the buffer is in use or
         * if it's an EOF token */
        if (tok == EOF || bufInUse) {
            if (!bufInUse) {
                buf.clear();
                bufInUse = true;
            }
            buf.append(jsonText, startOffset, jsonText.position() - startOffset);
            buf.position(0);

            if (tok != EOF) {
                outBuf = buf;
                outPos = 0;
                outLen = buf.limit();
                bufInUse = false;
            }
        } else if (tok != ERROR) {
            outBuf = jsonText;
            outPos = startOffset;
            outLen = jsonText.position() - startOffset;
        }

        /* special case for strings. skip the quotes. */
        if (tok == STRING || tok == STRING_WITH_ESCAPES) {
            assert outLen >= 2;
            outPos++;
            outLen -= 2;
        }


        if (LOG.isDebugEnabled()) {
            if (tok == ERROR) {
                LOG.debug("lexical error: " + error);
            } else if (tok == EOF) {
                LOG.debug("EOF hit");
            } else {
                LOG.debug("lexed %s: '" + tok + outBuf.bytes(outPos, outLen) + "'");
            }
        }

        return tok;
    }
}
