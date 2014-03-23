package org.jenkinsci.plugins.StashBranchParameter;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.util.JSONUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by erwin on 13/03/14.
 */
public class StashBranchParameterDefinition extends ParameterDefinition {

    private static final Logger LOGGER = Logger.getLogger(StashBranchParameterDefinition.class.getName());

    private String username;

    private String password;

    private String stashApiUrl;


    @DataBoundConstructor
    public StashBranchParameterDefinition(String name, String description, String stashApiUrl, String username, String password) {
        super(name, description);
        this.username = username;
        this.password = password;
        this.stashApiUrl = stashApiUrl;
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
        LOGGER.warning("Value "+ jsonObject.toString());
        String value = jsonObject.getString("value");
        return new StringParameterValue(this.getName(),value);
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest) {
        String[] parameterValues = staplerRequest.getParameterValues(getName());
        return new StringParameterValue(this.getName(),parameterValues[0]);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStashApiUrl() {
        return stashApiUrl;
    }

    public void setStashApiUrl(String stashApiUrl) {
        this.stashApiUrl = stashApiUrl;
    }

    public List<String> getDefaultValueMap() throws IOException {
        return computeDefaultValueMap();
    }

    private List<String> computeDefaultValueMap() throws IOException {
        List<String> list = new ArrayList<String>();
        URL url = new URL(getStashApiUrl());

        HttpHost target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(getUsername(), getPassword()));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();

        try {

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);
            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);
            HttpGet httpget = new HttpGet(url.getPath());

            CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
            try{
                HttpEntity entity = response.getEntity();
                StringWriter writer = new StringWriter();
                IOUtils.copy(entity.getContent(), writer);

                JSONObject json = JSONObject.fromObject(writer.toString());
                if(json.has("values")){
                    JSONArray values = json.getJSONArray("values");
                    Iterator<JSONObject> iterator = values.iterator();
                    while(iterator.hasNext()){
                        JSONObject branch = iterator.next();
                        if(branch.has("displayId")){
                            list.add(branch.getString("displayId"));
                        }
                    }
                }
            }finally {
                response.close();
            }

        } catch (IOException e) {
            throw e;
        }finally {
            httpclient.close();
        }
        return list;
    }

    private CloseableHttpResponse getResponse(Map<String, Boolean> map) throws IOException {
        URL url = new URL(getStashApiUrl());

        HttpHost target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(
                new AuthScope(target.getHostName(), target.getPort()),
                new UsernamePasswordCredentials(getUsername(), getPassword()));
        CloseableHttpClient httpclient = HttpClients.custom()
                .setDefaultCredentialsProvider(credsProvider).build();

        try {
            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(target, basicAuth);
            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);
            HttpGet httpget = new HttpGet(url.getPath());

            CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
                return response;

        }finally {
            httpclient.close();
        }
    }

    @Extension
    public static class DescriptorImpl extends ParameterDescriptor {
        @Override
        public String getDisplayName() {
            return "Stash Branch Parameter";
        }

        public FormValidation doCheckUsername(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password) throws IOException, ServletException {
            if(StringUtils.isBlank(stashApiUrl)) {
                return FormValidation.ok();
            }
            URL url = new URL(stashApiUrl);

            HttpHost target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(target.getHostName(), target.getPort()),
                    new UsernamePasswordCredentials(username, password));
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider).build();

            try {
                AuthCache authCache = new BasicAuthCache();
                BasicScheme basicAuth = new BasicScheme();
                authCache.put(target, basicAuth);
                HttpClientContext localContext = HttpClientContext.create();
                localContext.setAuthCache(authCache);
                HttpGet httpget = new HttpGet(url.getPath());

                CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
                try {
                    if(response.getStatusLine().getStatusCode()!=200){
                       return FormValidation.error("Authorization failed");
                    }
                    return FormValidation.ok();

                } finally {
                    response.close();
                }
            }
            catch(UnknownHostException e){
                return FormValidation.error("Couldn't connect with server");
            }catch(HttpHostConnectException e){
                return FormValidation.error("Couldn't connect with server");
            }finally {
                httpclient.close();
            }
        }

        public FormValidation doCheckPassword(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password) throws IOException, ServletException {
            return doCheckUsername(stashApiUrl, username, password);
        }

        public FormValidation doCheckStashApiUrl(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password) throws IOException, ServletException {
            return doCheckUsername(stashApiUrl, username, password);
        }


    }

}
