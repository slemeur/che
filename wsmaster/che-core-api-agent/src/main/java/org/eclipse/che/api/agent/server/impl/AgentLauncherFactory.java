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

import javax.inject.Named;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Anatolii Bazko
 */
@Singleton
public class AgentLauncherFactory {

    private final Map<String, AgentLauncher> launchers;
    private final AgentLauncher              defaultLauncher;

    @Inject
    public AgentLauncherFactory(Set<AgentLauncher> launchers, @Named("machine.agent.launcher.default") String defaultLauncherName) {
        this.launchers = launchers.stream().collect(Collectors.toMap(AgentLauncher::getName, l -> l));
        this.defaultLauncher = this.launchers.get(defaultLauncherName);
    }

    /**
     * Find launcher for given agent independently of version.
     * If the specific {@link AgentLauncher} isn't registered then the default one will be used.
     *
     * @see Agent#getName()
     *
     *  @param agentName
     *      the agent name
     * @return {@link AgentLauncher}
     */
    public AgentLauncher find(String agentName) {
        return launchers.getOrDefault(agentName, defaultLauncher);
    }
}
