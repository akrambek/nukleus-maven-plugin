/**
 * Copyright 2016-2020 The Reaktivity Project
 *
 * The Reaktivity Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.reaktivity.nukleus.maven.plugin.internal.generated;

import java.util.function.Consumer;

import org.agrona.BitUtil;
import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

public final class Array8FW<V extends Flyweight> extends ArrayFW<V>
{
    private static final int LENGTH_SIZE = BitUtil.SIZE_OF_BYTE;

    private static final int FIELD_COUNT_SIZE = BitUtil.SIZE_OF_BYTE;

    private static final int LENGTH_OFFSET = 0;

    private static final int FIELD_COUNT_OFFSET = LENGTH_OFFSET + LENGTH_SIZE;

    private static final int FIELDS_OFFSET = FIELD_COUNT_OFFSET + FIELD_COUNT_SIZE;

    private static final int LENGTH_MAX_VALUE = 0xFF;

    private final V itemRO;

    private final DirectBuffer itemsRO = new UnsafeBuffer(0L, 0);

    private int maxLength;

    public Array8FW(
        V itemRO)
    {
        this.itemRO = itemRO;
    }

    @Override
    public int length()
    {
        return buffer().getByte(offset() + LENGTH_OFFSET);
    }

    @Override
    public int fieldsOffset()
    {
        return offset() + FIELDS_OFFSET;
    }

    @Override
    public int fieldCount()
    {
        return buffer().getByte(offset() + FIELD_COUNT_OFFSET);
    }

    @Override
    public int maxLength()
    {
        return maxLength;
    }

    @Override
    public void forEach(
        Consumer<V> consumer)
    {
        int offset = offset() + FIELDS_OFFSET;
        for (int i = 0; i < fieldCount(); i++)
        {
            itemRO.wrap(buffer(), offset, limit(), this);
            consumer.accept(itemRO);
            offset = itemRO.limit();
        }
    }

    @Override
    public DirectBuffer items()
    {
        return itemsRO;
    }

    @Override
    public Array8FW<V> wrap(
        DirectBuffer buffer,
        int offset,
        int maxLimit)
    {
        super.wrap(buffer, offset, maxLimit);
        final int itemsSize = limit() - fieldsOffset();
        itemsRO.wrap(buffer, offset + FIELDS_OFFSET, itemsSize);
        checkLimit(limit(), maxLimit);
        return this;
    }

    @Override
    public Array8FW<V> tryWrap(
        DirectBuffer buffer,
        int offset,
        int maxLimit)
    {
        if (super.tryWrap(buffer, offset, maxLimit) == null)
        {
            return null;
        }
        final int itemsSize = limit() - fieldsOffset();
        itemsRO.wrap(buffer, offset + FIELDS_OFFSET, itemsSize);
        if (limit() > maxLimit)
        {
            return null;
        }
        return this;
    }

    @Override
    public int limit()
    {
        return offset() + LENGTH_SIZE + length();
    }

    @Override
    public String toString()
    {
        return String.format("array8<%d, %d>", length(), fieldCount());
    }

    @Override
    protected void maxLength(
        int maxLength)
    {
        this.maxLength = maxLength;
    }

    public static final class Builder<B extends Flyweight.Builder<V>, V extends Flyweight>
        extends ArrayFW.Builder<Array8FW<V>, B, V>
    {
        private final B itemRW;

        private final V itemRO;

        private int fieldCount;

        private int maxLength;

        public Builder(
            B itemRW,
            V itemRO)
        {
            super(new Array8FW<>(itemRO));
            this.itemRW = itemRW;
            this.itemRO = itemRO;
        }

        @Override
        public int fieldsOffset()
        {
            return offset() + FIELDS_OFFSET;
        }

        @Override
        public Builder<B, V> item(
            Consumer<B> consumer)
        {
            itemRW.wrap(this);
            consumer.accept(itemRW);
            maxLength = Math.max(maxLength, itemRW.sizeof());
            checkLimit(itemRW.limit(), maxLimit());
            limit(itemRW.limit());
            fieldCount++;
            return this;
        }

        @Override
        public Builder<B, V> items(
            DirectBuffer buffer,
            int srcOffset,
            int length,
            int fieldCount,
            int maxLength)
        {
            buffer().putBytes(offset() + FIELDS_OFFSET, buffer, srcOffset, length);
            int newLimit = offset() + FIELDS_OFFSET + length;
            checkLimit(newLimit, maxLimit());
            limit(newLimit);
            this.fieldCount = fieldCount;
            this.maxLength = maxLength;
            assert length <= LENGTH_MAX_VALUE : "Length is too large";
            assert fieldCount <= LENGTH_MAX_VALUE : "Field count is too large";
            buffer().putByte(offset() + LENGTH_OFFSET, (byte) (length + FIELD_COUNT_SIZE));
            buffer().putByte(offset() + FIELD_COUNT_OFFSET, (byte) fieldCount);
            return this;
        }

        @Override
        public Builder<B, V> wrap(
            MutableDirectBuffer buffer,
            int offset,
            int maxLimit)
        {
            super.wrap(buffer, offset, maxLimit);
            int newLimit = offset + FIELDS_OFFSET;
            checkLimit(newLimit, maxLimit);
            limit(newLimit);
            return this;
        }

        @Override
        public Array8FW<V> build()
        {
            int length = limit() - offset() - FIELD_COUNT_OFFSET;
            assert length <= LENGTH_MAX_VALUE : "Length is too large";
            assert fieldCount <= LENGTH_MAX_VALUE : "Field count is too large";
            buffer().putByte(offset() + LENGTH_OFFSET, (byte) length);
            buffer().putByte(offset() + FIELD_COUNT_OFFSET, (byte) fieldCount);
            final ArrayFW<V> array = super.build();
            final int maxLimit = maxLimit();

            limit(fieldsOffset());
            int itemOffset = fieldsOffset();
            for (int i = 0; i < fieldCount; i++)
            {
                final Flyweight item = itemRO.wrap(buffer(), itemOffset, maxLimit, array);
                itemOffset = item.limit();

                final Flyweight newItem = itemRW.wrap(this)
                                                .rebuild((V) item, maxLength);
                final int newLimit = newItem.limit();
                assert newLimit <= itemOffset;
                limit(newLimit);
            }

            length = limit() - offset() - FIELD_COUNT_OFFSET;
            buffer().putByte(offset() + LENGTH_OFFSET, (byte) length);
            final Array8FW<V> array8 = super.build();
            array8.maxLength(maxLength);
            return array8;
        }
    }
}
