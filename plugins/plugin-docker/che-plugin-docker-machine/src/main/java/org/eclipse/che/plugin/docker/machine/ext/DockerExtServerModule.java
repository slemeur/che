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
package org.eclipse.che.plugin.docker.machine.ext;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Names;

/**
 * Guice module for extension servers feature in docker machines
 *
 * @author Alexander Garagatyi
 * @author Sergii Leschenko
 * @author Roman Iuvshyn
 */
public class DockerExtServerModule extends AbstractModule {
    @Override
    protected void configure() {
        MapBinder<String, String> machineEnv = MapBinder.newMapBinder(binder(),
                                                                      String.class,
                                                                      String.class, Names.named("machine.docker.machine_env"))
                                                        .permitDuplicates();

        machineEnv.addBinding("machine.docker.che_api.endpoint")
                  .toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.ApiEndpointEnvVariableProvider.class);
        machineEnv.addBinding("che.machine.projects.internal.storage")
                  .toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.ProjectsRootEnvVariableProvider.class);
        machineEnv.addBinding("che.machine.java_opts")
                  .toProvider(org.eclipse.che.plugin.docker.machine.ext.provider.JavaOptsEnvVariableProvider.class);

    }
}
