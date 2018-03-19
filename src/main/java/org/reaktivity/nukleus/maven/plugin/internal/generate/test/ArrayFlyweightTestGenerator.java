package org.reaktivity.nukleus.maven.plugin.internal.generate.test;

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

import static com.squareup.javapoet.TypeSpec.classBuilder;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import com.squareup.javapoet.*;
import org.agrona.BitUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reaktivity.nukleus.maven.plugin.internal.generate.ClassSpecGenerator;


public final class ArrayFlyweightTestGenerator extends ClassSpecGenerator
{
    private final TypeSpec.Builder classBuilder;
    private final ClassName typeFlyweightClassName;
    private final ClassName typeFlyweightBuilderClassName;
    private final ClassName flyweightType;

    public ArrayFlyweightTestGenerator(
            ClassName flyweightType,
            String baseName)
    {
        super(flyweightType.peerClass(baseName + "Test"));
        this.flyweightType = flyweightType;
        this.typeFlyweightClassName = flyweightType.peerClass(baseName);
        this.typeFlyweightBuilderClassName = flyweightType.peerClass(baseName + ".Builder");
        this.classBuilder = classBuilder(thisName).addModifiers(PUBLIC, FINAL);
    }


    @Override
    public TypeSpec generate()
    {
        return classBuilder.addField(fieldConstantLengthSize())
                .addField(fieldBuffer())
                .addField(fieldExpected())
                .addField(fieldExpectedException())
                .addField(fieldRW())
                .addField(fieldRWWithByteOrder())
                .addField(fieldRO())
                .addField(fieldROWithByteOrder())
                .addMethod(shouldBuildEmptyList(false))
                .addMethod(shouldBuildEmptyList(true))
                .addMethod(shouldReadEmptyList(false))
                .addMethod(shouldReadEmptyList(true))
                .addMethod(shouldMatchItems(true))
                .addMethod(shouldMatchItems(false))
                .addMethod(shouldMatchFirstItem(true))
                .addMethod(shouldMatchFirstItem(false))
                .addMethod(shouldFailWrapWhenListSizeIsNegative())
                .addMethod(shouldSetItems())
                .addMethod(shouldReadItems())
                .addMethod(shouldDefaultToEmptyAfterRewrap())
                .addMethod(shouldFailToWrapWithInsufficientLength())
                .addMethod(shouldWrapWithSufficientLength())
                .addMethod(shouldFailToAddItemWhenExceedsMaxLimit())
                .addMethod(shouldDisplayAsString())
                .build();
    }

    private FieldSpec fieldBuffer()
    {
        return FieldSpec.builder(MutableDirectBuffer.class, "buffer", PRIVATE, FINAL)
                .initializer("new $T($T.allocateDirect(100)); \n" +
                        "{\n" +
                        "    buffer.setMemory(0, buffer.capacity(), (byte) 0xF);\n" +
                        "}", UnsafeBuffer.class, ByteBuffer.class)
                .build();
    }

    private FieldSpec fieldExpected()
    {
        return FieldSpec.builder(MutableDirectBuffer.class, "expected", PRIVATE, FINAL)
                .initializer("new $T($T.allocateDirect(100)); \n" +
                        "{\n" +
                        "    expected.setMemory(0, expected.capacity(), (byte) 0xF);\n" +
                        "}", UnsafeBuffer.class, ByteBuffer.class)
                .build();
    }

    private FieldSpec fieldConstantLengthSize()
    {
        return FieldSpec.builder(int.class, "LENGTH_SIZE", PRIVATE, STATIC, FINAL)
                .initializer("4")
                .build();
    }

    private FieldSpec fieldExpectedException()
    {
        return FieldSpec.builder(ExpectedException.class, "expectedException", PUBLIC)
                .addAnnotation(Rule.class)
                .initializer("$T.none()", ExpectedException.class)
                .build();
    }

    private FieldSpec fieldRW()
    {
        TypeName builderClass = ParameterizedTypeName.get(
                typeFlyweightBuilderClassName, flyweightType.peerClass("Varint64FW.Builder"),
                flyweightType.peerClass("Varint64FW"));
        return FieldSpec
                .builder(builderClass, "fieldRW", PRIVATE, FINAL)
                .initializer("new $T<>(new $T(), new $T())", typeFlyweightBuilderClassName,
                        flyweightType.peerClass("Varint64FW.Builder"), flyweightType.peerClass("Varint64FW"))
                .build();
    }

    private FieldSpec fieldRWWithByteOrder()
    {
        TypeName builderClass = ParameterizedTypeName.get(
                typeFlyweightBuilderClassName, flyweightType.peerClass("Varint64FW.Builder"),
                flyweightType.peerClass("Varint64FW"));
        return FieldSpec
                .builder(builderClass, "fieldByteOrderRW", PRIVATE, FINAL)
                .initializer("new $T<>(new $T(), new $T(), $T.nativeOrder())", typeFlyweightBuilderClassName,
                        flyweightType.peerClass("Varint64FW.Builder"), flyweightType.peerClass("Varint64FW"),
                        ByteOrder.class)
                .build();
    }


    private FieldSpec fieldRO()
    {
        TypeName builderClass = ParameterizedTypeName.get(
                typeFlyweightClassName, flyweightType.peerClass("Varint64FW"));
        return FieldSpec
                .builder(builderClass, "fieldRO", PRIVATE, FINAL)
                .initializer("new $T<>(new $T())", typeFlyweightClassName, flyweightType.peerClass("Varint64FW"))
                .build();
    }

    private FieldSpec fieldROWithByteOrder()
    {
        TypeName builderClass = ParameterizedTypeName.get(
                typeFlyweightClassName, flyweightType.peerClass("Varint64FW"));
        return FieldSpec
                .builder(builderClass, "fieldByteOrderRO", PRIVATE, FINAL)
                .initializer("new $T<>(new $T(), $T.nativeOrder())", typeFlyweightClassName,
                        flyweightType.peerClass("Varint64FW"), ByteOrder.class)
                .build();
    }

    private MethodSpec shouldBuildEmptyList(boolean isBuiltWithByteOrder)
    {
        return MethodSpec.methodBuilder("shouldBuildEmptyList" + (isBuiltWithByteOrder ? "WithByteOrder" : ""))
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("$T offset = 12", int.class)
                .addStatement("$T limit = field$LRW.wrap(buffer, offset, buffer.capacity())\n" +
                        "                .build()\n" +
                        "                .limit();", int.class, (isBuiltWithByteOrder ? "ByteOrder" : ""))
                .addStatement("$T.assertEquals(offset + 4, limit)", Assert.class)
                .addStatement("expected.putInt(offset, 0)")
                .addStatement("$T.assertEquals(expected.byteBuffer(), buffer.byteBuffer())", Assert.class)
                .build();
    }

    private MethodSpec shouldReadEmptyList(boolean isBuiltWithByteOrder)
    {
        return MethodSpec.methodBuilder("shouldReadEmptyList" + (isBuiltWithByteOrder ? "WithByteOrder" : ""))
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("buffer.putInt(10,  0)")
                .addStatement("field$LRO.wrap(buffer, 10, buffer.capacity())", (isBuiltWithByteOrder ? "ByteOrder" : ""))
                .addStatement("$T.assertEquals(14, field$LRO.limit())", Assert.class, (isBuiltWithByteOrder ? "ByteOrder" : ""))
                .addStatement("$T<$T> contents = new $T<$T>()", List.class, Long.class, ArrayList.class, Long.class)
                .addStatement("field$LRO.forEach(v -> contents.add(v.value()))", (isBuiltWithByteOrder ? "ByteOrder" : ""))
                .addStatement("$T.assertTrue(field$LRO.isEmpty())", Assert.class, (isBuiltWithByteOrder ? "ByteOrder" : ""))
                .addStatement("$T.assertEquals(0, contents.size())", Assert.class)
                .build();
    }

    private MethodSpec shouldFailWrapWhenListSizeIsNegative()
    {
        return MethodSpec.methodBuilder("shouldFailWrapWhenListSizeIsNegative")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("buffer.putInt(10,  -1)")
                .addStatement("expectedException.expect($T.class)", IllegalArgumentException.class)
                .addStatement("expectedException.expectMessage(\"size < 0\")")
                .addStatement("fieldRO.wrap(buffer, 10, buffer.capacity())")
                .build();
    }

    private MethodSpec shouldSetItems()
    {
        return MethodSpec.methodBuilder("shouldSetItems")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("final $T offset = 0", int.class)
                .addStatement("final $T limit = fieldRW.wrap(buffer, offset, buffer.capacity())\n" +
                        "                .item(b -> b.set(1L))\n" +
                        "                .item(b -> b.set(-1L))\n" +
                        "                .item(b -> b.set(12L))\n" +
                        "                .build()\n" +
                        "                .limit()", int.class)
                .addStatement("final $T expectedSizeInBytes = LENGTH_SIZE + 3", int.class)
                .addStatement("$T.assertEquals(offset + expectedSizeInBytes, limit)", Assert.class)
                .addStatement("expected.putInt(offset, 3)")
                .addStatement("expected.putByte(offset + 4, (byte) 2)")
                .addStatement("expected.putByte(offset + 5, (byte) 1)")
                .addStatement("expected.putByte(offset + 6, (byte) 0x18)")
                .addStatement("$T[] bytes = new $T[buffer.capacity()]", byte.class, byte.class)
                .addStatement("expected.getBytes(0, bytes)")
                .addStatement("$T.assertEquals(expected.byteBuffer(), buffer.byteBuffer())", Assert.class)
                .build();
    }

    private MethodSpec shouldReadItems()
    {
        return MethodSpec.methodBuilder("shouldReadItems")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("final $T offset = 23", int.class)
                .addStatement("buffer.putInt(offset, 3)")
                .addStatement("buffer.putByte(offset + 4, (byte) 2)")
                .addStatement("buffer.putByte(offset + 5, (byte) 1)")
                .addStatement("buffer.putByte(offset + 6, (byte) 0x18)")
                .addStatement("fieldRO.wrap(buffer, offset, buffer.capacity())")
                .addStatement("$T.assertEquals(offset + LENGTH_SIZE + 3, fieldRO.limit())", Assert.class)
                .addStatement("$T<$T> contents = new $T<$T>()", List.class, Long.class, ArrayList.class, Long.class)
                .addStatement("fieldRO.forEach(v -> contents.add(v.value()))")
                .addStatement("$T.assertEquals(3, contents.size())", Assert.class)
                .addStatement("$T.assertEquals(1L, contents.get(0).longValue())", Assert.class)
                .addStatement("$T.assertEquals(-1L, contents.get(1).longValue())", Assert.class)
                .addStatement("$T.assertEquals(12L, contents.get(2).longValue())", Assert.class)
                .build();
    }

    private MethodSpec shouldMatchItems(boolean shouldMatch)
    {
        return MethodSpec.methodBuilder("should" + (shouldMatch ? "" : "Not") + "MatchItems")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("final $T offset = 23", int.class)
                .addStatement("buffer.putInt(offset, 1)")
                .addStatement("buffer.putByte(offset + 4, (byte) 2)")
                .addStatement("buffer.putByte(offset + 5, (byte) 4)")
                .addStatement("fieldRO.wrap(buffer, offset, buffer.capacity())")
                .addStatement("$T varint64FW = new $T()", flyweightType.peerClass("Varint64FW"),
                        flyweightType.peerClass("Varint64FW"))
                .addStatement("expected.putInt(offset, 1)")
                .addStatement("expected.putByte(offset + 4, (byte) $L)", (shouldMatch ? 2 : 3))
                .addStatement("varint64FW.wrap(expected, offset + 4, expected.capacity())")
                .beginControlFlow("$T<$T> varint64FWPredicate = p -> ", Predicate.class,
                        flyweightType.peerClass("Varint64FW"))
                .addStatement("return p.value() == varint64FW.value()")
                .endControlFlow("")
                .addStatement("$T.assert$L(fieldRO.anyMatch(varint64FWPredicate))", Assert.class,
                        (shouldMatch ? "True" : "False"))
                .build();
    }

    private MethodSpec shouldMatchFirstItem(boolean shouldMatch)
    {
         MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder("should" + (shouldMatch ? "" : "Not") + "MatchFirstItem")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("final $T offset = 23", int.class)
                .addStatement("buffer.putInt(offset, 2)")
                .addStatement("buffer.putByte(offset + 4, (byte) 2)")
                .addStatement("buffer.putByte(offset + 5, (byte) 4)")
                .addStatement("fieldRO.wrap(buffer, offset, buffer.capacity())")
                .addStatement("$T varint64FW = new $T()", flyweightType.peerClass("Varint64FW"),
                        flyweightType.peerClass("Varint64FW"))
                .addStatement("expected.putInt(offset, 1)")
                .addStatement("expected.putByte(offset + 4, (byte) $L)", (shouldMatch ? 2 : 3))
                .addStatement("varint64FW.wrap(expected, offset + 4, expected.capacity())")
                .beginControlFlow("$T<$T> varint64FWPredicate = p -> ", Predicate.class,
                        flyweightType.peerClass("Varint64FW"))
                .addStatement("return p.value() == varint64FW.value()")
                .endControlFlow("");
        if(shouldMatch)
        {
            methodBuilder.addStatement("$T.assertEquals(varint64FW.value(), fieldRO.matchFirst(varint64FWPredicate).value())",
                    Assert.class);
        }
        else
        {
            methodBuilder.addStatement("$T.assertNull(fieldRO.matchFirst(varint64FWPredicate))", Assert.class);
        }
        return methodBuilder.build();
    }

    private MethodSpec shouldDefaultToEmptyAfterRewrap()
    {
        return MethodSpec.methodBuilder("shouldDefaultToEmptyAfterRewrap")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("$T offset = 10", int.class)
                .addStatement("$T limit = fieldRW.wrap(buffer, offset, buffer.capacity())\n" +
                        "                .item(b -> b.set(12L))\n" +
                        "                .build()\n" +
                        "                .limit()", int.class)
                .addStatement("$T<$T> field = fieldRW.wrap(buffer, offset, limit)\n" +
                        "                .build()", typeFlyweightClassName, flyweightType.peerClass("Varint64FW"))
                .addStatement("$T.assertEquals(offset + LENGTH_SIZE, field.limit())", Assert.class)
                .addStatement("$T.assertEquals(LENGTH_SIZE, field.sizeof())", Assert.class)
                .addStatement("$T<$T> contents = new $T<$T>()", List.class, Long.class, ArrayList.class, Long.class)
                .addStatement("field.forEach(v -> contents.add(v.value()))")
                .addStatement("$T.assertEquals(0, contents.size())", Assert.class)
                .build();
    }

    private MethodSpec shouldFailToWrapWithInsufficientLength()
    {
        return MethodSpec.methodBuilder("shouldFailToWrapWithInsufficientLength")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("expectedException.expect($T.class)", IndexOutOfBoundsException.class)
                .addStatement("fieldRW.wrap(buffer, 10, 13)")
                .build();
    }

    private MethodSpec shouldWrapWithSufficientLength()
    {
        return MethodSpec.methodBuilder("shouldWrapWithSufficientLength")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("$T limit = fieldRW.wrap(buffer, 10, 10 + LENGTH_SIZE).limit()", int.class)
                .addStatement("$T.assertEquals(14, limit)", Assert.class)
                .build();
    }

    private MethodSpec shouldFailToAddItemWhenExceedsMaxLimit()
    {
        return MethodSpec.methodBuilder("shouldFailToAddItemWhenExceedsMaxLimit")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("expectedException.expect($T.class)", IndexOutOfBoundsException.class)
                .addStatement("buffer.setMemory(0,  buffer.capacity(), (byte) 0x00)")
                .addStatement("final $T offset = 10", int.class)
                .beginControlFlow("try")
                .addStatement("fieldRW.wrap(buffer, offset, offset + LENGTH_SIZE + 4)\n" +
                        "                .item(b -> b.set(Integer.MAX_VALUE))")
                .endControlFlow()
                .beginControlFlow("finally")
                .addStatement("$T[] bytes = new $T[1 + LENGTH_SIZE]", byte.class, byte.class)
                .addStatement("buffer.getBytes(offset, bytes)")
                .addStatement("$T.assertEquals(\"Buffer shows memory was written beyond maxLimit: \" + $T.toHex(bytes),\n" +
                        "                         0, buffer.getByte(10 + LENGTH_SIZE))", Assert.class, BitUtil.class)
                .endControlFlow()
                .build();
    }

    private MethodSpec shouldDisplayAsString()
    {
        return MethodSpec.methodBuilder("shouldDisplayAsString")
                .addModifiers(PUBLIC)
                .addAnnotation(Test.class)
                .addException(Exception.class)
                .addStatement("$T offset = 0", int.class)
                .addStatement("$T<$T> field = fieldRW.wrap(buffer, offset, buffer.capacity())\n" +
                        "                .item(b -> b.set(1L))\n" +
                        "                .item(b -> b.set(-1L))\n" +
                        "                .item(b -> b.set(123L))\n" +
                        "                .build()", typeFlyweightClassName, flyweightType.peerClass("Varint64FW"))
                .addStatement("$T.assertTrue(field.toString().equals(\"ARRAY containing 4 bytes of data\"))", Assert.class)
                .build();
    }
}