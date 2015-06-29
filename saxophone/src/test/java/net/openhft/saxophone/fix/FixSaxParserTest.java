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

package net.openhft.saxophone.fix;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.bytes.NativeBytesStore;
import net.openhft.chronicle.bytes.StopCharTesters;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class FixSaxParserTest {
    @Test
    public void testParseSingleOrder() {
        String s = "8=FIX.4.2|9=130|35=D|34=659|49=BROKER04|56=REUTERS|52=20070123-19:09:43|38=1000|59=1|100=N|40=1|11=ORD10001|60=20070123-19:01:17|55=HPQ|54=1|21=2|10=004|";
        Bytes bbb = Bytes.from(s.replace('|', '\u0001'));
        final StringBuilder sb = new StringBuilder();
        FixSaxParser parser = new FixSaxParser((fieldNumber, value) -> sb.append(fieldNumber).append("=").append(value.toString()).append("|"));
        parser.parse(bbb);
        assertEquals(s, sb.toString());
    }

    @Test
    public void timeParseSingleOrder() {
        String s = "8=FIX.4.2|9=130|35=D|34=659|49=BROKER04|56=REUTERS|52=20070123-19:09:43|38=1000|59=1|100=N|40=1|11=ORD10001|60=20070123-19:01:17|55=HPQ|54=1|21=2|10=004|";
        Bytes nb = Bytes.from(s.replace('|', '\u0001'));

        final AtomicInteger count = new AtomicInteger();
        FixSaxParser parser = new FixSaxParser(new MyFixHandler(count));
        int runs = 200000;
        for (int t = 0; t < 5; t++) {
            count.set(0);
            long start = System.nanoTime();
            for (int i = 0; i < runs; i++) {
                nb.readPosition(0);
                parser.parse(nb);
            }
            long time = System.nanoTime() - start;
            System.out.printf("Average parse time was %.2f us, fields per message %.2f%n",
                    time / runs / 1e3, (double) count.get() / runs);
        }
    }

    static void processFixMessage(StringBuilder sender, StringBuilder target, StringBuilder clOrdId, StringBuilder symbol, double quantity, double price, int ordType) {
    }

    static class MyFixHandler implements FixHandler {
        final AtomicInteger count;
        final StringBuilder sender, target, clOrdId, symbol;
        double quantity, price;
        int ordType;

        public MyFixHandler(AtomicInteger count) {
            this.count = count;
            sender = new StringBuilder();
            target = new StringBuilder();
            clOrdId = new StringBuilder();
            symbol = new StringBuilder();
        }

        @Override
        public void onField(long fieldNumber, Bytes value) {
            switch ((int) fieldNumber) {
                case 8: // reset
                    resetAll();
                    break;

                case 35:
                    assert value.readByte() == 'D';
                    break;

                case 49:
                    value.parseUTF(sender, StopCharTesters.ALL);
                    break;

                case 56:
                    value.parseUTF(target, StopCharTesters.ALL);
                    break;

                case 11:
                    value.parseUTF(clOrdId, StopCharTesters.ALL);
                    break;

                case 55:
                    value.parseUTF(symbol, StopCharTesters.ALL);
                    break;

                case 38:
                    quantity = value.parseLong();
                    break;

                case 44:
                    price = value.parseDouble();
                    break;

                case 40:
                    ordType = (int) value.parseLong();
                    break;

                case 10:
                    processFixMessage(sender, target, clOrdId, symbol, quantity, price, ordType);
                    break;
            }
            count.incrementAndGet();
        }

        private void resetAll() {
            quantity = 0;
            price = Double.NaN;
            ordType = 0;
            sender.setLength(0);
            target.setLength(0);
            clOrdId.setLength(0);
            symbol.setLength(0);
        }
    }
}
