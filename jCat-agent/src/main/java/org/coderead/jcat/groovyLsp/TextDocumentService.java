package org.coderead.jcat.groovyLsp;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.coderead.jcat.Agent;
import org.coderead.jcat.common.Assert;
import org.coderead.jcat.console.ConsoleScript;
import groovy.lang.GroovyShell;
import lombok.Getter;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.messages.SyntaxErrorMessage;
import org.codehaus.groovy.syntax.SyntaxException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * groovy 语法服务
 * 1.同步、编译、获取错误项、
 * 2.获取元素定义、
 * 3.获取提示项、
 * 4.跳转到定义
 *
 * @author 鲁班大叔
 * @date 2024
 */
public class TextDocumentService {
    final GroovyShell groovyShell;
    private CompilationUnit unit;
    private SourceUnit consoleCodeSource;

    public TextDocumentService(GroovyShell groovyShell) {
        this.groovyShell = groovyShell;
    }

    // 同步文本
    // 返回错误、警告等信息

    /**
     * 1.同步、编译、获取错误项
     *
     * @param consoleCode
     */
    public List<CompileError> compile(String consoleCode) {
        unit = new CompilationUnit(groovyShell.getClassLoader());
        consoleCodeSource = unit.addSource("consoleCode", consoleCode);
        ArrayList<CompileError> results = new ArrayList<>();
        try {
            unit.compile(Phases.CANONICALIZATION);
        } catch (CompilationFailedException e) {
//            e.printStackTrace(); // 异常处理
            for (Object o : unit.getErrorCollector().getErrors()) {
                Assert.isTrue(o instanceof SyntaxErrorMessage, "未对异常作处理:" + o.getClass().getName());
                SyntaxException cause = ((SyntaxErrorMessage) o).getCause();
                CompileError error = new CompileError("error", cause.getOriginalMessage());
                error.range = new int[]{cause.getStartLine(), cause.getStartColumn(), cause.getEndLine(), cause.getEndColumn()};
                results.add(error);
            }
            if (results.isEmpty()) {
                throw new RuntimeException("未知编译异常", e);
            }
            unit = null;
        }
        return results; //
    }

    // 查看定义
    public void hover() {
    }

    // 跳转到定义
    public void definition() {
    }


    // 获取提示项
    public List<CompletionItem> completionByCursor(int[] position) {
        Assert.notNull(unit, "文件未正常编译");
        CompletionHandler handler = new CompletionHandler(groovyShell.getClassLoader(), ConsoleScript.class);
        return handler.completionByCursor(unit.getAST(), position);
    }

    public List<CompletionItem> completionByKeyword(String keyword, int maxSize) {
        CompletionHandler handler = new CompletionHandler(groovyShell.getClassLoader(), ConsoleScript.class);
        handler.setAllClass(() -> Agent.instrumentation.getAllLoadedClasses());
        return handler.completionByKeyword(keyword, maxSize);
    }

    @Getter
    public static class CompileError implements Serializable {
        public String level; //error, warring
        public String message;
        public int range[] = new int[4]; //范围 line,column,lastLine,lastColumn

        public CompileError() {
        }

        public CompileError(String level, String message) {
            this.level = level;
            this.message = message;
        }
    }


}
