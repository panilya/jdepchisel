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
            ByteBuffer readBuffer) {
        verifyMagicFileTypeHeader(readBuffer);
        final int constantPoolItemCount = getConstantPoolItemCount(readBuffer);
        ConstantPoolItemFlags flags = new ConstantPoolItemFlags(constantPoolItemCount);
        flagConstantPoolItemsAsDependencies(readBuffer, constantPoolItemCount, flags);
        return extractClassNamesFromConstantsBasedOnFlags(readBuffer,
                constantPoolItemCount, flags);
    }

    private static void flagConstantPoolItemsAsDependencies(ByteBuffer readBuffer,
                                                            final int constantPoolItemCount, ConstantPoolItemFlags flags) {
        for (int c = 1; c < constantPoolItemCount; c++) {
            c = readOneConstantPoolItemAndSetFlagIfClassOrNamedType(readBuffer,
                    flags, c);
        }
        skipPastAccessFlagsThisClassAndSuperClass(readBuffer);
        skipInterfaces(readBuffer);
        flagFieldsAndMethodsAsNamedTypes(readBuffer, flags.isNamedType);
    }

    private static int getConstantPoolItemCount(ByteBuffer readBuffer) {
        setCursorToConstantPoolCountPosition(readBuffer);
        return readBuffer.getChar();
    }

    /**
     * @param readBuffer
     */
    private static void skipInterfaces(ByteBuffer readBuffer) {
        readBuffer.position(readBuffer.getChar() * 2 + readBuffer.position());
    }

    /**
     * @param readBuffer
     */
    private static void skipPastAccessFlagsThisClassAndSuperClass(
            ByteBuffer readBuffer) {
        skipBytes(readBuffer, 6);
    }

    /**
     * @param readBuffer
     * @param numberOfConstants
     * @return
     * @throws AssertionError
     */
    private static HashSet<String> extractClassNamesFromConstantsBasedOnFlags(
            ByteBuffer readBuffer, final int numberOfConstants, ConstantPoolItemFlags flags) throws AssertionError {
        HashSet<String> names = new HashSet<>();
        returnBufferToStartOfConstantPool(readBuffer);
        for (int constantPoolIndex = 1; constantPoolIndex < numberOfConstants; constantPoolIndex++) {
            switch (readBuffer.get()) {
                case ConstantPoolTags.CONSTANT_Utf8:
                    readClassNamesInUTF8Value(readBuffer, flags,
                            names, constantPoolIndex);
                    break;
                case ConstantPoolTags.CONSTANT_Integer:
                case ConstantPoolTags.CONSTANT_Float:
                case ConstantPoolTags.CONSTANT_Fieldref:
                case ConstantPoolTags.CONSTANT_Methodref:
                case ConstantPoolTags.CONSTANT_InterfaceMethodref:
                case ConstantPoolTags.CONSTANT_NameAndType:
                case ConstantPoolTags.CONSTANT_InvokeDynamic:
                    skipBytes(readBuffer, 4);
                    break;
                case ConstantPoolTags.CONSTANT_Long:
                case ConstantPoolTags.CONSTANT_Double:
                    skipBytes(readBuffer, 8);
                    constantPoolIndex++; // long or double counts as 2 items
                    break;
                case ConstantPoolTags.CONSTANT_String:
                case ConstantPoolTags.CONSTANT_Class:
                case ConstantPoolTags.CONSTANT_MethodType:
                    skipBytes(readBuffer, 2);
                    break;
                case ConstantPoolTags.CONSTANT_MethodHandle:
                    skipBytes(readBuffer, 3);
                    break;
                default:
                    throw new AssertionError();
            }
        }
        return names;
    }

    /**
     * @param readBuffer
     * @param dependencyClassNames
     * @param constantNumber
     */
    private static void readClassNamesInUTF8Value(ByteBuffer readBuffer,
                                                  ConstantPoolItemFlags flags,
                                                  HashSet<String> dependencyClassNames, int constantNumber) {
        int strSize = readBuffer.getChar(), strStart = readBuffer.position();
        boolean multipleNames = flags.isNamedType.get(constantNumber);
        if (flags.isClass.get(constantNumber)) {
            if (readBuffer.get(readBuffer.position()) == ARRAY_START_CHAR) {
                multipleNames = true;
            } else {
                addClassNameToDependencySet(dependencyClassNames, readBuffer,
                        strStart, strSize);
            }
        }
        if (multipleNames) {
            addClassNamesToDependencySet(dependencyClassNames, readBuffer,
                    strStart, strSize);
        }
        readBuffer.position(strStart + strSize);
    }

    /**
     * @param readBuffer
     * @param isNamedType
     */
    private static void flagFieldsAndMethodsAsNamedTypes(ByteBuffer readBuffer,
                                                         BitSet isNamedType) {
        for (int type = 0; type < 2; type++) { // fields and methods
            int numMember = readBuffer.getChar();
            for (int member = 0; member < numMember; member++) {
                skipBytes(readBuffer, 4);
                isNamedType.set(readBuffer.getChar());
                int numAttr = readBuffer.getChar();
                for (int attr = 0; attr < numAttr; attr++) {
                    skipBytes(readBuffer, 2);
                    readBuffer.position(readBuffer.getInt()
                            + readBuffer.position());
                }
            }
        }
    }

    /**
     * @param readBuffer
     */
    private static void returnBufferToStartOfConstantPool(ByteBuffer readBuffer) {
        readBuffer.position(10);
    }

    /**
     * @param readBuffer
     * @param currentConstantIndex
     * @return
     */
    private static int readOneConstantPoolItemAndSetFlagIfClassOrNamedType(
            ByteBuffer readBuffer, ConstantPoolItemFlags flags,
            int currentConstantIndex) {
        switch (readBuffer.get()) {
            case ConstantPoolTags.CONSTANT_Utf8:
                skipPastVariableLengthString(readBuffer);
                break;
            case ConstantPoolTags.CONSTANT_Integer:
            case ConstantPoolTags.CONSTANT_Float:
            case ConstantPoolTags.CONSTANT_Fieldref:
            case ConstantPoolTags.CONSTANT_Methodref:
            case ConstantPoolTags.CONSTANT_InterfaceMethodref:
            case ConstantPoolTags.CONSTANT_InvokeDynamic:
                skipBytes(readBuffer, 4);
                break;
            case ConstantPoolTags.CONSTANT_Long:
            case ConstantPoolTags.CONSTANT_Double:
                skipBytes(readBuffer, 8);
                currentConstantIndex++;
                break;
            case ConstantPoolTags.CONSTANT_String:
                skipBytes(readBuffer, 2);
                break;
            case ConstantPoolTags.CONSTANT_NameAndType:
                skipBytes(readBuffer, 2);// skip name, fall through to flag as a
                // named type:
            case ConstantPoolTags.CONSTANT_MethodType:
                flags.isNamedType.set(readBuffer.getChar()); // flag as named type
                break;
            case ConstantPoolTags.CONSTANT_Class:
                flags.isClass.set(readBuffer.getChar()); // flag as class
                break;
            case ConstantPoolTags.CONSTANT_MethodHandle:
                skipBytes(readBuffer, 3);
                break;
            default:
                throw new IllegalArgumentException("constant pool item type "
                        + (readBuffer.get(readBuffer.position() - 1) & 0xff));
        }
        return currentConstantIndex;
    }

    private static void skipBytes(ByteBuffer readBuffer, int bytesToSkip) {
        readBuffer.position(readBuffer.position() + bytesToSkip);
    }

    private static void skipPastVariableLengthString(ByteBuffer readBuffer) {
        readBuffer.position(readBuffer.getChar() + readBuffer.position());
    }

    private static void setCursorToConstantPoolCountPosition(
            ByteBuffer readBuffer) {
        readBuffer.position(8);
    }

    private static void verifyMagicFileTypeHeader(ByteBuffer readBuffer) {
        if (readBuffer.getInt() != 0xcafebabe) {
            throw new IllegalArgumentException("Not a class file");
        }
    }

    private static void addClassNameToDependencySet(HashSet<String> names,
                                                    ByteBuffer readBuffer, int start, int length) {
        final int end = start + length;
        StringBuilder dst = new StringBuilder(length);
        ascii: {
            for (; start < end; start++) {
                byte b = readBuffer.get(start);
                if (b < 0) {
                    break ascii;
                }
                dst.append((char) (b == '/' ? '.' : b));
            }
            names.add(dst.toString());
            return;
        }
        final int oldLimit = readBuffer.limit(), oldPos = dst.length();
        readBuffer.limit(end).position(start);
        dst.append(StandardCharsets.UTF_8.decode(readBuffer));
        readBuffer.limit(oldLimit);
        for (int pos = oldPos, len = dst.length(); pos < len; pos++) {
            if (dst.charAt(pos) == '/') {
                dst.setCharAt(pos, '.');
            }
        }
        names.add(dst.toString());
        return;
    }

    private static void addClassNamesToDependencySet(HashSet<String> names,
                                                     ByteBuffer readBuffer, int start, int length) {
        final int end = start + length;
        for (; start < end; start++) {
            if (readBuffer.get(start) == 'L') {
                int endMarkerPosition = start + 1;
                while (readBuffer.get(endMarkerPosition) != ';') {
                    endMarkerPosition++;
                }
                addClassNameToDependencySet(names, readBuffer, start + 1,
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
