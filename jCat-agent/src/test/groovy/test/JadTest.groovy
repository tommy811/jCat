package test

import org.coderead.jcat.AgentTest
import org.coderead.jcat.Jad
import org.coderead.jcat.common.JadUtil
import org.junit.Test

/**
 * @author 鲁班大叔
 * @email 27686551@qq.com
 * @date 2024/08/
 1
 */
class JadTest {
    @Test
    void jadTest(){
        def stream = AgentTest.class.getResourceAsStream("AgentTest.class");
        def bytes = new byte[1024 * 1024]
        def size= stream.read(bytes)
       println JadUtil.decompiler(AgentTest.class.getName(),Arrays.copyOfRange(bytes,0,size))
    }



    @Test
    void jadTest2(){
        println JadUtil.decompiler(AgentTest.class.getName())
    }

    @Test
    void jadTest3(){
        def stream = AgentTest.class.getResourceAsStream("AgentTest.class");
        def bytes = new byte[1024 * 1024]
        def size= stream.read(bytes)
        println Jad.decompiler(Arrays.copyOfRange(bytes,0,size))
    }
}
