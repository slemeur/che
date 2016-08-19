/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.plugin.docker.machine;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.agent.server.exception.AgentException;
import org.eclipse.che.api.agent.server.impl.AgentsSorter;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.eclipse.che.plugin.docker.machine.DockerAgentsApplier.PROPERTIES.ENVIRONMENT;
import static org.eclipse.che.plugin.docker.machine.DockerAgentsApplier.PROPERTIES.PORTS;

/**
 * Applies agents over docker machine.
 *
 * @author Anatolii Bazko
 */
@Singleton
public class DockerAgentsApplier {
    private static final Logger  LOG                 = LoggerFactory.getLogger(DockerAgentsApplier.class);
    private static final Pattern CONF_VALUE_TEMPLATE = Pattern.compile("\\$\\{(.*)\\}");

    private final AgentsSorter         sorter;
    private final Map<String, String>  conf;

    @Inject
    public DockerAgentsApplier(@Named("machine.docker.machine_env") Map<String, String> conf,
                               AgentsSorter sorter) {
        this.conf = conf;
        this.sorter = sorter;
    }

    /**
     * Applies agents {@link MachineConfig#getAgents()} before starting machine respecting dependencies between agents.
     * It is means applying machine specific properties over machine configuration.
     *
     * @see Agent#getProperties()
     */
    public void applyOn(ContainerConfig containerConfig, List<String> agentKeys) throws MachineException {
        try {
            for (Agent agent : sorter.sort(agentKeys)) {
                addEnv(containerConfig, agent.getProperties());
                addExposedPorts(containerConfig, agent.getProperties());
            }
        } catch (AgentException e) {
            throw new MachineException(e.getMessage(), e);
        }
    }

    private void addEnv(ContainerConfig containerConfig, Map<String, String> properties) {
        String environment = properties.get(ENVIRONMENT.toString());
        if (isNullOrEmpty(environment)) {
            return;
        }

        List<String> newEnv = new LinkedList<>();
        if (containerConfig.getEnv() != null) {
            newEnv.addAll(asList(containerConfig.getEnv()));
        }

        for (String env : environment.split(",")) {
            String[] items = env.split("=");
            if (items.length != 2) {
                LOG.warn(format("Illegal environment variable '%s' format", env));
                continue;
            }

            String envName = items[0];
            String envValue = items[1];

            Matcher matcher = CONF_VALUE_TEMPLATE.matcher(envValue);
            if (matcher.find()) {
                String confValueName = matcher.group(1);

                String newEnvValue = conf.get(confValueName);
                if (!Strings.isNullOrEmpty(newEnvValue)) {
                    newEnv.add(envName + "=" + newEnvValue);
                } else {
                    LOG.warn(format("Environment variable value '%s' not found in the configuration", confValueName));
                }
            } else {
                newEnv.add(env);
            }
        }

        containerConfig.setEnv(newEnv.toArray(new String[newEnv.size()]));
    }

    private void addExposedPorts(ContainerConfig containerConfig, Map<String, String> properties) {
        String ports = properties.get(PORTS.toString());
        if (isNullOrEmpty(ports)) {
            return;
        }

        for (String port : ports.split(",")) {
            String[] items = port.split(":"); // ref:port
            if (items.length == 1) {
                containerConfig.getExposedPorts().put(items[0], Collections.emptyMap());
            } else {
                containerConfig.getExposedPorts().put(items[1], Collections.emptyMap());
            }
        }
    }


    enum PROPERTIES {
        PORTS("ports"),
        ENVIRONMENT("environment");

        private final String value;

        PROPERTIES(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}

