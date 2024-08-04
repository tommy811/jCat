// 获取所有系统属性
let getProperties = `
System.getProperties()
`;

// 获取Spring 上下文
let getSpringContext = `
import org.springframework.context.ApplicationContext
//get(Class) 获取类的实例
get ApplicationContext.class
`;

// 获取MyBatis中Mapper实例并调用其中方法
let getMapper = `
import coderead.tests.dao.UserMapper;
//get(Class) 获取类的实例
UserMapper userMapper=get(UserMapper.class)[0]
userMapper.selectByid(12) 
`;


// 获取Spring中所有的Bean
let getSpringBeanAll = `
import org.springframework.context.ApplicationContext
ApplicationContext context=get(ApplicationContext.class)[0]
context.getBeanDefinitionNames().collect {it-> 
  context.getBean(it)
}
`

// 获取数据库连接
let getHikariPool = `
import com.zaxxer.hikari.HikariPoolMXBean
import com.zaxxer.hikari.HikariDataSource
HikariDataSource source=get(HikariDataSource)[0]
HikariPoolMXBean mxBean= source.getHikariPoolMXBean()
  
def total=mxBean.getTotalConnections()
def active=mxBean.getActiveConnections()
def idle=mxBean.getIdleConnections()
def awaiting=mxBean.getThreadsAwaitingConnection()
String log="总数:$total,激活数:$active,闲置数:$idle,等待数:$awaiting}"
`

let getC3poPool = `
import com.mchange.v2.c3p0.PooledDataSource
get(PooledDataSource.class)[0].toString()
`
let getDruidPool = `
get(com.alibaba.druid.pool.DruidDataSource)[0].toString()`
let getDbcpPool = `
import org.apache.commons.dbcp2.BasicDataSource
BasicDataSource dataSource = get(BasicDataSource)[0];     
// 获取连接池信息
int maxActiveConnections = dataSource.getMaxTotal();
int maxIdleConnections = dataSource.getMaxIdle();
int minIdleConnections = dataSource.getMinIdle();
int numActiveConnections = dataSource.getNumActive();
int numIdleConnections = dataSource.getNumIdle();
String log="连接信息 最大激活数:$maxActiveConnections,最大闲置数:$maxIdleConnections,最小闲置数：$minIdleConnections,当前激活数:$numActiveConnections,当前闲置数:$numIdleConnections";
       `
export let items = [
    { title: "获取系统属性", code: getProperties },
    { title: "获取Spring 上下文", code: getSpringContext },
    { title: "调用MyBatis中Mapper实例方法", code: getMapper },
    { title: "获取Spring中所有Bean", code: getSpringBeanAll },

    { title: "HikariPool-连接池信息", code: getHikariPool },
    { title: "c3p0-连接池信息", code: getC3poPool },
    { title: "Druid-连接池信息", code: getDruidPool },
    { title: "Dbcp-连接池信息", code: getDbcpPool },

]

export default {
    items
}