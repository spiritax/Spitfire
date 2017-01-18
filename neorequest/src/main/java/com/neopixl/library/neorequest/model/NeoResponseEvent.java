package com.neopixl.library.neorequest.model;

import com.android.volley.VolleyError;

/**
 * Created by Florian ALONSO on 12/30/16.
 * For Neopixl
 */

public class NeoResponseEvent<T> {
    private int statusCode;
    private VolleyError error;
    private T data;

    /**
     * Constructor for a NeoResponseEvent (sent using EventBus)
     * @param data
     * @param error
     * @param statusCode
     */
    public NeoResponseEvent(T data, VolleyError error, int statusCode) {
        this.statusCode = statusCode;
        this.error = error;
        this.data = data;
    }

    /**
     * Get the status code
     * @return <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html">status code</a>
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Return the volleyError
     * @return {@link VolleyError}
     */
    public VolleyError getError() {
        return error;
    }

    /**
     * Returns the data
     * @return
     */
    public T getData() {
        return data;
    }

    /**
     * Check if the request is successful (It means no error found for the current event).
     * @return boolean value
     */
    public boolean isSuccess() {
        return error == null;
    }
}
