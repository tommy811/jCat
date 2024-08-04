
package org.coderead.jcat.common;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class IOUtils {
    public IOUtils() {
    }

    public static byte[] readFully(InputStream stream, int MaxSize, boolean EofCheck) throws IOException {
        byte[] bytes = new byte[0];
        if (MaxSize == -1) {
            MaxSize = Integer.MAX_VALUE;
        }

        int size;
        for(int index = 0; index < MaxSize; index += size) {
            int writeIndex;
            if (index >= bytes.length) {
                writeIndex = Math.min(MaxSize - index, bytes.length + 1024);
                if (bytes.length < index + writeIndex) {
                    bytes = Arrays.copyOf(bytes, index + writeIndex);
                }
            } else {
                writeIndex = bytes.length - index;
            }

            size = stream.read(bytes, index, writeIndex);
            if (size < 0) {
                if (EofCheck && MaxSize != Integer.MAX_VALUE) {
                    throw new EOFException("Detect premature EOF");
                }

                if (bytes.length != index) {
                    bytes = Arrays.copyOf(bytes, index);
                }
                break;
            }
        }

        return bytes;
    }


}
