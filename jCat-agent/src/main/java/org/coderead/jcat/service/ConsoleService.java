package org.coderead.jcat.service;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import org.coderead.jcat.Agent;
import org.coderead.jcat.common.*;
import org.coderead.jcat.console.ConsoleBase;
import org.coderead.jcat.console.ConsoleScript;
import org.coderead.jcat.console.GroovyConsoleLoader;
import org.coderead.jcat.groovyLsp.CompletionItem;
import org.coderead.jcat.groovyLsp.TextDocumentService;
import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyShell;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.reflection.SunClassLoader;
import org.codehaus.groovy.runtime.callsite.CallSiteClassLoader;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author 鲁班大叔
 * @date 2024
 */
public class ConsoleService {
    final static List<String> defaultClass = new ArrayList<>();
    Map<Integer, GroovyShell> shellMap = new HashMap<>();
    Map<Integer, TextDocumentService> documentServices = new HashMap<>();
    private final File localHomeFile;

    static {
        defaultClass.add("org.springframework.context.ApplicationContext");
        defaultClass.add("com.mysql.cj.jdbc.Driver");
        defaultClass.add("com.mysql.jdbc.Driver");
        defaultClass.add("org.apache.ibatis.session.SqlSession");
        defaultClass.add("org.slf4j.Logger");
        defaultClass.add("org.apache.log4j.Logger");
    }

    public ConsoleService() {
        File file = new File(System.getProperty("user.home"), ".jCat");
        Assert.isTrue(file.exists() || file.mkdirs());
        this.localHomeFile = file;

        DefaultHttpServer httpServer = DefaultHttpServer.getInstance();
        httpServer.registeGet("/console/open", this::openSession);
        httpServer.registeGet("/console/eval", this::eval);
        httpServer.registeGet("/console/allClassLoader", this::getAllLoader);//获取所有ClassLoader
        httpServer.registeGet("/console/close", this::closeSession);
        httpServer.registeGet("/console/detail", this::getObjectDetail);
        httpServer.registeGet("/console/analysis", this::codeAnalysis);
        httpServer.registeGet("/console/completion", this::completion);
        httpServer.registeGet("/console/file/save", this::saveFile);
        httpServer.registeGet("/console/file/open", this::openFile);
        httpServer.registeGet("/console/file/find", this::findFile);

    }


    private List<String> findFile(Map<String, String> stringStringMap) {
        String path = stringStringMap.get("parent");
        Assert.hasText(path, "参数'path'不能为空");
        String suffix = stringStringMap.get("suffix");
        File root = new File(localHomeFile, path);
        if (!root.exists()) {
            return new ArrayList<>(); // 返回空
        }
        try {
            return Files.walk(root.toPath())
                    .filter(p -> suffix == null || p.toString().endsWith(suffix))
                    .map(p -> p.toFile().toString().replace(localHomeFile.toString(), ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new IllegalStateException("目录遍历失败:" + root, e);
        }
    }

    private String openFile(Map<String, String> stringStringMap) {
        String path = stringStringMap.get("path");
//        Assert.isTrue(path.startsWith(localHomeFile.getPath()), "只能访问" + localHomeFile + "下的文件");
        File file = new File(localHomeFile, path);
        Assert.isTrue(file.exists(), "文件不存在:" + file);
        byte[] bytes = new byte[0];
        try {
            bytes = Files.readAllBytes(file.toPath());
        } catch (IOException e) {
            throw new IllegalStateException("文件打开失败:" + file, e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String saveFile(Map<String, String> stringStringMap) {
        String file = stringStringMap.get("file");
        String text = stringStringMap.get("text");
        Assert.hasText(file, "参数file不能为空");
        File to = new File(localHomeFile, file);
        try {
            Assert.isTrue(to.getParentFile().exists() || to.getParentFile().mkdirs(), "目录创建失败" + to.getParent());
            Assert.isTrue(to.exists() || to.createNewFile(), "文件创建失败:" + to);
            Files.write(to.toPath(), text.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("文件保存失败:" + to);
        }
        return to.toString();
    }


    // 语法分析，判断当前是否存在错误 ,返回空表示正确
    // 返回错误信息
    private List<TextDocumentService.CompileError> codeAnalysis(Map<String, String> params) {
        Integer sessionId = Integer.parseInt(params.get("sessionId"));
        String code = params.get("code");
        Assert.isTrue(shellMap.containsKey(sessionId), "找不到会话:" + sessionId);
        TextDocumentService documentService = documentServices.get(sessionId);
        return documentService.compile(code);
    }


    public List<CompletionItem> completion(Map<String, String> params) {
        Integer sessionId = Integer.parseInt(params.get("sessionId"));
        Assert.isTrue(shellMap.containsKey(sessionId), "找不到会话:" + sessionId);
        int[] cursor = null;
        String keyword = null;
        if (params.containsKey("cursor")) {
            cursor = Arrays.stream(params.get("cursor").split(",")).mapToInt(Integer::parseInt).toArray();
        } else if (params.containsKey("keyword")) {
            keyword = params.get("keyword");
        } else {
            throw new IllegalArgumentException("缺少必要的参数:cursor或keyword");
        }
        int maxSize = Optional.ofNullable(params.get("max")).map(Integer::parseInt).orElse(100);
        TextDocumentService documentService = documentServices.get(sessionId);
        // 基于光标查找 或 基于关键字查找
        return cursor != null ? documentService.completionByCursor(cursor) :
                documentService.completionByKeyword(keyword, maxSize);
    }

    //1.打开页面 创建会话
    //2.多次执行脚本
    //3.关闭页面 关闭会话
    public Map<String, String> openSession(Map<String, String> params) {
        final String id = params.get("loaderId");
        ClassLoader loader = getAllClassLoader().stream().filter(c -> id.equals(System.identityHashCode(c) + ""))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("找不到classLoader:" + id));
        GroovyShell shell = openSession(loader);
        int key = System.identityHashCode(shell);
        shellMap.put(key, shell);
        documentServices.put(key, new TextDocumentService(shell));
        return Maps.to("sessionId", key + "");
    }


    // 执行脚本
    // 将结果转成字符串
    public Object eval(Map<String, String> params) {
        Integer sessionId = Integer.parseInt(params.get("sessionId"));
        String code = params.get("code");
        Assert.isTrue(shellMap.containsKey(sessionId), "找不到会话:" + sessionId);
        GroovyShell groovyShell = shellMap.get(sessionId);
        Object result = null;// 执行脚本
        try {
            result = groovyShell.evaluate(code);
        } catch (Throwable e) {
//            throw new RuntimeException(e);
            return new EvalError(e);
        }
        ObjectItem item = new ObjectItem(null, result);
        item.flag = "root";
        if (result != null && !ClassUtil.isWrapperType(result.getClass())) {
            int hashCode = System.identityHashCode(result);
            groovyShell.setVariable("$" + Integer.toString(hashCode, 36), result); //保存对象
            item.setPath(hashCode + "");
        }
        return item;
    }

    /**
     * 获取对象的属性，或数组中的元素、以及List、Map中的元素，并转换成ObjectItem实例
     *
     * @param params sessionId:会话ID
     *               variable: GroovyShell中存储的变量名
     *               address: 当前对象的访问地址
     * @return
     */
    public List<ObjectItem> getObjectDetail(Map<String, String> params) {
        Integer sessionId = Integer.parseInt(params.get("sessionId"));
        Assert.isTrue(shellMap.containsKey(sessionId), "找不到会话:" + sessionId);
        Assert.hasText(params.get("objectPath"), "参数address不能为空:" + sessionId);
//        Assert.hasText(params.get("address"), "参数address不能为空:" + sessionId);
        String objectPath = params.get("objectPath");
        AtomicInteger level = new AtomicInteger(Integer.parseInt(params.getOrDefault("level", "0")));
        //用于 取list\map\数组中的值
        int begin = params.containsKey("begin") ? Integer.parseInt(params.get("begin")) : -1;
        int size = params.containsKey("size") ? Integer.parseInt(params.get("size")) : -1;
        Object object = getObjectByPath(sessionId, objectPath);
        List<ObjectItem> objectItems;
        if (object instanceof List) {
            objectItems = parseList((List<?>) object, begin, size);
        } else if (object.getClass().getComponentType() != null) {// 数组
            objectItems = parseArray(UnsafeUtil.toArray(object), begin, size);
        } else if (object instanceof Map) {
            objectItems = parseMap((Map<?, ?>) object);
        } else {
            objectItems = parseObject(object);
        }
        for (ObjectItem o : objectItems) {
            o.setPath(objectPath + "/" + o.path);
            if (!o.atomic && level.get() > 0) {
                level.decrementAndGet(); // 次数减一
                visitSetObjectDetail(sessionId, o, level);
            }
        }

        return objectItems;
    }

    //基于路径从shell中获取对象
    private Object getObjectByPath(Integer sessionId, String objectPath) {
        //用于 取list\map\数组中的值
        long[] addresses;
        try {
            addresses = Arrays.stream(objectPath.split("/"))
                    .filter(StringUtils::hasText)
                    .mapToLong(Long::valueOf).toArray();
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的对象路径:" + objectPath, e);
        }
        Assert.isTrue(addresses.length > 0, "无效的对象路径:" + objectPath);

        String variable = "$" + Integer.toString((int) addresses[0], 36);
        GroovyShell groovyShell = shellMap.get(sessionId);
        Object object = groovyShell.getVariable(variable);
        Assert.notNull(object, "找不到变量:" + variable);
        // 深度获取对象中的子孙元素
        if (addresses.length > 1) {
            long[] detailAddresses = Arrays.copyOfRange(addresses, 1, addresses.length);// 1~length
            object = UnsafeUtil.getChildValue(object, detailAddresses);
            Assert.notNull(object, "对象:" + objectPath + "为空");
        }
        return object;
    }

    private void visitSetObjectDetail(Integer sessionId, ObjectItem root, AtomicInteger levelLimit) {
        Assert.isTrue(!root.atomic, "对象值不能是原子的:" + root.type);

        Object object = getObjectByPath(sessionId, root.getPath());

        List<ObjectItem> objectItems;
        if (root.children != null && root.children.size() > 0) {
            objectItems = root.children; // 已经加载
        } else if (object instanceof List) {
            objectItems = parseList((List<?>) object, 0, 10);
        } else if (object.getClass().getComponentType() != null) {// 数组
            objectItems = parseArray(UnsafeUtil.toArray(object), 0, 10);
        } else if (object instanceof Map) {
            objectItems = parseMap((Map<?, ?>) object);
        } else {
            objectItems = parseObject(object);
        }
        root.children = objectItems;

        for (ObjectItem o : objectItems) {
            if (!o.path.startsWith(root.path)) {
                o.setPath(root.path + "/" + o.path);    // 合并父路径 填充完整路径
            }
            // 深度获取子节点
            if (!o.atomic && levelLimit.get() != 0) {
                levelLimit.decrementAndGet();
                visitSetObjectDetail(sessionId, o, levelLimit);// 道归访问
                levelLimit.incrementAndGet();
            }
        }

    }

    private List<ObjectItem> parseMap(Map<?, ?> map) {
//        for (Map.Entry<?, ?> entry : map.entrySet()) {
//            System.out.println(System.identityHashCode(entry));
//            System.out.println(System.identityHashCode(entry) * 10L);
//            System.out.println(System.identityHashCode(entry) * 10);
//        }
        List<ObjectItem> list = map.entrySet().stream()
                .map(entry -> new ObjectItem(null, entry, String.valueOf(System.identityHashCode(entry.getKey()) * 10L)))
                .collect(Collectors.toList());
        return list;
    }

    private List<ObjectItem> parseList(List<?> list, int begin, int maxSize) {
        return parseArray(list.toArray(), begin, maxSize);
    }

    // 132 *10+3 %10=
    private List<ObjectItem> parseArray(Object[] array, int begin, int maxSize) {
        begin = Math.max(begin, 0);
        maxSize = maxSize < 0 ? 20 : maxSize;
        List<ObjectItem> result = new ArrayList<>(maxSize);
        for (int i = begin; i < begin + maxSize && i < array.length; i++) {
            ObjectItem e = new ObjectItem(String.valueOf(i), array[i]);
            e.setPath(String.valueOf(i * 10));
            e.flag = "index";
            result.add(e);
        }
        return result;
    }

    // 获取对象中子元素
    private List<ObjectItem> parseObject(Object object) {
        Assert.notNull(object, "不能解析为null的对象");
//       Assert.isTrue(!object.getClass().isPrimitive(),"不能解析基础数据类型");// TODO Integer 等基础包装类 应被勿略
        Class<?> aClass = object.getClass();
        Map<String, Field> fields = ClassUtil.getDeepDeclaredFields(aClass);
        List<ObjectItem> list = fields.entrySet().stream().map(entry -> {
            try {
                Object value = UnsafeUtil.getValue(object, entry.getValue());
                ObjectItem objectItem = new ObjectItem(entry.getKey(), value);
                objectItem.flag = "property";
                objectItem.setPath(UnsafeUtil.toAddress(entry.getValue()) + "");
                return objectItem;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
        return list;
    }


    // 类型
    // 获取数据

    public String closeSession(Map<String, String> params) {
        Integer sessionId = Integer.parseInt(params.get("sessionId"));
        Assert.isTrue(shellMap.containsKey(sessionId), "找不到会话:" + sessionId);
        shellMap.remove(sessionId);
        return "ok";
    }


    public Set<ClassLoaderInfo> getAllLoader(Map<String, String> params) {
        Class[] allLoadedClasses = Agent.instrumentation.getAllLoadedClasses();
        ClassLoader defaultLoader = null;
        final Set<ClassLoader> loader = new HashSet<>();
        ClassLoader classLoader;
        for (Class<?> allLoadedClass : allLoadedClasses) {
            if (defaultLoader == null && defaultClass.stream().anyMatch(c -> c.equals(allLoadedClass.getName()))) {
                defaultLoader = allLoadedClass.getClassLoader();
            }
            classLoader = allLoadedClass.getClassLoader();
            // 排除Groovy类加载器 以及jmv内部加载器
            if (classLoader != null && !loader.contains(classLoader)
                    && !(classLoader instanceof GroovyClassLoader.InnerLoader)
                    && !(classLoader instanceof CallSiteClassLoader)
                    && !(classLoader instanceof SunClassLoader)
                    && !classLoader.getClass().getSimpleName().equals("DelegatingClassLoader")
            ) {
                loader.add(allLoadedClass.getClassLoader());
            }
        }
    // TODO 异常 java.lang.NoClassDefFoundError: org/coderead/jcat/service/ConsoleService$$Lambda$127
        ClassLoader finalDefaultLoader = Optional.ofNullable(defaultLoader).orElseGet(() -> loader.stream().filter(l -> l.getClass().getName().equals("sun.misc.Launcher$AppClassLoader")).findFirst().get());
        return loader.stream().map(l ->
                new ClassLoaderInfo(l.getClass().getName(), System.identityHashCode(l) + "", l == finalDefaultLoader)
        ).collect(Collectors.toSet());
    }

    protected Set<ClassLoader> getAllClassLoader() {
        Class[] allLoadedClasses = Agent.instrumentation.getAllLoadedClasses();
        return Arrays.stream(allLoadedClasses).map(Class::getClassLoader).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    @Getter
    @Setter
    public static class ClassLoaderInfo implements Serializable {
        String type;
        String id;
        boolean isDefault;

        public ClassLoaderInfo() {
        }

        public ClassLoaderInfo(String type, String id, boolean isDefault) {
            this.type = type;
            this.id = id;
            this.isDefault = isDefault;
        }
    }

    // 打开一个新会话
    public GroovyShell openSession(ClassLoader loader) {
        Binding binding = new Binding();
        CompilerConfiguration config = new CompilerConfiguration();
        config.setScriptBaseClass(ConsoleScript.class.getName());
        //TODO 可能造成方法区内存泄露问题
        GroovyShell groovyShell = new GroovyShell(new GroovyConsoleLoader(loader), binding, config);
        binding.setVariable("vars",new ConsoleBase());
//        try {
//            groovyShell.parse(getClass().getResource("/com/cbtu/agent/service/ConsoleBase.groovy").toURI());
//        groovyShell.evaluate("import com.coderead.jcat.console.ConsoleBase; setProperty('vars', new ConsoleBase())");
//        } catch (IOException | URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
        return groovyShell;
    }

    public static void main(String[] args) {
        Object obj = new int[12];
        System.out.println(obj.getClass().getComponentType());
        if (obj instanceof Object[]) {
            System.out.println("");
        }
    }

    public static class EvalError implements Serializable {
        public String errorType;
        public String errorMessage;
        public String errorStack;

        public EvalError(Throwable throwable) {
            errorType = throwable.getClass().getName();
            errorMessage = throwable.getMessage();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            throwable.printStackTrace(new PrintStream(out));
            errorStack = out.toString().trim();
            int index = errorStack.indexOf("at " + ConsoleService.class.getName());
            if (index > 0)
                errorStack = errorStack.substring(0, errorStack.indexOf("at " + ConsoleService.class.getName()));
        }
    }

    @Setter
    @Getter
    public static class ObjectItem implements Serializable {
        static AtomicInteger ID_INCREMENT = new AtomicInteger(0);
        String id; //
        String objectId;
        String type; // class 名称
        String flag; // 对象所属标志：根节点root、属性property、方法method、条目entry,索引index
        String name;
        String value;
        List<ObjectItem> children = new ArrayList<>();
        String path;// 访问路径
        Integer childSize;// 当type为Collection\map\Array类型时才有值
        boolean atomic = false;

        public ObjectItem(String name, Object valueObj, String path) {
            this.name = name;
            this.path = path;
            this.id = ID_INCREMENT.incrementAndGet() + "";
            if (valueObj == null) {
                this.value = "null";
            } else {
                this.objectId = Integer.toString(System.identityHashCode(valueObj), 36);
                this.type = valueObj.getClass().getName();
                // 子元素个数
                if (valueObj instanceof Collection) {
                    childSize = ((Collection<?>) valueObj).size();
                } else if (valueObj instanceof Map) {
                    childSize = ((Map<?, ?>) valueObj).size();
                } else if (valueObj.getClass().getComponentType() != null) {
                    childSize = UnsafeUtil.getArrayLength(valueObj);
                }
                String str = String.valueOf(valueObj);
                if (!str.equals(valueObj.getClass().getName() + "@" + Integer.toHexString(valueObj.hashCode()))) {
                    this.value = str.substring(0, Math.min(str.length(), 200));
                }
            }

            // map条目
            if (valueObj instanceof Map.Entry) {
                Map.Entry<?, ?> e = (Map.Entry<?, ?>) valueObj;
                children = Arrays.asList(
                        new ObjectItem("key", e.getKey(), "0")
                        , new ObjectItem("value", e.getValue(), "10"));
                this.flag = "entry";
            }
            atomic = valueObj == null || valueObj instanceof String || ClassUtil.isWrapperType(valueObj.getClass());
//            if (atomic) {
//                this.value = String.valueOf(valueObj);
//                this.children=null;
//            }
            if (atomic || new Integer(0).equals(childSize)) {
                this.children = null;
            }
        }

        public void setPath(String path) {
            this.path = path;
            if (children != null) { // 设置子路径
                children.forEach(r -> r.setPath(path + "/" + r.path));
            }
        }

        public ObjectItem(String name, Object valueObj) {
            this(name, valueObj, null);
        }

        public String getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "ObjectItem{" +
                    "id='" + id + '\'' +
                    ", type='" + type + '\'' +
                    ", flag='" + flag + '\'' +
                    ", name='" + name + '\'' +
                    ", value='" + value + '\'' +
                    ", children=" + children +
                    ", path='" + path + '\'' +
                    ", childSize=" + childSize +
                    ", atomic=" + atomic +
                    '}';
        }
    }

    class ShellWrapper {
        GroovyShell groovyShell;
        TextDocumentService documentService;

        ShellWrapper(GroovyShell groovyShell, TextDocumentService documentService) {
            this.groovyShell = groovyShell;
            this.documentService = documentService;
        }
    }
}
