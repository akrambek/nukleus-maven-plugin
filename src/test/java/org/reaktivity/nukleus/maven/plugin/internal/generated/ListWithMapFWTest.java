/**
 * Copyright 2016-2019 The Reaktivity Project
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

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Test;
import org.reaktivity.reaktor.internal.test.types.inner.EnumWithInt8;

public class ListWithMapFWTest
{
    private final MutableDirectBuffer buffer = new UnsafeBuffer(allocateDirect(100))
    {
        {
            // Make sure the code is not secretly relying upon memory being initialized to 0
            setMemory(0, capacity(), (byte) 0xab);
        }
    };

    private final ListWithMapFW.Builder flyweightRW = new ListWithMapFW.Builder();
    private final ListWithMapFW flyweightRO = new ListWithMapFW();

    private static final EnumWithInt8 KIND_MAP8 = EnumWithInt8.THREE;
    private static final EnumWithInt8 KIND_STRING8 = EnumWithInt8.ONE;
    private static final EnumWithInt8 KIND_LIST8 = EnumWithInt8.TWO;
    private final int kindSize = Byte.BYTES;
    private final int lengthSize = Byte.BYTES;
    private final int fieldCountSize = Byte.BYTES;

    private int setAlFields(
        MutableDirectBuffer buffer,
        int offset)
    {
        byte listLength = 60;
        byte listFieldCount = 2;
        byte mapLength = 49;
        byte mapFieldCount = 4;
        String field1 = "field1";
        String entry1Key = "entry1Key";
        String entry1Value = "entry1Value";
        String entry2Key = "entry2Key";
        String entry2Value = "entry2Value";

        buffer.putByte(offset, KIND_LIST8.value());
        int offsetListLength = offset + kindSize;
        buffer.putByte(offsetListLength, listLength);
        int offsetListFieldCount = offsetListLength + kindSize;
        buffer.putByte(offsetListFieldCount, listFieldCount);

        int offsetField1Kind = offsetListFieldCount + fieldCountSize;
        buffer.putByte(offsetField1Kind, KIND_STRING8.value());
        int offsetField1Length = offsetField1Kind + kindSize;
        buffer.putByte(offsetField1Length, (byte) field1.length());
        int offsetField1 = offsetField1Length + Byte.BYTES;
        buffer.putBytes(offsetField1, field1.getBytes());

        int offsetMapKind = offsetField1 + (byte) field1.length();
        buffer.putByte(offsetMapKind, KIND_MAP8.value());
        int offsetLength = offsetMapKind + kindSize;
        buffer.putByte(offsetLength, mapLength);
        int offsetFieldCount = offsetLength + lengthSize;
        buffer.putByte(offsetFieldCount, mapFieldCount);

        int offsetMapEntry1KeyKind = offsetFieldCount + fieldCountSize;
        buffer.putByte(offsetMapEntry1KeyKind, EnumWithInt8.ONE.value());
        int offsetEntry1KeyLength = offsetMapEntry1KeyKind + Byte.BYTES;
        buffer.putByte(offsetEntry1KeyLength, (byte) entry1Key.length());
        int offsetEntry1Key = offsetEntry1KeyLength + Byte.BYTES;
        buffer.putBytes(offsetEntry1Key, entry1Key.getBytes());

        int offsetMapEntry1ValueKind = offsetEntry1Key + entry1Key.length();
        buffer.putByte(offsetMapEntry1ValueKind, EnumWithInt8.ONE.value());
        int offsetEntry1ValueLength = offsetMapEntry1ValueKind + Byte.BYTES;
        buffer.putByte(offsetEntry1ValueLength, (byte) entry1Value.length());
        int offsetEntry1Value = offsetEntry1ValueLength + Byte.BYTES;
        buffer.putBytes(offsetEntry1Value, entry1Value.getBytes());

        int offsetMapEntry2KeyKind = offsetEntry1Value + entry1Value.length();
        buffer.putByte(offsetMapEntry2KeyKind, EnumWithInt8.ONE.value());
        int offsetEntry2KeyLength = offsetMapEntry2KeyKind + Byte.BYTES;
        buffer.putByte(offsetEntry2KeyLength, (byte) entry2Key.length());
        int offsetEntry2Key = offsetEntry2KeyLength + Byte.BYTES;
        buffer.putBytes(offsetEntry2Key, entry2Key.getBytes());

        int offsetMapEntry2ValueKind = offsetEntry2Key + entry2Key.length();
        buffer.putByte(offsetMapEntry2ValueKind, EnumWithInt8.ONE.value());
        int offsetEntry2ValueLength = offsetMapEntry2ValueKind + Byte.BYTES;
        buffer.putByte(offsetEntry2ValueLength, (byte) entry2Value.length());
        int offsetEntry2Value = offsetEntry2ValueLength + Byte.BYTES;
        buffer.putBytes(offsetEntry2Value, entry2Value.getBytes());

        return offset + listLength + kindSize + lengthSize;
    }

    static void assertAllTestValuesRead(
        ListWithMapFW flyweight,
        int offset)
    {
        assertEquals(60, flyweight.length());
        assertEquals(2, flyweight.fieldCount());
        assertEquals(offset + 62, flyweight.limit());
        assertEquals("field1", flyweight.field1().asString());
        assertEquals(6, flyweight.field1().length());
        List<String> mapItems = new ArrayList<>();
        flyweight.map().forEach(kv -> vv ->
        {
            mapItems.add(kv.get().asString());
            mapItems.add(vv.get().asString());
        });
        assertEquals(49, flyweight.map().length());
        assertEquals(4, flyweight.map().fieldCount());
        assertEquals(4, mapItems.size());
        assertEquals("entry1Key", mapItems.get(0));
        assertEquals("entry1Value", mapItems.get(1));
        assertEquals("entry2Key", mapItems.get(2));
        assertEquals("entry2Value", mapItems.get(3));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void shouldNotWrapWhenLengthInsufficientForMinimumRequiredLength()
    {
        int length = 60;
        setAlFields(buffer, 10);
        for (int maxLimit = 10; maxLimit <= length; maxLimit++)
        {
            flyweightRO.wrap(buffer, 10, maxLimit);
        }
    }

    @Test
    public void shouldNotTryWrapWhenLengthInsufficientForMinimumRequiredLength()
    {
        int offsetLength = 10;
        int length = 60;
        setAlFields(buffer, offsetLength);
        for (int maxLimit = 10; maxLimit <= length; maxLimit++)
        {
            assertNull(flyweightRO.tryWrap(buffer, offsetLength, maxLimit));
        }
    }

    @Test
    public void shouldWrapWhenLengthSufficientForMinimumRequiredLength()
    {
        final int offset = 10;
        int size = setAlFields(buffer, offset);

        final ListWithMapFW listWithMap = flyweightRO.wrap(buffer, offset, size);

        assertSame(flyweightRO, listWithMap);
        assertAllTestValuesRead(listWithMap, offset);
    }

    @Test
    public void shouldTryWrapWhenLengthSufficientForMinimumRequiredLength()
    {
        final int offset = 10;
        int size = setAlFields(buffer, offset);

        final ListWithMapFW listWithMap = flyweightRO.tryWrap(buffer, offset, size);

        assertSame(flyweightRO, listWithMap);
        assertAllTestValuesRead(listWithMap, offset);
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenFieldIsSetOutOfOrder() throws Exception
    {
        flyweightRW.wrap(buffer, 0, buffer.capacity())
            .map(asMapFW())
            .field1(asStringFW("field1"))
            .build();
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenSameFieldIsSetMoreThanOnce() throws Exception
    {
        flyweightRW.wrap(buffer, 0, buffer.capacity())
            .field1(asStringFW("field1"))
            .map(asMapFW())
            .map(asMapFW())
            .build();
    }

    @Test(expected = AssertionError.class)
    public void shouldFailToBuildWhenRequiredFieldIsNotSet() throws Exception
    {
        flyweightRW.wrap(buffer, 0, buffer.capacity())
            .build();
    }

    @Test(expected = AssertionError.class)
    public void shouldFailWhenRequiredFieldIsNotSet() throws Exception
    {
        flyweightRW.wrap(buffer, 0, buffer.capacity())
            .map(asMapFW());
    }

    @Test(expected = AssertionError.class)
    public void shouldAssertErrorWhenValueNotPresent() throws Exception
    {
        int limit = flyweightRW.wrap(buffer, 0, buffer.capacity())
            .field1(asStringFW("field1"))
            .build()
            .limit();

        final ListWithMapFW listWithMap = flyweightRO.wrap(buffer, 0, limit);

        listWithMap.map();
    }

    @Test
    public void shouldSetAllFields() throws Exception
    {
        int limit = flyweightRW.wrap(buffer, 0, buffer.capacity())
            .field1(asStringFW("field1"))
            .map(asMapFW())
            .build()
            .limit();

        final ListWithMapFW listWithMap = flyweightRO.wrap(buffer, 0, limit);

        assertAllTestValuesRead(listWithMap, 0);
    }

    private static StringFW asStringFW(
        String value)
    {
        int length = value.length();
        int highestByteIndex = Integer.numberOfTrailingZeros(Integer.highestOneBit(length)) >> 3;
        MutableDirectBuffer buffer;
        switch (highestByteIndex)
        {
        case 0:
            buffer = new UnsafeBuffer(allocateDirect(Byte.SIZE + value.length()));
            return new String8FW.Builder().wrap(buffer, 0, buffer.capacity()).set(value, UTF_8).build();
        case 1:
            buffer = new UnsafeBuffer(allocateDirect(Short.SIZE + value.length()));
            return new String16FW.Builder().wrap(buffer, 0, buffer.capacity()).set(value, UTF_8).build();
        case 2:
        case 3:
            buffer = new UnsafeBuffer(allocateDirect(Integer.SIZE + value.length()));
            return new String32FW.Builder().wrap(buffer, 0, buffer.capacity()).set(value, UTF_8).build();
        default:
            throw new IllegalArgumentException("Illegal value: " + value);
        }
    }

    private static MapFW<VariantEnumKindWithString32FW, TypedefStringFW> asMapFW()
    {
        VariantOfMapFW.Builder<VariantEnumKindWithString32FW.Builder, VariantEnumKindWithString32FW, EnumWithInt8,
            StringFW, TypedefStringFW.Builder, TypedefStringFW, EnumWithInt8, StringFW> variantOfMapRW =
            new VariantOfMapFW.Builder<>(new VariantEnumKindWithString32FW.Builder(), new VariantEnumKindWithString32FW(),
                new TypedefStringFW.Builder(), new TypedefStringFW());
        MutableDirectBuffer buffer = new UnsafeBuffer(allocateDirect(100));
        VariantOfMapFW<VariantEnumKindWithString32FW, TypedefStringFW> variantOfMapRO = variantOfMapRW.wrap(buffer, 0,
            buffer.capacity())
            .entry(asStringFW("entry1Key"), asStringFW("entry1Value"))
            .entry(asStringFW("entry2Key"), asStringFW("entry2Value"))
            .build();
        return variantOfMapRO.get();
    }
}
