/*
 * Copyright (c) 2014-2016 CODING.
 */

package net.coding.ide.web.message;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.AbstractMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Created by vangie on 15/2/12.
 */
public class GsonMessageConverter extends AbstractMessageConverter {

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    private static List<MimeType> mimeTypeList = Lists.newArrayList();

    static {
        mimeTypeList.add(new MediaType("application", "json", DEFAULT_CHARSET));
        mimeTypeList.add(new MediaType("application", "*+json", DEFAULT_CHARSET));
    }

    private Gson gson = new Gson();


    public GsonMessageConverter() {
        super(mimeTypeList);
    }


    /**
     * Set the {@code Gson} instance to use.
     * If not set, a default {@link Gson#Gson() Gson} instance is used.
     * <p>Setting a custom-configured {@code Gson} is one way to take further
     * control of the JSON serialization process.
     */
    public void setGson(Gson gson) {
        Assert.notNull(gson, "'gson' is required");
        this.gson = gson;
    }

    /**
     * Return the configured {@code Gson} instance for this converter.
     */
    public Gson getGson() {
        return this.gson;
    }


    @Override
    protected boolean supports(Class<?> clazz) {
        return true;
    }

    @Override
    public Object convertFromInternal(Message<?> message, Class<?> targetClass) {

        TypeToken<?> token = getTypeToken(targetClass);

        Object payload = message.getPayload();

        Charset charset = getCharset(getMimeType(message.getHeaders()));

        Reader reader;

        if (payload instanceof byte[]) {
            reader = new InputStreamReader(new ByteArrayInputStream((byte[]) payload), charset);
        } else {
            reader = new StringReader((String) payload);
        }

        try {

            return this.gson.fromJson(reader, token.getType());

        } catch (JsonParseException ex) {
            throw new MessageConversionException(message, "Could not read JSON: " + ex.getMessage(), ex);
        }
    }


    @Override
    public Object convertToInternal(Object payload, MessageHeaders headers) {

        Charset charset = getCharset(getMimeType(headers));
        try {
            if (byte[].class.equals(getSerializedPayloadClass())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
                OutputStreamWriter writer = new OutputStreamWriter(out, charset);
                this.gson.toJson(payload, writer);
                writer.close();
                payload = out.toByteArray();
            } else {
                Writer writer = new StringWriter();
                this.gson.toJson(payload, writer);
                payload = writer.toString();
            }
        } catch (IOException ex) {
            throw new MessageConversionException("Could not write JSON: " + ex.getMessage(), ex);
        }

        return payload;
    }

    protected TypeToken<?> getTypeToken(Type type) {
        return TypeToken.get(type);
    }

    private Charset getCharset(MimeType contentType) {

        if ((contentType != null) && (contentType.getCharSet() != null)) {
            return contentType.getCharSet();
        }

        return DEFAULT_CHARSET;

    }

}
