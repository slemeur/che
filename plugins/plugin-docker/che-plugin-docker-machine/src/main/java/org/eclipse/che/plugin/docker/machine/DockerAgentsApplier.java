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

import org.eclipse.che.api.agent.server.AgentRegistry;
import org.eclipse.che.api.agent.server.exception.AgentException;
import org.eclipse.che.api.agent.server.model.impl.AgentKeyImpl;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.agent.shared.model.AgentKey;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.util.AbstractLineConsumer;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.ErrorConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Named;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.eclipse.che.plugin.docker.machine.DockerAgentsApplier.PROPERTIES.ENVIRONMENT;
import static org.eclipse.che.plugin.docker.machine.DockerAgentsApplier.PROPERTIES.PORTS;

/**
 * @author Anatolii Bazko
 */
@Singleton
public class DockerAgentsApplier implements AgentsApplier {
    private static final Logger  LOG                 = LoggerFactory.getLogger(DockerAgentsApplier.class);
    private static final Pattern CONF_VALUE_TEMPLATE = Pattern.compile("\\$\\{(.*)\\}");

    private final AgentRegistry       agentRegistry;
    private final Map<String, String> conf;

    @Inject
    public DockerAgentsApplier(@Named("machine.docker.machine_env") Map<String, String> conf,
                               AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
        this.conf = conf;
    }

    @Override
    public void applyOn(ContainerConfig containerConfig, List<String> agentKeys) throws MachineException {
        List<Agent> agents = sortAgents(agentKeys);
        for (Agent agent : agents) {
            addEnv(containerConfig, agent.getProperties());
            addExposedPorts(containerConfig, agent.getProperties());
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

    @Override
    public void applyOn(Instance machine, List<String> agentKeys) throws MachineException {
        List<Agent> agents = sortAgents(agentKeys);
        for (Agent agent : agents) {
            LOG.info("Starting {} agent", agent.getName());
            startAgent(machine, new CommandImpl(agent.getName(), agent.getScript(), "agent"));
        }
    }

    protected List<Agent> sortAgents(List<String> agentKeys) throws MachineException {
        Map<String, Agent> sorted = new HashMap<>();
        Set<String> pending = new HashSet<>();

        for (String agentKey : agentKeys) {
            doSortAgents(AgentKeyImpl.parse(agentKey), sorted, pending);
        }
        doSortAgents(AgentKeyImpl.parse("org.eclipse.che.ws-agent"), sorted, pending);

        return new ArrayList<>(sorted.values());
    }

    private void doSortAgents(AgentKey agentKey, Map<String, Agent> sorted, Set<String> pending) throws MachineException {
        String agentName = agentKey.getName();

        if (sorted.containsKey(agentName)) {
            return;
        }
        if (!pending.add(agentName)) {
            throw new MachineException("Agents circular dependency found.");
        }

        Agent agent;
        try {
            agent = agentRegistry.createAgent(agentKey);
        } catch (AgentException e) {
            throw new MachineException("Agent can't be created " + agentKey, e);
        }

        for (String dependency : agent.getDependencies()) {
            doSortAgents(AgentKeyImpl.parse(dependency), sorted, pending);
        }

        sorted.put(agentName, agent);
        pending.remove(agentName);
    }

    private void startAgent(Instance machine, Command command) throws MachineException {
        final ErrorConsumer errorConsumer = new ErrorConsumer(new AbstractLineConsumer() {
            @Override
            public void writeLine(String line) throws IOException {
                machine.getLogger().writeLine(format("Agent %s error: %s", command.getName(), line));
            }
        });

        Thread thread = new Thread("Agent " + command.getName()) {
            @Override
            public void run() {
                try {
                    final InstanceProcess process = machine.createProcess(command, command.getName());
                    process.start(new CompositeLineConsumer(errorConsumer, new AbstractLineConsumer() {
                        @Override
                        public void writeLine(String line) throws IOException {
                            LOG.info(line);
                        }
                    }));
                } catch (ConflictException | MachineException e) {
                    try {
                        machine.getLogger().writeLine(format("Agent %s error: %s", command.getName(), e.getMessage()));
                    } catch (IOException e1) {
                        // ignore
                    }
                }
            }
        };
        thread.run();
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

