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

import org.eclipse.che.api.agent.server.AgentRegistry;
import org.eclipse.che.api.agent.server.model.impl.AgentKeyImpl;
import org.eclipse.che.api.agent.shared.model.Agent;
import org.eclipse.che.api.core.model.machine.Command;
import org.eclipse.che.api.core.model.machine.MachineConfig;
import org.eclipse.che.api.core.util.LineConsumer;
import org.eclipse.che.api.machine.server.exception.MachineException;
import org.eclipse.che.api.machine.server.spi.Instance;
import org.eclipse.che.api.machine.server.spi.InstanceProcess;
import org.eclipse.che.plugin.docker.client.json.ContainerConfig;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Anatolii Bazko
 */
@Listeners(value = {MockitoTestNGListener.class})
public class DockerAgentsApplierTest {

    @Mock
    private MachineConfig       machineConfig;
    @Mock
    private Instance            machine;
    @Mock
    private AgentRegistry       agentRegistry;
    @Mock
    private Agent               agent1;
    @Mock
    private Agent               agent2;
    @Mock
    private Agent               agent3;
    @Mock
    private InstanceProcess     instanceProcess;
    @Mock
    private Map<String, String> conf;

    private DockerAgentsApplier dockerAgentsApplier;

    @BeforeMethod
    public void setUp() throws Exception {
        dockerAgentsApplier = new DockerAgentsApplier(conf, agentRegistry);

        when(machine.getConfig()).thenReturn(machineConfig);
        when(machine.createProcess(any(), any())).thenReturn(instanceProcess);

        when(machineConfig.getAgents()).thenReturn(asList("fqn1:1.0.0", "fqn2"));

        when(agentRegistry.createAgent(eq(AgentKeyImpl.parse("fqn1:1.0.0")))).thenReturn(agent1);
        when(agentRegistry.createAgent(eq(AgentKeyImpl.parse("fqn2")))).thenReturn(agent2);
        when(agentRegistry.createAgent(eq(AgentKeyImpl.parse("fqn3")))).thenReturn(agent3);

        when(agent1.getScript()).thenReturn("script1");
        when(agent1.getDependencies()).thenReturn(singletonList("fqn3"));

        when(agent2.getScript()).thenReturn("script2");
        when(agent2.getDependencies()).thenReturn(singletonList("fqn3"));

        when(agent3.getScript()).thenReturn("script3");

    }

    @Test
    public void shouldAddExposedPorts() throws Exception {
        when(agent1.getProperties()).thenReturn(singletonMap("ports", "terminal:1111/udp,terminal:2222/tcp"));
        when(agent2.getProperties()).thenReturn(singletonMap("ports", "3333/udp"));
        ContainerConfig containerConfig = new ContainerConfig();

        dockerAgentsApplier.applyOn(containerConfig, machineConfig.getAgents());

        Map<String, Map<String, String>> exposedPorts = containerConfig.getExposedPorts();
        assertTrue(exposedPorts.containsKey("1111/udp"));
        assertTrue(exposedPorts.containsKey("2222/tcp"));
        assertTrue(exposedPorts.containsKey("3333/udp"));
    }

    @Test
    public void shouldAddEnvVariables() throws Exception {
        when(agent1.getProperties()).thenReturn(singletonMap("environment", "p1=v1,p2=v2"));
        when(agent2.getProperties()).thenReturn(singletonMap("environment", "p3=v3"));
        ContainerConfig containerConfig = new ContainerConfig();

        dockerAgentsApplier.applyOn(containerConfig, machineConfig.getAgents());

        String[] env = containerConfig.getEnv();
        assertEquals(env.length, 3);
        assertEquals(env[0], "p1=v1");
        assertEquals(env[1], "p2=v2");
        assertEquals(env[2], "p3=v3");
    }

    @Test
    public void shouldAddEnvironmentVariableValueFromConfiguration() throws Exception {
        when(agent1.getProperties()).thenReturn(singletonMap("environment", "p1=${v_template}"));
        when(conf.get("v_template")).thenReturn("v1");

        ContainerConfig containerConfig = new ContainerConfig();

        dockerAgentsApplier.applyOn(containerConfig, machineConfig.getAgents());

        String[] env = containerConfig.getEnv();
        assertEquals(env.length, 1);
        assertEquals(env[0], "p1=v1");
    }

    @Test
    public void shouldIgnoreEnvironmentVariableIfValueNotFoundInConfiguration() throws Exception {
        when(agent1.getProperties()).thenReturn(singletonMap("environment", "p1=${v_template}"));

        ContainerConfig containerConfig = new ContainerConfig();

        dockerAgentsApplier.applyOn(containerConfig, machineConfig.getAgents());

        String[] env = containerConfig.getEnv();
        assertEquals(env.length, 0);
    }

    @Test
    public void shouldIgnoreEnvironmentIfIllegalFormat() throws Exception {
        when(agent1.getProperties()).thenReturn(singletonMap("environment", "p1"));

        ContainerConfig containerConfig = new ContainerConfig();

        dockerAgentsApplier.applyOn(containerConfig, machineConfig.getAgents());

        String[] env = containerConfig.getEnv();
        assertEquals(env.length, 0);
    }

    //    @Test
    public void shouldRespectDependencies() throws Exception {
        ArgumentCaptor<Command> command = ArgumentCaptor.forClass(Command.class);

        dockerAgentsApplier.applyOn(machine, machineConfig.getAgents());

        verify(machine, times(3)).createProcess(command.capture(), any());

        List<Command> commands = command.getAllValues();
        assertEquals(commands.size(), 3);
        assertEquals(commands.get(0).getCommandLine(), "script3");
        assertEquals(commands.get(1).getCommandLine(), "script1");
        assertEquals(commands.get(2).getCommandLine(), "script2");
    }

    @Test(expectedExceptions = MachineException.class)
    public void shouldFailIfCircularDependenciesFound() throws Exception {
        when(agent1.getDependencies()).thenReturn(singletonList("fqn2"));
        when(agent2.getDependencies()).thenReturn(singletonList("fqn1"));

        dockerAgentsApplier.applyOn(machine, machineConfig.getAgents());
    }

//    @Test(expectedExceptions = MachineException.class, expectedExceptionsMessageRegExp = ".*Agent error.*")
    public void shouldFailIfAgentFailed() throws Exception {
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                LineConsumer lineConsumer = (LineConsumer)invocation.getArguments()[0];
                lineConsumer.writeLine("[STDERR] Agent error");
                return null;
            }
        }).when(instanceProcess).start(any(LineConsumer.class));

        dockerAgentsApplier.applyOn(machine, machineConfig.getAgents());
    }
}
