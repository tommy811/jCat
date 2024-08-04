package org.coderead.jcat.groovyLsp;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.coderead.jcat.common.StringUtils;
import groovy.lang.Closure;
import groovy.lang.Script;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.CompileUnit;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class CompletionHandler {
    private ClassLoader userLoader;// 应用使用的classLoader
    private Class<?> scriptBaseClass;
    Supplier<Class<?>[]> allClass;
    public static final String[] defaultImports = {
            "java.lang",
            "java.util",
            "java.io",
            "java.net", "groovy.lang", "groovy.util",
            "java.math.BigInteger",
            "java.math.BigDecimal",
    };
    // 是否为默认导入的类
    private static Predicate<Class<?>> isDefaultClass = c -> Arrays.stream(defaultImports).anyMatch(p -> p.equals(c.getPackage().getName()) || p.equals(c.getName()));

    public CompletionHandler(ClassLoader baseClassLoader, Class<?> scriptBaseClass) {
        this.userLoader = baseClassLoader;
        this.scriptBaseClass = scriptBaseClass;
    }

    public CompletionHandler() {
        this(ClassLoader.getSystemClassLoader(), Script.class);
    }


    public void setAllClass(Supplier<Class<?>[]> allClass) {
        this.allClass = allClass;
    }

    /**
     * 基于光标位置获取提示项
     *
     * @param position 光标位置 line,column
     * @return
     */
    public List<CompletionItem> completionByCursor(CompileUnit unit, int[] position) {
        FindCompletionNodeVisitor findNodeVisitor = new FindCompletionNodeVisitor(position);
        ASTNode astNode = unit.getModules().stream().
                flatMap(s -> s.getClasses().stream())
                .map(c -> {
                    c.visitContents(findNodeVisitor);
                    return findNodeVisitor.node;
                })
                .filter(Objects::nonNull)
                .findFirst().orElse(null);

        if (astNode instanceof VariableExpression) {
            return variableCompletion((VariableExpression) astNode);
        } else if (astNode instanceof ConstructorCallExpression) {
            return constructorCallCompletion((ConstructorCallExpression) astNode);
        } else if (astNode instanceof ClassExpression) {
            return classCompletion((ClassExpression) astNode);
        } else if (astNode instanceof ListExpression) {
            return typeCompletion(List.class);
        } else if (astNode instanceof MapExpression) {
            return typeCompletion(Map.class);
        }
        // 返回空
        return new ArrayList<>();
    }

    /**
     * 基于关键字获取提示：
     * 1.关键字及模板关键字
     * 2.groovy.lang.Script类中的方法
     * 3.当前定义的元素：变量、方法
     * 4.类名
     * <p>
     * 注意： 勿调用class.getSimpleName或isAnonymousClass 将导致NoClassDefFoundError 或 IllegalAccessError
     *
     * @return
     */
    public List<CompletionItem> completionByKeyword(String keyword, int maxSize) {
        //1.关键字过滤
        List<CompletionItem> items = Arrays.stream(JAVA_KEYWORD)
                .filter(k -> k.startsWith(keyword))
                .limit(maxSize)
                .map(this::keywordCompletion).collect(Collectors.toList());
        if (items.size() >= maxSize) return items;

        //2.脚本方法过滤
        List<CompletionItem> scriptMethodItems = typeCompletion(scriptBaseClass).stream()
                .filter(i -> StringUtils.camelSearch(i.filterText, keyword) > 0)
                .limit(maxSize - items.size())
                .collect(Collectors.toList());
        items.addAll(scriptMethodItems);
        if (items.size() >= maxSize) return items;
        //3.当前元素过滤 TODO
        // unit.getModules().stream().flatMap(m->m.getClasses().stream())

        //4.类名过滤

        /*LinkedList<ClassLoader> loaders=new LinkedList<>(Collections.singletonList(this.userLoader));
        while (loaders.getLast().getParent()!=null){// 获取能访问的loader
            loaders.add(loaders.getLast().getParent());
        }*/
        Class<?>[] allClasses = allClass == null ? new Class[0] : allClass.get();
        Map<Class<?>, Float> scores = new HashMap<>();

        List<CompletionItem> classItems = Arrays.stream(allClasses)
                .filter(a -> Modifier.isPublic(a.getModifiers()))
                .filter(a -> !(a.isSynthetic() || a.isArray() || a.getPackage() == null))
//              .filter(a -> a.getClassLoader() == null || loaders.stream().anyMatch(l -> a.getClassLoader() == l))// 必须为当前loader能访问的类
                .filter(a -> {

                    float score = StringUtils.camelSearch(getSimpleClassName(a), keyword);
                    // 如果是默认包，增加20%分值
                    scores.put(a, score * (isDefaultClass.test(a) ? 1.2f : 1f));
                    return score > 0;
                })
                .sorted((c1, c2) -> (int) (scores.get(c2) * 100 - scores.get(c1) * 100))
                .limit(maxSize - items.size()) // 优先级排序
                .map(this::classCompletion)
                .collect(Collectors.toList());
        items.addAll(classItems);

        return items;
    }


    //变量提示
    private List<CompletionItem> variableCompletion(VariableExpression variable) {
        Class<?> typeClass = variable.getType().getTypeClass();
        return typeCompletion(typeClass);
    }

    //构造方法提示
    private List<CompletionItem> constructorCallCompletion(ConstructorCallExpression exp) {
        Class<?> typeClass = exp.getType().getTypeClass();
        return typeCompletion(typeClass);
    }

    //类提示
    private List<CompletionItem> classCompletion(ClassExpression exp) {
        Class<?> typeClass = exp.getType().getTypeClass();
        List<CompletionItem> completionItems = typeCompletion(typeClass).stream().filter(t -> Modifier.isStatic(t.modifiers)).collect(Collectors.toList());
        completionItems.add(0,keywordCompletion("class"));
        return completionItems;
    }


    // TODO 暂不实现
    private List<CompletionItem> methodCallCompletion(MethodCallExpression exp) {
        // 获取方法返回结果
        return null;
    }

    // TODO 暂不实现
    private List<CompletionItem> methodCallCompletion(StaticMethodCallExpression exp) {
        return null;
    }


    //通过类型转换定义
    public List<CompletionItem> typeCompletion(Class<?> type) {
        List<Method> methods = new ArrayList<>(Arrays.asList(type.getMethods()));
        methods.addAll(Arrays.asList(type.getDeclaredMethods()));
        List<CompletionItem> items1 = methods.stream()
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .distinct()
                .map(this::methodCompletion)
                .collect(Collectors.toList());
        items1.addAll(groovyCompletion(type));// 添加groovy方法
        // 显示 过滤 insertText
        List<Field> fields = new ArrayList<>(Arrays.asList(type.getFields()));
        fields.addAll(Arrays.asList(type.getDeclaredFields()));
        List<CompletionItem> items2 = fields.stream().filter(f -> Modifier.isPublic(f.getModifiers())).map(this::filedCompletion).collect(Collectors.toList());
        items1.addAll(items2);
        return items1.stream().distinct().collect(Collectors.toList());
    }

    protected CompletionItem classCompletion(Class<?> cla) {
        CompletionItem item = new CompletionItem();
        String simpleName = getSimpleClassName(cla);
        item.label = String.format("%s:%s:%s", simpleName, cla.getPackage().getName(), "");
        item.filterText = simpleName;
        item.insertText = simpleName;

        if (!isDefaultClass.test(cla)) {
            item.insertImportText = "import " + cla.getCanonicalName();//TODO: 在内部类下，存在报找不到类的风险
        }
        item.kind = "class";
        // 因引发ArrayStoreException异常暂时关闭
//        item.deprecated = cla.getDeclaredAnnotation(Deprecated.class) != null;
        item.modifiers = cla.getModifiers();
        return item;
    }

    private  String getSimpleClassName(Class<?> cla) {
        if (cla.isArray()) {
            return getSimpleClassName(cla.getComponentType())+"[]";

        }
        return new LinkedList<>(Arrays.asList(cla.getName().split("[.|$]"))).getLast();//TODO:当类名中存在 $将导致不准确
    }

    private CompletionItem methodCompletion(Method method) {
        return methodCompletion(method, false);
    }

    /**
     * @param method
     * @param isGroovy DefaultGroovyMethods中的静态方法 至少包含一个参数
     * @return
     */
    protected CompletionItem methodCompletion(Method method, boolean isGroovy) {
        CompletionItem item = new CompletionItem();
        LinkedList<Class<?>> paramTypes = new LinkedList<>(Arrays.asList(method.getParameterTypes()));

        if (isGroovy && Modifier.isStatic(method.getModifiers())) {
            paramTypes.remove();// 第一个参数为当前调用对象
        }
        String paramText = paramTypes.stream().map(this::getSimpleClassName).collect(Collectors.joining(","));
        String returnText =getSimpleClassName(method.getReturnType());
        item.label = String.format("%s:(%s):%s", method.getName(), paramText, returnText);
        item.kind = "method";
        item.deprecated = method.getDeclaredAnnotation(Deprecated.class) != null;
        item.filterText = method.getName();
        item.modifiers = method.getModifiers();
        item.groovyMethod = isGroovy;
        item.tipsText = paramText;

        if (paramTypes.size() == 1 && paramTypes.getLast().equals(Closure.class)) {
            item.insertText = String.format("%s {it-> }", method.getName());
        } else if (paramTypes.size() > 1 && paramTypes.getLast().equals(Closure.class)) {
            item.insertText = String.format("%s() {it-> }", method.getName());
        } else {
            item.insertText = String.format("%s()", method.getName());
        }
        return item;
    }

    protected CompletionItem filedCompletion(Field field) {
        CompletionItem item = new CompletionItem();
        item.label = String.format("%s:%s:%s", field.getName(), "", getSimpleClassName(field.getType()));
        item.filterText = field.getName();
        item.kind = "field";
        item.deprecated = field.getDeclaredAnnotation(Deprecated.class) != null;
        item.modifiers = field.getModifiers();
        item.insertText = field.getName();
        return item;
    }

    protected List<CompletionItem> groovyCompletion(Class type) {
        return Arrays.stream(DefaultGroovyMethods.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()) && Modifier.isStatic(m.getModifiers()))
                .filter(m -> m.getParameters().length > 0)
                .filter(m -> m.getParameters()[0].getType().isAssignableFrom(type))
                .map(m -> methodCompletion(m, true))
                .collect(Collectors.toList());
    }

    protected CompletionItem keywordCompletion(String keyword) {
        CompletionItem item = new CompletionItem();
        item.label = keyword;
        item.filterText = keyword;
        item.kind = "keyword";
        item.insertText = keyword;

        return item;
    }


    //
    // 基于光标位置 找到可提示的项目
    /*
        基于光标位置 找到可提示的项目，有以下5种节点：
         1.VariableExpression
         2.MethodCallExpression
         3.ConstructorCallExpression
         4.StaticMethodCallExpression
         5.ClassExpression
     */
    private class FindCompletionNodeVisitor extends ClassCodeVisitorSupport {
        private int[] position;
        ASTNode node;

        public FindCompletionNodeVisitor(int[] position) {
            this.position = position;
        }

        protected SourceUnit getSourceUnit() {
            return null;
        }

        @Override
        public void visitVariableExpression(VariableExpression expression) {
            super.visitVariableExpression(expression);
            visitNode(expression);
        }

        @Override
        public void visitMethodCallExpression(MethodCallExpression call) {
            super.visitMethodCallExpression(call);
            visitNode(call);
        }

        @Override
        public void visitConstructorCallExpression(ConstructorCallExpression call) {
            super.visitConstructorCallExpression(call);
            visitNode(call);
        }

        @Override
        public void visitStaticMethodCallExpression(StaticMethodCallExpression call) {
            super.visitStaticMethodCallExpression(call);
            visitNode(call);
        }

        @Override
        public void visitClassExpression(ClassExpression expression) {
            super.visitClassExpression(expression);
            visitNode(expression);
        }

        @Override
        public void visitListExpression(ListExpression expression) {
            super.visitListExpression(expression);
            visitNode(expression);
        }

        @Override
        public void visitMapExpression(MapExpression expression) {
            super.visitMapExpression(expression);
            visitNode(expression);
        }

        void visitNode(ASTNode node) {
            if (node.getLastLineNumber() == position[0] && node.getLastColumnNumber() == position[1]) {
                this.node = node;
            }
        }
    }


    private static final String JAVA_KEYWORD[] = {
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "if", "implements", "import", "int", "interface", "instanceof", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"
    };
}
