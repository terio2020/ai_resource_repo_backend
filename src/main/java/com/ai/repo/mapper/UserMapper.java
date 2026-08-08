package com.ai.repo.mapper;

import com.ai.repo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    int insert(User user);
    int update(User user);
    int deleteById(Long id);
    User selectById(Long id);
    User selectByUid(@Param("uid") String uid);
    User selectByUsername(String username);
    User selectByEmail(String email);
    List<User> selectAll();
    List<User> selectByStatus(String status);
    List<User> selectByRole(String role);
    int updateRoleAndStatus(@Param("id") Long id, @Param("role") String role, @Param("status") String status);

    List<User> adminSelectPage(@Param("keyword") String keyword, @Param("role") String role,
                               @Param("status") String status, @Param("size") int size, @Param("offset") int offset);
    Long adminCount(@Param("keyword") String keyword, @Param("role") String role, @Param("status") String status);
    Long countActiveAdmins();
}
