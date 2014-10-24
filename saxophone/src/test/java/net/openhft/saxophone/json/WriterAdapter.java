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

import com.google.gson.stream.JsonWriter;
import net.openhft.saxophone.json.handler.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

final class WriterAdapter implements ObjectStartHandler, ObjectEndHandler,
        ArrayStartHandler, ArrayEndHandler, BooleanHandler, NullHandler, StringValueHandler,
        ObjectKeyHandler, IntegerHandler, FloatingHandler, Closeable {
    private final JsonWriter writer;

    WriterAdapter(Writer writer) {
        this.writer = new JsonWriter(writer);
        this.writer.setLenient(true);
    }

    @Override
    public boolean onArrayEnd() throws Exception {
        writer.endArray();
        return true;
    }

    @Override
    public boolean onArrayStart() throws Exception {
        writer.beginArray();
        return true;
    }

    @Override
    public boolean onBoolean(boolean value) throws Exception {
        writer.value(value);
        return true;
    }

    @Override
    public boolean onFloating(double value) throws Exception {
        writer.value(value);
        return true;
    }

    @Override
    public boolean onInteger(long value) throws Exception {
        writer.value(value);
        return true;
    }

    @Override
    public boolean onNull() throws Exception {
        writer.nullValue();
        return true;
    }

    @Override
    public boolean onObjectEnd() throws Exception {
        writer.endObject();
        return true;
    }

    @Override
    public boolean onObjectKey(CharSequence key) throws Exception {
        writer.name(key.toString());
        return true;
    }

    @Override
    public boolean onObjectStart() throws Exception {
        writer.beginObject();
        return true;
    }

    @Override
    public boolean onStringValue(CharSequence value) throws Exception {
        writer.value(value.toString());
        return true;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
