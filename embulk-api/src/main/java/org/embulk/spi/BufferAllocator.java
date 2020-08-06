/*
 * Copyright 2014 The Embulk project
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

package org.embulk.spi;

/**
 * Represents an allocator of {@link org.embulk.spi.Buffer}.
 */
public interface BufferAllocator {
    /**
     * Allocates a {@link org.embulk.spi.Buffer} with the default size of this {@link org.embulk.spi.BufferAllocator}.
     *
     * @return {@link org.embulk.spi.Buffer} allocated
     */
    Buffer allocate();

    /**
     * Allocates a {@link org.embulk.spi.Buffer} with the size of {@code minimumCapacity} at least.
     *
     * @param minimumCapacity  the minimum size of the {@link org.embulk.spi.Buffer} allocated
     * @return {@link org.embulk.spi.Buffer} allocated
     */
    Buffer allocate(int minimumCapacity);
}
