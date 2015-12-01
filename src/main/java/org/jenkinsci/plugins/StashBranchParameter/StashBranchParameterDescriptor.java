package org.jenkinsci.plugins.StashBranchParameter;

import hudson.model.ParameterDefinition;
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
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

public class StashBranchParameterDescriptor extends ParameterDefinition.ParameterDescriptor
{
	private String username;

	private Secret password;

	private String stashApiUrl;
	private String repo;

	public StashBranchParameterDescriptor()
	{
		super(StashBranchParameterDefinition.class);
		load();
	}

	@Override
	public String getDisplayName()
	{
		return "Stash Branch Parameter";
	}

	public String getUsername()
	{
		return username;
	}

	public void setUsername(String username)
	{
		this.username = username;
		save();
	}

	public String getPassword()
	{
		return password == null ? null : password.getPlainText();
	}

	public void setPassword(Secret password)
	{
		this.password = password;
	}

	public String getStashApiUrl()
	{
		return stashApiUrl;
	}

	public void setStashApiUrl(String stashApiUrl)
	{
		this.stashApiUrl = stashApiUrl;
	}

	public String getRepo()
	{
		return repo;
	}

	public void setRepo(String repo)
	{
		this.repo = repo;
	}

	@Override
	public boolean configure(StaplerRequest req, JSONObject formData) throws FormException
	{
		stashApiUrl = formData.getString("stashApiUrl");
		username = formData.getString("username");
		password = Secret.fromString(formData.getString("password"));

		save();
		return super.configure(req, formData);
	}

	public FormValidation doCheckUsername(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password) throws IOException, ServletException
	{
		if (StringUtils.isBlank(stashApiUrl))
		{
			return FormValidation.ok();
		}
		URL url = new URL(stashApiUrl);

		HttpHost target = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(target.getHostName(), target.getPort()), new UsernamePasswordCredentials(username, password));
		CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

		try
		{
			AuthCache authCache = new BasicAuthCache();
			BasicScheme basicAuth = new BasicScheme();
			authCache.put(target, basicAuth);
			HttpClientContext localContext = HttpClientContext.create();
			localContext.setAuthCache(authCache);
			HttpGet httpget = new HttpGet(url.getPath().concat("/repos"));

			CloseableHttpResponse response = httpclient.execute(target, httpget, localContext);
			try
			{
				if (response.getStatusLine().getStatusCode() != 200)
				{
					return FormValidation.error("Authorization failed");
				}
				return FormValidation.ok();

			}
			finally
			{
				response.close();
			}
		}
		catch (UnknownHostException e)
		{
			return FormValidation.error("Couldn't connect with server");
		}
		catch (HttpHostConnectException e)
		{
			return FormValidation.error("Couldn't connect with server");
		}
		finally
		{
			httpclient.close();
		}
	}

	public FormValidation doCheckPassword(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password) throws IOException, ServletException
	{
		return doCheckUsername(stashApiUrl, username, password);
	}

	public ListBoxModel doFillRepositoryItems() throws MalformedURLException
	{
		StashConnector connector = new StashConnector(getStashApiUrl(), getUsername(), getPassword());
		ListBoxModel items = new ListBoxModel();
		Map<String, List<String>> repositories = connector.getRepositories();

		for (Map.Entry<String, List<String>> entry : repositories.entrySet())
		{
			String project = entry.getKey();
			for (String repo : entry.getValue())
			{
				String name = project.concat(" / ").concat(repo);
				String value = name.replace(" ", "");
				items.add(new ListBoxModel.Option(name, value));
			}
		}
		return items;
	}
}