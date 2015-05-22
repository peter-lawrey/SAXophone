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

import net.openhft.lang.io.Bytes;

/**
 * Porting note: this class approximately corresponds to src/yajl_encode.c and src/yajl_encode.h
 * in YAJL.
 *
 * I tried to preserve method order and names to ease side-to-side comparison.
 */
final class Unescaper {

    private static char hexToDigit(Bytes hex, long pos) {
        int val = 0;
        for (int i = 0; i < 4; i++) {
            int c = hex.readUnsignedByte(pos + i);
            if (c >= 'A') c = (c & ~0x20) - 7;
            c -= '0';
            assert (c & 0xF0) == 0;
            val = (val << 4) | c;
        }
        return (char) val;
    }

    static void decode(StringBuilder buf, Bytes str) {
        long len = str.limit();
        long pos = str.position();
        int beg = 0;
        long end = pos;
        char codePoint;
        while (end < len) {
            if (str.readUnsignedByte(end) == '\\') {
                buf.append(str, beg, (int) (end - pos));
                switch (str.readUnsignedByte(++end)) {
                    case 'r': codePoint = '\r'; break;

                    case 'n': codePoint = '\n'; break;

                    case '\\': codePoint = '\\'; break;

                    case '/': codePoint = '/'; break;

                    case '"': codePoint = '\"'; break;

                    case 'f': codePoint = '\f'; break;

                    case 'b': codePoint = '\b'; break;

                    case 't': codePoint = '\t'; break;

                    case 'u': {
                        codePoint = hexToDigit(str, ++end);
                        end += 3;
                        break;
                    }

                    default:
                        throw new AssertionError("this should never happen");
                }
                buf.append(codePoint);
                beg = (int) ((++end) - pos);

            } else {
                end++;
            }
        }
        buf.append(str, beg, (int) (end - pos));
    }

    private Unescaper() {}
}
