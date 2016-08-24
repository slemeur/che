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
package org.eclipse.che.api.workspace.server.launcher;

import org.eclipse.che.api.agent.server.AgentLauncher;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.environment.server.MachineProcessManager;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;

import static java.lang.String.format;

/**
 * @author Anatolii Bazko
 */
public abstract class AbstractAgentLauncher implements AgentLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractAgentLauncher.class);

    private final Provider<MachineProcessManager> machineProcessManagerProvider;
    private final AgentLaunchingChecker           agentLaunchingChecker;
    private final long                            agentPingDelayMs;
    private final long                            agentMaxStartTimeMs;

    public AbstractAgentLauncher(long agentMaxStartTimeMs,
                                 long agentPingDelayMs,
                                 Provider<MachineProcessManager> machineProcessManagerProvider,
                                 AgentLaunchingChecker agentLaunchingChecker) {
        this.machineProcessManagerProvider = machineProcessManagerProvider;
        this.agentPingDelayMs = agentPingDelayMs;
        this.agentMaxStartTimeMs = agentMaxStartTimeMs;
        this.agentLaunchingChecker = agentLaunchingChecker;
    }

    @Override
    public void launch(Instance machine, Agent agent) throws MachineException {
        try {
            Command command = new CommandImpl(agent.getName(), agent.getScript(), "agent");
            final InstanceProcess process = machineProcessManagerProvider.get().exec(machine.getWorkspaceId(),
                                                                                     machine.getId(),
                                                                                     command,
                                                                                     null);

            LOG.debug("Starts waiting for agent {} is launched. Workspace ID:{}", agent.getName(), machine.getWorkspaceId());

            final long pingStartTimestamp = System.currentTimeMillis();
            while (System.currentTimeMillis() - pingStartTimestamp < agentMaxStartTimeMs) {
                if (agentLaunchingChecker.isLaunched(agent, process, machine)) {
                    return;
                } else {
                    Thread.sleep(agentPingDelayMs);
                }
            }

            process.kill();
        } catch (NotFoundException | BadRequestException e) {
            throw new MachineException(e.getServiceError());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MachineException(format("Launching agent %s is interrupted", agent.getName()));
        }

        final String errMsg = format("Fail launching agent %s. Workspace ID:%s", agent.getName(), machine.getWorkspaceId());
        LOG.error(errMsg);
        throw new MachineException(errMsg);
    }

}
