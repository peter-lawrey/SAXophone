/*
 * Copyright 2013 Peter Lawrey
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

package net.openhft.saxophone.fix;

import net.openhft.lang.io.Bytes;
import net.openhft.saxophone.BytesSaxParser;

public class FixSaxParser implements BytesSaxParser {
    private static final byte FIELD_TERMINATOR = 1;
    private final FixHandler handler;

    public FixSaxParser(FixHandler handler) {
        this.handler = handler;
    }

    @Override
    public void reset() {
    }

    @Override
    public void parse(Bytes bytes) {
        long limit = bytes.limit(), limit2 = limit;
        while (limit2 > bytes.position() && bytes.readByte(limit2 - 1) != FIELD_TERMINATOR)
            limit2--;
        bytes.limit(limit2);
        while (bytes.remaining() > 0) {
            long fieldNum = bytes.parseLong();
            long pos = bytes.position();
            while (bytes.readByte() != FIELD_TERMINATOR) ;
            long end = bytes.position() - 1;
            bytes.limit(end);
            bytes.position(pos);
            handler.onField(fieldNum, bytes);

            bytes.limit(limit);
            bytes.position(end + 1);
        }
        bytes.limit(limit);
        bytes.position(limit2);
    }
}
