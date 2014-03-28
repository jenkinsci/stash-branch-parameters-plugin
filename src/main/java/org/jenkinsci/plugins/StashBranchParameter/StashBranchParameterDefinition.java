package org.jenkinsci.plugins.StashBranchParameter;

import groovy.transform.Field;
import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
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
import org.kohsuke.stapler.export.Exported;

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

    private String project;
    private String repo;
    protected List<String> projects;

    @DataBoundConstructor
    public StashBranchParameterDefinition(String name, String description, String project, String repo) {
        super(name, description);
        this.project = project;
        this.repo = repo;
    }

    public String getProject() {
        return "Poepject";
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public List<String> getProjects() {
        return projects;
    }

    public void setProjects(List<String> projects) {
        this.projects = projects;
    }

    public  List<String> getAllProjects() throws MalformedURLException {
        System.out.println("jaja");
        LOGGER.warning("neenee");
        StashConnector connector = new StashConnector(getDescriptor().getStashApiUrl(),getDescriptor().getUsername(),getDescriptor().getPassword().getPlainText());

        return connector.getProjects();
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

    public Map<String, Map<String, String>> getDefaultValueMap() throws IOException {
        return computeDefaultValueMap();
    }

    private Map<String, Map<String, String>> computeDefaultValueMap() throws IOException {

        StashConnector connector = new StashConnector(getDescriptor().getStashApiUrl(),getDescriptor().getUsername(),getDescriptor().getPassword().getPlainText());
        Map<String,String> map = connector.getBranches();
        map.putAll(connector.getTags());
        Map<String, Map<String, String>> stringMapMap = MapsUtils.groupMap(map);
        return stringMapMap;
    }

    @Override
    public StashBranchParameterDescriptor getDescriptor() {
        return (StashBranchParameterDescriptor) super.getDescriptor();
    }

    @Extension
    public static class StashBranchParameterDescriptor extends ParameterDescriptor {
        private String username;

        private Secret password;

        private String stashApiUrl;
        private List<String> projects;

        public StashBranchParameterDescriptor() {
            super(StashBranchParameterDefinition.class);
            load();
        }

        @Override
        public String getDisplayName() {
            return "Stash Branch Parameter";
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public Secret getPassword() {
            return password;
        }

        public void setPassword(Secret password) {
            this.password = password;
        }

        public String getStashApiUrl() {
            return stashApiUrl;
        }

        public void setStashApiUrl(String stashApiUrl) {
            this.stashApiUrl = stashApiUrl;
        }

        public List<String> getProjects() {
            return projects;
        }

        public void setProjects(List<String> projects) {
            this.projects = projects;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            stashApiUrl =      formData.getString("stashApiUrl");
            username = formData.getString("username");
            password = Secret.fromString(formData.getString("password"));

            save();
            return super.configure(req,formData);
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

        public ListBoxModel doFillProjectItems() throws MalformedURLException {
            StashConnector connector = new StashConnector(getStashApiUrl(),getUsername(),getPassword().getPlainText());
            ListBoxModel items = new ListBoxModel();
            for(String project: connector.getProjects()){
                items.add(project,project);
            }
            return items;
        }
    }


}
