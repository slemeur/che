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
package org.eclipse.che.api.agent.server.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.agent.server.AgentLauncher;
import org.eclipse.che.api.agent.shared.model.Agent;

import java.util.Set;

/**
 * @author Anatolii Bazko
 */
@Singleton
public class AgentLauncherFactory {

    private final Set<AgentLauncher> launchers;

    @Inject
    public AgentLauncherFactory(Set<AgentLauncher> launchers) {this.launchers = launchers;}

    /**
     * Find launcher for given agent independently of version.
     * If {@link AgentLauncher} isn't registered then the default one will be used.
     *
     * @see Agent#getName()
     * @see DefaultAgentLauncher
     *
     * @param agentName
     *      the agent name
     * @return {@link AgentLauncher}
     */
    public AgentLauncher find(String agentName) {
        for (AgentLauncher launcher : launchers) {
            if (launcher.getName().equals(agentName)) {
                return launcher;
            }
        }

        return DefaultAgentLauncher.INSTANCE;
    }
}
