/*

Copyright (C) 2017-2019  Barry de Graaff

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see http://www.gnu.org/licenses/.

*/
package tk.barrydegraaff.ocs;

import java.text.SimpleDateFormat;
import java.util.*;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.ZimbraSoapContext;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OCS extends DocumentHandler {
    private String token = "";
    private JSONArray libraries;

    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        try {
            ZimbraSoapContext zsc = getZimbraSoapContext(context);
            Element response = zsc.createElement(
                    "response"
            );

            getToken(request, zsc);
            getLibraries(request, zsc, this.token);

            switch (request.getAttribute("action")) {
                case "createShare":
                    return createShare(request, response, zsc, this.token);
                default:
                    return (response);
            }
        } catch (
                Exception e) {
            throw ServiceException.FAILURE("exception occurred handling command", e);
        }
    }

    /**
     * Gets a Seafile API token
     */
    private String getToken(Element request, ZimbraSoapContext zsc) {
        try {
            if (checkPermissionOnTarget(request.getAttribute("owncloud_zimlet_server_name"))) {
                final String urlParameters = "username=" + request.getAttribute("owncloud_zimlet_username") + "&password=" + request.getAttribute("owncloud_zimlet_password");

                byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                int postDataLength = postData.length;

                String requestUrl = request.getAttribute("owncloud_zimlet_server_name") + ":" + request.getAttribute("owncloud_zimlet_server_port") + request.getAttribute("owncloud_zimlet_oc_folder") + "api2/auth-token/";
                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("Charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setRequestProperty("X-Forwarded-For", zsc.getRequestIP());
                conn.setUseCaches(false);

                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(postData);
                }

                InputStream _is;
                if (conn.getResponseCode() < 400) {
                    _is = conn.getInputStream();
                } else {
                    _is = conn.getErrorStream();
                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(_is));

                String inputLine;
                StringBuffer responseTxt = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseTxt.append(inputLine);
                }
                in.close();
                JSONObject obj = new JSONObject(responseTxt.toString());
                String token = obj.getString("token");
                this.token = token;
            }
        } catch (Exception err) {
            err.printStackTrace();
            this.token = "";
        }
        return this.token;
    }

    /**
     * Seafile defines the top level of storage as so called Libraries, we need to know which ones there are so we can us their ID's in API calls.
     */
    private void getLibraries(Element request, ZimbraSoapContext zsc, String token) {
        JSONArray obj = new JSONArray();
        try {
            if (checkPermissionOnTarget(request.getAttribute("owncloud_zimlet_server_name"))) {

                String requestUrl = request.getAttribute("owncloud_zimlet_server_name") + ":" + request.getAttribute("owncloud_zimlet_server_port") + request.getAttribute("owncloud_zimlet_oc_folder") + "api2/repos/";
                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Charset", "utf-8");
                conn.setRequestProperty("X-Forwarded-For", zsc.getRequestIP());
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json; indent=0");
                conn.setUseCaches(false);

                InputStream _is;
                if (conn.getResponseCode() < 400) {
                    _is = conn.getInputStream();
                } else {
                    _is = conn.getErrorStream();
                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(_is));

                String inputLine;
                StringBuffer responseTxt = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseTxt.append(inputLine);
                }
                in.close();
                obj = new JSONArray(responseTxt.toString());
                this.libraries = obj;
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return;
    }

    private Element createShare(Element request, Element response, ZimbraSoapContext zsc, String token) {
        JSONObject obj = new JSONObject();
        String shareLink = "";
        try {
            if (checkPermissionOnTarget(request.getAttribute("owncloud_zimlet_server_name"))) {
                String repo_id = getRepoId(request.getAttribute("path"));

                removeShareLinkIfExists(request, zsc, repo_id, getPath(request.getAttribute("path")));

                String urlParameters = "{\"repo_id\":\"" + repo_id + "\", \"path\":\"" + getPath(request.getAttribute("path")) + "\", \"permissions\":{\"can_edit\":false,\"can_download\":true}";

                if (!"".equals(request.getAttribute("password"))) {
                    urlParameters += ", \"password\":\"" + request.getAttribute("password") + "\"";
                }

                if (!"".equals(request.getAttribute("expiryDate"))) {
                    Date expirationDate = new SimpleDateFormat("yyyy-MM-dd").parse(request.getAttribute("expiryDate"));
                    long diff = expirationDate.getTime() - new Date().getTime();
                    long days = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                    if (days > 0) {
                        urlParameters += ", \"expire_days\":\"" + days + "\"";
                    }
                }

                urlParameters += "}";

                byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
                int postDataLength = postData.length;

                String requestUrl = request.getAttribute("owncloud_zimlet_server_name") + ":" + request.getAttribute("owncloud_zimlet_server_port") + request.getAttribute("owncloud_zimlet_oc_folder") + "api/v2.1/share-links/";
                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; utf-8");
                conn.setRequestProperty("Charset", "utf-8");
                conn.setRequestProperty("Content-Length", Integer.toString(postDataLength));
                conn.setRequestProperty("X-Forwarded-For", zsc.getRequestIP());
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json; indent=0");
                conn.setUseCaches(false);

                try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                    wr.write(postData);
                }

                InputStream _is;
                if (conn.getResponseCode() < 400) {
                    _is = conn.getInputStream();
                } else {
                    _is = conn.getErrorStream();
                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(_is));

                String inputLine;
                StringBuffer responseTxt = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseTxt.append(inputLine);
                }
                in.close();

                obj = new JSONObject(responseTxt.toString());
                shareLink = obj.getString("link");

                //result holds the url of the created share, if all went well, or also an error message if the sharing failed
                response.addAttribute("createShare", "{\"statuscode\":100,\"id\":\"" + shareLink + "\",\"message\":\"\",\"url\":\"" + shareLink + "\",\"status\":\"ok\",\"token\":\"\"}");
            }

            return response;
        } catch (
                Exception ex) {
            response.addAttribute("createShare", "{\"statuscode\":100,\"id\":0,\"message\":\"\",\"url\":\"" + "Could not create share. " + "\",\"status\":\"ok\",\"token\":\"\"}");
            return response;
        }
    }

    /*Seafile is stupid, it does not return the Library ID when doing a propfind in DAV, so we need to determine the correct
     * library based on the path of the DAV object. Also, Libraries can have the same name, but the seafdav implementation of
     * Seafile does not support it, and appends a part of the Library ID to the library name. This is all a bit shitty to work with.
     *
     * https://github.com/haiwen/seafdav/issues/29
     * */
    private String getRepoId(String path) {
        //remove any double slashes
        path = path.replaceAll("//", "/");
        String[] pathArray = path.split("/");
        path = pathArray[1];
        try {
            for (int i = 0; i < this.libraries.length(); i++) {
                JSONObject library = this.libraries.getJSONObject(i);
                String libName = library.getString("name");
                String libId = library.getString("id");
                String libShortId = libId.substring(0, 6);
                //Look for the path in the list of libraries
                if ((path.equals(libName)) || (path.equals(libName + "-" + libShortId))) {
                    return libId;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    /*Silly helper method that strips the `library` from the path.
     *
     * */
    private String getPath(String path) {
        //remove any double slashes
        path = path.replaceAll("//", "/");
        String[] pathArray = path.split("/");
        path = path.substring(pathArray[1].length() + 1);
        return path;
    }

    private void removeShareLinkIfExists(Element request, ZimbraSoapContext zsc, String repoId, String path) {
        JSONArray obj = new JSONArray();
        String linkId;
        try {
            if (checkPermissionOnTarget(request.getAttribute("owncloud_zimlet_server_name"))) {

                String requestUrl = request.getAttribute("owncloud_zimlet_server_name") + ":" + request.getAttribute("owncloud_zimlet_server_port") + request.getAttribute("owncloud_zimlet_oc_folder") + "api/v2.1/share-links/?repo_id=" + repoId + "&path=" + path;
                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Charset", "utf-8");
                conn.setRequestProperty("X-Forwarded-For", zsc.getRequestIP());
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json; indent=0");
                conn.setUseCaches(false);

                InputStream _is;
                if (conn.getResponseCode() < 400) {
                    _is = conn.getInputStream();
                } else {
                    _is = conn.getErrorStream();
                }

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(_is));

                String inputLine;
                StringBuffer responseTxt = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseTxt.append(inputLine);
                }
                in.close();
                try {
                    obj = new JSONArray(responseTxt.toString());
                    linkId = obj.getJSONObject(0).getString("token");
                } catch (Exception e) {
                    //link does not exist
                    return;
                }


                //Link already exists, delete it
                requestUrl = request.getAttribute("owncloud_zimlet_server_name") + ":" + request.getAttribute("owncloud_zimlet_server_port") + request.getAttribute("owncloud_zimlet_oc_folder") + "api/v2.1/share-links/" + linkId;
                url = new URL(requestUrl);
                conn = (HttpURLConnection) url.openConnection();

                conn.setDoOutput(true);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("DELETE");
                conn.setRequestProperty("Charset", "utf-8");
                conn.setRequestProperty("X-Forwarded-For", zsc.getRequestIP());
                conn.setRequestProperty("Authorization", "Token " + token);
                conn.setRequestProperty("Accept", "application/json; indent=0");
                conn.setUseCaches(false);

                if (conn.getResponseCode() < 400) {
                    _is = conn.getInputStream();
                } else {
                    _is = conn.getErrorStream();
                }

                in = new BufferedReader(
                        new InputStreamReader(_is));

                responseTxt = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    responseTxt.append(inputLine);
                }
                in.close();
            }
        } catch (Exception err) {
            err.printStackTrace();
        }
        return;
    }

    private String uriDecode(String dirty) {
        try {
            String clean = java.net.URLDecoder.decode(dirty, "UTF-8");
            return clean;
        } catch (Exception ex) {
            return ex.toString();
        }
    }

    private boolean isNumeric(String str) {
        try {
            double d = Double.parseDouble(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public static boolean checkPermissionOnTarget(String host) {

        Properties prop = new Properties();
        try {
            FileInputStream input = new FileInputStream("/opt/zimbra/lib/ext/ownCloud/config.properties");
            prop.load(input);

            String[] temp = prop.getProperty("allowdomains").split(";");
            Set<String> domains = new HashSet<String>(Arrays.asList(temp));

            input.close();

            for (String domain : domains) {
                if (domain.equals("*")) {
                    return true;
                }
                if (domain.charAt(0) == '*') {
                    domain = domain.substring(1);
                }
                if (host.endsWith(domain)) {
                    return true;
                }
            }
            return false;
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }
}
