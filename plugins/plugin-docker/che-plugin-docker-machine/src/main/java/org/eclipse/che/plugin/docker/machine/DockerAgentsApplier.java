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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.agent.server.AgentRegistry;
import org.eclipse.che.api.agent.server.exception.AgentException;
import org.eclipse.che.api.agent.server.model.impl.AgentKeyImpl;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.agent.shared.model.AgentKey;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineConfig;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

/**
 * @author Anatolii Bazko
 */
@Singleton
public class DockerAgentsApplier {
    private static final Logger LOG = LoggerFactory.getLogger(DockerAgentsApplier.class);

    private final AgentRegistry agentRegistry;

    @Inject
    public DockerAgentsApplier(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    public void applyOn(ContainerConfig containerConfig, MachineConfig machineConfig) throws MachineException {
        List<Agent> agents = sortAgents(machineConfig);
        for (Agent agent : agents) {
            addExposedPorts(containerConfig, agent);
        }
    }

    private void addExposedPorts(ContainerConfig containerConfig, Agent agent) {
        Map<String, String> properties = agent.getProperties();

        String ports = properties.get(PROPERTIES.PORTS.toString());
        if (!isNullOrEmpty(ports)) {
            for (String port : ports.split(",")) {
                String[] items = port.split(":"); // ref:port
                if (items.length == 1) {
                    containerConfig.getExposedPorts().put(items[0], Collections.emptyMap());
                } else {
                    containerConfig.getExposedPorts().put(items[1], Collections.emptyMap());
                }
            }
        }
    }

    /**
     * Applies agents {@link MachineConfig#getAgents()} over machine instance.
     * Respects dependencies between agents.
     */
    public void applyOn(Instance machine, MachineConfig machineConfig) throws MachineException {
        List<Agent> agents = sortAgents(machineConfig);
        for (Agent agent : agents) {
            LOG.info("Starting {} agent", agent.getName());
            startAgent(machine, new CommandImpl(agent.getName(), agent.getScript(), "agent"));
        }
    }

    protected List<Agent> sortAgents(MachineConfig machineConfig) throws MachineException {
        Map<String, Agent> sorted = new HashMap<>();
        Set<String> pending = new HashSet<>();

        for (String agentKey : machineConfig.getAgents()) {
            doSortAgents(AgentKeyImpl.parse(agentKey), sorted, pending);
        }
//        doSortAgents(AgentKeyImpl.parse("org.eclipse.che.terminal"), sorted, pending);

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
                            LOG.debug(line);
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
        thread.setDaemon(true);
        thread.start();
    }

    enum PROPERTIES {
        PORTS("ports");

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

