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

import net.openhft.lang.model.constraints.NotNull;
import net.openhft.lang.model.constraints.Nullable;
import net.openhft.saxophone.json.handler.*;

import java.util.*;

public final class JsonParserBuilder {
    
    @NotNull private EnumSet<JsonOption> options = EnumSet.noneOf(JsonOption.class);
    @Nullable private ObjectStartHandler objectStartHandler = null;
    @Nullable private ObjectEndHandler objectEndHandler = null;
    @Nullable private ArrayStartHandler arrayStartHandler = null;
    @Nullable private ArrayEndHandler arrayEndHandler = null;
    @Nullable private BooleanHandler booleanHandler = null;
    @Nullable private NullHandler nullHandler = null;
    @Nullable private StringHandler stringHandler = null;
    @Nullable private ObjectKeyHandler objectKeyHandler = null;
    @Nullable private NumberHandler numberHandler = null;
    @Nullable private IntegerHandler integerHandler = null;
    @Nullable private FloatingHandler floatingHandler = null;

    public JsonParser build() {
        checkAnyHandlerNonNull();
        return new JsonParser(options, objectStartHandler, objectEndHandler, arrayStartHandler,
                arrayEndHandler, booleanHandler, nullHandler, stringHandler, objectKeyHandler,
                numberHandler, integerHandler, floatingHandler);
    }

    private void checkAnyHandlerNonNull() {
        if (objectStartHandler != null) return;
        if (objectEndHandler != null) return;
        if (arrayStartHandler != null) return;
        if (arrayEndHandler != null) return;
        if (booleanHandler != null) return;
        if (nullHandler != null) return;
        if (stringHandler != null) return;
        if (objectKeyHandler != null) return;
        if (numberHandler != null) return;
        if (integerHandler != null) return;
        if (floatingHandler != null) return;
        throw new IllegalStateException("Parser should have at least one handler");
    }

    /**
     * Returns the parser options as read-only set.
     * Initially there are no options (the set is empty).
     *
     * @return the parser options as read-only set
     */
    public Set<JsonOption> options() {
        return Collections.unmodifiableSet(options);
    }

    /**
     * Sets the parser options. The previous options, if any, are discarded.
     * @param options parser options
     * @return a reference to this object
     */
    public JsonParserBuilder options(Collection<JsonOption> options) {
        this.options = EnumSet.copyOf(options);
        return this;
    }

    /**
     * Sets the parser options. The previous options, if any, are discarded.
     * @param first parser option
     * @param rest the rest parser options
     * @return a reference to this object
     */
    public JsonParserBuilder options(JsonOption first, JsonOption... rest) {
        this.options = EnumSet.of(first, rest);
        return this;
    }

    public JsonParserBuilder clearOptions() {
        options = EnumSet.noneOf(JsonOption.class);
        return this;
    }

    /**
     * Convenient method to apply the adapter which implements several handler interfaces
     * in one call.
     *
     * <p>Is equivalent to <pre>{@code
     * if (a instanceof ObjectStartHandler)
     *     objectStartHandler((ObjectStartHandler) a);
     * if (a instanceof ObjectEndHandler)
     *     objectEndHandler((ObjectEndHandler) a);
     * // ... the same job for the rest handler interfaces
     * }</pre>
     *
     * @param a the adapter - an object, implementing some of concrete handler interfaces
     * @return a reference to this object
     * @throws java.lang.IllegalArgumentException if the adapter doesn't implement any
     *         of concrete handler interfaces
     */
    public JsonParserBuilder applyAdapter(JsonHandlerBase a) {
        boolean applied = false;
        if (a instanceof ObjectStartHandler) { objectStartHandler((ObjectStartHandler) a); applied = true; }
        if (a instanceof ObjectEndHandler) { objectEndHandler((ObjectEndHandler) a); applied = true; }
        if (a instanceof ArrayStartHandler) { arrayStartHandler((ArrayStartHandler) a); applied = true; }
        if (a instanceof ArrayEndHandler) { arrayEndHandler((ArrayEndHandler) a); applied = true; }
        if (a instanceof BooleanHandler) { booleanHandler((BooleanHandler) a); applied = true; }
        if (a instanceof NullHandler) { nullHandler((NullHandler) a); applied = true; }
        if (a instanceof StringHandler) { stringHandler((StringHandler) a); applied = true; }
        if (a instanceof ObjectKeyHandler) { objectKeyHandler((ObjectKeyHandler) a); applied = true; }
        if (a instanceof NumberHandler) { numberHandler((NumberHandler) a); applied = true; }
        if (a instanceof IntegerHandler) { integerHandler((IntegerHandler) a); applied = true; }
        if (a instanceof FloatingHandler) { floatingHandler((FloatingHandler) a); applied = true; }
        if (!applied)
            throw new IllegalArgumentException(a + " isn't an instance of any handler interface");
        return this;
    }

    @Nullable
    public ObjectStartHandler objectStartHandler() {
        return objectStartHandler;
    }

    public JsonParserBuilder objectStartHandler(@Nullable ObjectStartHandler objectStartHandler) {
        this.objectStartHandler = objectStartHandler;
        return this;
    }

    @Nullable
    public ObjectEndHandler objectEndHandler() {
        return objectEndHandler;
    }

    public JsonParserBuilder objectEndHandler(@Nullable ObjectEndHandler objectEndHandler) {
        this.objectEndHandler = objectEndHandler;
        return this;
    }

    @Nullable
    public ArrayStartHandler arrayStartHandler() {
        return arrayStartHandler;
    }

    public JsonParserBuilder arrayStartHandler(@Nullable ArrayStartHandler arrayStartHandler) {
        this.arrayStartHandler = arrayStartHandler;
        return this;
    }

    @Nullable
    public ArrayEndHandler arrayEndHandler() {
        return arrayEndHandler;
    }

    public JsonParserBuilder arrayEndHandler(@Nullable ArrayEndHandler arrayEndHandler) {
        this.arrayEndHandler = arrayEndHandler;
        return this;
    }

    @Nullable
    public BooleanHandler booleanHandler() {
        return booleanHandler;
    }

    public JsonParserBuilder booleanHandler(@Nullable BooleanHandler booleanHandler) {
        this.booleanHandler = booleanHandler;
        return this;
    }

    @Nullable
    public NullHandler nullHandler() {
        return nullHandler;
    }

    public JsonParserBuilder nullHandler(@Nullable NullHandler nullHandler) {
        this.nullHandler = nullHandler;
        return this;
    }

    @Nullable
    public StringHandler stringHandler() {
        return stringHandler;
    }

    public JsonParserBuilder stringHandler(@Nullable StringHandler stringHandler) {
        this.stringHandler = stringHandler;
        return this;
    }

    @Nullable
    public ObjectKeyHandler objectKeyHandler() {
        return objectKeyHandler;
    }

    public JsonParserBuilder objectKeyHandler(@Nullable ObjectKeyHandler objectKeyHandler) {
        this.objectKeyHandler = objectKeyHandler;
        return this;
    }

    @Nullable
    public NumberHandler numberHandler() {
        return numberHandler;
    }

    public JsonParserBuilder numberHandler(@Nullable NumberHandler numberHandler) {
        checkNoConflict(integerHandler, "integer", numberHandler, "number");
        checkNoConflict(floatingHandler, "floating", numberHandler, "number");
        this.numberHandler = numberHandler;
        return this;
    }

    @Nullable
    public IntegerHandler integerHandler() {
        return integerHandler;
    }

    public JsonParserBuilder integerHandler(@Nullable IntegerHandler integerHandler) {
        checkNoConflict(integerHandler, "integer", numberHandler, "number");
        this.integerHandler = integerHandler;
        return this;
    }

    @Nullable
    public FloatingHandler floatingHandler() {
        return floatingHandler;
    }

    public JsonParserBuilder floatingHandler(@Nullable FloatingHandler floatingHandler) {
        checkNoConflict(floatingHandler, "floating", numberHandler, "number");
        this.floatingHandler = floatingHandler;
        return this;
    }

    private void checkNoConflict(Object h1, String name1, Object h2, String name2) {
        if (h1 != null && h2 != null)
            throw new IllegalStateException(
                    "Parser cannot have " + name1 + " and " + name2 + " handlers simultaneously");
    }
}
