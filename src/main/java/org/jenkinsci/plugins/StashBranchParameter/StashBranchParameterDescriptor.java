package org.jenkinsci.plugins.StashBranchParameter;

import hudson.model.ParameterDefinition;
import hudson.util.FormValidation;
import hudson.util.Secret;
import hudson.util.ListBoxModel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

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
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class StashBranchParameterDescriptor extends
ParameterDefinition.ParameterDescriptor {
	private String username;

	private Secret password;

	private String stashApiUrl;
	private String repo;

	public StashBranchParameterDescriptor() {
		super(StashBranchParameterDefinition.class);
		load();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * hudson.model.ParameterDefinition.ParameterDescriptor#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return "Stash Branch Parameter";
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
		save();
	}

	public String getPassword() {
		return password == null ? null : password.getPlainText();
	}

	public void setPassword(final Secret password) {
		this.password = password;
	}

	public String getStashApiUrl() {
		return stashApiUrl;
	}

	public void setStashApiUrl(final String stashApiUrl) {
		this.stashApiUrl = stashApiUrl;
	}

	public String getRepo() {
		return repo;
	}

	public void setRepo(final String repo) {
		this.repo = repo;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * hudson.model.Descriptor#configure(org.kohsuke.stapler.StaplerRequest,
	 * net.sf.json.JSONObject)
	 */
	@Override
	public boolean configure(final StaplerRequest req, final JSONObject formData)
			throws FormException {
		stashApiUrl = formData.getString("stashApiUrl");
		username = formData.getString("username");
		password = Secret.fromString(formData.getString("password"));
		save();
		return super.configure(req, formData);
	}

	public FormValidation doCheckUsername(
			@QueryParameter final String stashApiUrl,
			@QueryParameter final String username,
			@QueryParameter final String password) throws IOException,
			ServletException {
		if (StringUtils.isBlank(stashApiUrl)) {
			return FormValidation.ok();
		}

		final URL url = new URL(stashApiUrl);

		final HttpHost target = new HttpHost(url.getHost(), url.getPort(),
				url.getProtocol());
		final CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(target.getHostName(), target.getPort()),
				new UsernamePasswordCredentials(username, password));
		final CloseableHttpClient httpclient = HttpClients.custom()
				.setDefaultCredentialsProvider(credsProvider).build();

		try {
			final AuthCache authCache = new BasicAuthCache();
			final BasicScheme basicAuth = new BasicScheme();
			authCache.put(target, basicAuth);
			final HttpClientContext localContext = HttpClientContext.create();
			localContext.setAuthCache(authCache);
			final HttpGet httpget = new HttpGet(url.getPath().concat("/repos"));

			final CloseableHttpResponse response = httpclient.execute(target,
					httpget, localContext);
			try {
				if (response.getStatusLine().getStatusCode() != 200) {
					return FormValidation.error("Authorization failed");
				}
				return FormValidation.ok();

			} finally {
				response.close();
			}
		} catch (final UnknownHostException e) {
			return FormValidation.error("Couldn't connect with server");
		} catch (final HttpHostConnectException e) {
			return FormValidation.error("Couldn't connect with server");
		} finally {
			httpclient.close();
		}
	}

	public FormValidation doCheckPassword(
			@QueryParameter final String stashApiUrl,
			@QueryParameter final String username,
			@QueryParameter final String password) throws IOException,
			ServletException {
		return doCheckUsername(stashApiUrl, username, password);
	}

	public ListBoxModel doFillRepositoryItems() throws MalformedURLException {
		final StashConnector connector = new StashConnector(getStashApiUrl(),
				getUsername(), getPassword());
		final ListBoxModel items = new ListBoxModel();
		final Map<String, List<String>> repositories = connector
				.getRepositories();

		for (final Map.Entry<String, List<String>> entry : repositories
				.entrySet()) {
			final String project = entry.getKey();
			for (final String repo : entry.getValue()) {
				final String name = project.concat(" / ").concat(repo);
				final String value = name.replace(" ", "");
				items.add(new ListBoxModel.Option(name, value));
			}
		}
		return items;
	}
}