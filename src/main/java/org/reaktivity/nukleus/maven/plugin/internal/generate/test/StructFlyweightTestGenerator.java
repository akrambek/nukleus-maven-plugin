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
package org.reaktivity.nukleus.maven.plugin.internal.generate.test;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.TypeSpec.classBuilder;
import static java.util.Collections.unmodifiableMap;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static org.reaktivity.nukleus.maven.plugin.internal.ast.AstMemberNode.NULL_DEFAULT;
import static org.reaktivity.nukleus.maven.plugin.internal.generate.TypeNames.MUTABLE_DIRECT_BUFFER_TYPE;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.squareup.javapoet.*;
import org.agrona.BitUtil;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reaktivity.nukleus.maven.plugin.internal.ast.AstByteOrder;
import org.reaktivity.nukleus.maven.plugin.internal.ast.AstNode;
import org.reaktivity.nukleus.maven.plugin.internal.ast.AstNodeLocator;
import org.reaktivity.nukleus.maven.plugin.internal.ast.AstType;
import org.reaktivity.nukleus.maven.plugin.internal.generate.*;

public final class StructFlyweightTestGenerator extends ClassSpecGenerator
{

    private static final Set<String> RESERVED_METHOD_NAMES = new HashSet<>(Arrays.asList(new String[]
            {
                    "offset", "buffer", "limit", "sizeof", "maxLimit", "wrap", "checkLimit", "build"
            }));

    private static final ClassName INT_ITERATOR_CLASS_NAME = ClassName.get(PrimitiveIterator.OfInt.class);

    private static final ClassName LONG_ITERATOR_CLASS_NAME = ClassName.get(PrimitiveIterator.OfLong.class);


    private final String baseName;
    private final TypeSpec.Builder builder;
    private final MemberConstantGenerator memberConstant;
    private final TypeIdTestGenerator typeId;
    private final BufferGenerator buffer;
    private final ExpectedBufferGenerator expectedBuffer;
    private final ExpectedExceptionGenerator expectedException;
    private final FieldRWGenerator fieldRW;
    private final FieldROGenerator fieldRO;
    private final SetBufferValuesMethodGenerator setBufferValuesMethodGenerator;
    private final ToStringTestMethodGenerator toStringTestMethodGenerator;
    private final AstNodeLocator astNodeLocator;

    public StructFlyweightTestGenerator(
        ClassName structName,
        String baseName,
        AstNodeLocator astNodeLocator)
    {
        super(structName);
        this.baseName = baseName + "Test";
        this.builder = classBuilder(structName).addModifiers(PUBLIC, FINAL);
        this.memberConstant = new MemberConstantGenerator(structName, builder);
        this.typeId = new TypeIdTestGenerator(structName, builder); // should add tests for correct type set
        this.buffer = new BufferGenerator(structName, builder); // should add tests for correct type set
        this.expectedBuffer = new ExpectedBufferGenerator(structName, builder);
        this.expectedException = new ExpectedExceptionGenerator(structName, builder);
        this.fieldRW = new FieldRWGenerator(structName, builder, baseName);
        this.fieldRO = new FieldROGenerator(structName, builder, baseName);
        this.setBufferValuesMethodGenerator = new SetBufferValuesMethodGenerator(structName, builder);
        this.toStringTestMethodGenerator = new ToStringTestMethodGenerator(baseName);
        this.astNodeLocator = astNodeLocator;
    }

    public StructFlyweightTestGenerator typeId(
            int typeId)
    {
        this.typeId.typeId(typeId);
        return this;
    }

    public StructFlyweightTestGenerator addMember(
        AstType memberType,
        String name,
        TypeName type,
        TypeName unsignedType,
        int size,
        String sizeName,
        TypeName sizeType,
        boolean usedAsSize,
        Object defaultValue,
        AstByteOrder byteOrder)
    {
        if (memberType.isDynamic())
        {
            AstNode memberNode = astNodeLocator.locateNode(null, memberType.name(), null);
            System.out.println("memberNode for " + memberType.name() + " is " + memberNode);
        }

        try
        {
            memberConstant.addMember(name, type, unsignedType, size, sizeName, defaultValue);
            setBufferValuesMethodGenerator.addMember(name, type, unsignedType, usedAsSize, size, sizeName, sizeType,
                    byteOrder, defaultValue);
        }
        catch (UnsupportedOperationException uoe)
        {
        }
        return this;
    }

    @Override
    public TypeSpec generate()
    {
        // TODO: build fields and methods here
            memberConstant.build();
            buffer.build();
            expectedBuffer.build();
            fieldRW.build();
            fieldRO.build();
            expectedException.build();
            setBufferValuesMethodGenerator.build();


        return builder
                .addMethod(toStringTestMethodGenerator.generate())
                .build();
    }

    private static final class TypeIdTestGenerator extends ClassSpecMixinGenerator
    {
        private int typeId;

        private TypeIdTestGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public void typeId(
            int typeId)
        {
            this.typeId = typeId;
        }

        @Override
        public TypeSpec.Builder build()
        {
            if (typeId != 0)
            {
                builder.addField(FieldSpec.builder(int.class, "TYPE_ID", PUBLIC, STATIC, FINAL)
                        .initializer("$L", String.format("0x%08x", typeId))
                        .build());

                builder.addMethod(methodBuilder("typeId")
                        .addModifiers(PUBLIC)
                        .returns(int.class)
                        .addStatement("return TYPE_ID")
                        .build());
            }
            return builder;
        }
    }

    private static final class BufferGenerator extends ClassSpecMixinGenerator
    {

    private BufferGenerator(
            ClassName thisType,
            TypeSpec.Builder builder)
    {
        super(thisType, builder);
    }

    @Override
    public TypeSpec.Builder build()
    {
        FieldSpec bufferFieldSpec = FieldSpec.builder(MutableDirectBuffer.class, "buffer", PRIVATE, FINAL)
                .initializer("new $T($T.allocateDirect(100)); \n" +
                        "{\n"+
                        "    buffer.setMemory(0, buffer.capacity(), (byte) 0xF);\n" +
                        "}", UnsafeBuffer.class, ByteBuffer.class)
                .build();
        builder.addField(bufferFieldSpec);

        return builder;
    }
}

    private static final class ExpectedBufferGenerator extends ClassSpecMixinGenerator
    {

        private ExpectedBufferGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        @Override
        public TypeSpec.Builder build()
        {
            FieldSpec expectedBufferFieldSpec = FieldSpec.builder(MutableDirectBuffer.class, "expectedBuffer", PRIVATE, FINAL)
                    .initializer("new $T($T.allocateDirect(100)); \n" +
                            "{\n"+
                            "    expectedBuffer.setMemory(0, expectedBuffer.capacity(), (byte) 0xF);\n" +
                            "}", UnsafeBuffer.class, ByteBuffer.class)
                    .build();
            builder.addField(expectedBufferFieldSpec);

            return builder;
        }
    }

    private static final class ExpectedExceptionGenerator extends ClassSpecMixinGenerator
    {

        private ExpectedExceptionGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        @Override
        public TypeSpec.Builder build()
        {
            FieldSpec expectedBufferFieldSpec = FieldSpec.builder(ExpectedException.class, "expectedException", PUBLIC)
                    .addAnnotation(Rule.class)
                    .initializer("$T.none()", ExpectedException.class)
                    .build();
            builder.addField(expectedBufferFieldSpec);

            return builder;
        }
    }

    private static final class FieldRWGenerator extends ClassSpecMixinGenerator
    {
        private final ClassName fieldFWBuilderClassName;

        private FieldRWGenerator(
                ClassName thisType,
                TypeSpec.Builder builder,
                String baseName)
        {
            super(thisType, builder);
            fieldFWBuilderClassName = thisType.peerClass(baseName + "FW.Builder");
        }

        @Override
        public TypeSpec.Builder build()
        {
            FieldSpec fieldRWFieldSpec = FieldSpec.builder(fieldFWBuilderClassName, "fieldRW", PRIVATE, FINAL)
                    .initializer("new $T()", fieldFWBuilderClassName)
                    .build();
            builder.addField(fieldRWFieldSpec);

            return builder;
        }
    }

    private static final class FieldROGenerator extends ClassSpecMixinGenerator
    {
        private final ClassName fieldFWClassName;

        private FieldROGenerator(
                ClassName thisType,
                TypeSpec.Builder builder,
                String baseName)
        {
            super(thisType, builder);
            fieldFWClassName = thisType.peerClass(baseName + "FW");
        }

        @Override
        public TypeSpec.Builder build()
        {
            FieldSpec fieldRWFieldSpec = FieldSpec.builder(fieldFWClassName,  "fieldRO", PRIVATE, FINAL)
                    .initializer("new $T()", fieldFWClassName)
                    .build();
            builder.addField(fieldRWFieldSpec);
            return builder;
        }
    }

    private static final class MemberConstantGenerator extends ClassSpecMixinGenerator
    {
        private int nextIndex;
        private CodeBlock.Builder fieldsWithDefaultsInitializer = CodeBlock.builder();
        private List<String> fieldNames = new ArrayList<>();

        private MemberConstantGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public MemberConstantGenerator addMember(
                String name,
                TypeName type,
                TypeName unsignedType,
                int size,
                String sizeName,
                Object defaultValue)
        {
            builder.addField(
                    FieldSpec.builder(int.class, index(name), PRIVATE, STATIC, FINAL)
                            .initializer(Integer.toString(nextIndex++))
                            .build());
            fieldNames.add(name);
            boolean isOctetsType = isOctetsType(type);
            if (defaultValue != null && !isOctetsType)
            {
                Object defaultValueToSet = defaultValue == NULL_DEFAULT ? null : defaultValue;
                TypeName generateType = (unsignedType != null) ? unsignedType : type;
                if (size != -1 || sizeName != null)
                {
                    generateType = generateType == TypeName.LONG ? LONG_ITERATOR_CLASS_NAME
                            : INT_ITERATOR_CLASS_NAME;
                }
                if (isVarint32Type(type))
                {
                    generateType = TypeName.INT;
                }
                else if (isVarint64Type(type))
                {
                    generateType = TypeName.LONG;
                }
                builder.addField(
                        FieldSpec.builder(generateType, defaultName(name), PRIVATE, STATIC, FINAL)
                                .initializer(Objects.toString(defaultValueToSet))
                                .build());
                fieldsWithDefaultsInitializer.addStatement("set($L)", index(name));
            }
            else if (isImplicitlyDefaulted(type, size, sizeName))
            {
                fieldsWithDefaultsInitializer.addStatement("set($L)", index(name));
            }
            return this;
        }

        @Override
        public TypeSpec.Builder build()
        {
            builder.addField(FieldSpec.builder(String[].class, "FIELD_NAMES", PRIVATE, STATIC, FINAL)
                                    .initializer("{\n  \"" + String.join("\",\n  \"", fieldNames) + "\"\n}")
                                    .build());

            return super.build();
        }
    }

    private static final class SetBufferValuesMethodGenerator extends ClassSpecMixinGenerator
    {
        private static final Map<TypeName, String> PUTTER_NAMES;
        private static final Map<TypeName, String> TYPE_SIZE;
        private static final Map<TypeName, String> SIZEOF_BY_NAME = initSizeofByName();

        private boolean checkFollowingFieldsNotSetMethodIsRequired;

        static
        {
            Map<TypeName, String> putterNames = new HashMap<>();
            putterNames.put(TypeName.BYTE, "putByte");
            putterNames.put(TypeName.CHAR, "putChar");
            putterNames.put(TypeName.SHORT, "putShort");
            putterNames.put(TypeName.FLOAT, "putFloat");
            putterNames.put(TypeName.INT, "putInt");
            putterNames.put(TypeName.DOUBLE, "putDouble");
            putterNames.put(TypeName.LONG, "putLong");
            PUTTER_NAMES = unmodifiableMap(putterNames);
        }

        static
        {
            HashMap<TypeName, String> sizeNames = new HashMap<>();
            sizeNames.put(TypeName.BYTE, Integer.toString(BitUtil.SIZE_OF_BYTE));
            sizeNames.put(TypeName.CHAR, Integer.toString(BitUtil.SIZE_OF_CHAR));
            sizeNames.put(TypeName.SHORT, Integer.toString(BitUtil.SIZE_OF_SHORT));
            sizeNames.put(TypeName.FLOAT, Integer.toString(BitUtil.SIZE_OF_FLOAT));
            sizeNames.put(TypeName.INT, Integer.toString(BitUtil.SIZE_OF_INT));
            sizeNames.put(TypeName.DOUBLE, Integer.toString(BitUtil.SIZE_OF_DOUBLE));
            sizeNames.put(TypeName.LONG, Integer.toString(BitUtil.SIZE_OF_LONG));
            TYPE_SIZE = unmodifiableMap(sizeNames);
        }


        private final List<String> setAllBufferFormats = new LinkedList<>();
        private final List<String> setRequiredBufferFormats = new LinkedList<>();
        private int valueIncrement = 0;
        private String priorValueSize = "0";

        private SetBufferValuesMethodGenerator(
                ClassName thisType,
                TypeSpec.Builder builder)
        {
            super(thisType, builder);
        }

        public SetBufferValuesMethodGenerator addMember(
                String name,
                TypeName type,
                TypeName unsignedType,
                boolean usedAsSize,
                int size,
                String sizeName,
                TypeName sizeType,
                AstByteOrder byteOrder,
                Object defaultValue)
        {
            valueIncrement++;

            if (type.isPrimitive())
            {
                if (sizeName != null)
                {
                   //TODO: Add IntegerVariableArray
                }
                else if (size != -1)
                {
                    //TODO: Add integerFixedArrayIterator member
                }
                else
                {
                    //TODO: Add primitive member
                    String putterName = PUTTER_NAMES.get(type);
                    if (putterName == null)
                    {
                        throw new IllegalStateException("member type not supported: " + type);
                    }

                    setAllBufferFormats.add(String.format("buffer.%s(offset += %s, (%s) %d0)",
                            putterName,
                            priorValueSize,
                            SIZEOF_BY_NAME.get(type).toLowerCase(),
                            valueIncrement));
                    if(defaultValue != null)
                    {
                        setRequiredBufferFormats.add(String.format("buffer.%s(offset += %s, (%s) %s)",
                                putterName,
                                priorValueSize,
                                SIZEOF_BY_NAME.get(type).toLowerCase(),
                                defaultValue.toString()));
                    }
                    else
                    {
                        setRequiredBufferFormats.add(String.format("buffer.%s(offset += %s, (%s) %d0)",
                                putterName,
                                priorValueSize,
                                SIZEOF_BY_NAME.get(type).toLowerCase(),
                                valueIncrement));
                    }

                    priorValueSize = TYPE_SIZE.get(type);
                }
            }
            else
            {
               //TODO: Add nonprimative member
                setNonPremitiveValue(setAllBufferFormats);
                setNonPremitiveValue(setRequiredBufferFormats);
            }

            return this;
        }

        @Override
        public TypeSpec.Builder build()
        {
            addSetAllBufferValuesMember();
            addSetRequiredBufferValuesMember();

            return super.build();
        }

        private void setNonPremitiveValue(List<String> list)
        {
            String byteValue = String.format("buffer.putByte(offset += %s, (byte) \"value%d\".length())",
                    priorValueSize,
                    valueIncrement);
            String bytesValue = String.format("buffer.putBytes(offset += 1, \"value%d\".getBytes(%s.UTF_8))",
                    valueIncrement,
                    StandardCharsets.class.getName());
            list.add(byteValue);
            list.add(bytesValue);
            priorValueSize =  Integer.toString(("value"+valueIncrement).length());
        }

        private void addSetAllBufferValuesMember()
        {
            CodeBlock.Builder code = CodeBlock.builder();
            for (String format : setAllBufferFormats)
            {
                code.addStatement(format);
            }

            builder.addMethod(methodBuilder("setAllBufferValues")
                    .addModifiers(STATIC)
                    .addParameter(MUTABLE_DIRECT_BUFFER_TYPE, "buffer")
                    .addParameter(int.class, "offset")
                    .returns(int.class)
                    .addCode(code.build())
                    .addStatement("return offset + " + priorValueSize)
                    .build());

        }

        private void addSetRequiredBufferValuesMember()
        {
            CodeBlock.Builder code = CodeBlock.builder();
            for (String format : setRequiredBufferFormats)
            {
                code.addStatement(format);
            }

            builder.addMethod(methodBuilder("setRequiredBufferValues")
                    .addModifiers(STATIC)
                    .addParameter(MUTABLE_DIRECT_BUFFER_TYPE, "buffer")
                    .addParameter(int.class, "offset")
                    .returns(int.class)
                    .addCode(code.build())
                    .addStatement("return offset + " + priorValueSize)
                    .build());
        }
    }


    private final class ToStringTestMethodGenerator extends MethodSpecGenerator
    {
        private ToStringTestMethodGenerator(String baseName)
        {
            super(methodBuilder("shouldReportAllFieldValuesInToString")
                    .addAnnotation(Test.class)
                    .addModifiers(PUBLIC));
        }

        @Override
        public MethodSpec generate()
        {
            return builder.addStatement("final int offset = 11")
                   .addStatement("int limit = setAllBufferValues(buffer, offset)")
                   .addStatement("String result = fieldRO.wrap(buffer, offset, limit).toString()")
                   .addStatement("$T.assertNotNull(result)", Assert.class)
                   .addStatement(String.format("for (String fieldName : FIELD_NAMES)" +
                           "\n{" +
                           "\n  %s.%s" +
                           "\n}",
                           Assert.class.getName(),
                           "assertTrue(String.format(\"toString is missing %s\", fieldName), result.contains(fieldName));"))
                   .build();
        }
    }

    private static boolean isOctetsType(
            TypeName type)
    {
        return type instanceof ClassName && "OctetsFW".equals(((ClassName) type).simpleName());
    }

    private static boolean isStringType(
            ClassName classType)
    {
        String name = classType.simpleName();
        return ("StringFW".equals(name) || isString16Type(classType));
    }

    private static boolean isString16Type(
            ClassName classType)
    {
        String name = classType.simpleName();
        return "String16FW".equals(name);
    }

    private static boolean isVarintType(
            TypeName type)
    {
        return type instanceof ClassName && "Varint32FW".equals(((ClassName) type).simpleName())
                || type instanceof ClassName && "Varint64FW".equals(((ClassName) type).simpleName());
    }

    private static boolean isVarint32Type(
            TypeName type)
    {
        return type instanceof ClassName && "Varint32FW".equals(((ClassName) type).simpleName());
    }

    private static boolean isVarint64Type(
            TypeName type)
    {
        return type instanceof ClassName && "Varint64FW".equals(((ClassName) type).simpleName());
    }

    private static String index(
            String fieldName)
    {
        return String.format("INDEX_%s", constant(fieldName));
    }

    private static String initCap(String value)
    {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String arraySize(
            String fieldName)
    {
        return String.format("ARRAY_SIZE_%s", constant(fieldName));
    }

    private static String offset(
            String fieldName)
    {
        return String.format("FIELD_OFFSET_%s", constant(fieldName));
    }

    private static String size(
            String fieldName)
    {
        return String.format("FIELD_SIZE_%s", constant(fieldName));
    }

    private static String constant(
            String fieldName)
    {
        return fieldName.replaceAll("([^_A-Z])([A-Z])", "$1_$2").toUpperCase();
    }

    private static String dynamicLimit(String fieldName)
    {
        return "limit" + initCap(fieldName);
    }

    private static String iterator(String fieldName)
    {
        return "iterator" + initCap(fieldName);
    }

    private static ClassName iteratorClass(
            ClassName structName,
            TypeName type,
            TypeName unsignedType)
    {
        TypeName generateType = (unsignedType != null) ? unsignedType : type;
        return generateType == TypeName.LONG ? structName.nestedClass("LongPrimitiveIterator")
                : structName.nestedClass("IntPrimitiveIterator");
    }

    private static String methodName(String name)
    {
        return RESERVED_METHOD_NAMES.contains(name) ? name + "$" : name;
    }

    private static String appendMethodName(
            String fieldName)
    {
        return String.format("append%s", initCap(fieldName));
    }

    private static String defaultName(
            String fieldName)
    {
        return String.format("DEFAULT_%s", constant(fieldName));
    }

    private static boolean isImplicitlyDefaulted(
            TypeName type,
            int size,
            String sizeName)
    {
        boolean result = false;
        if (type instanceof ClassName && !isStringType((ClassName) type) && !isVarintType(type))
        {
            ClassName classType = (ClassName) type;
            if ("OctetsFW".equals(classType.simpleName()))
            {
                result = (size == -1 && sizeName == null);
            }
            else
            {
                result = true;
            }
        }
        if (type instanceof ParameterizedTypeName)
        {
            ParameterizedTypeName parameterizedType = (ParameterizedTypeName) type;
            if ("ListFW".equals(parameterizedType.rawType.simpleName())
                    || "ArrayFW".equals(parameterizedType.rawType.simpleName()))
            {
                result = true;
            }
        }
        return result;
    }

    private static String dynamicOffset(
            String fieldName)
    {
        return String.format("dynamicOffset%s", initCap(fieldName));
    }

    private static String dynamicValue(
            String fieldName)
    {
        return String.format("dynamicValue%s", initCap(fieldName));
    }

    private static Map<TypeName, String> initSizeofByName()
    {
        Map<TypeName, String> sizeofByName = new HashMap<>();
        sizeofByName.put(TypeName.BOOLEAN, "BOOLEAN");
        sizeofByName.put(TypeName.BYTE, "BYTE");
        sizeofByName.put(TypeName.CHAR, "CHAR");
        sizeofByName.put(TypeName.SHORT, "SHORT");
        sizeofByName.put(TypeName.INT, "INT");
        sizeofByName.put(TypeName.FLOAT, "FLOAT");
        sizeofByName.put(TypeName.LONG, "LONG");
        sizeofByName.put(TypeName.DOUBLE, "DOUBLE");
        return sizeofByName;
    }

}
