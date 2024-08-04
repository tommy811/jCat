package org.coderead.jcat.common;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @Discription: 强制读取和操作对象属性工具类
 * @Author Zaki Chen
 * @date 2019/9/30 14:15
 **/
public class UnsafeUtil {
    public final static Unsafe unsafe;

    static {
        try {
            Field theUnsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafeField.setAccessible(true);
            unsafe = (Unsafe) theUnsafeField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("无法获取unsafe对象", e);
        }
    }

    /**
     * @param field
     * @return
     */
    public static long toAddress(Field field) {
        long offset = unsafe.objectFieldOffset(field);
        Type type = getType(field);
        return offset * 10 + type.valueIndex();
    }

    public static Object getValue(Object object, Field field) throws NoSuchFieldException {
        long offset = unsafe.objectFieldOffset(field);
        Type type = getType(field);
        return getValue(object, offset, type);
    }

    /**
     * 取对象数据
     *
     * @param object 对象：可以是一个实例对象，也可以是一个类对象
     * @param offset 内存偏移地址,如果是数组就是其对应坐标
     * @param type   返回数据的类型
     * @return
     */
    public static Object getValue(Object object, long offset, Type type) {
        if (object.getClass().getComponentType() != null) {// 数组对象
            return getArrayValue(object, (int) offset);
        } else if (object instanceof List) {
            return ((List<?>) object).get((int) offset);
        } else if (object instanceof Map) {
            return ((Map<?, ?>) object).entrySet().stream().filter(s -> System.identityHashCode(s.getKey()) == offset).findFirst().orElseThrow(() -> new IllegalArgumentException("找不到entry" + offset));
        } else if (object instanceof Map.Entry) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) object;
            return offset == 0 ? entry.getKey() : entry.getValue();
        }
        switch (type) {
            case Char:
                return unsafe.getChar(object, offset);
            case Byte:
                return unsafe.getByte(object, offset);
            case Short:
                return unsafe.getShort(object, offset);
            case Integer:
                return unsafe.getInt(object, offset);
            case Double:
                return unsafe.getDouble(object, offset);
            case Float:
                return unsafe.getFloat(object, offset);
            case Long:
                return unsafe.getLong(object, offset);
            case Boolean:
                return unsafe.getBoolean(object, offset);
            default:
                return unsafe.getObject(object, offset);
        }
    }

    public static Object[] toArray(Object object) {
        Assert.notNull(object, "object不能为空");
        Assert.notNull(object.getClass().getComponentType(), "object必须为数组类型");
        if (object instanceof int[]) {
            return Arrays.stream((int[]) object).boxed().toArray(Integer[]::new);
        } else if (object instanceof short[]) {
            short[] shortArray = (short[]) object;
            Short[] wrapperArray = new Short[shortArray.length];
            for (int i = 0; i < shortArray.length; i++) {
                wrapperArray[i] = shortArray[i];
            }
            return wrapperArray;
        } else if (object instanceof boolean[]) {
            boolean[] boolArray = (boolean[]) object;
            Boolean[] wrapperArray = new Boolean[boolArray.length];
            for (int i = 0; i < boolArray.length; i++) {
                wrapperArray[i] = boolArray[i];
            }
            return wrapperArray;
        } else if (object instanceof double[]) {
            return Arrays.stream((double[]) object).boxed().toArray(Double[]::new);
        } else if (object instanceof long[]) {
            return Arrays.stream((long[]) object).boxed().toArray(Long[]::new);
        } else if (object instanceof char[]) {
            char[] charArray = (char[]) object;
            Character[] wrapperArray = new Character[charArray.length];
            for (int i = 0; i < charArray.length; i++) {
                wrapperArray[i] = charArray[i];
            }
            return wrapperArray;
        } else if (object instanceof float[]) {
            float[] floatArray = (float[]) object;
            Float[] wrapperArray = new Float[floatArray.length];
            for (int i = 0; i < floatArray.length; i++) {
                wrapperArray[i] = floatArray[i];
            }
            return wrapperArray;
        } else if (object instanceof byte[]) {
            byte[] byteArray = (byte[]) object;
            Byte[] wrapperArray = new Byte[byteArray.length];
            for (int i = 0; i < byteArray.length; i++) {
                wrapperArray[i] = byteArray[i];
            }
            return wrapperArray;
        }
        return (Object[]) object;
    }


    public static void main(String[] args) {
        Object[] array = Arrays.stream(new int[]{1, 2, 3}).boxed().toArray();
        System.out.println(array);
    }

    public static Object getArrayValue(Object object, int index) {
        if (object instanceof byte[]) {
            return ((byte[]) object)[index];
        } else if (object instanceof int[]) {
            return ((int[]) object)[index];
        } else if (object instanceof short[]) {
            return ((short[]) object)[index];
        } else if (object instanceof float[]) {
            return ((float[]) object)[index];
        } else if (object instanceof boolean[]) {
            return ((boolean[]) object)[index];
        } else if (object instanceof double[]) {
            return ((double[]) object)[index];
        } else if (object instanceof long[]) {
            return ((long[]) object)[index];
        } else if (object instanceof char[]) {
            return ((char[]) object)[index];
        } else if (object instanceof Object[]) {
            return ((Object[]) object)[index];
        } else {
            throw new IllegalArgumentException("对象并非数组");
        }
    }

    public static int getArrayLength(Object object) {
        if (object instanceof byte[]) {
            return ((byte[]) object).length;
        } else if (object instanceof int[]) {
            return ((int[]) object).length;
        } else if (object instanceof short[]) {
            return ((short[]) object).length;
        } else if (object instanceof float[]) {
            return ((float[]) object).length;
        } else if (object instanceof boolean[]) {
            return ((boolean[]) object).length;
        } else if (object instanceof double[]) {
            return ((double[]) object).length;
        } else if (object instanceof long[]) {
            return ((long[]) object).length;
        } else if (object instanceof char[]) {
            return ((char[]) object).length;
        } else if (object instanceof Object[]) {
            return ((Object[]) object).length;
        } else {
            throw new IllegalArgumentException("对象并非数组");
        }
    }


    /**
     * 深度访问子孙节点信息
     *
     * @param root    根节点对象
     * @param address [offset1,type1,offset2,type2,] 其中数组offset就是其访问下标
     * @return
     */
    public static Object getChildValue(Object root, long[] address) {
        Object parent = root;
        for (int i = 0; i < address.length; i++) {
            Type type = Type.indexOf((int) Math.abs(address[i] % 10));
            parent = getValue(parent, address[i] / 10, type);
        }
        return parent;
    }


    /**
     * 获取对象的类变量属性值
     *
     * @param clazz        Class对象
     * @param propertyName 对象类声明的属性名
     * @return
     * @throws NoSuchFieldException
     */
    public static Object getStaticValue(Class clazz, String propertyName) throws NoSuchFieldException {
        Field field = clazz.getDeclaredField(propertyName);
        long offset = unsafe.staticFieldOffset(field);
        return getValue(clazz, offset, getType(field));
    }

    /**
     * 更新对象实例属性的值
     *
     * @param object       实例对象
     * @param propertyName 实例属性名 非static关键字声明的属性
     * @param val          要修改的值
     * @return
     */
    public static void setValue(Object object, String propertyName, Object val) throws NoSuchFieldException {
        Field propNameField = object.getClass().getDeclaredField(propertyName);
        long offset = unsafe.objectFieldOffset(propNameField);
        Type dataType = getType(propNameField);
        setValue(object, offset, val, dataType);

    }

    /**
     * 修改类属性变量的值
     *
     * @param clazz        类
     * @param propertyName 类属性 static关键字声明的属性
     * @param val          值
     * @throws NoSuchFieldException
     */
    public static void setStaticValue(Class clazz, String propertyName, Object val) throws NoSuchFieldException {
        Field propNameField = clazz.getDeclaredField(propertyName);
        long offset = unsafe.staticFieldOffset(propNameField);
        Type dataType = getType(propNameField);
        setValue(clazz, offset, val, dataType);
    }


    /**
     * 数据值类型枚举
     */
    public enum Type {
        Object,
        Byte,
        Char,
        Boolean,
        Short,
        Integer,
        Double,
        Float,
        Long; // Collection

        public static Type indexOf(int index) {
            return Type.values()[index];
        }

        public int valueIndex() {
            Type[] values = Type.values();
            for (int i = 0; i < values.length; i++) {
                if (values[i] == this) {
                    return i;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public static Type getType(Field field) {
        Class<?> type = field.getType();

        if (type.equals(Character.TYPE)) {
            return Type.Char;
        } else if (type.equals(Byte.TYPE)) {
            return Type.Byte;
        } else if (type.equals(Short.TYPE)) {
            return Type.Short;
        } else if (type.equals(Integer.TYPE)) {
            return Type.Integer;
        } else if (type.equals(Double.TYPE)) {
            return Type.Double;
        } else if (type.equals(Float.TYPE)) {
            return Type.Float;
        } else if (type.equals(Long.TYPE)) {
            return Type.Long;
        } else if (type.equals(Boolean.TYPE)) {
            return Type.Boolean;
        }/* else if (type.getComponentType() != null) { // 数组类
            return Type.Array;
        }*/ else {
            return Type.Object;
        }
    }


    /**
     * 放对象数据
     *
     * @param object 对象：可以是一个实例对象，也可以是一个类对象
     * @param offset 内存偏移地址
     * @param val    修改后的值
     * @param type   被修改属性的声明类型
     */
    private static void setValue(Object object, long offset, Object val, Type type) {
        switch (type) {
            case Char:
                unsafe.putCharVolatile(object, offset, (Character) val);
                break;
            case Byte:
                unsafe.putByteVolatile(object, offset, (Byte) val);
                break;
            case Integer:
                unsafe.putIntVolatile(object, offset, (Integer) val);
                break;
            case Double:
                unsafe.putDoubleVolatile(object, offset, (Double) val);
                break;
            case Float:
                unsafe.putFloatVolatile(object, offset, (Float) val);
                break;
            case Long:
                unsafe.putLongVolatile(object, offset, (Long) val);
                break;
            case Boolean:
                unsafe.putBooleanVolatile(object, offset, (Boolean) val);
                break;
            default:
                unsafe.putObjectVolatile(object, offset, val);
        }
    }

}