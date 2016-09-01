package org.jenkinsci.plugins.StashBranchParameter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.StringParameterValue;
import net.sf.json.JSONObject;

public class StashBranchParameterDefinition extends ParameterDefinition {
	private String repository;
	private String defaultValue;
	private List<String> branchFilters;

	@DataBoundConstructor
	public StashBranchParameterDefinition(final String name, final String description, final String repository,
			final String defaultValue, final String branchFilters) {
		super(name, description);
		this.repository = repository;
		this.defaultValue = defaultValue;
		this.branchFilters = new ArrayList<String>();
		// Strip whitespace
		for (final String filter : branchFilters.split(",")) {
			this.branchFilters.add(filter.trim());
		}
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(final String repository) {
		this.repository = repository;
	}

	public String getBranchFilters() {
		return branchFilters.isEmpty() ? "" : String.join(",", branchFilters);
	}

	public void setBranchFilters(final List<String> branchFilters) {
		this.branchFilters = new ArrayList<String>(branchFilters);
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(final String defaultValue) {
		this.defaultValue = defaultValue;
	}

	@Override
	public ParameterValue createValue(final StaplerRequest staplerRequest, final JSONObject jsonObject) {
		final String value = jsonObject.getString("value");
		return new StringParameterValue(this.getName(), value);
	}

	@Override
	public ParameterValue createValue(final StaplerRequest staplerRequest) {
		final String[] parameterValues = staplerRequest.getParameterValues(getName());
		final String value = parameterValues[0];
		return new StringParameterValue(this.getName(), value);
	}

	@Override
	public ParameterValue getDefaultParameterValue() {
		return new StringParameterValue(this.getName(), defaultValue);
	}

	public Map<String, Map<String, String>> getDefaultValueMap() throws IOException {
		return computeDefaultValueMap();
	}

	private Map<String, Map<String, String>> computeDefaultValueMap() throws IOException {
		final String project = repository.split("/")[0];
		final String repo = repository.split("/")[1];
		final StashConnector connector = new StashConnector(getDescriptor().getStashApiUrl(), getDescriptor()
				.getUsername(), getDescriptor().getPassword());

		final Map<String, String> map;
		if (branchFilters.isEmpty()) {
			map = connector.getBranches(project, repo);
		} else {
			map = connector.getFilteredBranches(project, repo, branchFilters);
		}
		if (StringUtils.isNotBlank(defaultValue)) {
			map.put(defaultValue, defaultValue);
		}
		map.putAll(connector.getTags(project, repo));

		return MapsUtils.groupMap(map);
	}

	@Override
	public StashBranchParameterDescriptorImpl getDescriptor() {
		return (StashBranchParameterDescriptorImpl) super.getDescriptor();
	}

	@Extension
	public static class StashBranchParameterDescriptorImpl extends StashBranchParameterDescriptor {

	}

}
