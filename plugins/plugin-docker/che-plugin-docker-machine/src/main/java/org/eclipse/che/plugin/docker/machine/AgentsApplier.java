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

import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;

import java.util.List;

/**
 * Applies agents over machines.
 *
 * @author Anatolii Bazko
 */
public interface AgentsApplier {

    /**
     * Applies agents {@link MachineConfig#getAgents()} before starting machine respecting dependencies between agents.
     * It is means applying machine specific properties over machine configuration.
     *
     * @see Agent#getProperties()
     */
    void applyOn(ContainerConfig containerConfig, List<String> agentKeys) throws MachineException;

    /**
     * Applies agents {@link MachineConfig#getAgents()} over running machine respecting dependencies between agents.
     * It is basically means running scripts.
     *
     * @see Agent#getProperties()
     */
    void applyOn(Instance machine, List<String> agentKeys) throws MachineException;
}
