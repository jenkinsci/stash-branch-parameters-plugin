package org.jenkinsci.plugins.StashBranchParameter;

import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Created by erwin on 13/03/14.
 */
public class StashBranchParameterDefinition extends ParameterDefinition {

    public StashBranchParameterDefinition(String name) {
        super(name);
    }

    public StashBranchParameterDefinition(String name, String description) {
        super(name, description);
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest, JSONObject jsonObject) {
        return null;
    }

    @Override
    public ParameterValue createValue(StaplerRequest staplerRequest) {
        return null;
    }
}
