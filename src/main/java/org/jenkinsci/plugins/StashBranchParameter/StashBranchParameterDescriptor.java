package org.jenkinsci.plugins.StashBranchParameter;

import com.atlassian.stash.rest.client.api.StashRestException;
import com.atlassian.stash.rest.client.api.StashUnauthorizedRestException;
import com.atlassian.stash.rest.client.core.StashClientImpl;
import com.atlassian.stash.rest.client.httpclient.HttpClientConfig;
import com.atlassian.stash.rest.client.httpclient.HttpClientHttpExecutor;
import hudson.model.ParameterDefinition;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class StashBranchParameterDescriptor extends ParameterDefinition.ParameterDescriptor
{
	private static final Logger LOGGER = LoggerFactory.getLogger(StashBranchParameterDescriptor.class);

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

	public FormValidation doCheckPassword(@QueryParameter final String stashApiUrl, @QueryParameter final String username, @QueryParameter final String password)
	{
		return validateForm(stashApiUrl, username, password);
	}

	private FormValidation validateForm(@QueryParameter String stashApiUrl, @QueryParameter String username, @QueryParameter String password)
	{
		if (StringUtils.isBlank(stashApiUrl) || StringUtils.isBlank(username) || StringUtils.isBlank(password))
		{
			return FormValidation.ok();
		}
		try
		{
			URL url = new URL(stashApiUrl);
			HttpClientConfig httpClientConfig = new HttpClientConfig(url, username, password);
			HttpClientHttpExecutor httpClientHttpExecutor = new HttpClientHttpExecutor(httpClientConfig);
			StashClientImpl stashClient = new StashClientImpl(httpClientHttpExecutor);
			LOGGER.info("Successfully connected with Stash {}", stashClient.getAccessibleProjects(1,1));
			return FormValidation.ok("Successfully connected with Stash");
		}
		catch (MalformedURLException e)
		{
			return FormValidation.error(e, "Not a valid URL!");
		}
		catch (StashUnauthorizedRestException sure)
		{
			return FormValidation.error("Authorization failed");
		}
		catch (StashRestException sre)
		{
			return FormValidation.error(sre, "Something went wrong while connecting with Stash");
		}
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