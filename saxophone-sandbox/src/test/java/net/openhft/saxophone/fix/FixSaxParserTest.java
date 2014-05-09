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

import net.openhft.lang.io.ByteBufferBytes;
import net.openhft.lang.io.Bytes;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class FixSaxParserTest {
    @Test
    public void testParseSingleOrder() {
        String s = "8=FIX.4.2|9=130|35=D|34=659|49=BROKER04|56=REUTERS|52=20070123-19:09:43|38=1000|59=1|100=N|40=1|11=ORD10001|60=20070123-19:01:17|55=HPQ|54=1|21=2|10=004|";
        ByteBufferBytes bbb = new ByteBufferBytes(ByteBuffer.wrap(s.replace('|', '\u0001').getBytes()));
        final StringBuilder sb = new StringBuilder();
        FixSaxParser parser = new FixSaxParser(new FixHandler() {
            @Override
            public void onField(long fieldNumber, Bytes value) {
                sb.append(fieldNumber).append("=").append(value.asString()).append("|");
            }
        });
        parser.parse(bbb);
        assertEquals(s, sb.toString());
    }

    @Test
    public void timeParseSingleOrder() {
        String s = "8=FIX.4.2|9=130|35=D|34=659|49=BROKER04|56=REUTERS|52=20070123-19:09:43|38=1000|59=1|100=N|40=1|11=ORD10001|60=20070123-19:01:17|55=HPQ|54=1|21=2|10=004|";
        ByteBufferBytes bbb = new ByteBufferBytes(ByteBuffer.wrap(s.replace('|', '\u0001').getBytes()));

        final AtomicInteger count = new AtomicInteger();
        FixSaxParser parser = new FixSaxParser(new FixHandler() {
            @Override
            public void onField(long fieldNumber, Bytes value) {
                count.incrementAndGet();
            }
        });
        int runs = 20000;
        for (int t = 0; t < 5; t++) {
            count.set(0);
            long start = System.nanoTime();
            for (int i = 0; i < runs; i++) {
                bbb.position(0);
                parser.parse(bbb);
            }
            long time = System.nanoTime() - start;
            System.out.printf("Average parse time was %.2f us, fields per message %.2f%n",
                    time / runs / 1e3, (double) count.get() / runs);
        }
    }
}
