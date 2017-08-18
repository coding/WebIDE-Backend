/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.repository;

import net.coding.ide.annotation.RepositoryTest;
import net.coding.ide.entity.ProjectEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

@RunWith(SpringRunner.class)
@RepositoryTest
public class ProjectRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ProjectRepository projectRepo;

    @Test
    public void testFindBySshUrl() throws SQLException {
        ProjectEntity projectEntity = projectRepo.findByUrl("git@coding.net:duwan/coding-ide.git");

        Assert.assertEquals(0L, (long)projectEntity.getId());
    }

    @Test
    public void testDatabase() throws SQLException {
        org.hibernate.engine.spi.SessionImplementor sessionImp =
                (org.hibernate.engine.spi.SessionImplementor) entityManager.getEntityManager().getDelegate();
        DatabaseMetaData metadata = sessionImp.connection().getMetaData();

        Assert.assertEquals(metadata.getDatabaseProductName(), "H2");
    }
}