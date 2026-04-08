package com.ai.repo.mapper;

import com.ai.repo.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {
    int insert(User user);
    int update(User user);
    int deleteById(Long id);
    User selectById(Long id);
    User selectByUsername(String username);
    User selectByEmail(String email);
    List<User> selectAll();
    List<User> selectByStatus(String status);
    List<User> selectByRole(String role);
}
