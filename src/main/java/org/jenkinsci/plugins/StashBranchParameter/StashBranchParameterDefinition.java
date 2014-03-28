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
    private String defaultValue;

    @DataBoundConstructor
    public StashBranchParameterDefinition(String name, String description, String repository, String defaultValue) {
        super(name, description);
        this.repository = repository;
        this.defaultValue = defaultValue;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
        String value = jsonObject.getString("value");
        return new StringParameterValue(this.getName(),value);
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest) {
        String[] parameterValues = staplerRequest.getParameterValues(getName());
        String value = parameterValues[0];
        return new StringParameterValue(this.getName(),value);
    }

    @Override
    public ParameterValue getDefaultParameterValue() {
        return new StringParameterValue(this.getName(),defaultValue);
    }

    public Map<String, Map<String, String>> getDefaultValueMap() throws IOException {
        return computeDefaultValueMap();
    }

    private Map<String, Map<String, String>> computeDefaultValueMap() throws IOException {
        String project = repository.split("/")[0];
        String repo = repository.split("/")[1];
        StashConnector connector = new StashConnector(getDescriptor().getStashApiUrl(),getDescriptor().getUsername(),getDescriptor().getPassword());

        Map<String, String> map = connector.getBranches(project, repo);
        if(StringUtils.isNotBlank(defaultValue)){
            map.put(defaultValue,defaultValue);
        }
        map.putAll(connector.getTags(project, repo));

        Map<String, Map<String, String>> stringMapMap = MapsUtils.groupMap(map);
        return stringMapMap;
    }

    @Override
    public StashBranchParameterDescriptorImpl getDescriptor() {
        return (StashBranchParameterDescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static class StashBranchParameterDescriptorImpl extends StashBranchParameterDescriptor{

    }


}
