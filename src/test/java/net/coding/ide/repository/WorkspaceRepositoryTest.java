/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.repository;

import net.coding.ide.annotation.RepositoryTest;
import net.coding.ide.entity.WorkspaceEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import static net.coding.ide.entity.WorkspaceEntity.WsWorkingStatus.Offline;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Created by vangie on 14/12/5.
 */
@RunWith(SpringRunner.class)
@RepositoryTest
public class WorkspaceRepositoryTest {

    @Autowired
    private WorkspaceRepository wsRepo;

    @Test
    public void testFindBySpaceKey() {
        WorkspaceEntity ws = wsRepo.findBySpaceKey("qwerty");

        assertThat(ws.getId(), is(1L));
    }

    @Test
    public void testExist() {
        assertThat(wsRepo.isSpaceKeyExist("fadga"), is(false));
        assertThat(wsRepo.isSpaceKeyExist("qwerty"), is(true));
    }

    @Test
    public void testIsOnline() {
        assertThat(wsRepo.isOnline("qwerty"), is(true));
        assertThat(wsRepo.isOnline("aasdfh"), is(false));
    }

    @Test
    public void testUpdateWorkingStatus() {
        assertThat(wsRepo.isOnline("qwerty"), is(true));
        wsRepo.updateWorkingStatus("qwerty", Offline);
        assertThat(wsRepo.isOnline("qwerty"), is(false));
    }
}
