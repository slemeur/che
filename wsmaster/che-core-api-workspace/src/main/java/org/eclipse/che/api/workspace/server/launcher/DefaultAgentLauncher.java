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

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.environment.server.MachineProcessManager;

import javax.inject.Named;
import javax.inject.Provider;

/**
 * Launches agent and waits while it is finished.
 *
 * This agents is suited only for those types of agents that install software
 * and finish working without launching additional processes at the end.
 *
 * @author Anatolii Bazko
 */
@Singleton
public class DefaultAgentLauncher extends AbstractAgentLauncher {
    @Inject
    public DefaultAgentLauncher(@Named("machine.agent.max_start_time_ms") long agentMaxStartTimeMs,
                                @Named("machine.agent.ping_delay_ms") long agentPingDelayMs,
                                Provider<MachineProcessManager> machineProcessManagerProvider) {
        super(agentMaxStartTimeMs,
              agentPingDelayMs,
              machineProcessManagerProvider,
              ((agent, process, machine) -> !process.isAlive()));
    }

    @Override
    public String getName() {
        return getClass().getName();
    }
}
