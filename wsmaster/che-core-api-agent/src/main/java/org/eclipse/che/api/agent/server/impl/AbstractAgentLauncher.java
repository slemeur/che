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

import org.eclipse.che.api.agent.server.AgentLauncher;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;

import static java.lang.String.format;

/**
 * @author Anatolii Bazko
 */
public abstract class AbstractAgentLauncher implements AgentLauncher {

    protected void waitForProcessIsRun(Instance machine, Agent agent, String processName) throws MachineException {
        ListLineConsumer lineConsumer = new ListLineConsumer();
        Command command = new CommandImpl("Wait for " + agent.getName(), format("pidof %s", processName), "test");

        for (; ; ) {
            InstanceProcess process = machine.createProcess(command, null);
            try {
                process.start(lineConsumer);
            } catch (ConflictException ignored) {
                // never should happen
            }

            if (!lineConsumer.getText().isEmpty()) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new MachineException(format("Waiting for starting '%s' agent is interrupted", agent.getName()));
            }
        }
    }
}
