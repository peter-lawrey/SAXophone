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

public interface FixHandler {
    /**
     * Called for each field of a FIX message.
     *
     * @param fieldNumber 8 is the start and 10 is the end, for other field numbers see fixprotocol.org
     * @param value       to be ignored, parsed or copied.
     */
    void onField(long fieldNumber, Bytes value);
}
