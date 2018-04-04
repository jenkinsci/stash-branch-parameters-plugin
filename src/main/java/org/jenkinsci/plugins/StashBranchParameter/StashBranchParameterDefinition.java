package org.jenkinsci.plugins.StashBranchParameter;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.Map;

public class StashBranchParameterDefinition extends ParameterDefinition
{
	private final String repository;
	private final String defaultValue;
	private final String branchFilterText;
	private final String tagFilterText;
	private final String branchNameRegex;
	private final String tagNameRegex;

	@DataBoundConstructor
	public StashBranchParameterDefinition(String name, String description, String repository, String defaultValue,
                                          String branchFilterText, String tagFilterText, String branchNameRegex,
                                          String tagNameRegex)
	{
		super(name, description);
		this.repository = repository;
		this.defaultValue = defaultValue;
		this.branchFilterText = branchFilterText;
		this.tagFilterText = tagFilterText;
		this.branchNameRegex = branchNameRegex;
		this.tagNameRegex = tagNameRegex;
	}

	public String getRepository()
	{
		return repository;
	}

	public String getDefaultValue()
	{
		return defaultValue;
	}

	public String getBranchFilterText()
	{
		return branchFilterText;
	}

	public String getTagFilterText() {
		return tagFilterText;
	}

	public String getBranchNameRegex() {
		return branchNameRegex;
	}

	public String getTagNameRegex() {
		return tagNameRegex;
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
		String[] repositoryParts = repository.split("/");
		String project = repositoryParts[0];
		String repo = repositoryParts[1];
		StashConnector connector = new StashConnector(getDescriptor().getStashApiUrl(), getDescriptor().getUsername(), getDescriptor().getPassword());

		Map<String, String> map = connector.getBranches(project, repo, branchNameRegex, branchFilterText);
		if (StringUtils.isNotBlank(defaultValue))
		{
			map.put(defaultValue, defaultValue);
		}
		map.putAll(connector.getTags(project, repo, tagNameRegex, tagFilterText));

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
