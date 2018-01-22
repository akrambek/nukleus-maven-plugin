/**
 * Copyright 2016-2017 The Reaktivity Project
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

import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reaktivity.reaktor.internal.test.types.inner.NestedFW;


public class NestedFWTest
{
    private static final int INDEX_FLAT = 1;
    private static final int INDEX_FIXED5 = 2;

    private final NestedFW.Builder nestedRW = new NestedFW.Builder();
    private final NestedFW nestedRO = new NestedFW();
    private final MutableDirectBuffer buffer = new UnsafeBuffer(allocateDirect(100));
    {
        // Make sure the code is not secretly relying upon memory being initialized to 0
        buffer.setMemory(0, buffer.capacity(), (byte) 0xab);
    }

    MutableDirectBuffer expected = new UnsafeBuffer(allocateDirect(100));
    {
        // Make sure the code is not secretly relying upon memory being initialized to 0
        expected.setMemory(0, expected.capacity(), (byte) 0xab);
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldSetAllValues() throws Exception
    {
        int offset = 11;
        int expectedLimit = setAllBufferValues(expected, offset);

        NestedFW.Builder builder1 = setAllBuilderValues(nestedRW.wrap(buffer, offset, expectedLimit));

        int limit = builder1.build().limit();
        assertEquals(expectedLimit, limit);
        assertEquals(expected, buffer);

        nestedRO.wrap(buffer,  offset,  limit);
        assertAllValues(nestedRO);
    }

    @Test
    public void shouldDefaultValues() throws Exception
    {
        int offset = 11;
        int expectedLimit = setAllRequiredBufferValues(expected, offset);

        NestedFW.Builder builder = setAllRequiredBuilderField(nestedRW.wrap(buffer, offset, expectedLimit), INDEX_FIXED5);
        int limit = builder.build().limit();

        assertEquals(expectedLimit, limit);
        assertEquals(expected, buffer);

        nestedRO.wrap(buffer,  offset,  limit);
        assertAllDefaultValues(nestedRO);
    }

    @Test
    public void shouldFailToSetFixed2BeforeFixed1() throws Exception
    {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("fixed1");
        nestedRW.wrap(buffer, 0, 100)
                .flat(flat -> flat
                    .fixed2(10)
                )
                .build()
                .limit();
    }

    @Test
    public void shouldFailToSetFixed5BeforeFlatFixed1() throws Exception
    {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("fixed1");
        nestedRW.wrap(buffer, 0, 100)
                .fixed5(50);
    }

    @Test
    public void shouldFailToResetFlat() throws Exception
    {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("flat");
        nestedRW.wrap(buffer, 0, 100)
            .flat(flat -> flat
                .fixed1(10)
                .string1("value1")
                .string2("value2")
            )
            .flat(flat ->
            { })
            .build();
    }

    @Test
    public void shouldFailToResetFixed4() throws Exception
    {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("fixed4");
        nestedRW.wrap(buffer, 0, 100)
            .fixed4(40)
            .fixed4(4)
            .build();
    }

    @Test
    public void shouldFailToBuildWhenFlatIsNotSet() throws Exception
    {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("fixed1"); // first required field in flat
        nestedRW.wrap(buffer, 0, 100)
            .fixed5(12L)
            .build();
    }

    @Test
    public void shouldFailToBuildWhenFixed5IsNotSet() throws Exception
    {
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("fixed5");
        nestedRW.wrap(buffer, 0, 100)
            .flat(flat -> flat
                .fixed1(10)
                .string1("value1")
                .string2("value2")
            )
            .build();
    }

    static int setAllBufferValues(MutableDirectBuffer buffer, int offset)
    {
        buffer.putLong(offset, 10);
        buffer.putLong(offset += 8, 20);
        buffer.putShort(offset += 8, (short) 30);
        buffer.putByte(offset += 2, (byte) "value1".length());
        buffer.putBytes(offset += 1, "value1".getBytes(UTF_8));
        buffer.putInt(offset+=6, 40);
        buffer.putByte(offset += 4, (byte) "value2".length());
        buffer.putBytes(offset += 1, "value2".getBytes(UTF_8));
        buffer.putLong(offset += 6, 50);

        return offset + 8;
    }

    static int setAllRequiredBufferValues(MutableDirectBuffer buffer, int offset)
    {
        buffer.putLong(offset, 444);
        buffer.putLong(offset += 8, 20);
        buffer.putShort(offset += 8, (short) 222);
        buffer.putByte(offset += 2, (byte) "value1".length());
        buffer.putBytes(offset += 1, "value1".getBytes(UTF_8));
        buffer.putInt(offset+=6, 333);
        buffer.putByte(offset += 4, (byte) "value2".length());
        buffer.putBytes(offset += 1, "value2".getBytes(UTF_8));
        buffer.putLong(offset += 6, 50);

        return offset + 8;
    }

    static NestedFW.Builder setAllRequiredBuilderField(NestedFW.Builder builder, int fieldIndex)
    {
        switch (fieldIndex)
        {
            case INDEX_FLAT: builder.flat(t -> t
                    .fixed1(20)
                    .string1("value1")
                    .string2("value2"));
                break;
            case INDEX_FIXED5:builder.flat(t -> t
                    .fixed1(20)
                    .string1("value1")
                    .string2("value2"));
                builder.fixed5(50);
                break;
        }

        return builder;
    }


    static NestedFW.Builder setAllBuilderValues(NestedFW.Builder builder)
    {
        return builder.fixed4(10)
                    .flat(flat -> flat
                    .fixed1(20)
                    .fixed2(30)
                    .string1("value1")
                    .fixed3(40)
                    .string2("value2"))
                    .fixed5(50);
    }

    static void assertAllValues (NestedFW nestedFW)
    {
        assertEquals(10, nestedFW.fixed4());
        assertEquals(20, nestedFW.flat().fixed1());
        assertEquals(30, nestedFW.flat().fixed2());
        assertEquals("value1", nestedFW.flat().string1().asString());
        assertEquals(40, nestedFW.flat().fixed3());
        assertEquals("value2", nestedFW.flat().string2().asString());
        assertEquals(50, nestedFW.fixed5());
    }

    static void assertAllDefaultValues (NestedFW nestedFW)
    {
        assertEquals(444, nestedFW.fixed4());
        assertEquals(222, nestedFW.flat().fixed2());
        assertEquals(333, nestedFW.flat().fixed3());
    }
}
