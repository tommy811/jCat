package test

import com.sun.jndi.toolkit.url.UrlUtil
import org.junit.Test

import java.util.jar.JarFile

/**
 * @author 鲁班大叔
 * @email 27686551@qq.com
 * @date 2024/08/
 5
 */
class JarUrlTest {
    @Test
    void test() {
        URLClassLoader loader = this.class.classLoader as URLClassLoader
        loader.getResource("LICENSE-junit.txt")
        def url = loader.URLs[0]
        JarURLConnection con = new URL("jar:${url}!/").openConnection()
        def jarFile = con.getJarFile()
        println jarFile.stream().toArray().join("\n")
//        new URL("jar://file:////Users/tommy/.gradle/caches/modules-2/files-2.1/junit/junit/4.12/2973d150c0dc1fefe998f834810d68f278ea58ec/junit-4.12.jar")
    }
}
