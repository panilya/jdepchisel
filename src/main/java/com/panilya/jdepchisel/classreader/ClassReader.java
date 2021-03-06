package com.panilya.jdepchisel.classreader;

import com.panilya.jdepchisel.constantpool.ConstantPoolReader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;

public class ClassReader {

    // Specify directory to search in
    // TODO: Implement CLI
//    Path baseDirectory = Path.of("/home/sigma_male/dev/programs/jdepchisel/target/classes/com/panilya/jdepchisel/constantpool/");

    public ClassReader() { }

    public static ByteBuffer readClassBytes(Class<?> from) throws IOException {
        ConstantPoolReader.Buffer buffer = new ConstantPoolReader.Buffer();
        try (InputStream is = from.getResourceAsStream(from.getSimpleName()
                + ".class")) {
            int byteCountFromLastRead = 0;
            do {
                buffer.read += byteCountFromLastRead;
                adjustBufferSize(buffer, is);
                byteCountFromLastRead = is.read(buffer.buf, buffer.read,
                        buffer.buf.length - buffer.read);
            } while (byteCountFromLastRead > 0);
        }
        return buffer.toByteBuffer();
    }

    private static void adjustBufferSize(ConstantPoolReader.Buffer readBuf, InputStream is)
            throws IOException {
        int bufferSize = Math.max(is.available() + 100, 100);
        if (readBuf.buf == null) {
            readBuf.buf = new byte[bufferSize];
        } else if (readBuf.buf.length - readBuf.read < bufferSize) {
            System.arraycopy(readBuf.buf, 0,
                    readBuf.buf = new byte[readBuf.read + bufferSize], 0,
                    readBuf.read);
        }
    }
}
