package ru.dimsuz.collagecreator.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import timber.log.Timber;

public class NetworkUtils {
    /**
     * Encodes url by replacing all space characters (etc) with their correctly encoded analogs
     * @param urlString url to encode
     * @return a string suitable to passing to any http client as an url
     */
    public static String encodeUrl(String urlString) {
        // a 'clever' way to properly encode url, found on SO
        String result = null;
        try {
            URL url = new URL(urlString);
            URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(), url.getQuery(), url.getRef());
            // must use this method to perform conversion from non-ascii (cyrillic) characters to ascii or URL's would be invalid
            result = uri.toASCIIString();
        } catch (MalformedURLException | URISyntaxException e) {
            Timber.d("failed to encode url string: " + urlString);
        }
        return result;
    }
}
