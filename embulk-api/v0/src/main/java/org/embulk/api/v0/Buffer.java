/*
 * Copyright 2019 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.api.v0;

public interface Buffer {
    byte[] arrayUnsafe();

    int offset();

    Buffer offset(int offset);

    int limit();

    Buffer limit(int limit);

    int capacity();

    void setBytes(int index, byte[] source, int sourceIndex, int length);

    void setBytes(int index, Buffer source, int sourceIndex, int length);

    void getBytes(int index, byte[] dest, int destIndex, int length);

    void getBytes(int index, Buffer dest, int destIndex, int length);

    void release();
}
