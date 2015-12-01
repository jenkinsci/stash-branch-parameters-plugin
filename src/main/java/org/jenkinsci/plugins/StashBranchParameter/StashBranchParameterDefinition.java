package org.jenkinsci.plugins.StashBranchParameter;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Map;

public class StashBranchParameterDefinition extends ParameterDefinition
{
	private String repository;
	private String defaultValue;

	@DataBoundConstructor
	public StashBranchParameterDefinition(String name, String description, String repository, String defaultValue)
	{
		super(name, description);
		this.repository = repository;
		this.defaultValue = defaultValue;
	}

	public String getRepository()
	{
		return repository;
	}

	public void setRepository(String repository)
	{
		this.repository = repository;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue)
	{
		this.defaultValue = defaultValue;
	}

	@Override
	public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject)
	{
		String value = jsonObject.getString("value");
		return new StringParameterValue(this.getName(), value);
	}

	@Override
	public ParameterValue createValue(StaplerRequest staplerRequest)
	{
		String[] parameterValues = staplerRequest.getParameterValues(getName());
		String value = parameterValues[0];
		return new StringParameterValue(this.getName(), value);
	}

	@Override
	public ParameterValue getDefaultParameterValue()
	{
		return new StringParameterValue(this.getName(), defaultValue);
	}

	public Map<String, Map<String, String>> getDefaultValueMap() throws IOException
	{
		return computeDefaultValueMap();
	}

	private Map<String, Map<String, String>> computeDefaultValueMap() throws IOException
	{
		String project = repository.split("/")[0];
		String repo = repository.split("/")[1];
		StashConnector connector = new StashConnector(getDescriptor().getStashApiUrl(), getDescriptor().getUsername(), getDescriptor().getPassword());

		Map<String, String> map = connector.getBranches(project, repo);
		if (StringUtils.isNotBlank(defaultValue))
		{
			map.put(defaultValue, defaultValue);
		}
		map.putAll(connector.getTags(project, repo));

		return MapsUtils.groupMap(map);
	}

	@Override
	public StashBranchParameterDescriptorImpl getDescriptor()
	{
		return (StashBranchParameterDescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class StashBranchParameterDescriptorImpl extends StashBranchParameterDescriptor
	{

	}

}
