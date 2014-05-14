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

import net.openhft.lang.io.Bytes;
import net.openhft.saxophone.BytesSaxParser;

import java.util.ArrayList;
import java.util.List;

public class JsonSaxParser implements BytesSaxParser {

    private final JsonHandler handler;
    private final List<State> states = new ArrayList<State>();

    public JsonSaxParser(JsonHandler handler) {
        this.handler = handler;
        states.add(State.IN_OBJECT);
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void parse(Bytes bytes) {
        while (bytes.remaining() > 0) {
            State state = states.get(states.size() - 1);
            state.parse(this, bytes);
        }
    }

    static enum State {
        IN_OBJECT {
            @Override
            void parse(JsonSaxParser parser, Bytes bytes) {
                while (bytes.remaining() > 0) {
                    byte b = bytes.readByte();
                    if (b <= ' ') continue;
                    switch (b) {
                        case '{':
                            parser.handler.startOfObject();
                            parser.pushState(IN_OBJECT);
                            break;
                        case '[':
                            parser.handler.startOfArray();
                            parser.states.add(IN_ARRAY);
                            return;
                        case '}':
                            parser.handler.endOfObject();
                            parser.popState();
                            return;
                        case ']':
                            // todo shouldn't happen.
                            parser.handler.endOfArray();
                            parser.popState();
                            return;
                        case '"':
                            parser.pushState(IN_KEY);
                            parser.startOfKey(bytes.position());
                            return;
                        case ',':
                            // ignore for now
                            break;
                        default:
                            throw new AssertionError();
                    }
                }
            }
        }, IN_KEY {
            @Override
            void parse(JsonSaxParser parser, Bytes bytes) {
                while (bytes.remaining() > 0) {
                    byte b = bytes.readByte();
                    if (b != '"') continue;
                    parser.endOfKey(bytes.position());
                    parser.setState(AFTER_KEY);
                    return;
                }
            }
        }, AFTER_KEY {
            @Override
            void parse(JsonSaxParser parser, Bytes bytes) {
                while (bytes.remaining() > 0) {
                    byte b = bytes.readByte();
                    if (b != ':') continue;
                    parser.endOfKey(bytes.position());
                    parser.setState(AFTER_COLON);
                    return;
                }
            }
        }, AFTER_COLON {
            @Override
            void parse(JsonSaxParser parser, Bytes bytes) {
                while (bytes.remaining() > 0) {
                    byte b = bytes.readByte();
                    if (b != '"') continue;
                    parser.startOfValue(bytes.position());
                    parser.setState(IN_VALUE);
                    return;
                }
            }
        },
        IN_VALUE {
            @Override
            void parse(JsonSaxParser parser, Bytes bytes) {
                while (bytes.remaining() > 0) {
                    byte b = bytes.readByte();
                    if (b != '"') continue;
                    parser.endOfValue(bytes.position());
                    parser.setState(IN_OBJECT);
                    return;
                }
            }
        },
        IN_ARRAY {
            @Override
            void parse(JsonSaxParser parser, Bytes bytes) {
                while (bytes.remaining() > 0) {
                    byte b = bytes.readByte();
                    if (b <= ' ') continue;
                    switch (b) {
                        case '{':
                            parser.handler.startOfObject();
                            parser.pushState(IN_OBJECT);
                            return;
                        case '[':
                            parser.handler.startOfArray();
                            parser.states.add(IN_ARRAY);
                            break;
                        case '}':
                            // todo shouldn't happen.
                            parser.handler.endOfObject();
                            parser.popState();
                            return;
                        case ']':
                            parser.handler.endOfArray();
                            parser.popState();
                            return;
                    }
                }
            }
        };

        abstract void parse(JsonSaxParser parser, Bytes bytes);

    }

    private void endOfValue(long endOfValue) {

    }

    private void startOfValue(long startOfValue) {

    }

    private void startOfKey(long startOfKey) {

    }

    private void endOfKey(long endOfKey) {

    }

    void popState() {
        states.remove(states.size() - 1);
    }

    void pushState(State state) {
        states.add(state);
    }

    void setState(State state) {
        states.set(states.size() - 1, state);
    }
}
