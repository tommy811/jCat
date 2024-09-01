package org.coderead.jcat.common;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.coderead.jcat.Agent;
import org.jd.core.v1.ClassFileToJavaSourceDecompiler;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.api.loader.LoaderException;
import org.jd.core.v1.api.printer.Printer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author 鲁班大叔
 * @date 2021
 */
public class JadUtil {
    static Loader loader = new Loader() {
        @Override
        public byte[] load(String internalName) throws LoaderException {
            InputStream is = loadClass(internalName);
            if (is == null) {
                return null;
            } else {
                try (InputStream in = is; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[1024];
                    int read = in.read(buffer);

                    while (read > 0) {
                        out.write(buffer, 0, read);
                        read = in.read(buffer);
                    }

                    return out.toByteArray();
                } catch (IOException e) {
                    throw new LoaderException(e);
                }
            }
        }

        @Override
        public boolean canLoad(String internalName) {
            return loadClass(internalName) != null;
        }

        private InputStream loadClass(String internalName) {
            InputStream is = this.getClass().getResourceAsStream("/" + internalName.replaceAll("\\.", "/") + ".class");
            if (is == null && Agent.instrumentation != null) {
                for (Class allLoadedClass : Agent.instrumentation.getAllLoadedClasses()) {
                    if (allLoadedClass.getName().equals(internalName)) {
                        is = allLoadedClass.getResourceAsStream("/" + internalName.replaceAll("\\.", "/") + ".class");
                        break;
                    }
                }
            }
            return is;
        }
    };


    static Printer printer = new Printer() {
        protected static final String TAB = "  ";
        protected static final String NEWLINE = "\n";

        protected int indentationCount = 0;
        protected StringBuilder sb = new StringBuilder();

        @Override
        public String toString() {
            return sb.toString();
        }

        @Override
        public void start(int maxLineNumber, int majorVersion, int minorVersion) {
        }

        @Override
        public void end() {
        }

        @Override
        public void printText(String text) {
            sb.append(text);
        }

        @Override
        public void printNumericConstant(String constant) {
            sb.append(constant);
        }

        @Override
        public void printStringConstant(String constant, String ownerInternalName) {
            sb.append(constant);
        }

        @Override
        public void printKeyword(String keyword) {
            sb.append(keyword);
        }

        @Override
        public void printDeclaration(int type, String internalTypeName, String name, String descriptor) {
            sb.append(name);
        }

        @Override
        public void printReference(int type, String internalTypeName, String name, String descriptor, String ownerInternalName) {
            sb.append(name);
        }

        @Override
        public void indent() {
            this.indentationCount++;
        }

        @Override
        public void unindent() {
            this.indentationCount--;
        }

        @Override
        public void startLine(int lineNumber) {
            for (int i = 0; i < indentationCount; i++) sb.append(TAB);
        }

        @Override
        public void endLine() {
            sb.append(NEWLINE);
        }

        @Override
        public void extraLine(int count) {
            while (count-- > 0) sb.append(NEWLINE);
        }

        @Override
        public void startMarker(int type) {
        }

        @Override
        public void endMarker(int type) {
        }
    };

    public static String decompiler(String className) throws Exception {
        ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
        decompiler.decompile(loader, printer, className);
        String source = printer.toString();
        return source;
    }

   public static String decompiler(String name,byte [] bytes) throws Exception {
        ClassFileToJavaSourceDecompiler decompiler = new ClassFileToJavaSourceDecompiler();
        decompiler.decompile(new Loader() {
            @Override
            public boolean canLoad(String s) {
                return name.equals(s);
            }
            @Override
            public byte[] load(String s) throws LoaderException {
                return bytes;
            }
        }, printer, name);
        String source = printer.toString();
        return source;
    }
}
