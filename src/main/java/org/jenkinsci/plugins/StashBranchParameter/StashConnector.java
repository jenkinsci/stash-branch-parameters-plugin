package org.jenkinsci.plugins.StashBranchParameter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by erwin on 24/03/14.
 */
public class StashConnector {

    private static final Logger LOGGER = Logger.getLogger(StashConnector.class.getName());


    private String stashApiUrl;
    private String username;
    private String password;
    private URL url;
    private CloseableHttpClient httpclient =null;
    private HttpHost target = null;
    private HttpClientContext localContext;

    public StashConnector(String stashApiUrl, String username, String password) throws MalformedURLException {
        this.stashApiUrl = stashApiUrl;
        this.username = username;
        this.password = password;
        url = new URL(stashApiUrl);
        target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());



    }

    public Map<String,String> getBranches(){
        String path = url.getPath();
        if(!path.endsWith("branches")){
            path = path.concat("/branches");
        }
        path = path.concat("?orderBy=ALPHABETICAL&limit=1000");

        JSONObject json = getJson(path);
        Map<String,String> map = new TreeMap<String, String>();
        if(json.has("values")){
            JSONArray values = json.getJSONArray("values");
            Iterator<JSONObject> iterator = values.iterator();
            while(iterator.hasNext()){
                JSONObject branch = iterator.next();
                if(branch.has("displayId")){
                    map.put(branch.getString("displayId"), branch.getString("displayId"));
                }
            }
        }
        return map;
    }

    public Map<String,String> getTags(){
        String path = url.getPath();
        if(path.endsWith("/branches")){
            path = path.substring(0,path.indexOf("/branches"));
        }
        if(!path.endsWith("/tags")){
            path = path.concat("/tags");
        }
        path = path.concat("?orderBy=ALPHABETICAL&limit=1000");

        JSONObject json = getJson(path);
        Map<String,String> map = new TreeMap<String, String>();
        if(json.has("values")){
            JSONArray values = json.getJSONArray("values");
            Iterator<JSONObject> iterator = values.iterator();
            while(iterator.hasNext()){
                JSONObject branch = iterator.next();
                if(branch.has("displayId")){
                    String value = "tags/".concat(branch.getString("displayId"));
                    map.put(value,value);
                }
            }
        }
        return map;
    }

    private synchronized JSONObject getJson(String path){
        try {
            initConnections();
            HttpGet httpget = new HttpGet(path);

            CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
            try{
                HttpEntity entity = response.getEntity();
                StringWriter writer = new StringWriter();
                IOUtils.copy(entity.getContent(), writer);

                return JSONObject.fromObject(writer.toString());
            }finally {
                response.close();
            }

        } catch (IOException e) {
            throw new RuntimeException();
        }finally {
            if(httpclient!=null)
                try {
                    httpclient.close();
                } catch (IOException e) {
                    throw new RuntimeException();
                }
        }
    }

    private void initConnections() {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(username, password));
        httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(target, basicAuth);
        localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
    }

    public List<String> getProjects() {

        String path = url.getPath().concat("/projects");
        path = path.concat("?orderBy=ALPHABETICAL&limit=1000");
        JSONObject json = getJson(path);

        List<String> list = new LinkedList<String>();
        if(json.has("values")){
            JSONArray values = json.getJSONArray("values");
            Iterator<JSONObject> iterator = values.iterator();
            while(iterator.hasNext()){
                JSONObject project = iterator.next();
                if(project.has("key")){
                    list.add(project.getString("key"));
                }
            }
        }
        return list;
    }
}
