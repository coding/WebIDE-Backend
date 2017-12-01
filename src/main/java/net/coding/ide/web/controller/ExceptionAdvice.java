/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.controller;

import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonBooleanFormatVisitor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.coding.ide.model.exception.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.*;

/**
 * Created by vangie on 15/1/26.
 */
@ControllerAdvice
public class ExceptionAdvice {

    @Value("${spring.http.multipart.max-file-size}")
    private String fileSizeLimit;

    @ExceptionHandler(UnsupportedEncodingException.class)
    @ResponseBody
    public ResponseEntity<JsonObject> unsupportedEncodingException(UnsupportedEncodingException e) {
        return makeMsgWithHttpStatus(BAD_REQUEST.value(), "unsupported encoding: " + e.getMessage());
    }

    @ExceptionHandler(WorkspaceIOException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    public JsonObject workspaceIOException(WorkspaceIOException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(IOException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    public JsonObject IOErrors(IOException e) {
        e.printStackTrace();

        return makeMsg(e);
    }

    @ExceptionHandler(ResourceAccessException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    public JsonElement resourceAccessException(ResourceAccessException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(GitAPIException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    public JsonObject gitAPIErrors(GitAPIException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(JGitInternalException.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    @ResponseBody
    public JsonObject jGitInternalErrors(JGitInternalException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(WorkspaceException.class)
    @ResponseBody
    public ResponseEntity<JsonObject> workspaceException(WorkspaceException e) {

        if (e instanceof WorkspaceMaintainingException) {
            return makeMsgWithHttpStatus(SERVICE_UNAVAILABLE.value(), e.getMessage());
        } else if (e instanceof WorkspaceMissingException) {
            if (e instanceof WorkspaceDeletedException) {
                return makeMsgWithHttpStatus(GONE.value(), e.getMessage());
            } else {
                return makeMsgWithHttpStatus(NOT_FOUND.value(), e);
            }
        } else if (e instanceof WorkspaceCreationException) {
            return makeMsgWithHttpStatus(INTERNAL_SERVER_ERROR.value(), e);
        }

        return makeMsgWithHttpStatus(NOT_FOUND.value(), e.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(PAYLOAD_TOO_LARGE)
    @ResponseBody
    public JsonObject maxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        return makeMsg(format("Upload file size is limit to %s.", fileSizeLimit));
    }

    @ExceptionHandler(GitCloneAuthFailException.class)
    @ResponseStatus(FORBIDDEN)
    @ResponseBody
    public JsonObject gitCloneAuthFileException(GitCloneAuthFailException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(TransportProtocolUnsupportedException.class)
    @ResponseStatus(METHOD_NOT_ALLOWED)
    @ResponseBody
    public JsonObject transportProtocolUnsupportedException(TransportProtocolUnsupportedException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(METHOD_NOT_ALLOWED)
    @ResponseBody
    public JsonObject httpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(GitInvalidPathException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public JsonObject gitInvalidPathException(GitInvalidPathException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(GitInvalidRefException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public JsonObject gitInvalidRefException(GitInvalidRefException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public JsonObject missingServletRequestParameterException(MissingServletRequestParameterException e) {
        return makeMsg(format("parameter '%s' required", e.getParameterName()));
    }

    @ExceptionHandler(GitInvalidDiffException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public JsonObject gitInvalidDiffException(GitInvalidDiffException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(GitOperationException.class)
    @ResponseStatus(BAD_REQUEST)
    @ResponseBody
    public JsonObject gitOperationException(GitOperationException e) {
        return makeMsg(e);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    @ResponseBody
    public ResponseEntity<JsonObject> notfoundException(NotFoundException e) {
        return makeMsgWithHttpStatus(404, e.getMessage());
    }

    private JsonObject makeMsg(Throwable t) {
        JsonObject jsonObject = makeMsg(t.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);

        jsonObject.addProperty("stacktrace", sw.toString());

        return jsonObject;
    }

    private JsonObject makeMsg(String msg) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("msg", msg);

        return jsonObject;
    }

    private JsonObject makeMsg(int code, String msg) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("code", code);
        jsonObject.addProperty("msg", msg);

        return jsonObject;
    }

    private JsonObject makeMsg(int code, Throwable t) {
        JsonObject jsonObject = makeMsg(code, t.getMessage());

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);

        jsonObject.addProperty("stacktrace", sw.toString());

        return jsonObject;
    }


    private ResponseEntity<JsonObject> makeMsgWithHttpStatus(int httpStatus, String message) {
        return new ResponseEntity<>(makeMsg(httpStatus, message), HttpStatus.valueOf(httpStatus));
    }

    private ResponseEntity<JsonObject> makeMsgWithHttpStatus(int httpStatus, Throwable t) {
        return new ResponseEntity<>(makeMsg(httpStatus, t), HttpStatus.valueOf(httpStatus));
    }
}
