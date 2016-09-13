/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.controller;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import net.coding.ide.dto.DirDTO;
import net.coding.ide.dto.FileDTO;
import net.coding.ide.dto.FileSearchResultEntryDTO;
import net.coding.ide.dto.WorkspaceDTO;
import net.coding.ide.entity.WorkspaceEntity;
import net.coding.ide.model.FileInfo;
import net.coding.ide.model.FileSearchResultEntry;
import net.coding.ide.model.Workspace;
import net.coding.ide.model.exception.WorkspaceMissingException;
import net.coding.ide.service.GitManager;
import net.coding.ide.service.WorkspaceManager;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;
import static java.util.Collections.synchronizedList;
import static net.coding.ide.model.HttpSessions.OPENED_WORKSPACE_LIST;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * Created by vangie on 14/11/10.
 */
@Slf4j
@RestController
@RequestMapping(produces = APPLICATION_JSON_VALUE)
public class WorkspaceController {
    @Autowired
    private WorkspaceManager wsMgr;

    @Autowired
    private ModelMapper mapper;

    @Autowired
    private GitManager gitMgr;

    private final static String SETTINGS_PATH = ".coding-ide/settings.json";

    @RequestMapping(value = "/workspaces", method = GET)
    public List<WorkspaceDTO> list() {
        List<WorkspaceEntity> workspaces = wsMgr.list();

        return workspaces.stream()
                .map(source -> mapper.map(source, WorkspaceDTO.class))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/setup", method = POST)
    public WorkspaceDTO setup(@PathVariable String spaceKey,
                              HttpSession session) throws IOException {

        log.debug("setup workspace for spaceKey => {}", spaceKey);

        Workspace ws = wsMgr.setup(spaceKey);

        addWsToSession(session, spaceKey);

        log.debug("WorkspaceController create session id => {}", session.getId());

        return mapper.map(ws, WorkspaceDTO.class);
    }

    @RequestMapping(value = "/workspaces", method = POST)
    public WorkspaceDTO clone(@RequestParam String url,
                              HttpSession session) {

        log.debug("Import workspace for url => {}", url);

        Workspace ws = wsMgr.createFromUrl(url);

        String spaceKey = ws.getSpaceKey();

        addWsToSession(session, spaceKey);

        log.debug("WorkspaceController create session id => {}", session.getId());

        return mapper.map(ws, WorkspaceDTO.class);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}", method = GET)
    public WorkspaceDTO queryWorkspace(@PathVariable("spaceKey") String spaceKey) {;

        WorkspaceEntity workspaceEntity = wsMgr.getWorkspaceEntity(spaceKey);

        if (workspaceEntity == null) {
            throw new WorkspaceMissingException(format("Workspace %s is not found.", spaceKey));
        }

        return mapper.map(workspaceEntity, WorkspaceDTO.class);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}", method = DELETE)
    public ResponseEntity delete(@PathVariable String spaceKey) throws Exception {
        wsMgr.delete(spaceKey);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/settings", method = GET)
    public FileDTO getSettings(@PathVariable("spaceKey") Workspace ws,
                               @RequestParam(defaultValue = "false") boolean base64) throws IOException {

        if (!ws.exists(SETTINGS_PATH)) {
            ws.write(SETTINGS_PATH, "{}", false, true, true);
        }

        return FileDTO.of(SETTINGS_PATH,
                ws.read(SETTINGS_PATH, base64),
                base64);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/settings", method = PUT)
    public FileDTO setSettings(@PathVariable("spaceKey") Workspace ws,
                               @RequestParam String content,
                               @RequestParam(defaultValue = "false") boolean base64,
                               @RequestParam(defaultValue = "true") boolean override,
                               @RequestParam(defaultValue = "true") boolean createParent,
                               @RequestParam(defaultValue = "false") boolean addIgnore) throws IOException {

        ws.write(SETTINGS_PATH, content, base64, override, createParent);

        if (addIgnore) {
            if (!ws.exists(".gitignore")) {
                ws.create(".gitignore");
            }
            String ignoreFile = ws.read(".gitignore", false);
            InputStream in = new ByteArrayInputStream(ignoreFile.getBytes());
            if (!gitMgr.checkIgnore(in, ".coding-ide", true)) {
                ignoreFile += "\n#Coding WebIDE Settings\n/.coding-ide\n";
                ws.write(".gitignore", ignoreFile, false, true, true);
            }
        }

        return FileDTO.of(SETTINGS_PATH,
                ws.read(SETTINGS_PATH, base64),
                base64);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/files", method = GET)
    public List<FileInfo> list(@PathVariable("spaceKey") Workspace ws,
                               @RequestParam(defaultValue = "/") String path,
                               @RequestParam(defaultValue = "true") boolean order,
                               @RequestParam(defaultValue = "true") boolean group) throws Exception {

        return wsMgr.listFiles(ws, path, order, group);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/file/read", method = GET)
    public FileDTO read(@PathVariable("spaceKey") Workspace ws,
                        @RequestParam String path,
                        @RequestParam(defaultValue = "false") boolean base64) throws Exception {

        FileInfo fileInfo = wsMgr.getFileInfo(ws, path);

        return FileDTO.of(path,
                ws.read(path, base64),
                base64,
                fileInfo.getLastModified().getMillis());
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/files", method = PUT)
    public FileDTO write(@PathVariable("spaceKey") Workspace ws,
                         @RequestParam String path,
                         @RequestParam String content,
                         @RequestParam(defaultValue = "false") boolean base64,
                         @RequestParam(defaultValue = "true") boolean override,
                         @RequestParam(defaultValue = "true") boolean createParent) throws Exception {
        ws.write(path, content, base64, override, createParent);

        FileDTO fileDTO =  FileDTO.of(path,
                ws.read(path, base64),
                base64);

        if (ws.exists(path)) {
            FileInfo fileInfo = wsMgr.getFileInfo(ws, path);
            fileDTO.setLastModified(fileInfo.getLastModified().getMillis());
        }

        return fileDTO;
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/files", method = POST)
    public FileInfo createFile(@PathVariable("spaceKey") Workspace ws,
                               @RequestParam String path) throws Exception {

        ws.create(path);

        return wsMgr.getFileInfo(ws, path);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/files", method = DELETE)
    public ResponseEntity remove(@PathVariable("spaceKey") Workspace ws,
                                 @RequestParam String path,
                                 @RequestParam(defaultValue = "false") boolean recursive) throws Exception {
        ws.remove(path, recursive);

        return new ResponseEntity(HttpStatus.NO_CONTENT);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/move", method = POST)
    public FileInfo move(@PathVariable("spaceKey") Workspace ws,
                         @RequestParam String from,
                         @RequestParam String to,
                         @RequestParam(defaultValue = "false") boolean force) throws Exception {
        ws.move(from, to, force);

        return wsMgr.getFileInfo(ws, to);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/copy", method = POST)
    public FileInfo copy(@PathVariable("spaceKey") Workspace ws,
                         @RequestParam String from,
                         @RequestParam String to,
                         @RequestParam(defaultValue = "false") boolean force) throws Exception {
        ws.copy(from, to, force);
        return wsMgr.getFileInfo(ws, to);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/upload", method = POST)
    public List<FileInfo> upload(@PathVariable("spaceKey") Workspace ws,
                                 @RequestParam String path,
                                 @RequestParam List<MultipartFile> files) throws IOException {
        List<FileInfo> fileInfos = Lists.newArrayList();

        for (MultipartFile file : files) {
            InputStream in = file.getInputStream();
            String fileName = file.getOriginalFilename();
            ws.write(path, fileName, in);
            FileInfo fileInfo = null;
            try {
                fileInfo = wsMgr.getFileInfo(ws, Paths.get(path, fileName).toString());
            } catch (Exception e) {
                log.error("upload file error for spaceKey => {} failed: path is => {} and exception is {}", ws.getSpaceKey(), path, e.getMessage());
            }
            fileInfos.add(fileInfo);
        }

        return fileInfos;
    }


    @RequestMapping(value = "/workspaces/{spaceKey}/mkdir", method = POST)
    public DirDTO mkdir(@PathVariable("spaceKey") Workspace ws,
                        @RequestParam String path) throws IOException {
        ws.mkdir(path);

        return DirDTO.of(ws.getSpaceKey(), path);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/raw", method = GET)
    public ResponseEntity<InputStreamResource> raw(@PathVariable("spaceKey") Workspace ws,
                                                   @RequestParam String path,
                                                   @RequestParam(defaultValue = "true") Boolean inline) throws Exception {


        FileInfo fileInfo = wsMgr.getFileInfo(ws, path);

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.valueOf(fileInfo.getContentType()));
        headers.setContentLength(fileInfo.getSize());

        if (inline) {
            headers.add(CONTENT_DISPOSITION, format("inline; filename='%s'", encodeFileName(fileInfo.getName())));
        } else {
            headers.add(CONTENT_DISPOSITION, format("attachment; filename='%s'", encodeFileName(fileInfo.getName())));
        }

        InputStreamResource inputStreamResource = new InputStreamResource(ws.getInputStream(path));

        return ResponseEntity.ok()
                .headers(headers)
                .body(inputStreamResource);
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/pack", method = GET)
    public void pack(@PathVariable("spaceKey") Workspace ws,
                     @RequestParam String path,
                     HttpServletResponse resp) throws Exception {

        FileInfo fileInfo = wsMgr.getFileInfo(ws, path);

        resp.setHeader(CONTENT_TYPE, "application/x-gzip");
        resp.setHeader(CONTENT_DISPOSITION, "attachment; filename='" + encodeFileName(fileInfo.getName()) + ".tar.gz'");

        ws.pack(path, resp.getOutputStream());
    }

    private String encodeFileName(String fileName) {
        try {
            return encode(fileName.replaceAll(" ", "_"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return fileName;
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/search", method = POST)
    public List<FileSearchResultEntryDTO> search(@PathVariable("spaceKey") Workspace ws,
                             @RequestParam String keyword,
                             @RequestParam(defaultValue = "false") boolean includeNonProjectItems) throws IOException {
        List<FileSearchResultEntry> files = wsMgr.search(ws, keyword, includeNonProjectItems);

        return files.stream()
                .map(entry -> mapper.map(entry, FileSearchResultEntryDTO.class))
                .collect(Collectors.toList());
    }

    @RequestMapping(value = "/workspaces/{spaceKey}/encoding", method = PUT)
    public WorkspaceDTO setEncoding(@PathVariable("spaceKey") Workspace ws,
                                  @RequestParam(defaultValue = "UTF-8") String charset) {
        wsMgr.setEncoding(ws, charset);
        ws.setEncoding(charset);

        return mapper.map(ws, WorkspaceDTO.class);
    }

    private void addWsToSession(HttpSession session, String spaceKey) {
        List<Map<String, String>> openedWsList = (List<Map<String, String>>) session.getAttribute(OPENED_WORKSPACE_LIST);
        if (openedWsList == null) {
            openedWsList = synchronizedList(new ArrayList<Map<String, String>>());
            session.setAttribute(OPENED_WORKSPACE_LIST, openedWsList);
        }

        Map<String, String> ws = new HashMap<>();
        ws.put("spaceKey", spaceKey);

        openedWsList.add(ws);
    }
}
