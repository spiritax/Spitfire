package com.neopixl.spitfire.request;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.neopixl.spitfire.SpitfireManager;
import com.neopixl.spitfire.listener.RequestListener;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Florian ALONSO on 12/30/16.
 * For Neopixl
 * @param <T> The type used as the response for the request
 */

abstract class AbstractRequest<T> extends Request<T> {

    private List<Integer> mAcceptedStatusCodes;
    private Class<T> classResponse;

    @Nullable
    private final RequestListener<T> mListener;

    @Nullable
    private Map<String, String> headers;

    private NetworkResponse networkResponse;

    /**
     * Abstract class Builder used to create a new request
     * @param <T> Type used for the response of the request.
     */
    public static abstract class AbstractBuilder<T, RequestType extends AbstractRequest<T>> {

        protected int method = -10;

        @NonNull
        private String url;
        private Class<T> classResponse;

        @Nullable
        private RequestListener<T> mListener;
        private Map<String, String> headers;

        /**
         * Default
         * @param method used to send the request
         * @param url given url to access the resource, not nul
         * @param classResponse class used to parse the response
         */
        public AbstractBuilder(int method, @NonNull String url, Class<T> classResponse) {
            this.method = method;
            this.url = url;
            this.classResponse = classResponse;
        }

        /**
         * Sets the listener for the request
         * @param listener {@link RequestListener}, can be null
         * @return the builder
         */
        public AbstractBuilder<T, RequestType> listener(@Nullable RequestListener<T> listener) {
            this.mListener = listener;
            return this;
        }

        /**
         * Sets the headers for the request
         * @param headers used to send the request, can be null
         * @return Map&lt;String, String&gt;
         */
        public AbstractBuilder<T, RequestType> headers(@Nullable Map<String, String> headers) {
            this.headers = new HashMap<>(headers);
            return this;
        }

        /**
         * You must implement this method in your subclass.
         * @return AbstractNeoRequest
         */
        abstract public RequestType build() ;
    }


    /**
     * Constructor to create the request
     * @param builder a builder from the {@link AbstractBuilder}
     */
    AbstractRequest(AbstractBuilder builder) {
        super(builder.method, builder.url, null);

        Map<String, String> builderHeaders = builder.headers;

        this.headers = builderHeaders!=null ? builderHeaders : new HashMap<String, String>();
        this.classResponse = builder.classResponse;

        setShouldCache(builder.method == Method.GET);

        this.mListener = builder.mListener;

        mAcceptedStatusCodes = new ArrayList<>();
        mAcceptedStatusCodes.add(HttpURLConnection.HTTP_OK);
        mAcceptedStatusCodes.add(HttpURLConnection.HTTP_NO_CONTENT);
        mAcceptedStatusCodes.add(HttpURLConnection.HTTP_ACCEPTED);
        mAcceptedStatusCodes.add(HttpURLConnection.HTTP_CREATED);

        setRetryPolicy(SpitfireManager.getDefaultRetryPolicy());
    }

    /**
     * Add accepted status codes (<a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">Http status codes</a>), not null
     * @param statusCodes int[]
     */
    public void addAcceptedStatusCodes(@NonNull int[] statusCodes) {
        for (int statusCode : statusCodes) {
            mAcceptedStatusCodes.add(statusCode);
        }
    }

    /**
     * Add accepted status code (<a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">Http status codes</a>), not null
     * @param statusCodes int
     */
    public void addAcceptedStatusCodes(@NonNull int statusCodes) {
        mAcceptedStatusCodes.add(statusCodes);
    }

    /**
     * Get the list of all accepted status codes
     * @return list of accepted status codes
     */
    @NonNull
    public List<Integer> getAcceptedStatusCodes() {
        return mAcceptedStatusCodes;
    }

    /**
     * Delivers the response using the listener (if one is set) or post an event using EventBus.
     * Note: This method is called internally, you should never call it directly. But you override it.
     * @param response The response
     */
    @Override
    protected void deliverResponse(T response) {
        if (mListener != null) {
            mListener.onSuccess(this, networkResponse, response);
        }
    }

    /**
     * Delivers the error using the listener (if one is set) or post an event using EventBus.
     * Note: This method is called internally, you should never call it directly. But you override it.
     * @param error <b>VolleyError</b>
     */
    @Override
    public void deliverError(VolleyError error) {
        if (error != null && error.networkResponse != null && this.networkResponse == null) {
            this.networkResponse = error.networkResponse;
        }


        if (mListener != null) {
            mListener.onFailure(this, networkResponse, error);
        }
    }

    /**
     * Parses the network response {@link NetworkResponse} and returns the expected Type for the request.
     * @param response {@link NetworkResponse} The response for the request (Success or error).
     * @return Response object linked to a specific type
     */
    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        this.networkResponse = response;

        JavaType returnType = getReturnType();
        T returnData = null;
        if (returnType != null) {
            try {
                if (response.data != null) {
                    returnData = SpitfireManager.getObjectMapper().readValue(response.data, returnType);
                }
            } catch (Exception e) {
                VolleyLog.e(e, "An error occurred while parsing network response:");
                returnData = null;
            }
        }

        if (returnData == null && classResponse != Void.class) {
            ParseError parseError = new ParseError(response);
            String content = "";
            if (response.data != null) {
                content = new String(response.data);
            }
            VolleyLog.e(parseError, "Return data is null. API returned : "+ content);
            return Response.error(parseError);
        }

        return Response.success(returnData, HttpHeaderParser.parseCacheHeaders(response));
    }

    /**
     * Returns the type for the response.
     * @return null, Array, List, Map or Object.
     */
    @Nullable
    private JavaType getReturnType() {
        if (classResponse == Void.class) {
            return null;
        } else if (classResponse.isArray()) {
            return TypeFactory.defaultInstance().constructArrayType(classResponse.getComponentType());
        } else if (classResponse == List.class) {
            return TypeFactory.defaultInstance().constructCollectionType(List.class, classResponse.getComponentType());
        } else if (classResponse == Map.class) {
            return TypeFactory.defaultInstance().constructMapType(Map.class, String.class, classResponse.getComponentType());
        }
        return TypeFactory.defaultInstance().constructType(classResponse);
    }

    /**
     * Returns a list of extra HTTP headers to go along with this request. Can
     * throw <b>AuthFailureError</b> as authentication may be required to
     * provide these values.
     * @throws AuthFailureError In the event of auth failure
     * @return Map&lt;String, String&gt;
     */
    @Override
    @NonNull
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> currentHeader = new HashMap<>(super.getHeaders());
        currentHeader.putAll(this.headers);

        String bodyContentType = getBodyContentType();
        if (bodyContentType == null && currentHeader.containsKey("Content-Type")) {
            currentHeader.remove("Content-Type");
        }
        return currentHeader;
    }

    /**
     * Returns the raw POST or PUT body to be sent.
     *
     * <p>By default, the body consists of the request parameters in
     * application/x-www-form-urlencoded format. When overriding this method, consider overriding
     * {@link #getBodyContentType()} as well to match the new body format.
     *
     * @throws AuthFailureError in the event of auth failure
     * @return byte[] or null if the
     */
    @Nullable
    public byte[] getBody() throws AuthFailureError {
        if (getMethod() == Method.GET) {
            return null;
        }
        return super.getBody();
    }
}