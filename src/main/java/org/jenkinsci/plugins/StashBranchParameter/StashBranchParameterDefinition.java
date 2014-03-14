package org.jenkinsci.plugins.StashBranchParameter;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
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
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Created by erwin on 13/03/14.
 */
public class StashBranchParameterDefinition extends ParameterDefinition {

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
        return null;
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest) {
        return null;
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

                // Create AuthCache instance
                AuthCache authCache = new BasicAuthCache();
                // Generate BASIC scheme object and add it to the local
                // auth cache
                BasicScheme basicAuth = new BasicScheme();
                authCache.put(target, basicAuth);

                // Add AuthCache to the execution context
                HttpClientContext localContext = HttpClientContext.create();
                localContext.setAuthCache(authCache);

                HttpGet httpget = new HttpGet(url.getPath());

                System.out.println("Executing request " + httpget.getRequestLine() + " to target " + target);

                CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
                try {
                    return FormValidation.warning("status code: {}",response.getStatusLine().getReasonPhrase());

                } finally {
                    response.close();
                }
            }
            catch(UnknownHostException e){
                return FormValidation.error("Couldn't connect with server");
            } finally {
                httpclient.close();
            }
            //return FormValidation.ok();
        }

        public FormValidation doCheckPassword(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password) throws IOException, ServletException {
            return doCheckUsername(stashApiUrl, username, password);
        }

        public FormValidation doCheckStashApiUrl(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password) throws IOException, ServletException {
            return doCheckUsername(stashApiUrl, username, password);
        }


    }

}
