package com.neopixl.spitfire.request;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.VolleyLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.neopixl.spitfire.SpitfireManager;
import com.neopixl.spitfire.listener.RequestListener;

import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Florian ALONSO on 12/30/16.
 * For Neopixl
 */

public class BaseRequest<T> extends AbstractRequest<T> {

    @Nullable
    private Object jsonObject;

    @Nullable
    private Map<String, String> standardParams;

    /**
     * Class Builder used to create a new request
     */
    public static class Builder<T> extends AbstractBuilder<T, BaseRequest<T>> {


        private Map<String, String> parameters;

        @Nullable
        private Object jsonObject;

        /**
         * Default
         *
         * @param method        used to send the request
         * @param url           given url to access the resource, not null
         * @param classResponse class used to parse the response
         */
        public Builder(int method, @NonNull String url, Class<T> classResponse) {
            super(method, url, classResponse);
        }

        /**
         * Specifies the object
         * @param jsonObject The object to be embedded in the body, can be null
         * @return Builder {@link Builder}
         */
        public Builder<T> object(@Nullable Object jsonObject) {
            this.jsonObject = jsonObject;
            return this;
        }

        /**
         * Set the parameters for the request
         * @param parameters Map&lt;String, String&gt;, not null
         * @return Builder {@link Builder}
         */
        public Builder<T> parameters(@NonNull  Map<String, String> parameters) {
            this.parameters = new HashMap<>(parameters);
            return this;
        }

        /**
         * Set the listener for the request
         * @param listener {@link RequestListener}, can be null
         * @return
         */
        @Override
        public Builder<T> listener(@Nullable RequestListener<T> listener) {
            super.listener(listener);
            return this;
        }

        /**
         * Set the headers for the request
         * @param headers used to send the request, not null
         * @return
         */
        @Override
        public Builder<T> headers(@NonNull Map<String, String>  headers) {
            super.headers(headers);
            return this;
        }

        /**
         * Create a request based on the current request
         * @return The request
         */

        public BaseRequest<T> build() {
            return new BaseRequest<T>(this);
        }
    }

    /**
     * Constructor using the builder
     * @param builder {@link Builder}
     */
    protected BaseRequest(Builder builder) {
        super(builder);

        this.standardParams = builder.parameters;
        this.jsonObject = builder.jsonObject;

        if (builder.method == Method.GET && jsonObject != null) {
            throw new IllegalArgumentException("Cannot use json body request with GET");
        }
    }

    /**
     * Get the json content type  (default : "application/json; charset=UTF-8")
     * @return the current content type
     */
    @NonNull
    public String getJsonContentType() {
        return "application/json; charset=" + getParamsEncoding();
    }

    /**
     *
     * @return the current body content type
     */

    @Override
    @NonNull
    public String getBodyContentType() {
        if ((getMethod() == com.android.volley.Request.Method.POST || getMethod() == com.android.volley.Request.Method.PUT) && getJsonObject() != null) {
            return getJsonContentType();
        }
        return super.getBodyContentType();
    }

    /**
     * Returns the raw POST or PUT body to be sent.
     *
     * <p>By default, the body consists of the request parameters in
     * application/x-www-form-urlencoded format. When overriding this method, consider overriding
     * {@link #getBodyContentType()} as well to match the new body format.
     *
     * @throws AuthFailureError in the event of auth failure
     */
    @Override
    @Nullable
    public byte[] getBody() throws AuthFailureError {
        String bodyContentType = getBodyContentType();
        int method = getMethod();
        if (method != Method.GET && bodyContentType != null && bodyContentType.equals(getJsonContentType())) {
            return getJsonBody();
        }
        return super.getBody();
    }


    /**
     * Returns the raw POST or PUT body to be sent in a JSON format.
     *
     * <p>This method will use the object given in the builder in order to
     * build, parse and bind the exact value and format.
     *
     * @throws AuthFailureError in the event of auth failure
     */
    @Nullable
    public byte[] getJsonBody() {
        byte ptext[];
        try {
            ptext = SpitfireManager.getObjectMapper().writeValueAsBytes(getJsonObject());
            VolleyLog.d("Sending JSON BODY : " + new String(ptext, getParamsEncoding()));
        } catch (JsonProcessingException e) {
            JSONObject object = new JSONObject(getParams());
            VolleyLog.d("Sending JSON FROM PARAMS : " + object.toString());
            ptext = object.toString().getBytes();
        } catch (UnsupportedEncodingException e) {
            ptext = null;
            e.printStackTrace();
        }
        return ptext;
    }

    /**
     * Get the url for the request
     * @return the url used to access the ressource, not null
     */
    @Override
    @NonNull
    public String getUrl() {
        int method = getMethod();
        String topUrl = super.getUrl();
        if (method == Method.GET) {
            return parseGetUrl(method, topUrl, getParams(), getParamsEncoding());
        }
        return topUrl;
    }

    /**
     * Get the json object for the request
     * @return the json body, can be null
     */
    @Nullable
    public Object getJsonObject() {
        return jsonObject;
    }

    /**
     * Get the parameters for the request
     * @return the current parameters associated to the request, can be null
     */
    @Override
    @Nullable
    public Map<String, String> getParams() {
        return standardParams;
    }

    /**
     * Converts a base URL, endpoint, and parameters into a full URL
     *
     * @param method The <b>com.android.volley.Request.Method</b> of the URL
     * @param url    The URL, not null
     * @param params The parameters to be appended to the URL if a GET method is used, can be null
     * @param encoding The encoding used to parse parameters set in the url (GET method), can be null
     * @return The full URL
     */
    protected String parseGetUrl(int method, @NonNull String url, @Nullable Map<String, String> params, @NonNull String encoding) {
        if (method == Request.Method.GET && params != null && !params.isEmpty()) {
            final StringBuilder result = new StringBuilder(url);
            final int startLength = result.length();
            for (String key : params.keySet()) {
                try {
                    final String encodedKey = URLEncoder.encode(key, encoding);
                    final String encodedValue = URLEncoder.encode(params.get(key), encoding);
                    if (result.length() > startLength) {
                        result.append("&");
                    } else {
                        result.append("?");
                    }
                    result.append(encodedKey);
                    result.append("=");
                    result.append(encodedValue);
                } catch (Exception e) {
                }
            }
            return result.toString();
        } else {
            return url;
        }
    }
}
