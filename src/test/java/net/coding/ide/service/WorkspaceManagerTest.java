/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.service;

import com.google.common.cache.Cache;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import net.coding.ide.entity.ProjectEntity;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.FileSearchResultEntry;
import net.coding.ide.model.GitStatus;
import net.coding.ide.model.Workspace;
import net.coding.ide.model.exception.WorkspaceMissingException;
import net.coding.ide.repository.ProjectRepository;
import net.coding.ide.repository.WorkspaceRepository;
import net.coding.ide.utils.RandomGenerator;
import net.coding.ide.utils.RepositoryHelper;
import org.eclipse.jgit.lib.Repository;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static junit.framework.TestCase.assertEquals;
import static net.coding.ide.entity.WorkspaceEntity.WsWorkingStatus.Deleted;
import static net.coding.ide.utils.FilesUtils.createTempDirectoryAndDeleteOnExit;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Created by phy on 2014/12/8.
 */
public class WorkspaceManagerTest extends BaseServiceTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @InjectMocks
    private WorkspaceManagerImpl wsMgr;

    @Mock
    private GitManager gitMgr;

    @Mock
    private WorkspaceRepository wsRepo;

    @Mock
    private KeyManager keyMgr;

    @Mock
    private RandomGenerator randomGene;

    private File testSpaceHome;

    @Mock
    private Cache wsCache;

    @Mock
    private ProjectRepository prjRepo;

    @Mock
    private LoadingCache<String, Repository> repoCache;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ApplicationEventPublisher publisher;

    private Workspace ws;

    @Before
    public void setUp() throws Exception {
        testSpaceHome = createTempDirectoryAndDeleteOnExit("codingSpaceKeys");

        ws = initWorkspace();
        Repository repository = RepositoryHelper.createRepository(ws.getWorkingDir());

        when(repoCache.get(ws.getSpaceKey())).thenReturn(repository);
        when(repoCache.asMap()).thenReturn(new ConcurrentHashMap<>());

        when(gitMgr.status(ws, ws.getRelativePath("/"))).thenReturn(GitStatus.NONE);
    }

    private ProjectEntity initProjectEntity() {
        ProjectEntity prj = new ProjectEntity();
        prj.setName("mine");
        prj.setOwnerName("me");
        prj.setSshUrl("git@coding.net:kevenyoung03/Test02.git");

        return prj;
    }

    private WorkspaceEntity initWorkspaceEntity() {
        ProjectEntity prj = initProjectEntity();

        WorkspaceEntity wsEntity = new WorkspaceEntity();
        wsEntity.setSpaceKey("qwerty");
        wsEntity.setProject(prj);

        when(wsRepo.findBySpaceKey("qwerty")).thenReturn(wsEntity);
        when(randomGene.generate(Matchers.anyVararg())).thenReturn("qwerty");

        return wsEntity;
    }

    private Workspace initWorkspace() {
        WorkspaceEntity wsEntity = initWorkspaceEntity();
        return spy(new Workspace(wsEntity, new File(testSpaceHome, "qwerty")));
    }

    @Test
    public void testGetFileInfo() throws Exception {
        ws.create("a.txt");

        when(gitMgr.status(ws, Paths.get("a.txt"))).thenReturn(GitStatus.UNTRACKED);

        FileInfo fileInfo = wsMgr.getFileInfo(ws, "a.txt");

        assertEquals("a.txt", fileInfo.getName());
        assertEquals("/a.txt", fileInfo.getPath());
        assertEquals(0, fileInfo.getSize());
        assertEquals(false, fileInfo.isDir());
        assertEquals("text/plain", fileInfo.getContentType());
        assertEquals(GitStatus.UNTRACKED, fileInfo.getGitStatus());
    }

    @Test
    public void testGetSymbolLinkFileInfo() throws Exception {
        ws.create("a.txt");

        Files.createSymbolicLink(ws.getPath("linka"), ws.getPath("a.txt"));

        FileInfo fileInfo = wsMgr.getFileInfo(ws, "linka");

        assertEquals(fileInfo.getPath(), "/linka");
        assertEquals(fileInfo.isDir(), false);
        assertEquals(fileInfo.isSymbolicLink(), true);
    }

    @Test
    public void testGetTargetNotExistSymbolLinkFileInfo() throws Exception {
        Files.createSymbolicLink(ws.getPath("linka"), ws.getPath("a.txt"));

        FileInfo fileInfo = wsMgr.getFileInfo(ws, "linka");

        assertEquals(fileInfo.getPath(), "/linka");
        assertEquals(fileInfo.isDir(), false);
        assertEquals(fileInfo.isSymbolicLink(), true);
    }


    @Test
    public void testGetRootFileInfo() throws Exception {
        FileInfo fileInfo = wsMgr.getFileInfo(ws, "/");

        assertEquals(".", fileInfo.getName());
        assertEquals(Paths.get("/").toString(), fileInfo.getPath());
        assertEquals(true, fileInfo.isDir());
        assertEquals(null, fileInfo.getContentType());
    }

    @Test
    public void testGetFileInfoOfDirectory() throws Exception {
        ws.mkdir("mk");

        FileInfo fileInfo = wsMgr.getFileInfo(ws, "./mk");

        assertEquals("mk", fileInfo.getName());
        assertEquals(Paths.get("/mk").toString(), fileInfo.getPath());
        assertEquals(true, fileInfo.isDir());
        assertEquals(null, fileInfo.getContentType());
    }

    @Test
    public void testListFiles() throws Exception {
        ws.create("a.txt");
        ws.create("b.txt");
        ws.create(".a.txt.swp");
        ws.create(".swa");
        ws.create("zerocopy.swf");

        ws.mkdir("mk");
        ws.create("mk/a.txt");
        ws.create("mk/.a.txt.swp");

        assertEquals(5, wsMgr.listFiles(ws, "/", false, true).size());
        assertEquals(1, wsMgr.listFiles(ws, "mk/", false, true).size());

    }

    @Test
    public void testListFiles_sort() throws Exception {
        ws.create("a.txt");
        ws.create("c.txt");
        ws.create("b.txt");

        List<FileInfo> result = wsMgr.listFiles(ws, ".", true, true);

        assertEquals(".git", result.get(0).getName());
        assertEquals("a.txt", result.get(1).getName());
        assertEquals("b.txt", result.get(2).getName());
        assertEquals("c.txt", result.get(3).getName());
    }


    @Test
    public void testSetup() {
        WorkspaceEntity wsEntity = initWorkspaceEntity();

        when(wsRepo.findBySpaceKey("qwerty")).thenReturn(wsEntity);

        Workspace ws = wsMgr.setup("qwerty");

        assertThat(ws.getSpaceKey(), is("qwerty"));
        verify(wsRepo, times(1)).findBySpaceKey("qwerty");
    }

    @Test
    public void testCreateBySpaceKeyForWorkspaceNotFound() {

        initWorkspaceEntity();

        when(wsRepo.findBySpaceKey("qwerty")).thenReturn(null);

        exception.expect(WorkspaceMissingException.class);
        exception.expectMessage(is("Workspace qwerty is not found."));

        wsMgr.setup("qwerty");
    }

    @Test
    public void testGetWorkspace() {
        Workspace ws = wsMgr.getWorkspace("qwerty");

        assertThat(ws.getSpaceKey(), is("qwerty"));
    }

    @Test
    public void testDeleteWorkspace() {
        when(wsRepo.isDeleted("qwerty")).thenReturn(false);

        wsMgr.delete("qwerty");

        verify(wsRepo, times(1)).isDeleted("qwerty");
        verify(wsCache, times(1)).invalidate("qwerty");
    }

    @Test
    public void testSetEncoding() {

        Workspace ws = initWorkspace();
        WorkspaceEntity wsEntity = ws.getWorkspaceEntity();
        when(wsRepo.findBySpaceKey("qwerty")).thenReturn(wsEntity);

        wsMgr.setEncoding(ws, "GBK");

        verify(wsRepo, times(1)).save(wsEntity);

        assertThat(wsEntity.getEncoding(), is("GBK"));
    }

    @Test
    public void testSearchFile() throws Exception {
        Workspace ws = initWorkspace();
        List<String> paths = Lists.newArrayList("/a.txt", "/c/a.txt");
        List<FileSearchResultEntry> expected = Lists.newArrayList(new FileSearchResultEntry("/a.txt", "text/plain"), new FileSearchResultEntry("/c/a.txt", "text/plain"));

        when(ws.search("a", true)).thenReturn(paths);

        List<FileSearchResultEntry> actual = wsMgr.search(ws, "a", true);

        Assert.assertEquals(actual, expected);
    }
}
