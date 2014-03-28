package org.jenkinsci.plugins.StashBranchParameter;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by erwin on 13/03/14.
 */
public class StashBranchParameterDefinition extends ParameterDefinition {

    private static final Logger LOGGER = Logger.getLogger(StashBranchParameterDefinition.class.getName());

    private String repository;

    @DataBoundConstructor
    public StashBranchParameterDefinition(String name, String description, String repository) {
        super(name, description);
        this.repository = repository;
    }

    public String getRepository() {
        LOGGER.info(repository);
        LOGGER.info(getDescriptor().getRepo());
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
        getDescriptor().setRepo(repository);
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
        String project = repository.split("/")[0];
        String repo = repository.split("/")[1];
        StashConnector connector = new StashConnector(getDescriptor().getStashApiUrl(),getDescriptor().getUsername(),getDescriptor().getPassword());

        Map<String, String> map = connector.getBranches(project, repo);
        map.putAll(connector.getTags(project, repo));

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
        private String repo;

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
            save();
        }

        public String getPassword() {
            return password.getPlainText();
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

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
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
                HttpGet httpget = new HttpGet(url.getPath().concat("/repos"));

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

        public ListBoxModel doFillRepositoryItems() throws MalformedURLException {
            StashConnector connector = new StashConnector(getStashApiUrl(),getUsername(),getPassword());
            ListBoxModel items = new ListBoxModel();
            Map<String, List<String>> repositories = connector.getRepositories();

            for(Map.Entry<String,List<String>> entry: repositories.entrySet()){
                String project = entry.getKey();
                for(String repo: entry.getValue()){
                    String name = project.concat(" / ").concat(repo);
                    String value = name.replace(" ","");
                    items.add(new ListBoxModel.Option(name, value));
                }
            }
            return items;
        }
    }


}
