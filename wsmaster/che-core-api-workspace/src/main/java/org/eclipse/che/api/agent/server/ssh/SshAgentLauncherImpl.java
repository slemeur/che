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
package org.eclipse.che.api.agent.server.ssh;

import org.eclipse.che.api.agent.server.impl.AbstractAgentLauncher;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.environment.server.MachineProcessManager;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Starts terminal agent.
 *
 * @author Anatolii Bazko
 */
@Singleton
public class SshAgentLauncherImpl extends AbstractAgentLauncher {
    protected static final Logger LOG = LoggerFactory.getLogger(SshAgentLauncherImpl.class);

    private final Provider<MachineProcessManager> machineProcessManagerProvider;

    @Inject
    public SshAgentLauncherImpl(Provider<MachineProcessManager> machineProcessManagerProvider) {
        this.machineProcessManagerProvider = machineProcessManagerProvider;
    }

    @Override
    public String getName() {
        return "org.eclipse.che.ssh";
    }

    @Override
    public void launch(Instance machine, Agent agent) throws MachineException {
        try {
            machineProcessManagerProvider.get().exec(machine.getWorkspaceId(),
                                                     machine.getId(),
                                                     new CommandImpl(agent.getName(), agent.getScript(), "agent"),
                                                     null);
        } catch (BadRequestException | ServerException | NotFoundException e) {
            throw new MachineException(e.getLocalizedMessage(), e);
        }

        waitForProcessIsRun(machine, agent, "sshd");
    }
}
