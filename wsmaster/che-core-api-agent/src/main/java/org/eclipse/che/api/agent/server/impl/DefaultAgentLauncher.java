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
import org.eclipse.che.api.core.util.AbstractLineConsumer;
import org.eclipse.che.api.core.util.CompositeLineConsumer;
import org.eclipse.che.api.core.util.ErrorConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.model.impl.CommandImpl;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static java.lang.String.format;

/**
 *
 * @author Anatolii Bazko
 */
public class DefaultAgentLauncher implements AgentLauncher {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAgentLauncher.class);

    public static final AgentLauncher INSTANCE = new DefaultAgentLauncher();

    @Override
    public String getName() {
        return "default";
    }

    @Override
    public void launch(Instance machine, Agent agent) throws MachineException {
        Command command = new CommandImpl(agent.getName(), agent.getScript(), "agent");

        final ErrorConsumer errorConsumer = new ErrorConsumer(new AbstractLineConsumer() {
            @Override
            public void writeLine(String line) throws IOException {
                machine.getLogger().writeLine(format("Agent %s error: %s", command.getName(), line));
            }
        });

        try {
            final InstanceProcess process = machine.createProcess(command, command.getName());
            process.start(new CompositeLineConsumer(errorConsumer, new AbstractLineConsumer() {
                @Override
                public void writeLine(String line) throws IOException {
                    LOG.debug(line);
                }
            }));
        } catch (ConflictException e) {
            throw new MachineException(e.getServiceError());
        }
    }
}
