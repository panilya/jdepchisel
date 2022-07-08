package com.panilya.jdepchisel;

import com.panilya.jdepchisel.constantpool.ConstantPoolTags;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static com.panilya.jdepchisel.classreader.ClassReader.readClassBytes;

public class JDependencyChisel {

    public static void main(String[] args) {
        try {
            // Get dependencies for my class:
            Set<Class<?>> dependencies = getDependencies(Class
                    .forName("com.panilya.jdepchisel.JDependencyChisel"));  // REPLACE WITH YOUR CLASS NAME

            // Print the full class name for each interesting dependency:
            dependencies
                    .stream()
                    .filter(clazz -> !clazz.getCanonicalName().startsWith(
                            "java.lang")) // do not show java.lang dependencies,
                    // which add clutter
                    .forEach(c -> System.out.println(c.getCanonicalName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get the set of direct dependencies for the given class
     *
     * @param classToCheck
     * @return The direct dependencies for classToCheck, as a set of classes
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Set<Class<?>> getDependencies(final Class<?> classToCheck)
            throws IOException, ClassNotFoundException {
        Class<?> adjustedClassToCheck = adjustSourceClassIfArray(classToCheck);
        if (adjustedClassToCheck.isPrimitive()) {
            return Collections.emptySet();
        }
        return mapClassNamesToClasses(
                adjustedClassToCheck,
                getDependenciesFromClassBytes(readClassBytes(adjustedClassToCheck)));
    }

    private static Class<?> adjustSourceClassIfArray(final Class<?> sourceClass) {
        Class<?> adjustedSourceClass = sourceClass;
        while (adjustedSourceClass.isArray()) {
            adjustedSourceClass = sourceClass.getComponentType();
        }
        return adjustedSourceClass;
    }

    private static Set<Class<?>> mapClassNamesToClasses(Class<?> from,
                                                        Set<String> names) throws ClassNotFoundException {
        ClassLoader cl = from.getClassLoader();
        Set<Class<?>> classes = new HashSet<>(names.size());

        for (String name : names) {
            classes.add(Class.forName(name, false, cl));
        }
        classes.remove(from);// remove self-reference
        return classes;
    }

    private static Set<String> getDependenciesFromClassBytes(
            ByteBuffer byteBuffer) {
        verifyMagicFileTypeHeader(byteBuffer);
        final int constantPoolItemCount = getConstantPoolItemCount(byteBuffer);
        ConstantPoolItemFlags flags = new ConstantPoolItemFlags(constantPoolItemCount);
        flagConstantPoolItemsAsDependencies(byteBuffer, constantPoolItemCount, flags);
        return extractClassNamesFromConstantsBasedOnFlags(byteBuffer,
                constantPoolItemCount, flags);
    }

    private static void flagConstantPoolItemsAsDependencies(ByteBuffer byteBuffer,
                                                            final int constantPoolItemCount, ConstantPoolItemFlags flags) {
        for (int currentConstantIndex = 1; currentConstantIndex < constantPoolItemCount; currentConstantIndex++) {
            switch (byteBuffer.get()) {
                case ConstantPoolTags.CONSTANT_Utf8:
                    skipPastVariableLengthString(byteBuffer);
                    break;
                case ConstantPoolTags.CONSTANT_Integer:
                case ConstantPoolTags.CONSTANT_Float:
                case ConstantPoolTags.CONSTANT_Fieldref:
                case ConstantPoolTags.CONSTANT_Methodref:
                case ConstantPoolTags.CONSTANT_InterfaceMethodref:
                case ConstantPoolTags.CONSTANT_InvokeDynamic:
                    skipBytes(byteBuffer, 4);
                    break;
                case ConstantPoolTags.CONSTANT_Long:
                case ConstantPoolTags.CONSTANT_Double:
                    skipBytes(byteBuffer, 8);
                    currentConstantIndex++;
                    break;
                case ConstantPoolTags.CONSTANT_String:
                    skipBytes(byteBuffer, 2);
                    break;
                case ConstantPoolTags.CONSTANT_NameAndType:
                    skipBytes(byteBuffer, 2);// skip name, fall through to flag as a
                    // named type:
                case ConstantPoolTags.CONSTANT_MethodType:
                    flags.isNamedType.set(byteBuffer.getChar()); // flag as named type
                    break;
                case ConstantPoolTags.CONSTANT_Class:
                    flags.isClass.set(byteBuffer.getChar()); // flag as class
                    break;
                case ConstantPoolTags.CONSTANT_MethodHandle:
                    skipBytes(byteBuffer, 3);
                    break;
                default:
                    throw new IllegalArgumentException("constant pool item type "
                            + (byteBuffer.get(byteBuffer.position() - 1) & 0xff));
            }
        }
        skipPastAccessFlagsThisClassAndSuperClass(byteBuffer);
        skipInterfaces(byteBuffer);
        flagFieldsAndMethodsAsNamedTypes(byteBuffer, flags.isNamedType);
    }

    private static int getConstantPoolItemCount(ByteBuffer byteBuffer) {
        setCursorToConstantPoolCountPosition(byteBuffer);
        return byteBuffer.getChar();
    }

    /**
     * @param byteBuffer
     */
    private static void skipInterfaces(ByteBuffer byteBuffer) {
        byteBuffer.position(byteBuffer.getChar() * 2 + byteBuffer.position());
    }

    /**
     * @param byteBuffer
     */
    private static void skipPastAccessFlagsThisClassAndSuperClass(
            ByteBuffer byteBuffer) {
        skipBytes(byteBuffer, 6);
    }

    /**
     * @param byteBuffer
     * @param numberOfConstants
     * @return
     * @throws AssertionError
     */
    private static HashSet<String> extractClassNamesFromConstantsBasedOnFlags(
            ByteBuffer byteBuffer, final int numberOfConstants, ConstantPoolItemFlags flags) throws AssertionError {
        HashSet<String> names = new HashSet<>();
        returnBufferToStartOfConstantPool(byteBuffer);
        for (int constantPoolIndex = 1; constantPoolIndex < numberOfConstants; constantPoolIndex++) {
            switch (byteBuffer.get()) {
                case ConstantPoolTags.CONSTANT_Utf8:
                    readClassNamesInUTF8Value(byteBuffer, flags,
                            names, constantPoolIndex);
                    break;
                case ConstantPoolTags.CONSTANT_Integer:
                case ConstantPoolTags.CONSTANT_Float:
                case ConstantPoolTags.CONSTANT_Fieldref:
                case ConstantPoolTags.CONSTANT_Methodref:
                case ConstantPoolTags.CONSTANT_InterfaceMethodref:
                case ConstantPoolTags.CONSTANT_NameAndType:
                case ConstantPoolTags.CONSTANT_InvokeDynamic:
                    skipBytes(byteBuffer, 4);
                    break;
                case ConstantPoolTags.CONSTANT_Long:
                case ConstantPoolTags.CONSTANT_Double:
                    skipBytes(byteBuffer, 8);
                    constantPoolIndex++; // long or double counts as 2 items
                    break;
                case ConstantPoolTags.CONSTANT_String:
                case ConstantPoolTags.CONSTANT_Class:
                case ConstantPoolTags.CONSTANT_MethodType:
                    skipBytes(byteBuffer, 2);
                    break;
                case ConstantPoolTags.CONSTANT_MethodHandle:
                    skipBytes(byteBuffer, 3);
                    break;
                default:
                    throw new AssertionError();
            }
        }
        return names;
    }

    /**
     * @param byteBuffer
     * @param dependencyClassNames
     * @param constantNumber
     */
    private static void readClassNamesInUTF8Value(ByteBuffer byteBuffer,
                                                  ConstantPoolItemFlags flags,
                                                  HashSet<String> dependencyClassNames, int constantNumber) {
        int strSize = byteBuffer.getChar(), strStart = byteBuffer.position();
        boolean multipleNames = flags.isNamedType.get(constantNumber);
        if (flags.isClass.get(constantNumber)) {
            if (byteBuffer.get(byteBuffer.position()) == ARRAY_START_CHAR) {
                multipleNames = true;
            } else {
                addClassNameToDependencySet(dependencyClassNames, byteBuffer,
                        strStart, strSize);
            }
        }
        if (multipleNames) {
            addClassNamesToDependencySet(dependencyClassNames, byteBuffer,
                    strStart, strSize);
        }
        byteBuffer.position(strStart + strSize);
    }

    /**
     * @param byteBuffer
     * @param isNamedType
     */
    private static void flagFieldsAndMethodsAsNamedTypes(ByteBuffer byteBuffer,
                                                         BitSet isNamedType) {
        for (int type = 0; type < 2; type++) { // fields and methods
            int numMember = byteBuffer.getChar();
            for (int member = 0; member < numMember; member++) {
                skipBytes(byteBuffer, 4);
                isNamedType.set(byteBuffer.getChar());
                int numAttr = byteBuffer.getChar();
                for (int attr = 0; attr < numAttr; attr++) {
                    skipBytes(byteBuffer, 2);
                    byteBuffer.position(byteBuffer.getInt()
                            + byteBuffer.position());
                }
            }
        }
    }

    /**
     * @param byteBuffer
     */
    private static void returnBufferToStartOfConstantPool(ByteBuffer byteBuffer) {
        byteBuffer.position(10);
    }

    private static void skipBytes(ByteBuffer byteBuffer, int bytesToSkip) {
        byteBuffer.position(byteBuffer.position() + bytesToSkip);
    }

    private static void skipPastVariableLengthString(ByteBuffer byteBuffer) {
        byteBuffer.position(byteBuffer.getChar() + byteBuffer.position());
    }

    private static void setCursorToConstantPoolCountPosition(
            ByteBuffer byteBuffer) {
        byteBuffer.position(8);
    }

    private static void verifyMagicFileTypeHeader(ByteBuffer byteBuffer) {
        if (byteBuffer.getInt() != 0xcafebabe) {
            throw new IllegalArgumentException("Not a class file");
        }
    }

    private static void addClassNameToDependencySet(HashSet<String> names,
                                                    ByteBuffer byteBuffer, int start, int length) {
        final int end = start + length;
        StringBuilder dst = new StringBuilder(length);
        ascii: {
            for (; start < end; start++) {
                byte b = byteBuffer.get(start);
                if (b < 0) {
                    break ascii;
                }
                dst.append((char) (b == '/' ? '.' : b));
            }
            names.add(dst.toString());
            return;
        }
        final int oldLimit = byteBuffer.limit(), oldPos = dst.length();
        byteBuffer.limit(end).position(start);
        dst.append(StandardCharsets.UTF_8.decode(byteBuffer));
        byteBuffer.limit(oldLimit);
        for (int pos = oldPos, len = dst.length(); pos < len; pos++) {
            if (dst.charAt(pos) == '/') {
                dst.setCharAt(pos, '.');
            }
        }
        names.add(dst.toString());
    }

    private static void addClassNamesToDependencySet(HashSet<String> names,
                                                     ByteBuffer byteBuffer, int start, int length) {
        final int end = start + length;
        for (; start < end; start++) {
            if (byteBuffer.get(start) == 'L') {
                int endMarkerPosition = start + 1;
                while (byteBuffer.get(endMarkerPosition) != ';') {
                    endMarkerPosition++;
                }
                addClassNameToDependencySet(names, byteBuffer, start + 1,
                        calculateLength(start, endMarkerPosition));
                start = endMarkerPosition;
            }
        }
    }

    private static int calculateLength(int start, int endMarkerPosition) {
        return endMarkerPosition - start - 1;
    }

    private static final char ARRAY_START_CHAR = '[';

    // encapsulate byte buffer with its read count:
    public static class Buffer {
        public byte[] buf = null;
        public int read = 0;

        // convert to ByteBuffer
        public ByteBuffer toByteBuffer() {
            return ByteBuffer.wrap(this.buf, 0, this.read);
        }
    }

    // flags for identifying dependency names in the constant pool
    private static class ConstantPoolItemFlags {
        final BitSet isClass;
        final BitSet isNamedType;

        ConstantPoolItemFlags(int constantPoolItemCount) {
            isClass = new BitSet(constantPoolItemCount);
            isNamedType = new BitSet(constantPoolItemCount);
        }
    }

}
