package cn.coderead.testspring.mapper;

import cn.coderead.testspring.bean.User;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.StatementType;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select({"select * from users where id=#{userId}"})
    User selectByid(Integer id);

    @Select({"select * from users where id=#{userId}"})
    @Options
    User selectByid2(Integer id);

    @Select({"select * from users where name=#{name} or age=#{user.age}"})
    @Options
    User selectByNameOrAge(@Param("name") String name, @Param("user") User user);

    @Select({" select * from users where name='${name}'"})
    @Options(statementType = StatementType.PREPARED)
    List<User> selectByName(User user);


    @Insert("INSERT INTO `users`( `name`, `age`, `sex`, `email`, `phone_number`) VALUES ( #{name}, #{age}, #{sex}, #{email}, #{phoneNumber})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int addUser(User user);

    int editUser(User user);

    @Update("update  users set name=#{name} where id=#{id}")
//    @Options(flushCache = Options.FlushCachePolicy.FALSE)
    int setName(@Param("id") Integer id, @Param("name") String name);

    @Update("select * from users where id=#{id} or name=#{name}")
//    @Options(flushCache = Options.FlushCachePolicy.FALSE)
    int setName2(@Param("id") Integer id, @Param("name") String name);

    @Delete("delete from users where id=#{id}")
    int deleteUser(Integer id);

}
