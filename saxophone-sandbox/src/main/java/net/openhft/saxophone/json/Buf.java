/*
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

import net.openhft.lang.io.*;

import java.util.concurrent.atomic.AtomicInteger;


final class Buf extends DirectBytes {

    private static final long BUF_INIT_SIZE = 2048;

    public Buf() {
        super(new DirectStore(BUF_INIT_SIZE), new AtomicInteger(1));
        writeByte(0, 0);
    }

    private void ensureAvailable(long want) {
        long need = capacity();
        long used = limit();
        while (want >= (need - used))
            need <<= 1;
        if (need != capacity()) {
            DirectStore store = (DirectStore) store();
            store.resize(need, false);
            if (address() != store.address()) {
                long pos = position();
                long limit = limit();
                startAddr = store.address();
                positionAddr = startAddr + pos;
                limitAddr = startAddr + limit;
                capacityAddr = startAddr + store.size();
            }
        }
    }

    void append(RandomDataInput data, long off, long len) {
        ensureAvailable(len);

        long pos = position();
        position(limit());
        limit(capacity());

        write(data, off, len);

        limit(position());
        position(pos);
    }

    @Override
    public Buf clear() {
        super.clear();
        limit(0);
        return this;
    }
}
