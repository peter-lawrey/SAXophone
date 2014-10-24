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

package net.openhft.saxophone.json.handler;

/**
 * Reset hook is called at the end of {@link net.openhft.saxophone.json.JsonParser#reset()} call.
 * {@code JsonParser} is designed to be reused, because it's allocation costs are pretty high,
 * but parser's handlers couldn't be updated after construction, {@code ResetHook} allows to reuse
 * the handlers implementation too.
 *
 * <p>Example usage: <pre>{@code
 * class MapConstructor implements ObjectKeyHandler, StringValueHandler, ResetHook {
 *     private Map<String, String> map = new HashMap<String, String>();
 *     private String key;
 *
 *     public Map<String, String> getMap() {
 *         return map;
 *     }
 *
 *     &#64;Override
 *     public boolean onObjectKey(CharSequence key) {
 *         key = key.toString();
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public boolean onStringValue(CharSequence value) throws Exception {
 *         if (key == null)
 *             throw new Exception("Only objects are expected");
 *         map.put(key, value.toString());
 *         key = null;
 *         return true;
 *     }
 *
 *     &#64;Override
 *     public void onReset() {
 *         map = new HashMap&lt;String, String&gt;();
 *     }
 * }
 * }</pre>
 */
public interface ResetHook extends JsonHandlerBase {
    /**
     * Performs an action on {@link net.openhft.saxophone.json.JsonParser#reset()} call.
     *
     * <p>Unchecked exceptions, that might be thrown in this method, are relayed to
     * the {@link net.openhft.saxophone.json.JsonParser#reset()} caller. </p>
     */
    void onReset();
}
