/*
 * Copyright 2011 zhongl
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.github.zhongl.accessor;

import java.nio.ByteBuffer;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class CommonAccessors {
    public static final Accessor<Long> LONG = new LongAccessor();
    public static final Accessor<String> STRING = new StringAccessor();

    private CommonAccessors() {}

    private static class LongAccessor extends AbstractAccessor<Long> {

        private LongAccessor() {}

        @Override
        public int byteLengthOf(Long object) {
            return 8;
        }

        @Override
        public Long read(ByteBuffer buffer) {
            return buffer.getLong();
        }

        @Override
        protected void doWrite(Long object, ByteBuffer buffer) {
            buffer.putLong(object);
        }
    }

    private static class StringAccessor extends AbstractAccessor<String> {

        private static final int LENGTH_BYTES = 4;

        @Override
        public int byteLengthOf(String object) {
            return LENGTH_BYTES + object.length();
        }

        @Override
        public String read(ByteBuffer buffer) {
            int length = buffer.getInt();
            byte[] bytes = new byte[length];
            buffer.get(bytes);
            return new String(bytes);
        }

        @Override
        protected void doWrite(String object, ByteBuffer buffer) {
            buffer.putInt(object.length());
            buffer.put(object.getBytes());
        }
    }
}
