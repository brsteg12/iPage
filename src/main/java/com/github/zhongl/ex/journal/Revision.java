/*
 * Copyright 2012 zhongl
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

package com.github.zhongl.ex.journal;

import com.github.zhongl.ex.page.Number;

/** @author <a href="mailto:zhong.lunfu@gmail.com">zhongl<a> */
public class Revision extends Number<Revision> {

    private final Long value;

    public Revision(long value) {
        this.value = value;
    }

    public Revision increment() {
        return new Revision(value + 1);
    }

    public long value() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public int compareTo(Revision o) {
        return value.compareTo(o.value);
    }

}