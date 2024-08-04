package org.coderead.jcat.groovyLsp;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.io.Serializable;
import java.util.Objects;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class CompletionItem implements Serializable {
    public String label;
    public String kind;// 种类
    public int modifiers;
    public String filterText;// 过滤文本
    public boolean deprecated;// 是否弃用
    public boolean groovyMethod;// 是否为groovy方法
    public String insertText;
    public String insertImportText;
    public String tipsText;// 提示文本

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompletionItem that = (CompletionItem) o;

        if (!Objects.equals(label, that.label)) return false;
        if (!Objects.equals(kind, that.kind)) return false;
        if (!Objects.equals(filterText, that.filterText)) return false;
        if (!Objects.equals(insertText, that.insertText)) return false;
        return Objects.equals(insertImportText, that.insertImportText);
    }

    @Override
    public int hashCode() {
        int result = label != null ? label.hashCode() : 0;
        result = 31 * result + (kind != null ? kind.hashCode() : 0);
        result = 31 * result + (filterText != null ? filterText.hashCode() : 0);
        result = 31 * result + (insertText != null ? insertText.hashCode() : 0);
        result = 31 * result + (insertImportText != null ? insertImportText.hashCode() : 0);
        return result;
    }
}
