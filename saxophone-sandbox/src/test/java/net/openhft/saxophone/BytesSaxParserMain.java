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

package net.openhft.saxophone;

import net.openhft.lang.io.ByteBufferBytes;
import net.openhft.lang.io.Bytes;

import java.nio.ByteBuffer;

public class BytesSaxParserMain {
    public static void main(String... ignored) {
        BytesSaxParser parser = new BytesSaxParser() {
            @Override
            public void parse(Bytes bytes) {
                while (bytes.remaining() > 0)
                    System.out.print((char) bytes.readByte());
            }
        };
        ByteBuffer bb = ByteBuffer.allocateDirect(64 * 1024);
        ByteBufferBytes bbb = new ByteBufferBytes(bb);

        String text = "Hello World!";
        for (char ch : text.toCharArray()) {
            bb.put((byte) ch);
            bbb.limit(bb.position());
            parser.parse(bbb);
            if (bbb.remaining() == 0) {
                bb.clear();
                bbb.clear();
            }
        }
        System.out.println();
    }
}
