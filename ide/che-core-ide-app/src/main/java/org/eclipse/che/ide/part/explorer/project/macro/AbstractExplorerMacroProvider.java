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
package org.eclipse.che.ide.part.explorer.project.macro;

import com.google.common.annotations.Beta;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.data.tree.Node;
import org.eclipse.che.ide.api.machine.CommandPropertyValueProvider;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.part.explorer.project.ProjectExplorerPresenter;
import org.eclipse.che.ide.resources.tree.ResourceNode;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Base macro provider which belongs to the project explorer. Provides easy access to the resources
 * to allow fetch necessary information to use in custom commands, preview urls, etc.
 *
 * @author Vlad Zhukovskyi
 * @see ProjectExplorerPresenter
 * @see CommandPropertyValueProvider
 * @see ExplorerCurrentFileNameProvider
 * @see ExplorerCurrentFilePathProvider
 * @see ExplorerCurrentFileRelativePathProvider
 * @see ExplorerCurrentProjectNameProvider
 * @see ExplorerCurrentProjectTypeProvider
 * @since 4.7.0
 */
@Beta
public abstract class AbstractExplorerMacroProvider implements CommandPropertyValueProvider {

    private ProjectExplorerPresenter projectExplorer;
    private AppContext               appContext;

    Predicate<Node> resNodePredicate = new Predicate<Node>() {
        @Override
        public boolean apply(@Nullable Node input) {
            checkNotNull(input);

            return input instanceof ResourceNode;
        }
    };

    Function<Node, Resource> nodeToResourceFun = new Function<Node, Resource>() {
        @Nullable
        @Override
        public Resource apply(@Nullable Node input) {
            checkNotNull(input);
            checkState(input instanceof ResourceNode);

            return ((ResourceNode)input).getData();
        }
    };

    Function<Resource, String> resourceToNameFun = new Function<Resource, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Resource input) {
            checkNotNull(input);

            return input.getName();
        }
    };

    Function<Resource, String> resourceToAbsolutePathFun = new Function<Resource, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Resource input) {
            checkNotNull(input);

            return appContext.getProjectsRoot().append(input.getLocation()).toString();
        }
    };

    Function<Resource, String> resourceToPathFun = new Function<Resource, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Resource input) {
            checkNotNull(input);

            return input.getLocation().toString();
        }
    };

    public AbstractExplorerMacroProvider(ProjectExplorerPresenter projectExplorer,
                                         AppContext appContext) {
        this.projectExplorer = projectExplorer;
        this.appContext = appContext;
    }

    /**
     * Returns the instance of project explorer.
     *
     * @return project explore instance
     * @see ProjectExplorerPresenter
     * @since 4.7.0
     */
    public ProjectExplorerPresenter getProjectExplorer() {
        return projectExplorer;
    }

    /**
     * Returns the instance of application context.
     *
     * @return application context
     * @see AppContext
     * @since 4.7.0
     */
    public AppContext getAppContext() {
        return appContext;
    }
}
