package com.ai.repo.playground.service;

import javax.sql.DataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.ai.repo.playground.mapper.PlaygroundMapper;
import com.ai.repo.playground.mapper.PlaygroundPersistenceConfiguration;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlaygroundMapperConfigurationTest {
    @Test void realApplicationRegistersTheNewMapperOutsideItsExistingScanPackage() {
        Configuration configuration=new Configuration();
        configuration.setEnvironment(new Environment("test",new JdbcTransactionFactory(),mock(DataSource.class)));
        SqlSessionFactory factory=new SqlSessionFactoryBuilder().build(configuration);
        new ApplicationContextRunner().withUserConfiguration(PlaygroundPersistenceConfiguration.class)
                .withBean(SqlSessionFactory.class,()->factory).run(context->{
                    assertNull(context.getStartupFailure()); assertNotNull(context.getBean(PlaygroundMapper.class));
                    assertTrue(factory.getConfiguration().hasStatement(PlaygroundMapper.class.getName()+".lockTask"));
                });
    }
}
