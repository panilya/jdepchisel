package com.panilya.jdepchisel.constantpool;

import com.panilya.jdepchisel.classloader.ClassLoader;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class ConstantPoolReader {

    private final static int magicHeader = 0xcafebabe;

    private final ClassLoader classLoader;

    public ConstantPoolReader() {
        this.classLoader = new ClassLoader();
    }

    public void readClassFile(String name) {
        ByteBuffer classBuffer = classLoader.loadClass(name);

        // https://docs.oracle.com/javase/specs/jvms/se15/html/jvms-4.html#jvms-4.1
        checkMagicHeader(classBuffer);

        int minorVersion = classBuffer.getShort();
        int majorVersion = classBuffer.getShort();
        int constantPoolCount = classBuffer.getShort();
    }

    private void checkMagicHeader(ByteBuffer byteBuffer) {
        if (byteBuffer.getInt() != magicHeader) {
            throw new IllegalArgumentException("Magic header hasn't been found ("+magicHeader+"). Isn't class file");
        }
    }

    private static class ConstantPoolClassFlags {
        final BitSet classSet;

        private ConstantPoolClassFlags() {
            classSet = new BitSet();
        }
    }
}
