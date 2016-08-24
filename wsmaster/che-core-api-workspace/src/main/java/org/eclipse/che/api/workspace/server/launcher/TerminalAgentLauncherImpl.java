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

import org.eclipse.che.api.environment.server.MachineProcessManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

/**
 * Starts terminal agent.
 *
 * @author Anatolii Bazko
 */
@Singleton
public class TerminalAgentLauncherImpl extends AbstractAgentLauncher {
    protected static final Logger LOG = LoggerFactory.getLogger(TerminalAgentLauncherImpl.class);

    @Inject
    public TerminalAgentLauncherImpl(@Named("machine.agent.max_start_time_ms") long agentMaxStartTimeMs,
                                     @Named("machine.agent.ping_delay_ms") long agentPingDelayMs,
                                     Provider<MachineProcessManager> machineProcessManagerProvider) {
        super(agentMaxStartTimeMs,
              agentPingDelayMs,
              machineProcessManagerProvider,
              new WaitProcessToLaunchChecker("che-websocket-terminal"));
    }

    @Override
    public String getName() {
        return "org.eclipse.che.terminal";
    }
}
