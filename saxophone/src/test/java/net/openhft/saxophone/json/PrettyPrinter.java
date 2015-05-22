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

import net.openhft.saxophone.json.handler.*;

public final class PrettyPrinter
        implements ResetHook,
        ObjectStartHandler, ObjectEndHandler,
        ArrayStartHandler, ArrayEndHandler,
        ObjectKeyHandler, StringValueHandler,
        NumberHandler, BooleanHandler, NullHandler {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private int indent = 0;
    private int indentStep = 4;
    private StringBuilder sb = new StringBuilder();

    @Override
    public String toString() {
        return sb.toString();
    }

    @Override
    public void onReset() {
        indent = 0;
        sb.setLength(0);
    }

    private PrettyPrinter printIndent() {
        for (int i = 0; i < indent; i++) {
            sb.append(' ');
        }
        return this;
    }

    private PrettyPrinter newLine() {
        sb.append(LINE_SEPARATOR);
        return this;
    }

    private char last() {
        return sb.charAt(sb.length() - 1);
    }

    private void beforeValue() {
        if (sb.length() == 0 || indent == 0)
            return;
        if (last() == ':') {
            sb.append(' ');

        } else {
            if (last() != '[')
                sb.append(',');
            newLine().printIndent();
        }
    }

    private boolean start(char open) {
        beforeValue();
        sb.append(open);
        indent += indentStep;
        return true;
    }

    @Override
    public boolean onObjectStart() {
        return start('{');
    }

    @Override
    public boolean onArrayStart() {
        return start('[');
    }

    private boolean end(char open, char close) {
        indent -= indentStep;
        if (last() != open)
            newLine().printIndent();
        sb.append(close);
        return true;
    }

    @Override
    public boolean onArrayEnd() {
        return end('[', ']');
    }

    @Override
    public boolean onObjectEnd() {
        return end('{', '}');
    }

    @Override
    public boolean onObjectKey(CharSequence key) {
        if (last() != '{')
            sb.append(',');
        newLine().printIndent();
        sb.append('"').append(key).append('"').append(':');
        return true;
    }

    private boolean onValue(CharSequence value, boolean quotes) {
        beforeValue();
        if (quotes) sb.append('"');
        sb.append(value);
        if (quotes) sb.append('"');
        return true;
    }

    @Override
    public boolean onBoolean(boolean value) {
        return onValue(value ? "true" : "false", false);
    }

    @Override
    public boolean onNull() {
        return onValue("null", false);
    }

    @Override
    public boolean onNumber(CharSequence number) {
        return onValue(number, false);
    }

    @Override
    public boolean onStringValue(CharSequence value) {
        return onValue(value, true);
    }
}
