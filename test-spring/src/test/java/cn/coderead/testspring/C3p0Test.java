package cn.coderead.testspring;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * @author 鲁班大叔
 * @email 27686551@qq.com
 * @date 2024/7/26
 */
public class C3p0Test {

    @Test
    public void dbcpTest(){
        BasicDataSource dataSource = new BasicDataSource();
        // 配置数据源
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("");
        dataSource.setUsername("");
        dataSource.setPassword("");
        // 其他配置...
        // 获取连接池信息
        int maxActiveConnections = dataSource.getMaxTotal();
        int maxIdleConnections = dataSource.getMaxIdle();
        int minIdleConnections = dataSource.getMinIdle();
        int numActiveConnections = dataSource.getNumActive();
        int numIdleConnections = dataSource.getNumIdle();
        String log="连接信息 最大激活数:$maxActiveConnections,最大闲置数:$maxIdleConnections,最小闲置数：$minIdleConnections,当前激活数:$numActiveConnections,当前闲置数:$numIdleConnections";
        System.out.println("Max Active Connections: " + maxActiveConnections);
        System.out.println("Max Idle Connections: " + maxIdleConnections);
        System.out.println("Min Idle Connections: " + minIdleConnections);
        System.out.println("Num Active Connections: " + numActiveConnections);
        System.out.println("Num Idle Connections: " + numIdleConnections);
    }

}
